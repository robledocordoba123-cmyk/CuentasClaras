package co.cuentasclaras.auth;

import co.cuentasclaras.auth.AuthDtos.CambioPasswordRequest;
import co.cuentasclaras.auth.AuthDtos.LoginRequest;
import co.cuentasclaras.auth.AuthDtos.RegistroRequest;
import co.cuentasclaras.auth.AuthDtos.SesionIniciada;
import co.cuentasclaras.auth.AuthDtos.SesionResponse;
import co.cuentasclaras.auth.AuthDtos.UsuarioResponse;
import co.cuentasclaras.comun.error.AccionNoPermitidaException;
import co.cuentasclaras.comun.error.CredencialesInvalidasException;
import co.cuentasclaras.comun.error.RecursoEnConflictoException;
import co.cuentasclaras.comun.error.RecursoNoEncontradoException;
import co.cuentasclaras.comun.error.ReglaDeNegocioException;
import co.cuentasclaras.usuario.Usuario;
import co.cuentasclaras.usuario.UsuarioRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

	/** Cambia cuando cambie la política de datos: cada autorización guarda su versión. */
	public static final String VERSION_POLITICA = "2026-11";
	/** La cuenta de la demo pública no se puede borrar ni cambiar de clave. */
	static final String EMAIL_DEMO = co.cuentasclaras.demo.DatosDeDemo.EMAIL;

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final LimitadorDeIntentos limitador;
	private final JdbcTemplate jdbc;
	private final Clock reloj;

	/**
	 * Hash de una contraseña que nadie conoce. Cuando el correo no existe se
	 * compara igual contra este hash, para que la respuesta tarde lo mismo que
	 * con un correo real: si no, midiendo el tiempo se podría averiguar qué
	 * correos están registrados.
	 */
	private final String hashDeRelleno;

	public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
			TokenService tokenService, LimitadorDeIntentos limitador, JdbcTemplate jdbc, Clock reloj) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.limitador = limitador;
		this.jdbc = jdbc;
		this.reloj = reloj;
		this.hashDeRelleno = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	/** HU-01: registro. Queda con la sesión iniciada. */
	@Transactional
	public SesionIniciada registrar(RegistroRequest solicitud) {
		String email = normalizarEmail(solicitud.email());
		if (usuarioRepository.existsByEmail(email)) {
			throw new RecursoEnConflictoException("Ya existe una cuenta registrada con ese correo.");
		}

		Usuario usuario = new Usuario(solicitud.nombre().trim(), email, passwordEncoder.encode(solicitud.password()));
		usuario.autorizarDatos(VERSION_POLITICA, reloj.instant());
		usuarioRepository.save(usuario);
		return sesionPara(usuario);
	}

	@Transactional(readOnly = true)
	public SesionIniciada iniciarSesion(LoginRequest solicitud) {
		String email = normalizarEmail(solicitud.email());
		limitador.verificar(email);
		Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
		String hash = usuario != null ? usuario.getPasswordHash() : hashDeRelleno;

		// El mismo mensaje si el correo no existe o si la contraseña está mal:
		// no le decimos a un atacante cuál de las dos acertó.
		if (!passwordEncoder.matches(solicitud.password(), hash) || usuario == null) {
			limitador.registrarFallo(email);
			throw new CredencialesInvalidasException();
		}
		limitador.registrarExito(email);
		return sesionPara(usuario);
	}

	@Transactional(readOnly = true)
	public UsuarioResponse obtenerPerfil(UUID usuarioId) {
		return aRespuesta(buscar(usuarioId));
	}

	/** Cambia la contraseña y cierra las demás sesiones; esta queda abierta con un token nuevo. */
	@Transactional
	public SesionIniciada cambiarPassword(UUID usuarioId, CambioPasswordRequest solicitud) {
		Usuario usuario = buscar(usuarioId);
		noEnLaDemo(usuario);
		if (!passwordEncoder.matches(solicitud.actual(), usuario.getPasswordHash())) {
			throw new ReglaDeNegocioException("La contraseña actual no es correcta.");
		}
		usuario.cambiarPassword(passwordEncoder.encode(solicitud.nueva()));
		usuarioRepository.saveAndFlush(usuario);
		return sesionPara(usuario);
	}

	/**
	 * Ley 1581, derecho de supresión: borra la cuenta y todo lo que la persona
	 * registró. El orden respeta las llaves foráneas. No se puede deshacer.
	 */
	@Transactional
	public void borrarCuenta(UUID usuarioId, String password) {
		Usuario usuario = buscar(usuarioId);
		noEnLaDemo(usuario);
		if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
			throw new ReglaDeNegocioException("La contraseña no es correcta.");
		}
		jdbc.update("DELETE FROM movimientos WHERE usuario_id = ?", usuarioId);
		jdbc.update("DELETE FROM presupuestos WHERE usuario_id = ?", usuarioId);
		jdbc.update("DELETE FROM cuentas WHERE usuario_id = ?", usuarioId);
		jdbc.update("DELETE FROM categorias WHERE usuario_id = ?", usuarioId);
		jdbc.update("DELETE FROM usuarios WHERE id = ?", usuarioId);
	}

	private Usuario buscar(UUID usuarioId) {
		return usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
	}

	private static void noEnLaDemo(Usuario usuario) {
		if (EMAIL_DEMO.equals(usuario.getEmail())) {
			throw new AccionNoPermitidaException("Esta acción está desactivada en la cuenta de demostración.");
		}
	}

	private SesionIniciada sesionPara(Usuario usuario) {
		TokenService.TokenEmitido token = tokenService.emitirPara(usuario);
		return new SesionIniciada(new SesionResponse(token.expiraEn(), aRespuesta(usuario)), token);
	}

	private static UsuarioResponse aRespuesta(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail());
	}

	/** "Ana@Gmail.com " y "ana@gmail.com" son la misma persona. */
	static String normalizarEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
