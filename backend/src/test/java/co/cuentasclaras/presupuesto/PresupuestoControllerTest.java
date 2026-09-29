package co.cuentasclaras.presupuesto;

import co.cuentasclaras.soporte.PruebaDeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PresupuestoControllerTest extends PruebaDeApi {

	private String tokenAna;
	private String cuenta;

	@BeforeEach
	void preparar() throws Exception {
		tokenAna = registrar("ana@correo.com");
		cuenta = idDe(pedir(POST, "/api/cuentas", tokenAna, """
				{"nombre": "Nequi", "tipo": "BILLETERA_DIGITAL", "saldoInicial": 1000000}
				"""));
	}

	private ResultActions definir(String limite) throws Exception {
		return pedir(PUT, "/api/presupuestos", tokenAna, """
				{"categoriaId": "%s", "mes": "2026-09", "montoLimite": %s}
				""".formatted(CATEGORIA_MERCADO, limite));
	}

	private void gastarEnMercado(String monto, String fecha) throws Exception {
		pedir(POST, "/api/movimientos", tokenAna, """
				{"cuentaId": "%s", "categoriaId": "%s", "tipo": "GASTO", "monto": %s, "fecha": "%s"}
				""".formatted(cuenta, CATEGORIA_MERCADO, monto, fecha)).andExpect(status().isCreated());
	}

	@Test
	void hu06_definirUnPresupuestoMuestraLoGastadoYLoDisponible() throws Exception {
		gastarEnMercado("150000", "2026-09-10");

		definir("400000")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.categoria.nombre").value("Mercado"))
				.andExpect(jsonPath("$.gastado").value(150000))
				.andExpect(jsonPath("$.disponible").value(250000))
				.andExpect(jsonPath("$.porcentajeUsado").value(37.5))
				.andExpect(jsonPath("$.estado").value("EN_CONTROL"));
	}

	@Test
	void rn08_definirDosVecesElMismoMesActualizaElLimiteEnVezDeDuplicar() throws Exception {
		definir("400000");
		definir("300000").andExpect(jsonPath("$.montoLimite").value(300000));

		pedir(GET, "/api/presupuestos?mes=2026-09", tokenAna)
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].montoLimite").value(300000));
	}

	@Test
	void hu07_elEstadoPasaAAlertaDesdeEl80YAExcedidoDesdeEl100() throws Exception {
		definir("100000");

		gastarEnMercado("79000", "2026-09-05");
		pedir(GET, "/api/presupuestos?mes=2026-09", tokenAna).andExpect(jsonPath("$[0].estado").value("EN_CONTROL"));

		gastarEnMercado("1000", "2026-09-06"); // 80 %
		pedir(GET, "/api/presupuestos?mes=2026-09", tokenAna).andExpect(jsonPath("$[0].estado").value("ALERTA"));

		gastarEnMercado("25000", "2026-09-07"); // 105 %
		pedir(GET, "/api/presupuestos?mes=2026-09", tokenAna)
				.andExpect(jsonPath("$[0].estado").value("EXCEDIDO"))
				.andExpect(jsonPath("$[0].disponible").value(-5000));
	}

	@Test
	void soloCuentaLoGastadoDentroDelMes() throws Exception {
		definir("100000");
		gastarEnMercado("50000", "2026-08-31");
		gastarEnMercado("10000", "2026-09-30");
		gastarEnMercado("70000", "2026-10-01");

		pedir(GET, "/api/presupuestos?mes=2026-09", tokenAna).andExpect(jsonPath("$[0].gastado").value(10000));
	}

	@Test
	void noSePuedePresupuestarUnaCategoriaDeIngresoNiUnMesMalEscrito() throws Exception {
		pedir(PUT, "/api/presupuestos", tokenAna, """
				{"categoriaId": "%s", "mes": "2026-09", "montoLimite": 1000}
				""".formatted(CATEGORIA_SALARIO)).andExpect(status().isUnprocessableContent());

		pedir(PUT, "/api/presupuestos", tokenAna, """
				{"categoriaId": "%s", "mes": "2026-13", "montoLimite": 1000}
				""".formatted(CATEGORIA_MERCADO))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.mes").exists());

		pedir(GET, "/api/presupuestos?mes=septiembre", tokenAna).andExpect(status().isBadRequest());
	}

	@Test
	void rn01_otraPersonaNoVeNiBorraElPresupuesto() throws Exception {
		String presupuesto = idDe(definir("100000"));
		String tokenBeto = registrar("beto@correo.com");

		pedir(GET, "/api/presupuestos?mes=2026-09", tokenBeto).andExpect(jsonPath("$", hasSize(0)));
		pedir(DELETE, "/api/presupuestos/" + presupuesto, tokenBeto).andExpect(status().isNotFound());
		pedir(DELETE, "/api/presupuestos/" + presupuesto, tokenAna).andExpect(status().isNoContent());
	}

}
