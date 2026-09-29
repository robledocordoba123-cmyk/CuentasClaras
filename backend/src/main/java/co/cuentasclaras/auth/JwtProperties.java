package co.cuentasclaras.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Configuración del JWT, leída de application.yml (prefijo cuentasclaras.jwt).
 *
 * @param secreto  clave para firmar los tokens con HMAC-SHA256. En producción
 *                 viene de la variable de entorno JWT_SECRET.
 * @param duracion cuánto tiempo es válido un token.
 */
@ConfigurationProperties(prefix = "cuentasclaras.jwt")
public record JwtProperties(String secreto, Duration duracion) {

	/** HS256 exige una clave de al menos 256 bits (32 bytes). */
	private static final int LONGITUD_MINIMA_BYTES = 32;

	public JwtProperties {
		// Fallar al arrancar es mejor que arrancar con una clave débil o vacía
		// y descubrirlo cuando alguien falsifique un token.
		if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_BYTES) {
			throw new IllegalStateException(
					"cuentasclaras.jwt.secreto (JWT_SECRET) debe tener al menos 32 caracteres.");
		}
		if (duracion == null) {
			duracion = Duration.ofHours(8);
		}
	}

}
