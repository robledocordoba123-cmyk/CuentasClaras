package co.cuentasclaras.transferencia;

import co.cuentasclaras.comun.UsuarioActual;
import co.cuentasclaras.transferencia.TransferenciaDtos.TransferenciaRequest;
import co.cuentasclaras.transferencia.TransferenciaDtos.TransferenciaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/transferencias")
public class TransferenciaController {

	private final TransferenciaService transferenciaService;

	public TransferenciaController(TransferenciaService transferenciaService) {
		this.transferenciaService = transferenciaService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TransferenciaResponse crear(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody TransferenciaRequest solicitud) {
		return transferenciaService.crear(UsuarioActual.id(jwt), solicitud);
	}

	@GetMapping("/{transferenciaId}")
	public TransferenciaResponse obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID transferenciaId) {
		return transferenciaService.obtener(UsuarioActual.id(jwt), transferenciaId);
	}

	@DeleteMapping("/{transferenciaId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID transferenciaId) {
		transferenciaService.eliminar(UsuarioActual.id(jwt), transferenciaId);
	}

}
