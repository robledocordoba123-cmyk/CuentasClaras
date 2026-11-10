package co.cuentasclaras.deuda;

import co.cuentasclaras.comun.Meses;
import co.cuentasclaras.comun.Pesos;
import co.cuentasclaras.comun.error.RecursoEnConflictoException;
import co.cuentasclaras.comun.error.RecursoNoEncontradoException;
import co.cuentasclaras.comun.error.ReglaDeNegocioException;
import co.cuentasclaras.deuda.DeudaDtos.AbonoRequest;
import co.cuentasclaras.deuda.DeudaDtos.AbonoResponse;
import co.cuentasclaras.deuda.DeudaDtos.DeudaRequest;
import co.cuentasclaras.deuda.DeudaDtos.DeudaResponse;
import co.cuentasclaras.deuda.DeudaDtos.Direccion;
import co.cuentasclaras.deuda.DeudaDtos.PersonaRequest;
import co.cuentasclaras.deuda.DeudaDtos.PersonaResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Cuentas claras, amistades largas": lo que te deben y lo que debes, con abonos.
 *
 * <p>Se escribe en SQL directo (JdbcTemplate) porque casi todo son sumas por
 * persona y por deuda, que en SQL se leen mejor que en entidades. Como en el
 * resto de la API, cada consulta filtra por la dueña (RN-01) y lo ajeno es 404.
 *
 * <p>Las deudas no cambian el saldo de las cuentas: prestar no es gastar, y
 * mezclarlas con los gastos dañaría los presupuestos.
 */
@Service
public class DeudaService {

	private static final String CONSULTA_DEUDAS = """
			SELECT d.id, d.persona_id, p.nombre AS persona, d.direccion, d.monto, d.descripcion, d.fecha,
			       d.saldada_en, COALESCE((SELECT SUM(a.monto) FROM abonos a WHERE a.deuda_id = d.id), 0) AS abonado
			FROM deudas d JOIN personas p ON p.id = d.persona_id
			WHERE d.usuario_id = ?""";

	private final JdbcTemplate jdbc;
	private final Meses meses;

	public DeudaService(JdbcTemplate jdbc, Meses meses) {
		this.jdbc = jdbc;
		this.meses = meses;
	}

	@Transactional(readOnly = true)
	public List<PersonaResponse> listarPersonas(UUID usuarioId) {
		return jdbc.query("""
				SELECT p.id, p.nombre,
				       COALESCE(SUM(CASE WHEN d.direccion = 'ME_DEBEN' THEN d.monto - COALESCE(a.abonado, 0) END), 0) AS te_debe,
				       COALESCE(SUM(CASE WHEN d.direccion = 'DEBO' THEN d.monto - COALESCE(a.abonado, 0) END), 0) AS le_debes,
				       COUNT(d.id) FILTER (WHERE d.saldada_en IS NULL) AS pendientes
				FROM personas p
				LEFT JOIN deudas d ON d.persona_id = p.id
				LEFT JOIN (SELECT deuda_id, SUM(monto) AS abonado FROM abonos GROUP BY deuda_id) a ON a.deuda_id = d.id
				WHERE p.usuario_id = ?
				GROUP BY p.id, p.nombre
				ORDER BY lower(p.nombre)""", (rs, i) -> {
			BigDecimal teDebe = rs.getBigDecimal("te_debe");
			BigDecimal leDebes = rs.getBigDecimal("le_debes");
			return new PersonaResponse(rs.getObject("id", UUID.class), rs.getString("nombre"), teDebe, leDebes,
					teDebe.subtract(leDebes), rs.getInt("pendientes"));
		}, usuarioId);
	}

