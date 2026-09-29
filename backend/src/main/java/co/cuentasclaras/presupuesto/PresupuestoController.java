package co.cuentasclaras.presupuesto;

import co.cuentasclaras.comun.Meses;
import co.cuentasclaras.comun.UsuarioActual;
import co.cuentasclaras.presupuesto.PresupuestoDtos.PresupuestoRequest;
import co.cuentasclaras.presupuesto.PresupuestoDtos.PresupuestoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/presupuestos")
public class PresupuestoController {

	private final PresupuestoService presupuestoService;
	private final Meses meses;

	public PresupuestoController(PresupuestoService presupuestoService, Meses meses) {
		this.presupuestoService = presupuestoService;
		this.meses = meses;
	}

	/** ?mes=2026-09. Sin mes: el mes actual en hora de Colombia (RN-09). */
	@GetMapping
	public List<PresupuestoResponse> delMes(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String mes) {
		return presupuestoService.delMes(UsuarioActual.id(jwt), meses.parsearOActual(mes));
	}

	@PutMapping
	public PresupuestoResponse definir(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PresupuestoRequest solicitud) {
		return presupuestoService.definir(UsuarioActual.id(jwt), solicitud);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		presupuestoService.eliminar(UsuarioActual.id(jwt), id);
	}

}
