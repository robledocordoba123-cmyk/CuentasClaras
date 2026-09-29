package co.cuentasclaras.comun;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Obtiene el id de la persona autenticada a partir del JWT ya validado. Los
 * controladores lo usan para que cada consulta filtre por su dueño (RN-01):
 * el id sale del token firmado, nunca de un parámetro que mande el cliente.
 */
public final class UsuarioActual {

	private UsuarioActual() {
	}

	public static UUID id(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

}
