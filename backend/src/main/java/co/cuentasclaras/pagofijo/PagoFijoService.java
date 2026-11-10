package co.cuentasclaras.pagofijo;

import co.cuentasclaras.categoria.Categoria;
import co.cuentasclaras.categoria.CategoriaRepository;
import co.cuentasclaras.categoria.TipoCategoria;
import co.cuentasclaras.comun.Meses;
import co.cuentasclaras.comun.error.RecursoEnConflictoException;
import co.cuentasclaras.comun.error.RecursoNoEncontradoException;
import co.cuentasclaras.comun.error.ReglaDeNegocioException;
import co.cuentasclaras.cuenta.Cuenta;
import co.cuentasclaras.cuenta.CuentaRepository;
import co.cuentasclaras.movimiento.MovimientoDtos.MovimientoRequest;
import co.cuentasclaras.movimiento.MovimientoDtos.MovimientoResponse;
import co.cuentasclaras.movimiento.MovimientoRepository;
import co.cuentasclaras.movimiento.MovimientoService;
import co.cuentasclaras.movimiento.TipoMovimiento;
import co.cuentasclaras.pagofijo.PagoFijoDtos.EstadoPago;
import co.cuentasclaras.pagofijo.PagoFijoDtos.PagarRequest;
import co.cuentasclaras.pagofijo.PagoFijoDtos.PagoFijoRequest;
import co.cuentasclaras.pagofijo.PagoFijoDtos.PagoFijoResponse;
import co.cuentasclaras.pagofijo.PagoFijoDtos.Referencia;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Pagos fijos del mes. Pagar uno crea un gasto normal con las mismas reglas de
 * cualquier movimiento (MovimientoService) y lo marca con el pago y el mes. El
 * índice único uq_pago_fijo_periodo impide pagar dos veces el mismo mes, aunque
 * lleguen dos clics al mismo tiempo. Borrar ese movimiento lo deja pendiente otra vez.
 */
@Service
public class PagoFijoService {

	private final JdbcTemplate jdbc;
	private final Meses meses;
	private final MovimientoService movimientoService;
	private final MovimientoRepository movimientoRepository;
	private final CategoriaRepository categoriaRepository;
	private final CuentaRepository cuentaRepository;

	public PagoFijoService(JdbcTemplate jdbc, Meses meses, MovimientoService movimientoService,
			MovimientoRepository movimientoRepository, CategoriaRepository categoriaRepository,
			CuentaRepository cuentaRepository) {
		this.jdbc = jdbc;
		this.meses = meses;
		this.movimientoService = movimientoService;
		this.movimientoRepository = movimientoRepository;
		this.categoriaRepository = categoriaRepository;
		this.cuentaRepository = cuentaRepository;
	}

	/** Los pagos activos con su estado en el mes: pagado, pendiente o vencido. */
	@Transactional(readOnly = true)
	public List<PagoFijoResponse> listar(UUID usuarioId, YearMonth mes) {
		LocalDate hoy = meses.hoy();
		return jdbc.query("""
				SELECT pf.id, pf.nombre, pf.monto, pf.dia_del_mes, pf.activo,
				       c.id AS cat_id, c.nombre AS cat_nombre, c.color AS cat_color,
				       cu.id AS cuenta_id, cu.nombre AS cuenta_nombre,
				       m.id AS movimiento_id, m.monto AS monto_pagado
				FROM pagos_fijos pf
				JOIN categorias c ON c.id = pf.categoria_id
				JOIN cuentas cu ON cu.id = pf.cuenta_id
				LEFT JOIN movimientos m ON m.pago_fijo_id = pf.id AND m.periodo = ?
				WHERE pf.usuario_id = ? AND (pf.activo OR m.id IS NOT NULL)
				ORDER BY pf.dia_del_mes, lower(pf.nombre)""", (rs, i) -> {
			int dia = rs.getInt("dia_del_mes");
			LocalDate vence = vencimiento(mes, dia);
			UUID movimientoId = rs.getObject("movimiento_id", UUID.class);
			EstadoPago estado = movimientoId != null ? EstadoPago.PAGADO
					: hoy.isAfter(vence) ? EstadoPago.VENCIDO : EstadoPago.PENDIENTE;
			return new PagoFijoResponse(rs.getObject("id", UUID.class), rs.getString("nombre"), rs.getBigDecimal("monto"), dia,
					new Referencia(rs.getObject("cat_id", UUID.class), rs.getString("cat_nombre"), rs.getString("cat_color")),
					new Referencia(rs.getObject("cuenta_id", UUID.class), rs.getString("cuenta_nombre"), null),
					rs.getBoolean("activo"), vence, estado, movimientoId, rs.getBigDecimal("monto_pagado"));
		}, mes.toString(), usuarioId);
	}

	@Transactional
	public UUID crear(UUID usuarioId, PagoFijoRequest solicitud) {
		validar(usuarioId, solicitud);
		UUID id = UUID.randomUUID();
		jdbc.update("""
				INSERT INTO pagos_fijos (id, usuario_id, nombre, monto, dia_del_mes, categoria_id, cuenta_id)
				VALUES (?, ?, ?, ?, ?, ?, ?)""", id, usuarioId, solicitud.nombre(), solicitud.monto(), solicitud.diaDelMes(),
				solicitud.categoriaId(), solicitud.cuentaId());
		return id;
	}

