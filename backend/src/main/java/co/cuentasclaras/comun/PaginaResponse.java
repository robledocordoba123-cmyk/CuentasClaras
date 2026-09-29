package co.cuentasclaras.comun;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados con un formato estable para el frontend. No se devuelve
 * el objeto Page de Spring directamente porque su JSON expone detalles internos
 * y puede cambiar entre versiones.
 */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

	public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> convertir) {
		return new PaginaResponse<>(pagina.getContent().stream().map(convertir).toList(),
				pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
	}

	public static <T> PaginaResponse<T> de(Page<?> pagina, List<T> contenido) {
		return new PaginaResponse<>(contenido, pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(),
				pagina.getTotalPages());
	}

}
