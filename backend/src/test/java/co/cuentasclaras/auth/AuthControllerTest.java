package co.cuentasclaras.auth;

import co.cuentasclaras.TestcontainersConfiguration;
import co.cuentasclaras.soporte.LimpiadorBD;
import co.cuentasclaras.usuario.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	UsuarioRepository usuarioRepository;

	@Autowired
	LimpiadorBD limpiadorBD;

	@BeforeEach
	void limpiar() {
		limpiadorBD.limpiar();
	}

	private ResultActions registrar(String nombre, String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/registro")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre": "%s", "email": "%s", "password": "%s"}
						""".formatted(nombre, email, password)));
	}

	private ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "%s", "password": "%s"}
						""".formatted(email, password)));
	}

	private String tokenDe(ResultActions respuesta) throws Exception {
		return JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(), "$.token");
	}

	@Nested
	@DisplayName("HU-01: registro")
	class Registro {

		@Test
		void registraYDevuelveTokenSinExponerElHash() throws Exception {
			registrar("Ana Gómez", "ana@correo.com", "clave-segura-123")
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.token").value(not(emptyString())))
					.andExpect(jsonPath("$.tipo").value("Bearer"))
					.andExpect(jsonPath("$.usuario.email").value("ana@correo.com"))
					.andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());
		}

		@Test
		void guardaLaContrasenaConHashBCryptNuncaEnTextoPlano() throws Exception {
			registrar("Ana", "ana@correo.com", "clave-segura-123");

			String hash = usuarioRepository.findByEmail("ana@correo.com").orElseThrow().getPasswordHash();
			assertThat(hash).isNotEqualTo("clave-segura-123").startsWith("$2a$");
		}

		@Test
		void elCorreoNoDistingueMayusculasNiEspacios() throws Exception {
			registrar("Ana", "ana@correo.com", "clave-segura-123").andExpect(status().isCreated());

			registrar("Otra Ana", "  ANA@Correo.com ", "otra-clave-123")
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.detail").value("Ya existe una cuenta registrada con ese correo."));
		}

		@Test
		void rechazaDatosInvalidosIndicandoCadaCampo() throws Exception {
			registrar("", "no-es-un-correo", "123")
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.title").value("Datos inválidos"))
					.andExpect(jsonPath("$.errores.nombre").exists())
					.andExpect(jsonPath("$.errores.email").exists())
					.andExpect(jsonPath("$.errores.password").exists());
		}

		@Test
		void rechazaUnJsonMalFormadoCon400() throws Exception {
			mockMvc.perform(post("/api/auth/registro")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{esto no es json"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.title").value("Solicitud mal formada"));
		}

	}

	@Nested
	@DisplayName("Inicio de sesión")
	class Login {

		@BeforeEach
		void crearUsuaria() throws Exception {
			registrar("Ana", "ana@correo.com", "clave-segura-123");
		}

		@Test
		void conCredencialesCorrectasDevuelveToken() throws Exception {
			login("ANA@correo.com", "clave-segura-123")
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.token").value(not(emptyString())));
		}

		@Test
		void conContrasenaIncorrectaResponde401() throws Exception {
			login("ana@correo.com", "equivocada")
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
		}

		@Test
		void conCorreoInexistenteRespondeExactamenteIgualQueConContrasenaIncorrecta() throws Exception {
			// Mismo código y mismo mensaje: no se revela si el correo está registrado.
			login("nadie@correo.com", "clave-segura-123")
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
		}

	}

	@Nested
	@DisplayName("Rutas protegidas con JWT")
	class RutasProtegidas {

		@Test
		void conTokenValidoDevuelveElPerfilDeSuDueno() throws Exception {
			String token = tokenDe(registrar("Ana", "ana@correo.com", "clave-segura-123"));

			mockMvc.perform(get("/api/auth/yo").header("Authorization", "Bearer " + token))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.nombre").value("Ana"))
					.andExpect(jsonPath("$.email").value("ana@correo.com"));
		}

		@Test
		void sinTokenResponde401() throws Exception {
			mockMvc.perform(get("/api/auth/yo"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void conTokenAlteradoResponde401() throws Exception {
			String token = tokenDe(registrar("Ana", "ana@correo.com", "clave-segura-123"));
			// Cambiar un solo carácter de la firma invalida el token.
			String alterado = token.substring(0, token.length() - 2)
					+ (token.endsWith("A") ? "BB" : "AA");

			mockMvc.perform(get("/api/auth/yo").header("Authorization", "Bearer " + alterado))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void conTokenFirmadoPorOtroResponde401() throws Exception {
			// Token con la estructura correcta pero firmado con otra clave.
			String ajeno = "eyJhbGciOiJIUzI1NiJ9."
					+ "eyJpc3MiOiJjdWVudGFzY2xhcmFzIiwic3ViIjoiMDAwMDAwMDAtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMDAxIiwiZXhwIjo0MTAyNDQ0ODAwfQ."
					+ "c29sby11bmEtZmlybWEtZmFsc2EtcXVlLW5vLXNpcnZlLXBhcmEtbmFkYQ";

			mockMvc.perform(get("/api/auth/yo").header("Authorization", "Bearer " + ajeno))
					.andExpect(status().isUnauthorized());
		}

	}

}
