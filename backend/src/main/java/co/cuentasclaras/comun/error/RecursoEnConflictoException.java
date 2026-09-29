package co.cuentasclaras.comun.error;

/** El recurso ya existe o choca con otro (HTTP 409). Ej.: correo ya registrado. */
public class RecursoEnConflictoException extends RuntimeException {

	public RecursoEnConflictoException(String mensaje) {
		super(mensaje);
	}

}
