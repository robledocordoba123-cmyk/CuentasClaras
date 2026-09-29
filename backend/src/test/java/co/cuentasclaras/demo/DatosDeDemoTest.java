package co.cuentasclaras.demo;

import co.cuentasclaras.soporte.PruebaDeApi;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({ "local", "demo" })
class DatosDeDemoTest extends PruebaDeApi {

	@Autowired
	DatosDeDemo datosDeDemo;

	private String loginDemo() throws Exception {
		String respuesta = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "%s", "password": "%s"}
								""".formatted(DatosDeDemo.EMAIL, DatosDeDemo.PASSWORD)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(respuesta, "$.token");
	}

	@Test
	void laCuentaDemoTieneTresCuentasMovimientosYPresupuestosEnLosTresEstados() throws Exception {
		datosDeDemo.run(null);
		String token = loginDemo();

		pedir(GET, "/api/cuentas", token).andExpect(jsonPath("$.length()").value(3));
		pedir(GET, "/api/movimientos", token).andExpect(jsonPath("$.totalElementos", greaterThan(30)));

		String presupuestos = pedir(GET, "/api/presupuestos", token).andReturn().getResponse().getContentAsString();
		List<String> estados = JsonPath.read(presupuestos, "$[*].estado");
		assertThat(estados).contains("EN_CONTROL", "EXCEDIDO");
	}

	@Test
	void reiniciarLaDemoNoDuplicaDatosNiTocaAOtrasPersonas() throws Exception {
		String tokenAna = registrar("ana@correo.com");
		pedir(org.springframework.http.HttpMethod.POST, "/api/cuentas", tokenAna, """
				{"nombre": "Cuenta de Ana", "tipo": "BANCO", "saldoInicial": 1000}
				""");

		datosDeDemo.run(null);
		Integer movimientosPrimeraVez = jdbc.queryForObject("SELECT COUNT(*) FROM movimientos", Integer.class);
		datosDeDemo.run(null);

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM movimientos", Integer.class)).isEqualTo(movimientosPrimeraVez);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class)).isEqualTo(2);
		pedir(GET, "/api/cuentas", tokenAna).andExpect(jsonPath("$[0].nombre").value("Cuenta de Ana"));
	}

}
