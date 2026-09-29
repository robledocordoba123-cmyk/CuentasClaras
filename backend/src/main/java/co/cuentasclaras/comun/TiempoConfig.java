package co.cuentasclaras.comun;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TiempoConfig {

	/** El reloj real. Las pruebas pueden reemplazarlo por uno fijo. */
	@Bean
	Clock reloj() {
		return Clock.systemUTC();
	}

}
