package co.cuentasclaras;

import org.springframework.boot.SpringApplication;

/**
 * Arranca la API en local con un PostgreSQL de Testcontainers, sin tener que
 * levantar docker compose. Útil para probar algo rápido desde el IDE.
 */
public class TestCuentasClarasApplication {

	public static void main(String[] args) {
		SpringApplication.from(CuentasClarasApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
