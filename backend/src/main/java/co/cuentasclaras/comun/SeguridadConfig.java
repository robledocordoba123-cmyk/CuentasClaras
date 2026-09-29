package co.cuentasclaras.comun;

import co.cuentasclaras.auth.JwtProperties;
import co.cuentasclaras.auth.TokenService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Seguridad de la API.
 *
 * <p>Flujo: la persona hace login y recibe un JWT firmado. En cada petición lo
 * manda en el encabezado {@code Authorization: Bearer <token>}. Spring Security
 * (como "resource server") verifica la firma, la expiración y el emisor antes
 * de que la petición llegue a cualquier controlador. Si algo falla: 401.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SeguridadConfig {

	@Bean
	SecurityFilterChain filtroDeSeguridad(HttpSecurity http) throws Exception {
		return http
				// API REST con tokens en encabezado: no hay sesión ni cookies,
				// así que CSRF no aplica.
				.csrf(csrf -> csrf.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
						// Spring redirige los errores internos a /error: debe ser
						// público para que el cliente vea el 400/404 real y no un 401.
						.requestMatchers("/error").permitAll()
						// Documentación de la API (Swagger UI y la especificación OpenAPI).
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
				}))
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.build();
	}

	/**
	 * BCrypt: función de hash lenta a propósito y con "sal" aleatoria. Aunque
	 * alguien robe la base de datos, sacar las contraseñas por fuerza bruta es
	 * muy costoso, y dos personas con la misma contraseña tienen hashes distintos.
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	JwtEncoder jwtEncoder(JwtProperties propiedades) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(clave(propiedades)));
	}

	@Bean
	JwtDecoder jwtDecoder(JwtProperties propiedades) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave(propiedades))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		// Además de firma y expiración, exige que el token lo hayamos emitido nosotros.
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenService.EMISOR));
		return decoder;
	}

	private static SecretKey clave(JwtProperties propiedades) {
		return new SecretKeySpec(propiedades.secreto().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

}
