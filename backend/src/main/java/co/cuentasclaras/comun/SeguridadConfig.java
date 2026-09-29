package co.cuentasclaras.comun;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Configuración base de seguridad. En este paso solo el health check es
 * público; el login con JWT se agrega en feature/auth.
 */
@Configuration
public class SeguridadConfig {

	@Bean
	SecurityFilterChain filtroDeSeguridad(HttpSecurity http) throws Exception {
		return http
				// API REST con tokens: no hay sesión ni formularios, así que
				// CSRF no aplica (el token no viaja en una cookie).
				.csrf(csrf -> csrf.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health/**").permitAll()
						.anyRequest().authenticated())
				// Sin credenciales: 401 limpio, en vez del formulario de login
				// o la ventana de usuario/contraseña del navegador.
				.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.build();
	}

}
