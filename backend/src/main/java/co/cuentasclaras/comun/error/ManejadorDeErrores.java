package co.cuentasclaras.comun.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Todos los errores de la API salen con el mismo formato: ProblemDetail, el
 * estándar RFC 9457 ("application/problem+json"). Así el frontend siempre sabe
 * dónde leer el mensaje:
 *
 * <pre>
 * { "status": 409, "title": "Conflicto", "detail": "Ya existe una cuenta..." }
 * </pre>
 *
 * Hereda de ResponseEntityExceptionHandler para que los errores propios de
 * Spring MVC (ruta inexistente, método no permitido…) conserven su código
 * correcto (404, 405…) con este mismo formato, en vez de caer como 500.
 */
@RestControllerAdvice
public class ManejadorDeErrores extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

	@ExceptionHandler(RecursoEnConflictoException.class)
	ProblemDetail conflicto(RecursoEnConflictoException ex) {
		return problema(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
	}

	@ExceptionHandler(RecursoNoEncontradoException.class)
	ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
		return problema(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage());
	}

	@ExceptionHandler(AccionNoPermitidaException.class)
	ProblemDetail noPermitida(AccionNoPermitidaException ex) {
		return problema(HttpStatus.FORBIDDEN, "Acción no permitida", ex.getMessage());
	}

	@ExceptionHandler(ReglaDeNegocioException.class)
	ProblemDetail reglaDeNegocio(ReglaDeNegocioException ex) {
		return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio", ex.getMessage());
	}

	@ExceptionHandler(CredencialesInvalidasException.class)
	ProblemDetail credencialesInvalidas(CredencialesInvalidasException ex) {
		return problema(HttpStatus.UNAUTHORIZED, "No autenticado", ex.getMessage());
	}

	/** @Valid falló: se devuelve qué campo está mal y por qué. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errores = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errores.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Datos inválidos", "Revisa los campos marcados.");
		problema.setProperty("errores", errores);
		return ResponseEntity.badRequest().body(problema);
	}

	/** El cuerpo no es JSON válido o un campo tiene el tipo equivocado. */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "Solicitud mal formada",
				"El cuerpo de la solicitud no es un JSON válido."));
	}

	/**
	 * Cualquier otro error no previsto: se registra completo en el log del
	 * servidor, pero al cliente solo le llega un mensaje genérico.
	 */
	@ExceptionHandler(Exception.class)
	ProblemDetail inesperado(Exception ex) {
		log.error("Error no controlado", ex);
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
				"Ocurrió un error inesperado. Intenta de nuevo más tarde.");
	}

	private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
		problema.setTitle(titulo);
		return problema;
	}

}
