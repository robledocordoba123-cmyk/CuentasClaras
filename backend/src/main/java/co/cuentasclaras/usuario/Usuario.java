package co.cuentasclaras.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Persona usuaria de CuentasClaras. Mapea la tabla {@code usuarios} creada en
 * V1__esquema_inicial.sql (Hibernate solo valida, no crea tablas).
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

	@Id
	private UUID id;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(nullable = false, unique = true, length = 160)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	/** Requerido por JPA. */
	protected Usuario() {
	}

	public Usuario(String nombre, String email, String passwordHash) {
		// El id se genera en Java y no en la base de datos: así la entidad
		// tiene identidad desde que se crea, antes de guardarse.
		this.id = UUID.randomUUID();
		this.nombre = nombre;
		this.email = email;
		this.passwordHash = passwordHash;
		this.creadoEn = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}

}
