package co.cuentasclaras.auth;

import co.cuentasclaras.auth.AuthDtos.BorrarCuentaRequest;
import co.cuentasclaras.auth.AuthDtos.CambioPasswordRequest;
import co.cuentasclaras.auth.AuthDtos.LoginRequest;
import co.cuentasclaras.auth.AuthDtos.RegistroRequest;
import co.cuentasclaras.auth.AuthDtos.SesionIniciada;
import co.cuentasclaras.auth.AuthDtos.SesionResponse;
import co.cuentasclaras.auth.AuthDtos.UsuarioResponse;
import co.cuentasclaras.comun.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * El controlador solo traduce HTTP a llamadas del servicio: valida la entrada
 * (@Valid), decide el código de respuesta y pone o quita la cookie de sesión.
 * La lógica vive en AuthService.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final MisDatosService misDatosService;
	private final CookieDeSesion cookie;

	public AuthController(AuthService authService, MisDatosService misDatosService, CookieDeSesion cookie) {
		this.authService = authService;
		this.misDatosService = misDatosService;
		this.cookie = cookie;
	}

	@PostMapping("/registro")
	public ResponseEntity<SesionResponse> registrar(@Valid @RequestBody RegistroRequest solicitud) {
		return conCookie(HttpStatus.CREATED, authService.registrar(solicitud));
	}

	@PostMapping("/login")
	public ResponseEntity<SesionResponse> iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
		return conCookie(HttpStatus.OK, authService.iniciarSesion(solicitud));
	}

	/** Cierra la sesión en este navegador. Es pública: borrar la cookie no necesita una sesión válida. */
	@PostMapping("/logout")
	public ResponseEntity<Void> cerrarSesion() {
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.borrar()).build();
	}

	/** Datos de la persona dueña de la sesión. Sirve para comprobar la sesión al abrir la app. */
	@GetMapping("/yo")
	public UsuarioResponse yo(@AuthenticationPrincipal Jwt jwt) {
		return authService.obtenerPerfil(UsuarioActual.id(jwt));
	}

	/** Cambia la contraseña: las demás sesiones abiertas se cierran. */
	@PutMapping("/password")
	public ResponseEntity<SesionResponse> cambiarPassword(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CambioPasswordRequest solicitud) {
		return conCookie(HttpStatus.OK, authService.cambiarPassword(UsuarioActual.id(jwt), solicitud));
	}

	/** Ley 1581, derecho de acceso: todo lo que la app guarda de la persona. */
	@GetMapping("/mis-datos")
	public ResponseEntity<Map<String, Object>> misDatos(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mis-datos-cuentasclaras.json\"")
				.body(misDatosService.exportar(UsuarioActual.id(jwt)));
	}

	/** Ley 1581, derecho de supresión: borra la cuenta y todos sus datos. */
	@DeleteMapping("/cuenta")
	public ResponseEntity<Void> borrarCuenta(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody BorrarCuentaRequest solicitud) {
		authService.borrarCuenta(UsuarioActual.id(jwt), solicitud.password());
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.borrar()).build();
	}

	private ResponseEntity<SesionResponse> conCookie(HttpStatus estado, SesionIniciada iniciada) {
		return ResponseEntity.status(estado)
				.header(HttpHeaders.SET_COOKIE, cookie.crear(iniciada.token()))
				.body(iniciada.sesion());
	}

}
