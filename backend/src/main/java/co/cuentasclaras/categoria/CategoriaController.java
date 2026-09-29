package co.cuentasclaras.categoria;

import co.cuentasclaras.categoria.CategoriaDtos.CategoriaRequest;
import co.cuentasclaras.categoria.CategoriaDtos.CategoriaResponse;
import co.cuentasclaras.comun.UsuarioActual;
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
@RequestMapping("/api/categorias")
public class CategoriaController {

	private final CategoriaService categoriaService;

	public CategoriaController(CategoriaService categoriaService) {
		this.categoriaService = categoriaService;
	}

	/** Por defecto + propias. Filtro opcional ?tipo=GASTO o ?tipo=INGRESO. */
	@GetMapping
	public List<CategoriaResponse> listar(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) TipoCategoria tipo,
			@RequestParam(defaultValue = "false") boolean incluirArchivadas) {
		return categoriaService.listar(UsuarioActual.id(jwt), tipo, incluirArchivadas);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CategoriaResponse crear(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CategoriaRequest solicitud) {
		return categoriaService.crear(UsuarioActual.id(jwt), solicitud);
	}

	@PutMapping("/{id}")
	public CategoriaResponse actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@Valid @RequestBody CategoriaRequest solicitud) {
		return categoriaService.actualizar(UsuarioActual.id(jwt), id, solicitud);
	}

	/** 204 si se eliminó; 200 con la categoría si estaba en uso y se archivó (RN-10). */
	@DeleteMapping("/{id}")
	public ResponseEntity<CategoriaResponse> eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		CategoriaResponse archivada = categoriaService.eliminar(UsuarioActual.id(jwt), id);
		return archivada != null ? ResponseEntity.ok(archivada) : ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/restaurar")
	public CategoriaResponse restaurar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return categoriaService.restaurar(UsuarioActual.id(jwt), id);
	}

}
