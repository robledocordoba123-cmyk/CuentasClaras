package co.cuentasclaras.deuda;

import co.cuentasclaras.soporte.PruebaDeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeudaControllerTest extends PruebaDeApi {

	private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Bogota"));

	private String ana;
	private String valentina;

	@BeforeEach
	void preparar() throws Exception {
		ana = registrar("ana@correo.com");
		valentina = idDe(pedir(POST, "/api/personas", ana, """
				{"nombre": "Valentina"}"""));
	}

	private String deuda(String direccion, int monto) throws Exception {
		return idDe(pedir(POST, "/api/deudas", ana, """
				{"personaId": "%s", "direccion": "%s", "monto": %d, "descripcion": "Almuerzo", "fecha": "%s"}
				""".formatted(valentina, direccion, monto, HOY)).andExpect(status().isCreated()));
	}

	private org.springframework.test.web.servlet.ResultActions abonar(String deudaId, int monto) throws Exception {
		return pedir(POST, "/api/deudas/" + deudaId + "/abonos", ana, """
				{"monto": %d, "fecha": "%s"}""".formatted(monto, HOY));
	}

	@Nested
	@DisplayName("Quién debe a quién")
	class Saldos {

		@Test
		void elNetoPorPersonaRestaLoQueLeDebesDeLoQueTeDebe() throws Exception {
			String almuerzo = deuda("ME_DEBEN", 60000);
			abonar(almuerzo, 20000).andExpect(status().isCreated())
					.andExpect(jsonPath("$.pendiente").value(40000))
					.andExpect(jsonPath("$.saldada").value(false));
			deuda("DEBO", 15000);

			pedir(GET, "/api/personas", ana)
					.andExpect(jsonPath("$[0].teDebe").value(40000))
					.andExpect(jsonPath("$[0].leDebes").value(15000))
					.andExpect(jsonPath("$[0].neto").value(25000))
					.andExpect(jsonPath("$[0].deudasPendientes").value(2));
		}

		@Test
		void abonarLoQueFaltaLaSaldaYQuitarElAbonoLaReabre() throws Exception {
			String almuerzo = deuda("ME_DEBEN", 60000);
			abonar(almuerzo, 70000).andExpect(status().isUnprocessableContent())
					.andExpect(jsonPath("$.detail", containsString("$ 60.000")));
			String abono = com.jayway.jsonpath.JsonPath.read(abonar(almuerzo, 60000)
					.andExpect(jsonPath("$.saldada").value(true))
					.andReturn().getResponse().getContentAsString(), "$.abonos[0].id");
			abonar(almuerzo, 1000).andExpect(status().isUnprocessableContent());

			pedir(DELETE, "/api/deudas/" + almuerzo + "/abonos/" + abono, ana)
					.andExpect(jsonPath("$.saldada").value(false))
					.andExpect(jsonPath("$.pendiente").value(60000));
		}

		@Test
		void cincoAbonosSimultaneosNuncaSuperanLaDeuda() throws Exception {
			String prestamo = deuda("ME_DEBEN", 100000);
			ExecutorService hilos = Executors.newFixedThreadPool(5);
			List<Callable<Integer>> tareas = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				tareas.add(() -> abonar(prestamo, 30000).andReturn().getResponse().getStatus());
			}
			List<Integer> estados = new ArrayList<>();
			for (Future<Integer> f : hilos.invokeAll(tareas)) {
				estados.add(f.get());
			}
			hilos.shutdown();

			assertThat(estados).filteredOn(e -> e == 201).hasSize(3);
			BigDecimal abonado = jdbc.queryForObject("SELECT SUM(monto) FROM abonos", BigDecimal.class);
			assertThat(abonado).isEqualByComparingTo("90000");
		}

	}

	@Nested
	@DisplayName("Reglas")
	class Reglas {

		@Test
		void noRepiteNombresNiBorraAQuienTieneDeudas() throws Exception {
			pedir(POST, "/api/personas", ana, """
					{"nombre": "  valentina "}""").andExpect(status().isConflict());
			deuda("DEBO", 5000);
			pedir(DELETE, "/api/personas/" + valentina, ana).andExpect(status().isUnprocessableContent());
			String otra = idDe(pedir(POST, "/api/personas", ana, """
					{"nombre": "Andrés"}"""));
			pedir(DELETE, "/api/personas/" + otra, ana).andExpect(status().isNoContent());
		}

		@Test
		void noAceptaFechasFuturasNiAbonosAntesDeLaDeuda() throws Exception {
			pedir(POST, "/api/deudas", ana, """
					{"personaId": "%s", "direccion": "DEBO", "monto": 1000, "descripcion": "x", "fecha": "%s"}
					""".formatted(valentina, HOY.plusDays(1))).andExpect(status().isUnprocessableContent());
			String d = deuda("DEBO", 1000);
			pedir(POST, "/api/deudas/" + d + "/abonos", ana, """
					{"monto": 500, "fecha": "%s"}""".formatted(HOY.minusDays(1))).andExpect(status().isUnprocessableContent());
		}

		@Test
		void otraPersonaNoVeNiAbonaDeudasAjenas() throws Exception {
			String d = deuda("ME_DEBEN", 1000);
			String pedro = registrar("pedro@correo.com");
			pedir(GET, "/api/deudas", pedro).andExpect(jsonPath("$.length()").value(0));
			pedir(POST, "/api/deudas/" + d + "/abonos", pedro, """
					{"monto": 500, "fecha": "%s"}""".formatted(HOY)).andExpect(status().isNotFound());
			pedir(POST, "/api/deudas", pedro, """
					{"personaId": "%s", "direccion": "DEBO", "monto": 1000, "descripcion": "x", "fecha": "%s"}
					""".formatted(valentina, HOY)).andExpect(status().isNotFound());
		}

	}

}
