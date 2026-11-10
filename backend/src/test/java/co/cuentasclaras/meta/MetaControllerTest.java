package co.cuentasclaras.meta;

import co.cuentasclaras.soporte.PruebaDeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MetaControllerTest extends PruebaDeApi {

	private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Bogota"));

	private String ana;

	@BeforeEach
	void preparar() throws Exception {
		ana = registrar("ana@correo.com");
	}

	private String meta(int objetivo, LocalDate fecha) throws Exception {
		return idDe(pedir(POST, "/api/metas", ana, """
				{"nombre": "Viaje", "montoObjetivo": %d, "fechaObjetivo": %s}
				""".formatted(objetivo, fecha == null ? "null" : "\"" + fecha + "\"")).andExpect(status().isCreated()));
	}

	private org.springframework.test.web.servlet.ResultActions aportar(String meta, int monto) throws Exception {
		return pedir(POST, "/api/metas/" + meta + "/aportes", ana, """
				{"monto": %d, "fecha": "%s"}""".formatted(monto, HOY));
	}

	@Test
	void reparteLoQueFaltaEntreLosMesesQueQuedan() throws Exception {
		// Tres meses contando el actual: 1.800.000 / 3 = 600.000.
		String viaje = meta(1_800_000, YearMonth.from(HOY).plusMonths(2).atEndOfMonth());
		pedir(GET, "/api/metas", ana).andExpect(jsonPath("$[0].aporteMensual").value(600000));
		aportar(viaje, 300_000).andExpect(status().isCreated())
				.andExpect(jsonPath("$.ahorrado").value(300000))
				.andExpect(jsonPath("$.falta").value(1500000))
				.andExpect(jsonPath("$.aporteMensual").value(500000));
	}

	@Test
	void sinFechaNoHaySugerenciaYAlLlegarSeCumple() throws Exception {
		String fondo = meta(500_000, null);
		aportar(fondo, 500_000)
				.andExpect(jsonPath("$.aporteMensual").doesNotExist())
				.andExpect(jsonPath("$.porcentaje").value(100.0))
				.andExpect(jsonPath("$.cumplida").value(true));
	}

	@Test
	void noSePuedeSacarMasDeLoAhorrado() throws Exception {
		String fondo = meta(500_000, null);
		aportar(fondo, 100_000);
		aportar(fondo, -150_000).andExpect(status().isUnprocessableContent());
		aportar(fondo, -100_000).andExpect(jsonPath("$.ahorrado").value(0));
		aportar(fondo, 0).andExpect(status().isUnprocessableContent());
	}

	@Test
	void validaLaFechaObjetivoYLoAjeno() throws Exception {
		pedir(POST, "/api/metas", ana, """
				{"nombre": "Viaje", "montoObjetivo": 1000, "fechaObjetivo": "%s"}""".formatted(HOY.minusDays(1)))
				.andExpect(status().isUnprocessableContent());
		String fondo = meta(500_000, null);
		String pedro = registrar("pedro@correo.com");
		pedir(POST, "/api/metas/" + fondo + "/aportes", pedro, """
				{"monto": 1000, "fecha": "%s"}""".formatted(HOY)).andExpect(status().isNotFound());
		pedir(DELETE, "/api/metas/" + fondo, pedro).andExpect(status().isNotFound());
	}

	@Test
	void borrarLaCuentaBorraTambienDeudasPagosFijosYMetas() throws Exception {
		String fondo = meta(500_000, null);
		aportar(fondo, 1000);
		String persona = idDe(pedir(POST, "/api/personas", ana, """
				{"nombre": "Valentina"}"""));
		pedir(POST, "/api/deudas", ana, """
				{"personaId": "%s", "direccion": "DEBO", "monto": 1000, "descripcion": "x", "fecha": "%s"}
				""".formatted(persona, HOY));
		pedir(GET, "/api/auth/mis-datos", ana)
				.andExpect(jsonPath("$.metas.length()").value(1))
				.andExpect(jsonPath("$.deudas.length()").value(1));

		pedir(DELETE, "/api/auth/cuenta", ana, """
				{"password": "clave-segura-123"}""").andExpect(status().isNoContent());
		for (String tabla : new String[] { "metas", "aportes", "personas", "deudas" }) {
			assertThat(jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class)).as(tabla).isZero();
		}
	}

}
