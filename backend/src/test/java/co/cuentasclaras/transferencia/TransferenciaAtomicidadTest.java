package co.cuentasclaras.transferencia;

import co.cuentasclaras.movimiento.Movimiento;
import co.cuentasclaras.movimiento.MovimientoRepository;
import co.cuentasclaras.movimiento.TipoMovimiento;
import co.cuentasclaras.soporte.PruebaDeApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RN-05, la prueba más importante de las transferencias: si el servidor falla
 * justo después de guardar la salida y antes de guardar la entrada, la base de
 * datos debe deshacer TODO. Se simula la falla con un "espía" que deja pasar el
 * primer save (la salida) y hace fallar el segundo (la entrada).
 */
class TransferenciaAtomicidadTest extends PruebaDeApi {

	@MockitoSpyBean
	MovimientoRepository movimientoRepository;

	@Test
	void siFallaLaSegundaParteNoQuedaNingunMovimientoYLosSaldosNoCambian() throws Exception {
		String token = registrar("ana@correo.com");
		String banco = idDe(pedir(POST, "/api/cuentas", token, """
				{"nombre": "Banco", "tipo": "BANCO", "saldoInicial": 500000}
				"""));
		String efectivo = idDe(pedir(POST, "/api/cuentas", token, """
				{"nombre": "Efectivo", "tipo": "EFECTIVO", "saldoInicial": 0}
				"""));

		// La salida (primer save) se guarda normal; la entrada (segundo save) falla.
		doThrow(new IllegalStateException("Falla simulada: se cayó la conexión"))
				.when(movimientoRepository)
				.save(argThat((Movimiento m) -> m != null && m.getTipo() == TipoMovimiento.TRANSFERENCIA_ENTRADA));

		pedir(POST, "/api/transferencias", token, """
				{"cuentaOrigenId": "%s", "cuentaDestinoId": "%s", "monto": 50000, "fecha": "2026-09-20"}
				""".formatted(banco, efectivo))
				.andExpect(status().isInternalServerError());

		// La salida alcanzó a guardarse, pero la transacción la deshizo.
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM movimientos", Integer.class)).isZero();
		pedir(GET, "/api/cuentas/" + banco, token).andExpect(jsonPath("$.saldoActual").value(500000));
	}

}
