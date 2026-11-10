package co.cuentasclaras.deuda;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Cuentas con personas: quién te debe, a quién le debes y los abonos. */
public final class DeudaDtos {

	private DeudaDtos() {
	}

	/** ME_DEBEN: le prestaste o pagaste por la persona. DEBO: ella te prestó a ti. */
	public enum Direccion {
		ME_DEBEN, DEBO
	}

	public record PersonaRequest(
			@NotBlank(message = "Escribe el nombre de la persona.")
			@Size(max = 60, message = "El nombre no puede tener más de 60 caracteres.")
			String nombre) {

		public PersonaRequest {
			nombre = nombre == null ? null : nombre.strip();
		}
	}

	/**
	 * neto = lo que te debe menos lo que le debes. Positivo: te debe; negativo:
	 * le debes; cero: están a paz y salvo.
	 */
	public record PersonaResponse(UUID id, String nombre, BigDecimal teDebe, BigDecimal leDebes, BigDecimal neto,
			int deudasPendientes) {
	}

	public record DeudaRequest(
			@NotNull(message = "Elige a la persona.")
			UUID personaId,

			@NotNull(message = "Indica si te deben o si debes.")
			Direccion direccion,

			@NotNull(message = "El monto es obligatorio.")
			@Positive(message = "El monto debe ser mayor que cero.")
			@Digits(integer = 12, fraction = 2, message = "El monto admite hasta 12 dígitos y 2 decimales.")
			BigDecimal monto,

			@NotBlank(message = "Cuenta de qué es la deuda, por ejemplo: almuerzo del viernes.")
			@Size(max = 200, message = "La descripción no puede tener más de 200 caracteres.")
			String descripcion,

			@NotNull(message = "La fecha es obligatoria.")
			LocalDate fecha) {

		public DeudaRequest {
			descripcion = descripcion == null ? null : descripcion.strip();
		}
	}

	public record AbonoRequest(
			@NotNull(message = "El monto es obligatorio.")
			@Positive(message = "El abono debe ser mayor que cero.")
			@Digits(integer = 12, fraction = 2, message = "El monto admite hasta 12 dígitos y 2 decimales.")
			BigDecimal monto,

			@NotNull(message = "La fecha es obligatoria.")
			LocalDate fecha) {
	}

	public record AbonoResponse(UUID id, BigDecimal monto, LocalDate fecha) {
	}

	public record DeudaResponse(UUID id, UUID personaId, String persona, Direccion direccion, BigDecimal monto,
			BigDecimal abonado, BigDecimal pendiente, String descripcion, LocalDate fecha, boolean saldada,
			List<AbonoResponse> abonos) {
	}

}
