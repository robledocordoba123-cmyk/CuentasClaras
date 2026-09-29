package co.cuentasclaras;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Las pruebas corren contra un PostgreSQL real en Docker, no contra una base
 * en memoria: así se prueban de verdad las migraciones de Flyway y el SQL.
 * Versión fija (16) para que coincida con docker-compose y con producción.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
	}

}
