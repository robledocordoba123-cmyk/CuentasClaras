package co.cuentasclaras.cuenta;

import co.cuentasclaras.soporte.PruebaDeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CuentaControllerTest extends PruebaDeApi {

	private String tokenAna;

	@BeforeEach
	void crearUsuaria() throws Exception {
		tokenAna = registrar("ana@correo.com");
	}

	private ResultActions crearCuenta(String token, String nombre, String tipo, String saldoInicial) throws Exception {
		return pedir(POST, "/api/cuentas", token, """
				{"nombre": "%s", "tipo": "%s", "saldoInicial": %s}
				""".formatted(nombre, tipo, saldoInicial));
	}

	@Nested
	@DisplayName("HU-02: crear y consultar cuentas")
	class CrearYConsultar {

		@Test
		void creaUnaCuentaConSuSaldoInicialComoSaldoActual() throws Exception {
			crearCuenta(tokenAna, "Nequi", "BILLETERA_DIGITAL", "250000.50")
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.nombre").value("Nequi"))
					.andExpect(jsonPath("$.tipo").value("BILLETERA_DIGITAL"))
					.andExpect(jsonPath("$.saldoInicial").value(250000.50))
					.andExpect(jsonPath("$.saldoActual").value(250000.50))
					.andExpect(jsonPath("$.archivada").value(false));
		}

		@Test
		void rechazaSaldoNegativoTipoInexistenteYNombreVacio() throws Exception {
			crearCuenta(tokenAna, "Banco", "BANCO", "-100")
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errores.saldoInicial").exists());

			crearCuenta(tokenAna, "Alcancía", "CRIPTOMONEDA", "0")
					.andExpect(status().isBadRequest());

			crearCuenta(tokenAna, "   ", "EFECTIVO", "0")
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errores.nombre").exists());
		}

		@Test
		void noPermiteDosCuentasActivasConElMismoNombre() throws Exception {
			crearCuenta(tokenAna, "Efectivo", "EFECTIVO", "0").andExpect(status().isCreated());

			crearCuenta(tokenAna, "EFECTIVO", "EFECTIVO", "0")
					.andExpect(status().isConflict());
		}

		@Test
		void sinTokenNoSePuedeConsultar() throws Exception {
			mockMvc.perform(get("/api/cuentas")).andExpect(status().isUnauthorized());
		}

	}

	@Nested
	@DisplayName("RN-01: cada persona solo ve y modifica lo suyo")
	class Aislamiento {

		private String cuentaDeAna;
		private String tokenBeto;

		@BeforeEach
		void preparar() throws Exception {
			cuentaDeAna = idDe(crearCuenta(tokenAna, "Ahorros de Ana", "BANCO", "1000000"));
			tokenBeto = registrar("beto@correo.com");
		}

		@Test
		void elListadoSoloTraeLasCuentasPropias() throws Exception {
			crearCuenta(tokenBeto, "Billetera de Beto", "EFECTIVO", "5000");

			pedir(GET, "/api/cuentas", tokenBeto)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(1)))
					.andExpect(jsonPath("$[0].nombre").value("Billetera de Beto"));
		}

		@Test
		void otraPersonaRecibe404AlVerEditarOEliminarUnaCuentaAjena() throws Exception {
			String ruta = "/api/cuentas/" + cuentaDeAna;

			pedir(GET, ruta, tokenBeto).andExpect(status().isNotFound());
			pedir(PUT, ruta, tokenBeto, """
					{"nombre": "Robada", "tipo": "BANCO", "saldoInicial": 0}
					""").andExpect(status().isNotFound());
			pedir(DELETE, ruta, tokenBeto).andExpect(status().isNotFound());

			// La cuenta de Ana sigue intacta.
			pedir(GET, ruta, tokenAna)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.nombre").value("Ahorros de Ana"));
		}

	}

	@Nested
	@DisplayName("RN-02 y RN-04: saldo calculado con precisión exacta")
	class Saldo {

		@Test
		void elSaldoActualSeCalculaDesdeLosMovimientosSinErroresDeRedondeo() throws Exception {
			String cuenta = idDe(crearCuenta(tokenAna, "Efectivo", "EFECTIVO", "100.00"));
			// Con double, 100 - 0.10 - 0.20 da 99.69999999999999. Con BigDecimal, 99.70 exacto.
			insertarMovimiento(cuenta, CATEGORIA_MERCADO, "GASTO", "0.10");
			insertarMovimiento(cuenta, CATEGORIA_MERCADO, "GASTO", "0.20");
			insertarMovimiento(cuenta, CATEGORIA_SALARIO, "INGRESO", "1500000.00");

			pedir(GET, "/api/cuentas/" + cuenta, tokenAna)
					.andExpect(jsonPath("$.saldoInicial").value(100.00))
					.andExpect(jsonPath("$.saldoActual").value(1500099.70));
		}

	}

	@Nested
	@DisplayName("RN-10: eliminar o archivar")
	class EliminarOArchivar {

		@Test
		void unaCuentaSinMovimientosSeEliminaDeVerdad() throws Exception {
			String cuenta = idDe(crearCuenta(tokenAna, "Temporal", "EFECTIVO", "0"));

			pedir(DELETE, "/api/cuentas/" + cuenta, tokenAna).andExpect(status().isNoContent());
			pedir(GET, "/api/cuentas/" + cuenta, tokenAna).andExpect(status().isNotFound());
		}

		@Test
		void unaCuentaConMovimientosSeArchivaYSePuedeRestaurar() throws Exception {
			String cuenta = idDe(crearCuenta(tokenAna, "Vieja", "BANCO", "0"));
			insertarMovimiento(cuenta, CATEGORIA_SALARIO, "INGRESO", "50000");

			pedir(DELETE, "/api/cuentas/" + cuenta, tokenAna)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.archivada").value(true));

			// Ya no aparece en el listado normal, pero el historial se conserva.
			pedir(GET, "/api/cuentas", tokenAna).andExpect(jsonPath("$", hasSize(0)));
			pedir(GET, "/api/cuentas?incluirArchivadas=true", tokenAna)
					.andExpect(jsonPath("$", hasSize(1)))
					.andExpect(jsonPath("$[0].saldoActual").value(50000));

			pedir(PATCH, "/api/cuentas/" + cuenta + "/restaurar", tokenAna)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.archivada").value(false));
		}

	}

}