	/** Cambiar el monto o el día no toca los meses ya pagados: cada pago guardó su movimiento. */
	@Transactional
	public void actualizar(UUID usuarioId, UUID pagoId, PagoFijoRequest solicitud) {
		validar(usuarioId, solicitud);
		if (jdbc.update("""
				UPDATE pagos_fijos SET nombre = ?, monto = ?, dia_del_mes = ?, categoria_id = ?, cuenta_id = ?
				WHERE id = ? AND usuario_id = ?""", solicitud.nombre(), solicitud.monto(), solicitud.diaDelMes(),
				solicitud.categoriaId(), solicitud.cuentaId(), pagoId, usuarioId) == 0) {
			throw noEncontrado();
		}
	}

	/**
	 * Como las cuentas (RN-10): si ya tiene pagos registrados no se borra, se
	 * desactiva, para que el historial siga diciendo de qué pago fue cada gasto.
	 *
	 * @return true si se desactivó, false si se borró
	 */
	@Transactional
	public boolean eliminar(UUID usuarioId, UUID pagoId) {
		buscar(usuarioId, pagoId);
		Integer pagos = jdbc.queryForObject("SELECT count(*) FROM movimientos WHERE pago_fijo_id = ?", Integer.class, pagoId);
		if (pagos != null && pagos > 0) {
			jdbc.update("UPDATE pagos_fijos SET activo = FALSE WHERE id = ?", pagoId);
			return true;
		}
		jdbc.update("DELETE FROM pagos_fijos WHERE id = ?", pagoId);
		return false;
	}

	@Transactional
	public MovimientoResponse pagar(UUID usuarioId, UUID pagoId, PagarRequest solicitud) {
		PagoFijo pago = buscar(usuarioId, pagoId);
		if (!pago.activo()) {
			throw new ReglaDeNegocioException("Este pago fijo está desactivado.");
		}
		YearMonth mes = meses.parsearOActual(solicitud == null ? null : solicitud.mes());
		LocalDate hoy = meses.hoy();
		// Por defecto se paga hoy si es el mes en curso; si es otro mes, en su día de vencimiento.
		LocalDate fecha = solicitud != null && solicitud.fecha() != null ? solicitud.fecha()
				: mes.equals(YearMonth.from(hoy)) ? hoy : vencimiento(mes, pago.dia());
		if (!YearMonth.from(fecha).equals(mes)) {
			throw new ReglaDeNegocioException("La fecha del pago debe estar dentro de " + mes + ".");
		}
		if (fecha.isAfter(hoy)) {
			throw new ReglaDeNegocioException("No puedes registrar un pago con fecha futura.");
		}
		BigDecimal monto = solicitud != null && solicitud.monto() != null ? solicitud.monto() : pago.monto();
		if (yaPagado(pagoId, mes)) {
			throw yaPagadoError(pago, mes);
		}
		MovimientoResponse movimiento = movimientoService.crear(usuarioId, new MovimientoRequest(pago.cuentaId(),
				pago.categoriaId(), TipoMovimiento.GASTO, monto, fecha, pago.nombre()));
		movimientoRepository.flush();
		try {
			jdbc.update("UPDATE movimientos SET pago_fijo_id = ?, periodo = ? WHERE id = ?", pagoId, mes.toString(), movimiento.id());
		} catch (DuplicateKeyException e) {
			// Dos pagos del mismo mes al mismo tiempo: gana el primero y este se deshace entero.
			throw yaPagadoError(pago, mes);
		}
		return movimiento;
	}

	private boolean yaPagado(UUID pagoId, YearMonth mes) {
		Integer cuenta = jdbc.queryForObject("SELECT count(*) FROM movimientos WHERE pago_fijo_id = ? AND periodo = ?",
				Integer.class, pagoId, mes.toString());
		return cuenta != null && cuenta > 0;
	}

	/** Un pago del día 31 vence el último día en los meses más cortos. */
	static LocalDate vencimiento(YearMonth mes, int dia) {
		return mes.atDay(Math.min(dia, mes.lengthOfMonth()));
	}

	private void validar(UUID usuarioId, PagoFijoRequest solicitud) {
		Categoria categoria = categoriaRepository.visiblePara(solicitud.categoriaId(), usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada."));
		if (categoria.getTipo() != TipoCategoria.GASTO || categoria.isArchivada()) {
			throw new ReglaDeNegocioException("Elige una categoría de gasto activa.");
		}
		Cuenta cuenta = cuentaRepository.findByIdAndUsuarioId(solicitud.cuentaId(), usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada."));
		if (cuenta.isArchivada()) {
			throw new ReglaDeNegocioException("La cuenta \"" + cuenta.getNombre() + "\" está archivada.");
		}
	}

	private PagoFijo buscar(UUID usuarioId, UUID pagoId) {
		return jdbc.query("""
				SELECT nombre, monto, dia_del_mes, categoria_id, cuenta_id, activo
				FROM pagos_fijos WHERE id = ? AND usuario_id = ?""",
				(rs, i) -> new PagoFijo(rs.getString("nombre"), rs.getBigDecimal("monto"), rs.getInt("dia_del_mes"),
						rs.getObject("categoria_id", UUID.class), rs.getObject("cuenta_id", UUID.class), rs.getBoolean("activo")),
				pagoId, usuarioId).stream().findFirst().orElseThrow(PagoFijoService::noEncontrado);
	}

	private static RecursoEnConflictoException yaPagadoError(PagoFijo pago, YearMonth mes) {
		return new RecursoEnConflictoException("Ya registraste el pago de " + pago.nombre() + " de " + mes + ".");
	}

	private static RecursoNoEncontradoException noEncontrado() {
		return new RecursoNoEncontradoException("Pago fijo no encontrado.");
	}

	private record PagoFijo(String nombre, BigDecimal monto, int dia, UUID categoriaId, UUID cuentaId, boolean activo) {
	}

}
