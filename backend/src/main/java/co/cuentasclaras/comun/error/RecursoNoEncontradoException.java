package co.cuentasclaras.comun.error;

/**
 * El recurso no existe o no pertenece a quien lo pide (HTTP 404). Se usa 404 y
 * no 403 para no revelar que existe un recurso de otra persona (RN-01).
 */
public class RecursoNoEncontradoException extends RuntimeException {

	public RecursoNoEncontradoException(String mensaje) {
		super(mensaje);
	}

}
