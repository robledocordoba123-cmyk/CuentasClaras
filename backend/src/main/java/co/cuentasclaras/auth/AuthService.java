package co.cuentasclaras.auth;

import co.cuentasclaras.auth.AuthDtos.LoginRequest;
import co.cuentasclaras.auth.AuthDtos.RegistroRequest;
import co.cuentasclaras.auth.AuthDtos.TokenResponse;
import co.cuentasclaras.auth.AuthDtos.UsuarioResponse;
import co.cuentasclaras.comun.error.CredencialesInvalidasException;
import co.cuentasclaras.comun.error.RecursoEnConflictoException;
import co.cuentasclaras.comun.error.RecursoNoEncontradoException;
import co.cuentasclaras.usuario.Usuario;
import co.cuentasclaras.usuario.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;

	/**
	 * Hash de una contraseña que nadie conoce. Cuando el correo no existe se
	 * compara igual contra este hash, para que la respuesta tarde lo mismo que
	 * con un correo real: si no, midiendo el tiempo se podría averiguar qué
	 * correos están registrados.
	 */
	private final String hashDeRelleno;

	public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
			TokenService tokenService) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.hashDeRelleno = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	/** HU-01: registro. Devuelve un token para que quede con la sesión iniciada. */
	@Transactional
	public TokenResponse registrar(RegistroRequest solicitud) {
		String email = normalizarEmail(solicitud.email());
		if (usuarioRepository.existsByEmail(email)) {
			throw new RecursoEnConflictoException("Ya existe una cuenta registrada con ese correo.");
		}

		Usuario usuario = new Usuario(solicitud.nombre().trim(), email, passwordEncoder.encode(solicitud.password()));
		usuarioRepository.save(usuario);
		return respuestaConToken(usuario);
	}

	@Transactional(readOnly = true)
	public TokenResponse iniciarSesion(LoginRequest solicitud) {
		Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(solicitud.email())).orElse(null);
		String hash = usuario != null ? usuario.getPasswordHash() : hashDeRelleno;

		// El mismo mensaje si el correo no existe o si la contraseña está mal:
		// no le decimos a un atacante cuál de las dos acertó.
		if (!passwordEncoder.matches(solicitud.password(), hash) || usuario == null) {
			throw new CredencialesInvalidasException();
		}
		return respuestaConToken(usuario);
	}

	@Transactional(readOnly = true)
	public UsuarioResponse obtenerPerfil(UUID usuarioId) {
		return usuarioRepository.findById(usuarioId)
				.map(AuthService::aRespuesta)
				.orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
	}

	private TokenResponse respuestaConToken(Usuario usuario) {
		TokenService.TokenEmitido token = tokenService.emitirPara(usuario);
		return new TokenResponse(token.valor(), "Bearer", token.expiraEn(), aRespuesta(usuario));
	}

	private static UsuarioResponse aRespuesta(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail());
	}

	/** "Ana@Gmail.com " y "ana@gmail.com" son la misma persona. */
	static String normalizarEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
