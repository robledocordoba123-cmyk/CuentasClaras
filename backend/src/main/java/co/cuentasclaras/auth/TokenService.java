package co.cuentasclaras.auth;

import co.cuentasclaras.usuario.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Crea los JWT. La validación de los tokens que llegan en cada petición no se
 * hace aquí: la hace Spring Security con el JwtDecoder de SeguridadConfig.
 */
@Service
public class TokenService {

	public static final String EMISOR = "cuentasclaras";

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;

	public TokenService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
		this.jwtEncoder = jwtEncoder;
		this.jwtProperties = jwtProperties;
	}

	public TokenEmitido emitirPara(Usuario usuario) {
		Instant ahora = Instant.now();
		Instant expira = ahora.plus(jwtProperties.duracion());

		// El token lleva solo lo necesario para identificar a la persona. Nada
		// secreto va aquí: el contenido de un JWT se puede leer (base64), lo que
		// no se puede es modificarlo sin invalidar la firma.
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(EMISOR)
				.subject(usuario.getId().toString())
				.issuedAt(ahora)
				.expiresAt(expira)
				.claim("email", usuario.getEmail())
				.claim("nombre", usuario.getNombre())
				.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new TokenEmitido(token, expira);
	}

	public record TokenEmitido(String valor, Instant expiraEn) {
	}

}
