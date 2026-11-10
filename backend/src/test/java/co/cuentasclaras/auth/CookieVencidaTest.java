package co.cuentasclaras.auth;

import co.cuentasclaras.soporte.PruebaDeApi;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Una cookie que ya no sirve (la cuenta se borró, la demo se recreó, cambió la
 * clave) no puede impedir volver a entrar: las rutas públicas la ignoran.
 */
class CookieVencidaTest extends PruebaDeApi {

	@Test
	void conUnaCookieViejaSePuedeEntrarRegistrarseYVerLaSalud() throws Exception {
		String vieja = registrar("ana@correo.com");
		jdbc.update("UPDATE usuarios SET version_sesion = version_sesion + 1");
		Cookie cookie = new Cookie(CookieDeSesion.NOMBRE, vieja);

		mockMvc.perform(get("/api/auth/yo").cookie(cookie)).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/actuator/health").cookie(cookie)).andExpect(status().isOk());
		mockMvc.perform(post("/api/auth/login").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "ana@correo.com", "password": "clave-segura-123"}"""))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/auth/registro").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre": "Pedro", "email": "pedro@correo.com", "password": "clave-segura-123", "aceptaPolitica": true}"""))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/auth/logout").cookie(cookie)).andExpect(status().isNoContent());
	}

}
