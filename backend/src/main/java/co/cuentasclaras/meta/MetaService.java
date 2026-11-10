package co.cuentasclaras.meta;

import co.cuentasclaras.comun.Meses;
import co.cuentasclaras.comun.Pesos;
import co.cuentasclaras.comun.error.RecursoNoEncontradoException;
import co.cuentasclaras.comun.error.ReglaDeNegocioException;
import co.cuentasclaras.meta.MetaDtos.AporteRequest;
import co.cuentasclaras.meta.MetaDtos.MetaRequest;
import co.cuentasclaras.meta.MetaDtos.MetaResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Metas de ahorro. Lo ahorrado es la suma de aportes y retiros; nunca queda
 * en negativo. Como las deudas, no mueve plata entre cuentas: es un sobre
 * mental dentro de lo que ya tienes.
 */
@Service
public class MetaService {

	private static final String CONSULTA = """
			SELECT m.id, m.nombre, m.monto_objetivo, m.fecha_objetivo,
			       COALESCE((SELECT SUM(a.monto) FROM aportes a WHERE a.meta_id = m.id), 0) AS ahorrado
			FROM metas m WHERE m.usuario_id = ?""";

	private final JdbcTemplate jdbc;
	private final Meses meses;

	public MetaService(JdbcTemplate jdbc, Meses meses) {
		this.jdbc = jdbc;
		this.meses = meses;
	}

	@Transactional(readOnly = true)
	public List<MetaResponse> listar(UUID usuarioId) {
		return jdbc.query(CONSULTA + " ORDER BY m.creado_en", this::aRespuesta, usuarioId);
	}

	@Transactional
	public MetaResponse crear(UUID usuarioId, MetaRequest solicitud) {
		validar(solicitud);
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO metas (id, usuario_id, nombre, monto_objetivo, fecha_objetivo) VALUES (?, ?, ?, ?, ?)",
				id, usuarioId, solicitud.nombre(), solicitud.montoObjetivo(), fecha(solicitud.fechaObjetivo()));
		return obtener(usuarioId, id);
	}

	@Transactional
	public MetaResponse actualizar(UUID usuarioId, UUID metaId, MetaRequest solicitud) {
		validar(solicitud);
		if (jdbc.update("UPDATE metas SET nombre = ?, monto_objetivo = ?, fecha_objetivo = ? WHERE id = ? AND usuario_id = ?",
				solicitud.nombre(), solicitud.montoObjetivo(), fecha(solicitud.fechaObjetivo()), metaId, usuarioId) == 0) {
			throw noEncontrada();
		}
		return obtener(usuarioId, metaId);
	}

	@Transactional
	public void eliminar(UUID usuarioId, UUID metaId) {
		if (jdbc.update("DELETE FROM metas WHERE id = ? AND usuario_id = ?", metaId, usuarioId) == 0) {
			throw noEncontrada();
		}
	}

	/** La meta queda bloqueada mientras se aporta, para que dos retiros a la vez no la dejen en negativo. */
	@Transactional
	public MetaResponse aportar(UUID usuarioId, UUID metaId, AporteRequest solicitud) {
		if (jdbc.query("SELECT id FROM metas WHERE id = ? AND usuario_id = ? FOR UPDATE", (rs, i) -> 1, metaId, usuarioId).isEmpty()) {
			throw noEncontrada();
		}
		if (solicitud.monto().signum() == 0) {
			throw new ReglaDeNegocioException("El aporte no puede ser cero.");
		}
		if (solicitud.fecha().isAfter(meses.hoy())) {
			throw new ReglaDeNegocioException("La fecha no puede ser futura.");
		}
		MetaResponse actual = obtener(usuarioId, metaId);
		if (actual.ahorrado().add(solicitud.monto()).signum() < 0) {
			throw new ReglaDeNegocioException("No puedes sacar más de lo ahorrado (" + Pesos.formato(actual.ahorrado()) + ").");
		}
		jdbc.update("INSERT INTO aportes (id, meta_id, monto, fecha) VALUES (?, ?, ?, ?)", UUID.randomUUID(), metaId,
				solicitud.monto(), Date.valueOf(solicitud.fecha()));
		return obtener(usuarioId, metaId);
	}

	private void validar(MetaRequest solicitud) {
		if (solicitud.fechaObjetivo() != null && solicitud.fechaObjetivo().isBefore(meses.hoy())) {
			throw new ReglaDeNegocioException("La fecha objetivo debe ser de hoy en adelante.");
		}
	}

	private MetaResponse obtener(UUID usuarioId, UUID metaId) {
		return jdbc.query(CONSULTA + " AND m.id = ?", this::aRespuesta, usuarioId, metaId).stream().findFirst()
				.orElseThrow(MetaService::noEncontrada);
	}

	private MetaResponse aRespuesta(ResultSet rs, int i) throws SQLException {
		BigDecimal objetivo = rs.getBigDecimal("monto_objetivo");
		BigDecimal ahorrado = rs.getBigDecimal("ahorrado");
		BigDecimal falta = objetivo.subtract(ahorrado).max(BigDecimal.ZERO);
		BigDecimal porcentaje = ahorrado.multiply(BigDecimal.valueOf(100)).divide(objetivo, 1, RoundingMode.HALF_UP);
		LocalDate fechaObjetivo = rs.getDate("fecha_objetivo") == null ? null : rs.getDate("fecha_objetivo").toLocalDate();
		return new MetaResponse(rs.getObject("id", UUID.class), rs.getString("nombre"), objetivo, ahorrado, falta, porcentaje,
				fechaObjetivo, aporteMensual(falta, fechaObjetivo), falta.signum() == 0);
	}

	/** Lo que falta entre los meses que quedan, contando el actual; redondeado hacia arriba a miles. */
	BigDecimal aporteMensual(BigDecimal falta, LocalDate fechaObjetivo) {
		if (fechaObjetivo == null || falta.signum() == 0) {
			return null;
		}
		long mesesRestantes = Math.max(1, ChronoUnit.MONTHS.between(meses.actual(), YearMonth.from(fechaObjetivo)) + 1);
		return falta.divide(BigDecimal.valueOf(mesesRestantes), -3, RoundingMode.CEILING);
	}

	private static Date fecha(LocalDate fecha) {
		return fecha == null ? null : Date.valueOf(fecha);
	}

	private static RecursoNoEncontradoException noEncontrada() {
		return new RecursoNoEncontradoException("Meta no encontrada.");
	}

}
