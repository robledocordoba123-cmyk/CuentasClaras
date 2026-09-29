package co.cuentasclaras.movimiento;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class MovimientoDtos {

	private MovimientoDtos() {
	}

	/** Crear o editar un ingreso o un gasto. Las transferencias van por /api/transferencias. */
	public record MovimientoRequest(
			@NotNull(message = "La cuenta es obligatoria.")
			UUID cuentaId,

			@NotNull(message = "La categoría es obligatoria.")
			UUID categoriaId,

			@NotNull(message = "El tipo es obligatorio (INGRESO o GASTO).")
			TipoMovimiento tipo,

			@NotNull(message = "El monto es obligatorio.")
			@Positive(message = "El monto debe ser mayor que cero.")
			@Digits(integer = 12, fraction = 2, message = "El monto admite hasta 12 dígitos y 2 decimales.")
			BigDecimal monto,

			@NotNull(message = "La fecha es obligatoria (formato AAAA-MM-DD).")
			LocalDate fecha,

			@Size(max = 200, message = "La descripción no puede tener más de 200 caracteres.")
			String descripcion) {

		public MovimientoRequest {
			descripcion = (descripcion == null || descripcion.isBlank()) ? null : descripcion.strip();
		}
	}

	public record Referencia(UUID id, String nombre) {
	}

	public record CategoriaResumen(UUID id, String nombre, String color) {
	}

	public record MovimientoResponse(
			UUID id,
			TipoMovimiento tipo,
			BigDecimal monto,
			LocalDate fecha,
			String descripcion,
			Referencia cuenta,
			CategoriaResumen categoria,
			UUID transferenciaId) {
	}

}