	@Transactional
	public PersonaResponse crearPersona(UUID usuarioId, PersonaRequest solicitud) {
		UUID id = UUID.randomUUID();
		try {
			jdbc.update("INSERT INTO personas (id, usuario_id, nombre) VALUES (?, ?, ?)", id, usuarioId, solicitud.nombre());
		} catch (DuplicateKeyException e) {
			throw new RecursoEnConflictoException("Ya tienes a " + solicitud.nombre() + " en tu lista.");
		}
		return new PersonaResponse(id, solicitud.nombre(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
	}

	@Transactional
	public void renombrarPersona(UUID usuarioId, UUID personaId, PersonaRequest solicitud) {
		try {
			if (jdbc.update("UPDATE personas SET nombre = ? WHERE id = ? AND usuario_id = ?", solicitud.nombre(), personaId,
					usuarioId) == 0) {
				throw personaNoEncontrada();
			}
		} catch (DuplicateKeyException e) {
			throw new RecursoEnConflictoException("Ya tienes a " + solicitud.nombre() + " en tu lista.");
		}
	}

	/** Solo se borra una persona sin deudas: así nunca se pierde lo que alguien te debe. */
	@Transactional
	public void eliminarPersona(UUID usuarioId, UUID personaId) {
		verificarPersona(usuarioId, personaId);
		Integer deudas = jdbc.queryForObject("SELECT count(*) FROM deudas WHERE persona_id = ?", Integer.class, personaId);
		if (deudas != null && deudas > 0) {
			throw new ReglaDeNegocioException("Esta persona tiene deudas registradas: bórralas primero.");
		}
		jdbc.update("DELETE FROM personas WHERE id = ?", personaId);
	}

	@Transactional(readOnly = true)
	public List<DeudaResponse> listarDeudas(UUID usuarioId, UUID personaId, boolean soloPendientes) {
		StringBuilder sql = new StringBuilder(CONSULTA_DEUDAS);
		List<Object> parametros = new ArrayList<>(List.of(usuarioId));
		if (personaId != null) {
			sql.append(" AND d.persona_id = ?");
			parametros.add(personaId);
		}
		if (soloPendientes) {
			sql.append(" AND d.saldada_en IS NULL");
		}
		sql.append(" ORDER BY d.saldada_en IS NOT NULL, d.fecha DESC, d.creado_en DESC");
		return conAbonos(jdbc.query(sql.toString(), this::aFila, parametros.toArray()));
	}

	@Transactional
	public DeudaResponse crearDeuda(UUID usuarioId, DeudaRequest solicitud) {
		verificarPersona(usuarioId, solicitud.personaId());
		if (solicitud.fecha().isAfter(meses.hoy())) {
			throw new ReglaDeNegocioException("La fecha de la deuda no puede ser futura.");
		}
		UUID id = UUID.randomUUID();
		jdbc.update("""
				INSERT INTO deudas (id, usuario_id, persona_id, direccion, monto, descripcion, fecha)
				VALUES (?, ?, ?, ?, ?, ?, ?)""", id, usuarioId, solicitud.personaId(), solicitud.direccion().name(),
				solicitud.monto(), solicitud.descripcion(), Date.valueOf(solicitud.fecha()));
		return obtener(usuarioId, id);
	}

	@Transactional
	public void eliminarDeuda(UUID usuarioId, UUID deudaId) {
		if (jdbc.update("DELETE FROM deudas WHERE id = ? AND usuario_id = ?", deudaId, usuarioId) == 0) {
			throw deudaNoEncontrada();
		}
	}

	/**
	 * Registra un abono. La deuda queda bloqueada (FOR UPDATE) hasta terminar:
	 * si dos abonos llegan a la vez, el segundo espera y ve lo que dejó el primero,
	 * así la suma de los abonos nunca supera el monto.
	 */
	@Transactional
	public DeudaResponse abonar(UUID usuarioId, UUID deudaId, AbonoRequest solicitud) {
		Fila deuda = bloquear(usuarioId, deudaId);
		if (deuda.saldada()) {
			throw new ReglaDeNegocioException("Esta deuda ya está saldada.");
		}
		if (solicitud.fecha().isBefore(deuda.fecha()) || solicitud.fecha().isAfter(meses.hoy())) {
			throw new ReglaDeNegocioException("La fecha del abono debe estar entre la de la deuda y hoy.");
		}
		BigDecimal pendiente = deuda.monto().subtract(deuda.abonado());
		if (solicitud.monto().compareTo(pendiente) > 0) {
			throw new ReglaDeNegocioException("El abono supera lo pendiente (" + Pesos.formato(pendiente) + ").");
		}
		jdbc.update("INSERT INTO abonos (id, deuda_id, monto, fecha) VALUES (?, ?, ?, ?)", UUID.randomUUID(), deudaId,
				solicitud.monto(), Date.valueOf(solicitud.fecha()));
		if (solicitud.monto().compareTo(pendiente) == 0) {
			jdbc.update("UPDATE deudas SET saldada_en = now() WHERE id = ?", deudaId);
		}
		return obtener(usuarioId, deudaId);
	}

	/** Quitar un abono mal registrado: la deuda vuelve a quedar pendiente. */
	@Transactional
	public DeudaResponse quitarAbono(UUID usuarioId, UUID deudaId, UUID abonoId) {
		bloquear(usuarioId, deudaId);
		if (jdbc.update("DELETE FROM abonos WHERE id = ? AND deuda_id = ?", abonoId, deudaId) == 0) {
			throw new RecursoNoEncontradoException("Abono no encontrado.");
		}
		jdbc.update("UPDATE deudas SET saldada_en = NULL WHERE id = ?", deudaId);
		return obtener(usuarioId, deudaId);
	}

	private DeudaResponse obtener(UUID usuarioId, UUID deudaId) {
		List<DeudaResponse> deudas = conAbonos(jdbc.query(CONSULTA_DEUDAS + " AND d.id = ?", this::aFila, usuarioId, deudaId));
		if (deudas.isEmpty()) {
			throw deudaNoEncontrada();
		}
		return deudas.getFirst();
	}

	private Fila bloquear(UUID usuarioId, UUID deudaId) {
		List<Fila> filas = jdbc.query("SELECT id FROM deudas WHERE id = ? AND usuario_id = ? FOR UPDATE",
				(rs, i) -> null, deudaId, usuarioId);
		if (filas.isEmpty()) {
			throw deudaNoEncontrada();
		}
		return jdbc.query(CONSULTA_DEUDAS + " AND d.id = ?", this::aFila, usuarioId, deudaId).getFirst();
	}

	private void verificarPersona(UUID usuarioId, UUID personaId) {
		Integer existe = jdbc.queryForObject("SELECT count(*) FROM personas WHERE id = ? AND usuario_id = ?",
				Integer.class, personaId, usuarioId);
		if (existe == null || existe == 0) {
			throw personaNoEncontrada();
		}
	}

	private List<DeudaResponse> conAbonos(List<Fila> filas) {
		if (filas.isEmpty()) {
			return List.of();
		}
		Map<UUID, List<AbonoResponse>> abonos = new LinkedHashMap<>();
		filas.forEach(f -> abonos.put(f.id(), new ArrayList<>()));
		String marcadores = String.join(",", filas.stream().map(f -> "?").toList());
		jdbc.query("SELECT id, deuda_id, monto, fecha FROM abonos WHERE deuda_id IN (" + marcadores + ") ORDER BY fecha, creado_en",
				(ResultSet rs) -> {
					abonos.get(rs.getObject("deuda_id", UUID.class)).add(new AbonoResponse(rs.getObject("id", UUID.class),
							rs.getBigDecimal("monto"), rs.getDate("fecha").toLocalDate()));
				}, filas.stream().map(Fila::id).toArray());
		return filas.stream().map(f -> new DeudaResponse(f.id(), f.personaId(), f.persona(), f.direccion(), f.monto(),
				f.abonado(), f.monto().subtract(f.abonado()), f.descripcion(), f.fecha(), f.saldada(), abonos.get(f.id())))
				.toList();
	}

	private Fila aFila(ResultSet rs, int i) throws SQLException {
		return new Fila(rs.getObject("id", UUID.class), rs.getObject("persona_id", UUID.class), rs.getString("persona"),
				Direccion.valueOf(rs.getString("direccion")), rs.getBigDecimal("monto"), rs.getBigDecimal("abonado"),
				rs.getString("descripcion"), rs.getDate("fecha").toLocalDate(), rs.getTimestamp("saldada_en") != null);
	}

	private static RecursoNoEncontradoException personaNoEncontrada() {
		return new RecursoNoEncontradoException("Persona no encontrada.");
	}

	private static RecursoNoEncontradoException deudaNoEncontrada() {
		return new RecursoNoEncontradoException("Deuda no encontrada.");
	}

	private record Fila(UUID id, UUID personaId, String persona, Direccion direccion, BigDecimal monto, BigDecimal abonado,
			String descripcion, java.time.LocalDate fecha, boolean saldada) {
	}

}
