package co.cuentasclaras.movimiento;

import co.cuentasclaras.soporte.PruebaDeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MovimientoControllerTest extends PruebaDeApi {

	private String tokenAna;
	private String nequi;

	@BeforeEach
	void preparar() throws Exception {
		tokenAna = registrar("ana@correo.com");
		nequi = crearCuenta(tokenAna, "Nequi", "100000");
	}

	private String crearCuenta(String token, String nombre, String saldo) throws Exception {
		return idDe(pedir(POST, "/api/cuentas", token, """
				{"nombre": "%s", "tipo": "BILLETERA_DIGITAL", "saldoInicial": %s}
				""".formatted(nombre, saldo)));
	}

	private ResultActions registrarMovimiento(String token, String cuenta, UUID categoria, String tipo, String monto,
			String fecha) throws Exception {
		return pedir(POST, "/api/movimientos", token, """
				{"cuentaId": "%s", "categoriaId": "%s", "tipo": "%s", "monto": %s, "fecha": "%s", "descripcion": "Prueba"}
				""".formatted(cuenta, categoria, tipo, monto, fecha));
	}

	@Test
	void registraUnGastoConNombresDeCuentaYCategoriaYActualizaElSaldo() throws Exception {
		registrarMovimiento(tokenAna, nequi, CATEGORIA_MERCADO, "GASTO", "35000.50", "2026-09-15")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("GASTO"))
				.andExpect(jsonPath("$.monto").value(35000.50))
				.andExpect(jsonPath("$.fecha").value("2026-09-15"))
				.andExpect(jsonPath("$.cuenta.nombre").value("Nequi"))
				.andExpect(jsonPath("$.categoria.nombre").value("Mercado"));

		pedir(GET, "/api/cuentas/" + nequi, tokenAna)
				.andExpect(jsonPath("$.saldoActual").value(64999.50));
	}

	@Test
	void rechazaMontoCeroONegativoYFechaFaltante() throws Exception {
		registrarMovimiento(tokenAna, nequi, CATEGORIA_MERCADO, "GASTO", "0", "2026-09-15")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.monto").exists());

		pedir(POST, "/api/movimientos", tokenAna, """
				{"cuentaId": "%s", "categoriaId": "%s", "tipo": "GASTO", "monto": 1000}
				""".formatted(nequi, CATEGORIA_MERCADO))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.fecha").exists());
	}

	@Test
	void rn07_unaCategoriaDeIngresoNoSirveParaUnGasto() throws Exception {
		registrarMovimiento(tokenAna, nequi, CATEGORIA_SALARIO, "GASTO", "5000", "2026-09-15")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.title").value("Regla de negocio"));
	}

	@Test
	void noSePuedeRegistrarUnaTransferenciaPorEstaRuta() throws Exception {
		pedir(POST, "/api/movimientos", tokenAna, """
				{"cuentaId": "%s", "categoriaId": "%s", "tipo": "TRANSFERENCIA_SALIDA", "monto": 1000, "fecha": "2026-09-15"}
				""".formatted(nequi, CATEGORIA_MERCADO))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void rn01_noSePuedeUsarLaCuentaDeOtraPersonaNiVerSusMovimientos() throws Exception {
		String tokenBeto = registrar("beto@correo.com");
		String movimientoDeAna = idDe(
				registrarMovimiento(tokenAna, nequi, CATEGORIA_MERCADO, "GASTO", "1000", "2026-09-15"));

		// Beto intenta cargar un gasto a la cuenta de Ana.
		registrarMovimiento(tokenBeto, nequi, CATEGORIA_MERCADO, "GASTO", "1000", "2026-09-15")
				.andExpect(status().isNotFound());

		pedir(GET, "/api/movimientos/" + movimientoDeAna, tokenBeto).andExpect(status().isNotFound());
		pedir(DELETE, "/api/movimientos/" + movimientoDeAna, tokenBeto).andExpect(status().isNotFound());
		pedir(GET, "/api/movimientos", tokenBeto).andExpect(jsonPath("$.totalElementos").value(0));
	}

	@Test
	void noSePuedeRegistrarEnUnaCuentaArchivada() throws Exception {
		registrarMovimiento(tokenAna, nequi, CATEGORIA_MERCADO, "GASTO", "1000", "2026-09-15");
		pedir(DELETE, "/api/cuentas/" + nequi, tokenAna); // tiene movimientos: se archiva

		registrarMovimiento(tokenAna, nequi, CATEGORIA_MERCADO, "GASTO", "1000", "2026-09-16")
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void filtraPorFechasYTipoYPaginaDelMasRecienteAlMasAntiguo() throws Exception {
		registrarMovimiento(tokenAna, nequi, CATEGORIA_MERCADO, "GASTO", "1000", "2026-08-31");
		registrarMovimiento(tokenAna, nequi, CATEGORIA_MERCADO, "GASTO", "2000", "2026-09-01");
		registrarMovimiento(tokenAna, nequi, CATEGORIA_SALARIO, "INGRESO", "3000", "2026-09-15");
		registrarMovimiento(tokenAna, nequi, CATEGORIA_MERCADO, "GASTO", "4000", "2026-09-30");

		pedir(GET, "/api/movimientos?desde=2026-09-01&hasta=2026-09-30&tipo=GASTO", tokenAna)
				.andExpect(jsonPath("$.totalElementos").value(2))
				.andExpect(jsonPath("$.contenido[0].fecha").value("2026-09-30"))
				.andExpect(jsonPath("$.contenido[1].fecha").value("2026-09-01"));

		pedir(GET, "/api/movimientos?tamano=3&pagina=1", tokenAna)
				.andExpect(jsonPath("$.contenido", hasSize(1)))
				.andExpect(jsonPath("$.totalElementos").value(4))
				.andExpect(jsonPath("$.totalPaginas").value(2))
				.andExpect(jsonPath("$.contenido[0].fecha").value("2026-08-31"));
	}

	@Test
	void rangoDeFechasInvertidoResponde422() throws Exception {
		pedir(GET, "/api/movimientos?desde=2026-09-30&hasta=2026-09-01", tokenAna)
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void editarYEliminarActualizanElSaldo() throws Exception {
		String movimiento = idDe(
				registrarMovimiento(tokenAna, nequi, CATEGORIA_MERCADO, "GASTO", "10000", "2026-09-15"));

		pedir(PUT, "/api/movimientos/" + movimiento, tokenAna, """
				{"cuentaId": "%s", "categoriaId": "%s", "tipo": "GASTO", "monto": 25000, "fecha": "2026-09-15"}
				""".formatted(nequi, CATEGORIA_MERCADO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.monto").value(25000));
		pedir(GET, "/api/cuentas/" + nequi, tokenAna).andExpect(jsonPath("$.saldoActual").value(75000));

		pedir(DELETE, "/api/movimientos/" + movimiento, tokenAna).andExpect(status().isNoContent());
		pedir(GET, "/api/cuentas/" + nequi, tokenAna).andExpect(jsonPath("$.saldoActual").value(100000));
	}

}
