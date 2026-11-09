package co.cuentasclaras.soporte;

import co.cuentasclaras.auth.CookieDeSesion;
import org.springframework.test.web.servlet.ResultActions;

/** Saca el token de la cookie de sesión que pone la API al registrarse o entrar. */
public final class Cookies {

	private Cookies() {
	}

	public static String tokenDe(ResultActions respuesta) {
		String encabezado = respuesta.andReturn().getResponse().getHeader("Set-Cookie");
		if (encabezado == null || !encabezado.startsWith(CookieDeSesion.NOMBRE + "=")) {
			throw new AssertionError("La respuesta no trae la cookie de sesión: " + encabezado);
		}
		return encabezado.substring(CookieDeSesion.NOMBRE.length() + 1, encabezado.indexOf(';'));
	}

}
