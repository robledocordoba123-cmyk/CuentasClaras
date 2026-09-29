package co.cuentasclaras.transferencia;

import co.cuentasclaras.movimiento.MovimientoDtos.Referencia;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class TransferenciaDtos {

	private TransferenciaDtos() {
	}

	public record TransferenciaRequest(
			@NotNull(message = "La cuenta de origen es obligatoria.")
			UUID cuentaOrigenId,

			@NotNull(message = "La cuenta de destino es obligatoria.")
			UUID cuentaDestinoId,

			@NotNull(message = "El monto es obligatorio.")
			@Positive(message = "El monto debe ser mayor que cero.")
			@Digits(integer = 12, fraction = 2, message = "El monto admite hasta 12 dígitos y 2 decimales.")
			BigDecimal monto,

			@NotNull(message = "La fecha es obligatoria (formato AAAA-MM-DD).")
			LocalDate fecha,

			@Size(max = 200, message = "La descripción no puede tener más de 200 caracteres.")
			String descripcion) {

		public TransferenciaRequest {
			descripcion = (descripcion == null || descripcion.isBlank()) ? null : descripcion.strip();
		}
	}

	public record TransferenciaResponse(
			UUID transferenciaId,
			BigDecimal monto,
			LocalDate fecha,
			String descripcion,
			Referencia origen,
			Referencia destino) {
	}

}
