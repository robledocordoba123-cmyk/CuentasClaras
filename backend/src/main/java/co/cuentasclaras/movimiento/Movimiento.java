package co.cuentasclaras.movimiento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un ingreso, un gasto o una de las dos partes de una transferencia.
 *
 * <p>El monto siempre es positivo (RN-03): si suma o resta lo dice el tipo.
 * La fecha es LocalDate (un día del calendario, sin hora), porque "el 15 de
 * marzo" es el mismo día en cualquier zona horaria.
 */
@Entity
@Table(name = "movimientos")
public class Movimiento {

	@Id
	private UUID id;

	@Column(name = "usuario_id", nullable = false, updatable = false)
	private UUID usuarioId;

	@Column(name = "cuenta_id", nullable = false)
	private UUID cuentaId;

	/** Null en las transferencias (RN-07). */
	@Column(name = "categoria_id")
	private UUID categoriaId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 25)
	private TipoMovimiento tipo;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal monto;

	@Column(nullable = false)
	private LocalDate fecha;

	@Column(length = 200)
	private String descripcion;

	/** Une las dos partes de una transferencia (RN-05). Null en ingresos y gastos. */
	@Column(name = "transferencia_id", updatable = false)
	private UUID transferenciaId;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	protected Movimiento() {
	}

	private Movimiento(UUID usuarioId, UUID cuentaId, UUID categoriaId, TipoMovimiento tipo, BigDecimal monto,
			LocalDate fecha, String descripcion, UUID transferenciaId) {
		this.id = UUID.randomUUID();
		this.usuarioId = usuarioId;
		this.cuentaId = cuentaId;
		this.categoriaId = categoriaId;
		this.tipo = tipo;
		this.monto = monto;
		this.fecha = fecha;
		this.descripcion = descripcion;
		this.transferenciaId = transferenciaId;
		this.creadoEn = Instant.now();
	}

	public static Movimiento ingresoOGasto(UUID usuarioId, UUID cuentaId, UUID categoriaId, TipoMovimiento tipo,
			BigDecimal monto, LocalDate fecha, String descripcion) {
		return new Movimiento(usuarioId, cuentaId, categoriaId, tipo, monto, fecha, descripcion, null);
	}

	public static Movimiento parteDeTransferencia(UUID usuarioId, UUID cuentaId, TipoMovimiento tipo,
			BigDecimal monto, LocalDate fecha, String descripcion, UUID transferenciaId) {
		return new Movimiento(usuarioId, cuentaId, null, tipo, monto, fecha, descripcion, transferenciaId);
	}

	public void actualizar(UUID cuentaId, UUID categoriaId, TipoMovimiento tipo, BigDecimal monto, LocalDate fecha,
			String descripcion) {
		this.cuentaId = cuentaId;
		this.categoriaId = categoriaId;
		this.tipo = tipo;
		this.monto = monto;
		this.fecha = fecha;
		this.descripcion = descripcion;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUsuarioId() {
		return usuarioId;
	}

	public UUID getCuentaId() {
		return cuentaId;
	}

	public UUID getCategoriaId() {
		return categoriaId;
	}

	public TipoMovimiento getTipo() {
		return tipo;
	}

	public BigDecimal getMonto() {
		return monto;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public UUID getTransferenciaId() {
		return transferenciaId;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
