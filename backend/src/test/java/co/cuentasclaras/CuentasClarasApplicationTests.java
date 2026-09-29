package co.cuentasclaras;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CuentasClarasApplicationTests {

	@Autowired
	MockMvc mockMvc;

	@Test
	void laAplicacionArrancaYAplicaLasMigraciones() {
		// Si Flyway o el mapeo de JPA fallan, el contexto no carga y esta prueba falla.
	}

	@Test
	void elHealthCheckEsPublicoYRespondeUp() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void cualquierOtraRutaExigeAutenticacion() throws Exception {
		mockMvc.perform(get("/api/cuentas"))
				.andExpect(status().isUnauthorized());
	}

}
