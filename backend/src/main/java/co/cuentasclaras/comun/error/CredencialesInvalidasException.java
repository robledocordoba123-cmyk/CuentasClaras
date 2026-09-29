package co.cuentasclaras.comun.error;

/** Correo o contraseña incorrectos (HTTP 401), sin decir cuál de los dos. */
public class CredencialesInvalidasException extends RuntimeException {

	public CredencialesInvalidasException() {
		super("Correo o contraseña incorrectos.");
	}

}
