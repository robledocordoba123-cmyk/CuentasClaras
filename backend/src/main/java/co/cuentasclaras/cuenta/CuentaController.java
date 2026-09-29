package co.cuentasclaras.cuenta;

import co.cuentasclaras.comun.UsuarioActual;
import co.cuentasclaras.cuenta.CuentaDtos.CuentaRequest;
import co.cuentasclaras.cuenta.CuentaDtos.CuentaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/cuentas")
public class CuentaController {

	private final CuentaService cuentaService;

	public CuentaController(CuentaService cuentaService) {
		this.cuentaService = cuentaService;
	}

	@GetMapping
	public List<CuentaResponse> listar(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "false") boolean incluirArchivadas) {
		return cuentaService.listar(UsuarioActual.id(jwt), incluirArchivadas);
	}

	@GetMapping("/{id}")
	public CuentaResponse obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return cuentaService.obtener(UsuarioActual.id(jwt), id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CuentaResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CuentaRequest solicitud) {
		return cuentaService.crear(UsuarioActual.id(jwt), solicitud);
	}

	@PutMapping("/{id}")
	public CuentaResponse actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@Valid @RequestBody CuentaRequest solicitud) {
		return cuentaService.actualizar(UsuarioActual.id(jwt), id, solicitud);
	}

	/** 204 si se eliminó; 200 con la cuenta si tenía movimientos y se archivó (RN-10). */
	@DeleteMapping("/{id}")
	public ResponseEntity<CuentaResponse> eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		UUID usuarioId = UsuarioActual.id(jwt);
		boolean archivada = cuentaService.eliminar(usuarioId, id);
		return archivada
				? ResponseEntity.ok(cuentaService.obtener(usuarioId, id))
				: ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/restaurar")
	public CuentaResponse restaurar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return cuentaService.restaurar(UsuarioActual.id(jwt), id);
	}

}
