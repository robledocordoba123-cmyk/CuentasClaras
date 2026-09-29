package co.cuentasclaras.comun;

import co.cuentasclaras.comun.error.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba unitaria pura (sin Spring ni base de datos): corre en milisegundos. */
class MesesTest {

	@Test
	void rn09_el30DeSeptiembreALas10pmEnColombiaSigueSiendoSeptiembreAunqueEnUtcYaSeaOctubre() {
		// 2026-10-01 03:00 UTC = 2026-09-30 22:00 en Bogotá (UTC-5).
		Clock reloj = Clock.fixed(Instant.parse("2026-10-01T03:00:00Z"), ZoneOffset.UTC);

		Meses meses = new Meses(reloj, "America/Bogota");

		assertThat(meses.actual()).isEqualTo(YearMonth.of(2026, 9));
	}

	@Test
	void convierteElTextoDelMesYRechazaFormatosInvalidos() {
		Meses meses = new Meses(Clock.systemUTC(), "America/Bogota");

		assertThat(meses.parsearOActual("2026-02")).isEqualTo(YearMonth.of(2026, 2));
		assertThatThrownBy(() -> meses.parsearOActual("02-2026")).isInstanceOf(SolicitudInvalidaException.class);
	}

}
