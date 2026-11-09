package co.cuentasclaras.auth;

import co.cuentasclaras.demo.DatosDeDemo;
import co.cuentasclaras.soporte.Cookies;
import co.cuentasclaras.soporte.PruebaDeApi;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeguridadDeSesionTest extends PruebaDeApi {

	private ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "%s", "password": "%s"}
						""".formatted(email, password)));
	}

	@Nested
	@DisplayName("Cookie de sesión")
	class CookieDeSesionHttpOnly {

		@Test
		void laCookieEsHttpOnlyStrictYSoloParaLaApi() throws Exception {
			registrar("ana@correo.com");
			login("ana@correo.com", "clave-segura-123")
					.andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
					.andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
					.andExpect(header().string("Set-Cookie", containsString("Path=/api")))
					.andExpect(jsonPath("$.token").doesNotExist())
					.andExpect(jsonPath("$.usuario.email").value("ana@correo.com"));
		}

		@Test
		void laCookieSolaAbreLasRutasProtegidas() throws Exception {
			String token = registrar("ana@correo.com");
			mockMvc.perform(get("/api/auth/yo").cookie(new Cookie(CookieDeSesion.NOMBRE, token)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.email").value("ana@correo.com"));
		}

		@Test
		void cerrarSesionBorraLaCookie() throws Exception {
			mockMvc.perform(post("/api/auth/logout"))
					.andExpect(status().isNoContent())
					.andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
		}

	}

	@Nested
	@DisplayName("Ley 1581: autorización, acceso y supresión")
	class DatosPersonales {

		@Test
		void sinAutorizarElTratamientoDeDatosNoSeCreaLaCuenta() throws Exception {
			mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"nombre": "Ana", "email": "ana@correo.com", "password": "clave-segura-123", "aceptaPolitica": false}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errores.aceptaPolitica").exists());
			assertThat(jdbc.queryForObject("SELECT count(*) FROM usuarios", Integer.class)).isZero();
		}

		@Test
		void guardaCuandoYConQueVersionDeLaPoliticaAutorizo() throws Exception {
			registrar("ana@correo.com");
			var fila = jdbc.queryForMap("SELECT autorizo_datos_en, version_politica FROM usuarios WHERE email = 'ana@correo.com'");
			assertThat(fila.get("autorizo_datos_en")).isNotNull();
			assertThat(fila.get("version_politica")).isEqualTo(AuthService.VERSION_POLITICA);
		}

		@Test
		void misDatosTraeLoPropioSinElHashNiLoDeOtros() throws Exception {
			String ana = registrar("ana@correo.com");
			String pedro = registrar("pedro@correo.com");
			pedir(POST, "/api/cuentas", ana, """
					{"nombre": "Nequi de Ana", "tipo": "BILLETERA_DIGITAL", "saldoInicial": 1000}""");
			pedir(POST, "/api/cuentas", pedro, """
					{"nombre": "Banco de Pedro", "tipo": "BANCO", "saldoInicial": 5000}""");

			pedir(GET, "/api/auth/mis-datos", ana)
					.andExpect(status().isOk())
					.andExpect(header().string("Content-Disposition", containsString("attachment")))
					.andExpect(jsonPath("$.persona.email").value("ana@correo.com"))
					.andExpect(jsonPath("$.persona.password_hash").doesNotExist())
					.andExpect(jsonPath("$.cuentas.length()").value(1))
					.andExpect(jsonPath("$.cuentas[0].nombre").value("Nequi de Ana"));
		}

		@Test
		void borrarLaCuentaEliminaTodoLoSuyoYCierraLaSesion() throws Exception {
			String ana = registrar("ana@correo.com");
			String pedro = registrar("pedro@correo.com");
			pedir(POST, "/api/cuentas", ana, """
					{"nombre": "Efectivo", "tipo": "EFECTIVO", "saldoInicial": 0}""");
			pedir(POST, "/api/cuentas", pedro, """
					{"nombre": "Efectivo", "tipo": "EFECTIVO", "saldoInicial": 0}""");

			pedir(DELETE, "/api/auth/cuenta", ana, """
					{"password": "equivocada"}""").andExpect(status().isUnprocessableContent());

			pedir(DELETE, "/api/auth/cuenta", ana, """
					{"password": "clave-segura-123"}""")
					.andExpect(status().isNoContent())
					.andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

			assertThat(jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE email = 'ana@correo.com'", Integer.class)).isZero();
			assertThat(jdbc.queryForObject("SELECT count(*) FROM cuentas", Integer.class)).isEqualTo(1);
			pedir(GET, "/api/auth/yo", ana).andExpect(status().isUnauthorized());
			pedir(GET, "/api/auth/yo", pedro).andExpect(status().isOk());
		}

		@Test
		void laCuentaDeDemostracionNoSePuedeBorrarNiCambiarDeClave() throws Exception {
			String demo = registrar(DatosDeDemo.EMAIL);
			pedir(DELETE, "/api/auth/cuenta", demo, """
					{"password": "clave-segura-123"}""").andExpect(status().isForbidden());
			pedir(PUT, "/api/auth/password", demo, """
					{"actual": "clave-segura-123", "nueva": "otra-clave-456"}""").andExpect(status().isForbidden());
		}

	}

	@Nested
	@DisplayName("Cambio de contraseña")
	class CambioDeContrasena {

		@Test
		void cambiarLaClaveCierraLasDemasSesionesYAbreUnaNueva() throws Exception {
			String anterior = registrar("ana@correo.com");

			pedir(PUT, "/api/auth/password", anterior, """
					{"actual": "equivocada", "nueva": "otra-clave-456"}""").andExpect(status().isUnprocessableContent());

			String nueva = Cookies.tokenDe(pedir(PUT, "/api/auth/password", anterior, """
					{"actual": "clave-segura-123", "nueva": "otra-clave-456"}""").andExpect(status().isOk()));

			pedir(GET, "/api/auth/yo", anterior).andExpect(status().isUnauthorized());
			pedir(GET, "/api/auth/yo", nueva).andExpect(status().isOk());
			login("ana@correo.com", "clave-segura-123").andExpect(status().isUnauthorized());
			login("ana@correo.com", "otra-clave-456").andExpect(status().isOk());
		}

	}

	@Nested
	@DisplayName("Límite de intentos")
	class LimiteDeIntentos {

		@Test
		void diezFallosBloqueanEsaCuentaAunqueLuegoAcierte() throws Exception {
			registrar("limite@correo.com");
			registrar("vecina@correo.com");
			for (int i = 0; i < LimitadorDeIntentos.MAXIMO; i++) {
				login("limite@correo.com", "equivocada-" + i).andExpect(status().isUnauthorized());
			}
			login("limite@correo.com", "clave-segura-123")
					.andExpect(status().isTooManyRequests())
					.andExpect(jsonPath("$.detail", not(containsString("contraseña incorrecta"))));
			// Otra cuenta no se ve afectada.
			login("vecina@correo.com", "clave-segura-123").andExpect(status().isOk());
		}

		@Test
		void unIngresoCorrectoReiniciaLaCuenta() throws Exception {
			registrar("reinicio@correo.com");
			for (int i = 0; i < LimitadorDeIntentos.MAXIMO - 1; i++) {
				login("reinicio@correo.com", "equivocada").andExpect(status().isUnauthorized());
			}
			login("reinicio@correo.com", "clave-segura-123").andExpect(status().isOk());
			login("reinicio@correo.com", "equivocada").andExpect(status().isUnauthorized());
			login("reinicio@correo.com", "clave-segura-123").andExpect(status().isOk());
		}

	}

}
