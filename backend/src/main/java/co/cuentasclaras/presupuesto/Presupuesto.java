package co.cuentasclaras.presupuesto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

/** Tope de gasto de una categoría en un mes (HU-06). Uno por categoría y mes (RN-08). */
@Entity
@Table(name = "presupuestos")
public class Presupuesto {

	@Id
	private UUID id;

	@Column(name = "usuario_id", nullable = false, updatable = false)
	private UUID usuarioId;

	@Column(name = "categoria_id", nullable = false, updatable = false)
	private UUID categoriaId;

	/**
	 * Se guarda como texto "AAAA-MM" en una columna CHAR(7): fácil de leer y
	 * de consultar. @JdbcTypeCode le dice a Hibernate que es CHAR y no VARCHAR;
	 * sin eso, la validación del esquema (ddl-auto: validate) no deja arrancar.
	 */
	@Column(nullable = false, updatable = false, length = 7)
	@JdbcTypeCode(SqlTypes.CHAR)
	private String mes;

	@Column(name = "monto_limite", nullable = false, precision = 14, scale = 2)
	private BigDecimal montoLimite;

	protected Presupuesto() {
	}

	public Presupuesto(UUID usuarioId, UUID categoriaId, YearMonth mes, BigDecimal montoLimite) {
		this.id = UUID.randomUUID();
		this.usuarioId = usuarioId;
		this.categoriaId = categoriaId;
		this.mes = mes.toString();
		this.montoLimite = montoLimite;
	}

	public void cambiarLimite(BigDecimal montoLimite) {
		this.montoLimite = montoLimite;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUsuarioId() {
		return usuarioId;
	}

	public UUID getCategoriaId() {
		return categoriaId;
	}

	public YearMonth getMes() {
		return YearMonth.parse(mes);
	}

	public BigDecimal getMontoLimite() {
		return montoLimite;
	}

}
