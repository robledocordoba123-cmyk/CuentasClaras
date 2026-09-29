package co.cuentasclaras.cuenta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Un "bolsillo" donde la persona tiene plata: efectivo, Nequi, banco…
 *
 * <p>No guarda el saldo actual (RN-04): el saldo se calcula a partir del saldo
 * inicial y los movimientos, así nunca queda desincronizado.
 */
@Entity
@Table(name = "cuentas")
public class Cuenta {

	@Id
	private UUID id;

	/** Dueña de la cuenta. Toda consulta filtra por este campo (RN-01). */
	@Column(name = "usuario_id", nullable = false, updatable = false)
	private UUID usuarioId;

	@Column(nullable = false, length = 60)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoCuenta tipo;

	/** BigDecimal, nunca double: con dinero no se aceptan errores de redondeo (RN-02). */
	@Column(name = "saldo_inicial", nullable = false, precision = 14, scale = 2)
	private BigDecimal saldoInicial;

	@Column(nullable = false)
	private boolean archivada;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	protected Cuenta() {
	}

	public Cuenta(UUID usuarioId, String nombre, TipoCuenta tipo, BigDecimal saldoInicial) {
		this.id = UUID.randomUUID();
		this.usuarioId = usuarioId;
		this.creadoEn = Instant.now();
		actualizar(nombre, tipo, saldoInicial);
	}

	public void actualizar(String nombre, TipoCuenta tipo, BigDecimal saldoInicial) {
		this.nombre = nombre;
		this.tipo = tipo;
		this.saldoInicial = saldoInicial;
	}

	public void archivar() {
		this.archivada = true;
	}

	public void restaurar() {
		this.archivada = false;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUsuarioId() {
		return usuarioId;
	}

	public String getNombre() {
		return nombre;
	}

	public TipoCuenta getTipo() {
		return tipo;
	}

	public BigDecimal getSaldoInicial() {
		return saldoInicial;
	}

	public boolean isArchivada() {
		return archivada;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
