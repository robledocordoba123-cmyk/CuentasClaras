package co.cuentasclaras.comun;

import co.cuentasclaras.auth.JwtProperties;
import co.cuentasclaras.auth.CookieDeSesion;
import co.cuentasclaras.auth.TokenService;
import co.cuentasclaras.auth.ValidadorDeVersionDeSesion;
import co.cuentasclaras.usuario.UsuarioRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Seguridad de la API.
 *
 * <p>Flujo: la persona hace login y recibe un JWT firmado dentro de una cookie
 * HttpOnly ({@link CookieDeSesion}). El navegador la manda sola en cada petición;
 * también se acepta el encabezado {@code Authorization: Bearer <token>} (Swagger
 * y pruebas). Spring Security
 * (como "resource server") verifica la firma, la expiración y el emisor antes
 * de que la petición llegue a cualquier controlador. Si algo falla: 401.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SeguridadConfig {

	@Bean
	SecurityFilterChain filtroDeSeguridad(HttpSecurity http) throws Exception {
		return http
				// La cookie es SameSite=Strict: el navegador no la envía desde otro
				// sitio, así que no hace falta un token CSRF aparte.
				.cors(cors -> {
				})
				.csrf(csrf -> csrf.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login", "/api/auth/logout").permitAll()
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
	/**
	 * CORS: el navegador solo deja que un frontend en otro dominio (Vercel) llame
	 * a esta API si la API lo autoriza. Se permiten únicamente los orígenes de
	 * CORS_ORIGIN (separados por coma). Si está vacío no se registra ninguna
	 * regla: en desarrollo no hace falta, porque Vite redirige /api al backend y
	 * para el navegador todo viene del mismo origen. (Registrar una regla con la
	 * lista vacía haría que Spring rechazara con 403 toda petición del navegador.)
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${cuentasclaras.cors-origen:}") String origenes) {
		List<String> permitidos = Arrays.stream(origenes.split(",")).map(String::strip).filter(o -> !o.isEmpty()).toList();
		CorsConfiguration configuracion = new CorsConfiguration();
		configuracion.setAllowedOrigins(permitidos);
		configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuracion.setExposedHeaders(List.of("Content-Disposition"));
		configuracion.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		if (permitidos.isEmpty()) {
			return fuente;
		}
		fuente.registerCorsConfiguration("/**", configuracion);
		return fuente;
	}

	/** Lee el token de la cookie de sesión; si no está, del encabezado Authorization. */
	@Bean
	BearerTokenResolver bearerTokenResolver() {
		DefaultBearerTokenResolver encabezado = new DefaultBearerTokenResolver();
		return peticion -> {
			var cookie = WebUtils.getCookie(peticion, CookieDeSesion.NOMBRE);
			if (cookie != null && !cookie.getValue().isBlank()) {
				return cookie.getValue();
			}
			return encabezado.resolve(peticion);
		};
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	JwtEncoder jwtEncoder(JwtProperties propiedades) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(clave(propiedades)));
	}

	@Bean
	JwtDecoder jwtDecoder(JwtProperties propiedades, UsuarioRepository usuarioRepository) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave(propiedades))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		// Además de firma y expiración, exige que el token lo hayamos emitido
		// nosotros y que la sesión siga vigente (no cambió la clave ni se borró la cuenta).
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(TokenService.EMISOR),
				new ValidadorDeVersionDeSesion(usuarioRepository)));
		return decoder;
	}

	private static SecretKey clave(JwtProperties propiedades) {
		return new SecretKeySpec(propiedades.secreto().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

}
