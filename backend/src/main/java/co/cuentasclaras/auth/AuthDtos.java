package co.cuentasclaras.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Objetos que entran y salen de la API de autenticación (DTO). Son records:
 * inmutables y sin código repetido. La entidad Usuario nunca sale tal cual,
 * para no exponer por error el hash de la contraseña.
 */
public final class AuthDtos {

	private AuthDtos() {
	}

	public record RegistroRequest(
			@NotBlank(message = "El nombre es obligatorio.")
			@Size(max = 100, message = "El nombre no puede tener más de 100 caracteres.")
			String nombre,

			@NotBlank(message = "El correo es obligatorio.")
			@Email(message = "El correo no tiene un formato válido.")
			@Size(max = 160, message = "El correo no puede tener más de 160 caracteres.")
			String email,

			// Máximo 72: BCrypt solo tiene en cuenta los primeros 72 bytes; una
			// contraseña más larga se truncaría sin avisar.
			@NotBlank(message = "La contraseña es obligatoria.")
			@Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
			String password) {

		// Se quitan los espacios de los extremos ANTES de validar: un correo
		// copiado y pegado con un espacio al final no debe dar "formato inválido".
		// La contraseña no se toca: un espacio puede ser parte de ella.
		public RegistroRequest {
			nombre = recortar(nombre);
			email = recortar(email);
		}
	}

	public record LoginRequest(
			@NotBlank(message = "El correo es obligatorio.")
			String email,

			@NotBlank(message = "La contraseña es obligatoria.")
			String password) {

		public LoginRequest {
			email = recortar(email);
		}
	}

	private static String recortar(String texto) {
		return texto == null ? null : texto.strip();
	}

	public record UsuarioResponse(UUID id, String nombre, String email) {
	}

	public record TokenResponse(String token, String tipo, Instant expiraEn, UsuarioResponse usuario) {
	}

}
