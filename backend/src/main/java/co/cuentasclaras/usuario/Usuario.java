package co.cuentasclaras.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Persona usuaria de CuentasClaras. Mapea la tabla {@code usuarios} creada en
 * las migraciones de Flyway (Hibernate solo valida, no crea tablas).
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

	/** Sube cada vez que hay que cerrar todas las sesiones abiertas. */
	@Column(name = "version_sesion", nullable = false)
	private int versionSesion;

	@Column(name = "autorizo_datos_en")
	private Instant autorizoDatosEn;

	@Column(name = "version_politica", length = 10)
	private String versionPolitica;

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

	/** Ley 1581: guarda cuándo autorizó y qué versión de la política aceptó. */
	public void autorizarDatos(String version, Instant cuando) {
		this.autorizoDatosEn = cuando;
		this.versionPolitica = version;
	}

	/** Nueva contraseña: las sesiones abiertas con la anterior dejan de servir. */
	public void cambiarPassword(String nuevoHash) {
		this.passwordHash = nuevoHash;
		this.versionSesion++;
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

	public int getVersionSesion() {
		return versionSesion;
	}

	public Instant getAutorizoDatosEn() {
		return autorizoDatosEn;
	}

	public String getVersionPolitica() {
		return versionPolitica;
	}

}
