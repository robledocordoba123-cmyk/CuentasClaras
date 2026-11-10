package co.cuentasclaras.meta;

import co.cuentasclaras.comun.UsuarioActual;
import co.cuentasclaras.meta.MetaDtos.AporteRequest;
import co.cuentasclaras.meta.MetaDtos.MetaRequest;
import co.cuentasclaras.meta.MetaDtos.MetaResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/metas")
public class MetaController {

	private final MetaService metaService;

	public MetaController(MetaService metaService) {
		this.metaService = metaService;
	}

	@GetMapping
	public List<MetaResponse> listar(@AuthenticationPrincipal Jwt jwt) {
		return metaService.listar(UsuarioActual.id(jwt));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MetaResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MetaRequest solicitud) {
		return metaService.crear(UsuarioActual.id(jwt), solicitud);
	}

	@PutMapping("/{id}")
	public MetaResponse actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody MetaRequest solicitud) {
		return metaService.actualizar(UsuarioActual.id(jwt), id, solicitud);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		metaService.eliminar(UsuarioActual.id(jwt), id);
	}

	@PostMapping("/{id}/aportes")
	@ResponseStatus(HttpStatus.CREATED)
	public MetaResponse aportar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody AporteRequest solicitud) {
		return metaService.aportar(UsuarioActual.id(jwt), id, solicitud);
	}

}
