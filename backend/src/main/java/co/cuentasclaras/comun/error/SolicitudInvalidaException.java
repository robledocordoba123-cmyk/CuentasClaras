package co.cuentasclaras.comun.error;

/** Un parámetro de la petición tiene un formato inválido (HTTP 400). */
public class SolicitudInvalidaException extends RuntimeException {

	public SolicitudInvalidaException(String mensaje) {
		super(mensaje);
	}

}
