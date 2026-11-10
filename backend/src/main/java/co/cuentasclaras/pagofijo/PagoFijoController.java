package co.cuentasclaras.pagofijo;

import co.cuentasclaras.comun.Meses;
import co.cuentasclaras.comun.UsuarioActual;
import co.cuentasclaras.movimiento.MovimientoDtos.MovimientoResponse;
import co.cuentasclaras.pagofijo.PagoFijoDtos.PagarRequest;
import co.cuentasclaras.pagofijo.PagoFijoDtos.PagoFijoRequest;
import co.cuentasclaras.pagofijo.PagoFijoDtos.PagoFijoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/pagos-fijos")
public class PagoFijoController {

	private final PagoFijoService pagoFijoService;
	private final Meses meses;

	public PagoFijoController(PagoFijoService pagoFijoService, Meses meses) {
		this.pagoFijoService = pagoFijoService;
		this.meses = meses;
	}

	@GetMapping
	public List<PagoFijoResponse> listar(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String mes) {
		return pagoFijoService.listar(UsuarioActual.id(jwt), meses.parsearOActual(mes));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, UUID> crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PagoFijoRequest solicitud) {
		return Map.of("id", pagoFijoService.crear(UsuarioActual.id(jwt), solicitud));
	}

	@PutMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody PagoFijoRequest solicitud) {
		pagoFijoService.actualizar(UsuarioActual.id(jwt), id, solicitud);
	}

	/** 204 si se borró; 200 con {"desactivado": true} si ya tenía pagos. */
	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Boolean>> eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return pagoFijoService.eliminar(UsuarioActual.id(jwt), id)
				? ResponseEntity.ok(Map.of("desactivado", true))
				: ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/pagar")
	@ResponseStatus(HttpStatus.CREATED)
	public MovimientoResponse pagar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@Valid @RequestBody(required = false) PagarRequest solicitud) {
		return pagoFijoService.pagar(UsuarioActual.id(jwt), id, solicitud);
	}

}
