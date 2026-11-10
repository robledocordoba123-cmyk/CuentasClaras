package co.cuentasclaras.pagofijo;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Gastos que se repiten cada mes: arriendo, servicios, plan de celular. */
public final class PagoFijoDtos {

	private PagoFijoDtos() {
	}

	public enum EstadoPago {
		PAGADO, PENDIENTE, VENCIDO
	}

	public record PagoFijoRequest(
			@NotBlank(message = "Escribe el nombre, por ejemplo: Arriendo.")
			@Size(max = 60, message = "El nombre no puede tener más de 60 caracteres.")
			String nombre,

			@NotNull(message = "El monto es obligatorio.")
			@Positive(message = "El monto debe ser mayor que cero.")
			@Digits(integer = 12, fraction = 2, message = "El monto admite hasta 12 dígitos y 2 decimales.")
			BigDecimal monto,

			@NotNull(message = "Indica qué día del mes se paga.")
			@Min(value = 1, message = "El día va de 1 a 31.")
			@Max(value = 31, message = "El día va de 1 a 31.")
			Integer diaDelMes,

			@NotNull(message = "Elige la categoría de gasto.")
			UUID categoriaId,

			@NotNull(message = "Elige la cuenta de la que sale.")
			UUID cuentaId) {

		public PagoFijoRequest {
			nombre = nombre == null ? null : nombre.strip();
		}
	}

	/** Para pagar con otro monto o en otra fecha (por ejemplo, el recibo de la luz cambia). Todo es opcional. */
	public record PagarRequest(
			String mes,

			@Positive(message = "El monto debe ser mayor que cero.")
			@Digits(integer = 12, fraction = 2, message = "El monto admite hasta 12 dígitos y 2 decimales.")
			BigDecimal monto,

			LocalDate fecha) {
	}

	public record Referencia(UUID id, String nombre, String color) {
	}

	public record PagoFijoResponse(UUID id, String nombre, BigDecimal monto, int diaDelMes, Referencia categoria,
			Referencia cuenta, boolean activo, LocalDate vence, EstadoPago estado, UUID movimientoId, BigDecimal montoPagado) {
	}

}
