package co.cuentasclaras.resumen;

import co.cuentasclaras.soporte.PruebaDeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumenControllerTest extends PruebaDeApi {

	private String token;
	private String banco;
	private String efectivo;

	@BeforeEach
	void preparar() throws Exception {
		token = registrar("ana@correo.com");
		banco = cuenta("Banco", "BANCO");
		efectivo = cuenta("Efectivo", "EFECTIVO");
	}

	private String cuenta(String nombre, String tipo) throws Exception {
		return idDe(pedir(POST, "/api/cuentas", token, """
				{"nombre": "%s", "tipo": "%s", "saldoInicial": 0}
				""".formatted(nombre, tipo)));
	}

	private void movimiento(String cuenta, Object categoria, String tipo, String monto, String fecha,
			String descripcion) throws Exception {
		pedir(POST, "/api/movimientos", token, """
				{"cuentaId": "%s", "categoriaId": "%s", "tipo": "%s", "monto": %s, "fecha": "%s", "descripcion": "%s"}
				""".formatted(cuenta, categoria, tipo, monto, fecha, descripcion)).andExpect(status().isCreated());
	}

	@Test
	void hu08_resumenDelMesConGastoPorCategoriaSinContarTransferencias() throws Exception {
		movimiento(banco, CATEGORIA_SALARIO, "INGRESO", "2000000", "2026-09-01", "Sueldo");
		movimiento(banco, CATEGORIA_MERCADO, "GASTO", "300000", "2026-09-05", "Mercado del mes");
		movimiento(efectivo, "00000000-0000-0000-0000-000000000202", "GASTO", "100000", "2026-09-08", "Bus");
		// RN-06: retirar del banco a efectivo no es ingreso ni gasto.
		pedir(POST, "/api/transferencias", token, """
				{"cuentaOrigenId": "%s", "cuentaDestinoId": "%s", "monto": 500000, "fecha": "2026-09-10"}
				""".formatted(banco, efectivo)).andExpect(status().isCreated());

		pedir(GET, "/api/resumen?mes=2026-09", token)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalIngresos").value(2000000))
				.andExpect(jsonPath("$.totalGastos").value(400000))
				.andExpect(jsonPath("$.balance").value(1600000))
				.andExpect(jsonPath("$.gastosPorCategoria", hasSize(2)))
				.andExpect(jsonPath("$.gastosPorCategoria[0].categoria.nombre").value("Mercado"))
				.andExpect(jsonPath("$.gastosPorCategoria[0].porcentaje").value(75.0))
				.andExpect(jsonPath("$.gastosPorCategoria[1].categoria.nombre").value("Transporte"));
	}

	@Test
	void hu07_elResumenTraeComoAlertasLosPresupuestosEnRiesgo() throws Exception {
		pedir(PUT, "/api/presupuestos", token, """
				{"categoriaId": "%s", "mes": "2026-09", "montoLimite": 100000}
				""".formatted(CATEGORIA_MERCADO));
		movimiento(banco, CATEGORIA_MERCADO, "GASTO", "90000", "2026-09-05", "Mercado");

		pedir(GET, "/api/resumen?mes=2026-09", token)
				.andExpect(jsonPath("$.alertas", hasSize(1)))
				.andExpect(jsonPath("$.alertas[0].estado").value("ALERTA"));
	}

	@Test
	void unMesSinMovimientosDevuelveCeros() throws Exception {
		pedir(GET, "/api/resumen?mes=2020-01", token)
				.andExpect(jsonPath("$.totalIngresos").value(0))
				.andExpect(jsonPath("$.totalGastos").value(0))
				.andExpect(jsonPath("$.gastosPorCategoria", hasSize(0)));
	}

	@Test
	void laTendenciaIncluyeLosMesesSinMovimientosEnOrden() throws Exception {
		YearMonth actual = YearMonth.now(ZoneId.of("America/Bogota"));
		movimiento(banco, CATEGORIA_SALARIO, "INGRESO", "1000", actual.minusMonths(2).atDay(15).toString(), "x");
		movimiento(banco, CATEGORIA_MERCADO, "GASTO", "300", actual.atDay(1).toString(), "y");

		pedir(GET, "/api/resumen/tendencia?meses=3", token)
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].mes").value(actual.minusMonths(2).toString()))
				.andExpect(jsonPath("$[0].ingresos").value(1000))
				.andExpect(jsonPath("$[1].ingresos").value(0))
				.andExpect(jsonPath("$[2].gastos").value(300));

		pedir(GET, "/api/resumen/tendencia?meses=100", token).andExpect(status().isBadRequest());
	}

	@Test
	void hu09_exportaElMesEnCsvListoParaExcel() throws Exception {
		movimiento(banco, CATEGORIA_SALARIO, "INGRESO", "2000000", "2026-09-01", "Sueldo; septiembre");
		movimiento(banco, CATEGORIA_MERCADO, "GASTO", "35000.5", "2026-09-05", "=HYPERLINK(\\\"x\\\")");

		String csv = pedir(GET, "/api/movimientos/exportar?mes=2026-09", token)
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition",
						org.hamcrest.Matchers.containsString("cuentasclaras-2026-09.csv")))
				.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

		assertThat(csv).startsWith("﻿Fecha;Tipo;Cuenta;Categoría;Descripción;Monto");
		assertThat(csv).contains("2026-09-01;Ingreso;Banco;Salario;\"Sueldo; septiembre\";2000000,00");
		// Gasto con signo negativo y coma decimal; la "fórmula" queda como texto.
		assertThat(csv).contains("2026-09-05;Gasto;Banco;Mercado;\"'=HYPERLINK(\"\"x\"\")\";-35000,50");
	}

}
