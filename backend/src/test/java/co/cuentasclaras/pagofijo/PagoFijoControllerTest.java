package co.cuentasclaras.pagofijo;

import co.cuentasclaras.soporte.PruebaDeApi;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PagoFijoControllerTest extends PruebaDeApi {

	private static final UUID CATEGORIA_ARRIENDO = UUID.fromString("00000000-0000-0000-0000-000000000203");
	private static final YearMonth MES = YearMonth.now(ZoneId.of("America/Bogota"));

	private String ana;
	private String banco;
	private String arriendo;

	@BeforeEach
	void preparar() throws Exception {
		ana = registrar("ana@correo.com");
		banco = idDe(pedir(POST, "/api/cuentas", ana, """
				{"nombre": "Banco", "tipo": "BANCO", "saldoInicial": 2000000}"""));
		arriendo = idDe(pedir(POST, "/api/pagos-fijos", ana, """
				{"nombre": "Arriendo", "monto": 950000, "diaDelMes": 31, "categoriaId": "%s", "cuentaId": "%s"}
				""".formatted(CATEGORIA_ARRIENDO, banco)).andExpect(status().isCreated()));
	}

	@Test
	void pagarCreaElGastoYNoDejaPagarDosVecesElMismoMes() throws Exception {
		YearMonth pasado = MES.minusMonths(1);
		pedir(GET, "/api/pagos-fijos?mes=" + pasado, ana).andExpect(jsonPath("$[0].estado").value("VENCIDO"));

		String movimiento = idDe(pedir(POST, "/api/pagos-fijos/" + arriendo + "/pagar", ana, """
				{"mes": "%s"}""".formatted(pasado)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("GASTO"))
				.andExpect(jsonPath("$.monto").value(950000))
				.andExpect(jsonPath("$.fecha").value(pasado.atEndOfMonth().toString())));

		pedir(GET, "/api/pagos-fijos?mes=" + pasado, ana)
				.andExpect(jsonPath("$[0].estado").value("PAGADO"))
				.andExpect(jsonPath("$[0].movimientoId").value(movimiento));
		pedir(GET, "/api/cuentas/" + banco, ana).andExpect(jsonPath("$.saldoActual").value(1050000));
		pedir(POST, "/api/pagos-fijos/" + arriendo + "/pagar", ana, """
				{"mes": "%s"}""".formatted(pasado)).andExpect(status().isConflict());

		// Borrar el gasto deja el pago pendiente otra vez.
		pedir(DELETE, "/api/movimientos/" + movimiento, ana).andExpect(status().isNoContent());
		pedir(GET, "/api/pagos-fijos?mes=" + pasado, ana).andExpect(jsonPath("$[0].estado").value("VENCIDO"));
	}

	@Test
	void unPagoDelDia31VenceElUltimoDiaDeLosMesesCortos() throws Exception {
		pedir(GET, "/api/pagos-fijos?mes=2027-02", ana).andExpect(jsonPath("$[0].vence").value("2027-02-28"));
		pedir(GET, "/api/pagos-fijos?mes=2028-02", ana).andExpect(jsonPath("$[0].vence").value("2028-02-29"));
		pedir(GET, "/api/pagos-fijos?mes=" + MES.plusMonths(1), ana).andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
	}

	@Test
	void rechazaCategoriasDeIngresoFechasFuturasYFechasDeOtroMes() throws Exception {
		pedir(POST, "/api/pagos-fijos", ana, """
				{"nombre": "Sueldo", "monto": 1, "diaDelMes": 1, "categoriaId": "%s", "cuentaId": "%s"}
				""".formatted(CATEGORIA_SALARIO, banco)).andExpect(status().isUnprocessableContent());
		pedir(POST, "/api/pagos-fijos/" + arriendo + "/pagar", ana, """
				{"mes": "%s"}""".formatted(MES.plusMonths(1))).andExpect(status().isUnprocessableContent());
		LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));
		pedir(POST, "/api/pagos-fijos/" + arriendo + "/pagar", ana, """
				{"mes": "%s", "fecha": "%s"}""".formatted(MES, hoy.minusMonths(2))).andExpect(status().isUnprocessableContent());
	}

	@Test
	void conPagosSeDesactivaYSinPagosSeBorra() throws Exception {
		pedir(POST, "/api/pagos-fijos/" + arriendo + "/pagar", ana, """
				{"mes": "%s"}""".formatted(MES.minusMonths(1))).andExpect(status().isCreated());
		pedir(DELETE, "/api/pagos-fijos/" + arriendo, ana).andExpect(status().isOk())
				.andExpect(jsonPath("$.desactivado").value(true));
		// Sigue apareciendo en el mes en que se pagó, pero no en los demás.
		pedir(GET, "/api/pagos-fijos?mes=" + MES.minusMonths(1), ana).andExpect(jsonPath("$.length()").value(1));
		pedir(GET, "/api/pagos-fijos?mes=" + MES, ana).andExpect(jsonPath("$.length()").value(0));

		String internet = idDe(pedir(POST, "/api/pagos-fijos", ana, """
				{"nombre": "Internet", "monto": 89900, "diaDelMes": 20, "categoriaId": "%s", "cuentaId": "%s"}
				""".formatted(CATEGORIA_ARRIENDO, banco)));
		pedir(DELETE, "/api/pagos-fijos/" + internet, ana).andExpect(status().isNoContent());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM pagos_fijos", Integer.class)).isEqualTo(1);
	}

	@Test
	void otraPersonaNoPuedePagarLoAjeno() throws Exception {
		String pedro = registrar("pedro@correo.com");
		pedir(POST, "/api/pagos-fijos/" + arriendo + "/pagar", pedro, "{}").andExpect(status().isNotFound());
		String id = JsonPath.read(pedir(GET, "/api/pagos-fijos", pedro).andReturn().getResponse().getContentAsString(), "$.length()").toString();
		assertThat(id).isEqualTo("0");
	}

}
