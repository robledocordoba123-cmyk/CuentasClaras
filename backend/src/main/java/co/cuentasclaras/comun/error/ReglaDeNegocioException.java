package co.cuentasclaras.comun.error;

/**
 * Los datos tienen el formato correcto, pero violan una regla del negocio
 * (HTTP 422). Ej.: usar una categoría de ingreso para registrar un gasto.
 */
public class ReglaDeNegocioException extends RuntimeException {

	public ReglaDeNegocioException(String mensaje) {
		super(mensaje);
	}

}
