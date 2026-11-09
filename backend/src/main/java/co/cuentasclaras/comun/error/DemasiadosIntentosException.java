package co.cuentasclaras.comun.error;

/** Demasiados intentos fallidos de inicio de sesión (HTTP 429). */
public class DemasiadosIntentosException extends RuntimeException {

	public DemasiadosIntentosException() {
		super("Demasiados intentos fallidos. Espera 15 minutos e intenta de nuevo.");
	}

}
