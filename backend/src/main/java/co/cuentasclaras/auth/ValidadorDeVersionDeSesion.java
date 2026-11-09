package co.cuentasclaras.auth;

import co.cuentasclaras.usuario.UsuarioRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Un JWT firmado es válido hasta que vence, aunque la persona cambie la
 * contraseña o borre su cuenta. Este validador consulta en cada petición si la
 * cuenta sigue existiendo y si el token se emitió con su versión de sesión
 * actual. Si no, el token deja de servir de inmediato (401).
 */
public class ValidadorDeVersionDeSesion implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error SESION_CERRADA =
			new OAuth2Error("invalid_token", "La sesión ya no es válida.", null);

	private final UsuarioRepository usuarioRepository;

	public ValidadorDeVersionDeSesion(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		Object version = jwt.getClaims().get(TokenService.CLAIM_VERSION);
		if (!(version instanceof Number numero)) {
			return OAuth2TokenValidatorResult.failure(SESION_CERRADA);
		}
		boolean vigente;
		try {
			vigente = usuarioRepository.findById(UUID.fromString(jwt.getSubject()))
					.map(u -> u.getVersionSesion() == numero.intValue())
					.orElse(false);
		} catch (IllegalArgumentException e) {
			vigente = false;
		}
		return vigente ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(SESION_CERRADA);
	}

}
