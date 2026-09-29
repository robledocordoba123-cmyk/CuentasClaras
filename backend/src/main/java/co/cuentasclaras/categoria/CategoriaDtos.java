package co.cuentasclaras.categoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class CategoriaDtos {

	private CategoriaDtos() {
	}

	public record CategoriaRequest(
			@NotBlank(message = "El nombre es obligatorio.")
			@Size(max = 60, message = "El nombre no puede tener más de 60 caracteres.")
			String nombre,

			@NotNull(message = "El tipo es obligatorio (INGRESO o GASTO).")
			TipoCategoria tipo,

			/** Opcional. Color hexadecimal para las gráficas, ej. #F97316. */
			@Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe tener el formato #RRGGBB.")
			String color) {

		public CategoriaRequest {
			nombre = nombre == null ? null : nombre.strip();
			color = (color == null || color.isBlank()) ? "#6366F1" : color.toUpperCase();
		}
	}

	public record CategoriaResponse(
			UUID id,
			String nombre,
			TipoCategoria tipo,
			String color,
			boolean porDefecto,
			boolean archivada) {
	}

}
