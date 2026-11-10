package co.cuentasclaras.meta;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Metas de ahorro: un sobre para algo que quieres. */
public final class MetaDtos {

	private MetaDtos() {
	}

	public record MetaRequest(
			@NotBlank(message = "Escribe para qué es, por ejemplo: viaje a Cartagena.")
			@Size(max = 60, message = "El nombre no puede tener más de 60 caracteres.")
			String nombre,

			@NotNull(message = "El monto objetivo es obligatorio.")
			@Positive(message = "El objetivo debe ser mayor que cero.")
			@Digits(integer = 12, fraction = 2, message = "El monto admite hasta 12 dígitos y 2 decimales.")
			BigDecimal montoObjetivo,

			/** Opcional: si se indica, se calcula cuánto apartar cada mes. */
			LocalDate fechaObjetivo) {

		public MetaRequest {
			nombre = nombre == null ? null : nombre.strip();
		}
	}

	/** Positivo para guardar, negativo para sacar. */
	public record AporteRequest(
			@NotNull(message = "El monto es obligatorio.")
			@Digits(integer = 12, fraction = 2, message = "El monto admite hasta 12 dígitos y 2 decimales.")
			BigDecimal monto,

			@NotNull(message = "La fecha es obligatoria.")
			LocalDate fecha) {
	}

	/**
	 * aporteMensual: lo que falta repartido en los meses que quedan hasta la fecha
	 * objetivo (contando el actual). Null si no hay fecha o si ya se cumplió.
	 */
	public record MetaResponse(UUID id, String nombre, BigDecimal montoObjetivo, BigDecimal ahorrado, BigDecimal falta,
			BigDecimal porcentaje, LocalDate fechaObjetivo, BigDecimal aporteMensual, boolean cumplida) {
	}

}
