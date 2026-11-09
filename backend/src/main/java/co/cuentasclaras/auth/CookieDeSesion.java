package co.cuentasclaras.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * La sesión viaja en una cookie que el JavaScript de la página no puede leer
 * (HttpOnly): un script inyectado no se la puede robar, como sí podría con un
 * token guardado en localStorage. SameSite=Strict hace que el navegador no la
 * envíe desde otros sitios, lo que frena el CSRF. El frontend llama a /api en
 * su misma dirección y Vercel la reenvía a la API, así la cookie es del mismo sitio.
 */
@Component
public class CookieDeSesion {

	public static final String NOMBRE = "cc_sesion";

	private final boolean segura;

	public CookieDeSesion(@Value("${cuentasclaras.cookie-segura:true}") boolean segura) {
		this.segura = segura;
	}

	public String crear(TokenService.TokenEmitido token) {
		return base(token.valor()).maxAge(Duration.between(Instant.now(), token.expiraEn())).build().toString();
	}

	public String borrar() {
		return base("").maxAge(0).build().toString();
	}

	private ResponseCookie.ResponseCookieBuilder base(String valor) {
		return ResponseCookie.from(NOMBRE, valor).httpOnly(true).secure(segura).sameSite("Strict").path("/api");
	}

}
