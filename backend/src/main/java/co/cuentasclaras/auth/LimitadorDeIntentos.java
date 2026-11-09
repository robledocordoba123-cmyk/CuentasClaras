package co.cuentasclaras.auth;

import co.cuentasclaras.comun.error.DemasiadosIntentosException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Frena a quien prueba contraseñas contra una misma cuenta: después de 10
 * intentos fallidos en 15 minutos, esa cuenta no acepta más intentos hasta que
 * pase la ventana. Va por cuenta y no por IP porque la API está detrás de varios
 * proxies (Vercel y Render), y una IP mal leída bloquearía a todo el mundo a la vez.
 *
 * <p>Vive en memoria: alcanza para una sola instancia. Con varias, iría a una
 * caché compartida como Redis.
 */
@Component
public class LimitadorDeIntentos {

	static final int MAXIMO = 10;
	static final Duration VENTANA = Duration.ofMinutes(15);

	private final ConcurrentHashMap<String, Ventana> fallos = new ConcurrentHashMap<>();
	private final Clock reloj;

	public LimitadorDeIntentos(Clock reloj) {
		this.reloj = reloj;
	}

	/** Lanza 429 si la cuenta ya agotó sus intentos. */
	public void verificar(String email) {
		Ventana ventana = fallos.get(email);
		if (ventana != null && !ventana.vencida(ahora()) && ventana.cantidad() >= MAXIMO) {
			throw new DemasiadosIntentosException();
		}
	}

	public void registrarFallo(String email) {
		Instant ahora = ahora();
		fallos.compute(email, (clave, actual) -> actual == null || actual.vencida(ahora)
				? new Ventana(ahora, 1)
				: new Ventana(actual.inicio(), actual.cantidad() + 1));
		// Limpieza simple para que el mapa no crezca sin fin.
		if (fallos.size() > 10_000) {
			fallos.entrySet().removeIf(e -> e.getValue().vencida(ahora));
		}
	}

	/** Un ingreso correcto deja la cuenta como nueva. */
	public void registrarExito(String email) {
		fallos.remove(email);
	}

	private Instant ahora() {
		return reloj.instant();
	}

	private record Ventana(Instant inicio, int cantidad) {
		boolean vencida(Instant ahora) {
			return inicio.plus(VENTANA).isBefore(ahora);
		}
	}

}
