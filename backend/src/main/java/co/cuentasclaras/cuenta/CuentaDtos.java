package co.cuentasclaras.cuenta;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public final class CuentaDtos {

	private CuentaDtos() {
	}

	/** Se usa igual para crear y para editar. */
	public record CuentaRequest(
			@NotBlank(message = "El nombre es obligatorio.")
			@Size(max = 60, message = "El nombre no puede tener más de 60 caracteres.")
			String nombre,

			@NotNull(message = "El tipo es obligatorio (EFECTIVO, BILLETERA_DIGITAL o BANCO).")
			TipoCuenta tipo,

			@NotNull(message = "El saldo inicial es obligatorio (puede ser 0).")
			@PositiveOrZero(message = "El saldo inicial no puede ser negativo.")
			@Digits(integer = 12, fraction = 2, message = "El saldo admite hasta 12 dígitos y 2 decimales.")
			BigDecimal saldoInicial) {

		public CuentaRequest {
			nombre = nombre == null ? null : nombre.strip();
		}
	}

	public record CuentaResponse(
			UUID id,
			String nombre,
			TipoCuenta tipo,
			BigDecimal saldoInicial,
			BigDecimal saldoActual,
			boolean archivada) {
	}

}
