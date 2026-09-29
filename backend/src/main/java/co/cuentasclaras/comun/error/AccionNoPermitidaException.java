package co.cuentasclaras.comun.error;

/**
 * La persona puede ver el recurso, pero esta acción sobre él no está permitida
 * (HTTP 403). Ej.: modificar una categoría por defecto.
 */
public class AccionNoPermitidaException extends RuntimeException {

	public AccionNoPermitidaException(String mensaje) {
		super(mensaje);
	}

}
