package co.cuentasclaras.deuda;

import co.cuentasclaras.comun.UsuarioActual;
import co.cuentasclaras.deuda.DeudaDtos.AbonoRequest;
import co.cuentasclaras.deuda.DeudaDtos.DeudaRequest;
import co.cuentasclaras.deuda.DeudaDtos.DeudaResponse;
import co.cuentasclaras.deuda.DeudaDtos.PersonaRequest;
import co.cuentasclaras.deuda.DeudaDtos.PersonaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DeudaController {

	private final DeudaService deudaService;

	public DeudaController(DeudaService deudaService) {
		this.deudaService = deudaService;
	}

	@GetMapping("/personas")
	public List<PersonaResponse> personas(@AuthenticationPrincipal Jwt jwt) {
		return deudaService.listarPersonas(UsuarioActual.id(jwt));
	}

	@PostMapping("/personas")
	@ResponseStatus(HttpStatus.CREATED)
	public PersonaResponse crearPersona(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PersonaRequest solicitud) {
		return deudaService.crearPersona(UsuarioActual.id(jwt), solicitud);
	}

	@PutMapping("/personas/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void renombrar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody PersonaRequest solicitud) {
		deudaService.renombrarPersona(UsuarioActual.id(jwt), id, solicitud);
	}

	@DeleteMapping("/personas/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminarPersona(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		deudaService.eliminarPersona(UsuarioActual.id(jwt), id);
	}

	@GetMapping("/deudas")
	public List<DeudaResponse> deudas(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) UUID personaId,
			@RequestParam(defaultValue = "false") boolean soloPendientes) {
		return deudaService.listarDeudas(UsuarioActual.id(jwt), personaId, soloPendientes);
	}

	@PostMapping("/deudas")
	@ResponseStatus(HttpStatus.CREATED)
	public DeudaResponse crearDeuda(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeudaRequest solicitud) {
		return deudaService.crearDeuda(UsuarioActual.id(jwt), solicitud);
	}

	@DeleteMapping("/deudas/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminarDeuda(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		deudaService.eliminarDeuda(UsuarioActual.id(jwt), id);
	}

	@PostMapping("/deudas/{id}/abonos")
	@ResponseStatus(HttpStatus.CREATED)
	public DeudaResponse abonar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody AbonoRequest solicitud) {
		return deudaService.abonar(UsuarioActual.id(jwt), id, solicitud);
	}

	@DeleteMapping("/deudas/{id}/abonos/{abonoId}")
	public DeudaResponse quitarAbono(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID abonoId) {
		return deudaService.quitarAbono(UsuarioActual.id(jwt), id, abonoId);
	}

}
