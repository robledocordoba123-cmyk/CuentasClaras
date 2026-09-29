package co.cuentasclaras.comun;

import co.cuentasclaras.soporte.PruebaDeApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los navegadores mandan el encabezado Origin en cada petición; curl y
 * MockMvc no, a menos que se lo pongamos. Sin estas pruebas, un error de CORS
 * solo se descubre al abrir la app en el navegador.
 */
class CorsTest {

	private static final String LOGIN = """
			{"email": "nadie@correo.com", "password": "clave-segura-123"}
			""";

	@Nested
	class SinOrigenesConfigurados extends PruebaDeApi {

		@Test
		void unaPeticionDelNavegadorConOriginNoSeRechaza() throws Exception {
			// Como en desarrollo con el proxy de Vite: 401 por credenciales, no 403 por CORS.
			mockMvc.perform(post("/api/auth/login").header("Origin", "http://localhost:5173")
							.contentType(MediaType.APPLICATION_JSON).content(LOGIN))
					.andExpect(status().isUnauthorized());
		}

	}

	@Nested
	@TestPropertySource(properties = "cuentasclaras.cors-origen=https://cuentasclaras.vercel.app")
	class ConOrigenConfigurado extends PruebaDeApi {

		@Test
		void elFrontendPermitidoPuedeLlamarALaApi() throws Exception {
			mockMvc.perform(options("/api/auth/login").header("Origin", "https://cuentasclaras.vercel.app")
							.header("Access-Control-Request-Method", "POST"))
					.andExpect(status().isOk())
					.andExpect(header().string("Access-Control-Allow-Origin", "https://cuentasclaras.vercel.app"));
		}

		@Test
		void otroSitioEsRechazado() throws Exception {
			mockMvc.perform(options("/api/auth/login").header("Origin", "https://sitio-malicioso.com")
							.header("Access-Control-Request-Method", "POST"))
					.andExpect(status().isForbidden());
		}

	}

}
