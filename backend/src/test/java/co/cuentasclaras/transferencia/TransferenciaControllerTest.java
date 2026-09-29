package co.cuentasclaras.transferencia;

import co.cuentasclaras.soporte.PruebaDeApi;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferenciaControllerTest extends PruebaDeApi {

	private String tokenAna;
	private String banco;
	private String efectivo;

	@BeforeEach
	void preparar() throws Exception {
		tokenAna = registrar("ana@correo.com");
		banco = crearCuenta(tokenAna, "Bancolombia", "BANCO", "500000");
		efectivo = crearCuenta(tokenAna, "Efectivo", "EFECTIVO", "20000");
	}

	private String crearCuenta(String token, String nombre, String tipo, String saldo) throws Exception {
		return idDe(pedir(POST, "/api/cuentas", token, """
				{"nombre": "%s", "tipo": "%s", "saldoInicial": %s}
				""".formatted(nombre, tipo, saldo)));
	}

	private ResultActions transferir(String token, String origen, String destino, String monto) throws Exception {
		return pedir(POST, "/api/transferencias", token, """
				{"cuentaOrigenId": "%s", "cuentaDestinoId": "%s", "monto": %s, "fecha": "2026-09-20", "descripcion": "Retiro cajero"}
				""".formatted(origen, destino, monto));
	}

	private Integer contarMovimientos() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM movimientos", Integer.class);
	}

	@Test
	void hu04_retirarDelBancoAEfectivoMueveLaPlataEntreLasDosCuentas() throws Exception {
		transferir(tokenAna, banco, efectivo, "50000")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.origen.nombre").value("Bancolombia"))
				.andExpect(jsonPath("$.destino.nombre").value("Efectivo"))
				.andExpect(jsonPath("$.monto").value(50000));

		pedir(GET, "/api/cuentas/" + banco, tokenAna).andExpect(jsonPath("$.saldoActual").value(450000));
		pedir(GET, "/api/cuentas/" + efectivo, tokenAna).andExpect(jsonPath("$.saldoActual").value(70000));
	}

	@Test
	void rn05_seGuardanExactamenteDosMovimientosUnidosPorElMismoId() throws Exception {
		transferir(tokenAna, banco, efectivo, "50000");

		Integer partes = jdbc.queryForObject(
				"SELECT COUNT(DISTINCT transferencia_id) FROM movimientos WHERE transferencia_id IS NOT NULL",
				Integer.class);
		org.assertj.core.api.Assertions.assertThat(contarMovimientos()).isEqualTo(2);
		org.assertj.core.api.Assertions.assertThat(partes).isEqualTo(1);
	}

	@Test
	void rechazaTransferirAUnaMismaCuentaSinGuardarNada() throws Exception {
		transferir(tokenAna, banco, banco, "1000").andExpect(status().isUnprocessableContent());
		org.assertj.core.api.Assertions.assertThat(contarMovimientos()).isZero();
	}

	@Test
	void rn01_noSePuedeTransferirHaciaNiDesdeUnaCuentaAjena() throws Exception {
		String tokenBeto = registrar("beto@correo.com");
		String cuentaBeto = crearCuenta(tokenBeto, "Nequi Beto", "BILLETERA_DIGITAL", "0");

		// Ana intenta mandar plata a la cuenta de Beto, y Beto intenta sacar de la de Ana.
		transferir(tokenAna, banco, cuentaBeto, "1000").andExpect(status().isNotFound());
		transferir(tokenBeto, banco, cuentaBeto, "1000").andExpect(status().isNotFound());
		org.assertj.core.api.Assertions.assertThat(contarMovimientos()).isZero();
	}

	@Test
	void eliminarLaTransferenciaBorraLasDosPartesYRestauraLosSaldos() throws Exception {
		String respuesta = transferir(tokenAna, banco, efectivo, "50000").andReturn().getResponse().getContentAsString();
		String transferenciaId = JsonPath.read(respuesta, "$.transferenciaId");

		pedir(GET, "/api/transferencias/" + transferenciaId, tokenAna)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.descripcion").value("Retiro cajero"));

		pedir(DELETE, "/api/transferencias/" + transferenciaId, tokenAna).andExpect(status().isNoContent());
		org.assertj.core.api.Assertions.assertThat(contarMovimientos()).isZero();
		pedir(GET, "/api/cuentas/" + banco, tokenAna).andExpect(jsonPath("$.saldoActual").value(500000));
	}

	@Test
	void unaParteDeLaTransferenciaNoSeEditaNiBorraSuelta() throws Exception {
		transferir(tokenAna, banco, efectivo, "50000");
		String salida = jdbc.queryForObject("SELECT id::text FROM movimientos WHERE tipo = 'TRANSFERENCIA_SALIDA'",
				String.class);

		pedir(DELETE, "/api/movimientos/" + salida, tokenAna).andExpect(status().isConflict());
		pedir(PUT, "/api/movimientos/" + salida, tokenAna, """
				{"cuentaId": "%s", "categoriaId": "%s", "tipo": "GASTO", "monto": 1, "fecha": "2026-09-20"}
				""".formatted(banco, CATEGORIA_MERCADO)).andExpect(status().isConflict());
	}

}
