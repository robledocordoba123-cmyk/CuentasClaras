package co.cuentasclaras.soporte;

import co.cuentasclaras.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Base para las pruebas de la API: arranca la aplicación contra PostgreSQL en
 * Docker, limpia la base antes de cada prueba y trae atajos para no repetir
 * el mismo código de registro y peticiones en cada clase.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public abstract class PruebaDeApi {

	/** Categorías por defecto sembradas en V1__esquema_inicial.sql. */
	protected static final UUID CATEGORIA_SALARIO = UUID.fromString("00000000-0000-0000-0000-000000000101");
	protected static final UUID CATEGORIA_MERCADO = UUID.fromString("00000000-0000-0000-0000-000000000201");

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected JdbcTemplate jdbc;

	@Autowired
	private LimpiadorBD limpiadorBD;

	@BeforeEach
	void baseLimpia() {
		limpiadorBD.limpiar();
	}

	/** Registra a una persona y devuelve su token. */
	protected String registrar(String email) throws Exception {
		String respuesta = mockMvc.perform(post("/api/auth/registro")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombre": "Persona de prueba", "email": "%s", "password": "clave-segura-123"}
								""".formatted(email)))
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(respuesta, "$.token");
	}

	protected ResultActions pedir(HttpMethod metodo, String ruta, String token) throws Exception {
		return pedir(metodo, ruta, token, null);
	}

	protected ResultActions pedir(HttpMethod metodo, String ruta, String token, String cuerpoJson) throws Exception {
		var peticion = request(metodo, ruta).header("Authorization", "Bearer " + token);
		if (cuerpoJson != null) {
			peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpoJson);
		}
		return mockMvc.perform(peticion);
	}

	protected static String idDe(ResultActions respuesta) throws Exception {
		return JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(), "$.id");
	}

	/**
	 * Inserta un movimiento directo en la base. El módulo de movimientos todavía
	 * no existe; esto permite probar el saldo calculado y el archivado desde ya.
	 */
	protected void insertarMovimiento(String cuentaId, UUID categoriaId, String tipo, String monto) {
		UUID usuarioId = jdbc.queryForObject("SELECT usuario_id FROM cuentas WHERE id = ?", UUID.class,
				UUID.fromString(cuentaId));
		jdbc.update("""
				INSERT INTO movimientos (id, usuario_id, cuenta_id, categoria_id, tipo, monto, fecha)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""", UUID.randomUUID(), usuarioId, UUID.fromString(cuentaId), categoriaId, tipo,
				new BigDecimal(monto), LocalDate.now());
	}

}
