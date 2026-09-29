package co.cuentasclaras.comun;

import co.cuentasclaras.comun.error.SolicitudInvalidaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * RN-09: "el mes" es el mes en la zona horaria de las personas usuarias
 * (America/Bogota), no en UTC. El 30 de septiembre a las 10:00 p. m. en
 * Colombia ya es 1 de octubre en UTC; para quien usa la app sigue siendo
 * septiembre.
 *
 * <p>El reloj (Clock) se inyecta en vez de usar YearMonth.now() directo: así
 * las pruebas pueden fijar la hora y comprobar este caso exacto.
 */
@Component
public class Meses {

	private final Clock reloj;
	private final ZoneId zona;

	public Meses(Clock reloj, @Value("${cuentasclaras.zona-horaria}") String zona) {
		this.reloj = reloj;
		this.zona = ZoneId.of(zona);
	}

	public YearMonth actual() {
		return YearMonth.now(reloj.withZone(zona));
	}

	/** Convierte "2026-09" en un YearMonth; si viene vacío, devuelve el mes actual. */
	public YearMonth parsearOActual(String mes) {
		if (mes == null || mes.isBlank()) {
			return actual();
		}
		try {
			return YearMonth.parse(mes.strip());
		} catch (DateTimeParseException e) {
			throw new SolicitudInvalidaException("El mes debe tener el formato AAAA-MM, por ejemplo 2026-09.");
		}
	}

}
