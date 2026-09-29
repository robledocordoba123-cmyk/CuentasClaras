package co.cuentasclaras.resumen;

import co.cuentasclaras.comun.Meses;
import co.cuentasclaras.comun.UsuarioActual;
import co.cuentasclaras.comun.error.SolicitudInvalidaException;
import co.cuentasclaras.resumen.ResumenService.ResumenMensual;
import co.cuentasclaras.resumen.ResumenService.TotalesMes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/resumen")
public class ResumenController {

	private static final int MAXIMO_MESES = 24;

	private final ResumenService resumenService;
	private final Meses meses;

	public ResumenController(ResumenService resumenService, Meses meses) {
		this.resumenService = resumenService;
		this.meses = meses;
	}

	/** Totales del mes, gasto por categoría y presupuestos en alerta. ?mes=2026-09 (por defecto, el actual). */
	@GetMapping
	public ResumenMensual delMes(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String mes) {
		return resumenService.delMes(UsuarioActual.id(jwt), meses.parsearOActual(mes));
	}

	/** Ingresos y gastos de los últimos meses para la gráfica. ?meses=6 (entre 1 y 24). */
	@GetMapping("/tendencia")
	public List<TotalesMes> tendencia(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "6") int meses) {
		if (meses < 1 || meses > MAXIMO_MESES) {
			throw new SolicitudInvalidaException("meses debe estar entre 1 y " + MAXIMO_MESES + ".");
		}
		return resumenService.tendencia(UsuarioActual.id(jwt), this.meses.actual(), meses);
	}

}
