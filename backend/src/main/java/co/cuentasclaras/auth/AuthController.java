package co.cuentasclaras.auth;

import co.cuentasclaras.auth.AuthDtos.LoginRequest;
import co.cuentasclaras.auth.AuthDtos.RegistroRequest;
import co.cuentasclaras.auth.AuthDtos.TokenResponse;
import co.cuentasclaras.auth.AuthDtos.UsuarioResponse;
import co.cuentasclaras.comun.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * El controlador solo traduce HTTP a llamadas del servicio: valida la entrada
 * (@Valid) y decide el código de respuesta. La lógica vive en AuthService.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.CREATED)
	public TokenResponse registrar(@Valid @RequestBody RegistroRequest solicitud) {
		return authService.registrar(solicitud);
	}

	@PostMapping("/login")
	public TokenResponse iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
		return authService.iniciarSesion(solicitud);
	}

	/** Datos de la persona dueña del token. Sirve para comprobar la sesión. */
	@GetMapping("/yo")
	public UsuarioResponse yo(@AuthenticationPrincipal Jwt jwt) {
		return authService.obtenerPerfil(UsuarioActual.id(jwt));
	}

}
