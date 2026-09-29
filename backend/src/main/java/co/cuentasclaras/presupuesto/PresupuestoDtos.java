package co.cuentasclaras.presupuesto;

import co.cuentasclaras.movimiento.MovimientoDtos.CategoriaResumen;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public final class PresupuestoDtos {

	private PresupuestoDtos() {
	}

	/** Crea el presupuesto o, si ya existe para esa categoría y mes, cambia su límite. */
	public record PresupuestoRequest(
			@NotNull(message = "La categoría es obligatoria.")
			UUID categoriaId,

			@NotBlank(message = "El mes es obligatorio (formato AAAA-MM).")
			@Pattern(regexp = "^[0-9]{4}-(0[1-9]|1[0-2])$", message = "El mes debe tener el formato AAAA-MM.")
			String mes,

			@NotNull(message = "El monto límite es obligatorio.")
			@Positive(message = "El monto límite debe ser mayor que cero.")
			@Digits(integer = 12, fraction = 2, message = "El monto admite hasta 12 dígitos y 2 decimales.")
			BigDecimal montoLimite) {
	}

	public record PresupuestoResponse(
			UUID id,
			CategoriaResumen categoria,
			String mes,
			BigDecimal montoLimite,
			BigDecimal gastado,
			BigDecimal disponible,
			BigDecimal porcentajeUsado,
			EstadoPresupuesto estado) {
	}

}
