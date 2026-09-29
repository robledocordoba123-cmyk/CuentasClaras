package co.cuentasclaras.comun;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseDeDatosConfigTest {

	@Test
	void convierteElEnlaceDeNeonAlFormatoJdbcConUsuarioYClaveAparte() {
		var conexion = BaseDeDatosConfig.ConexionJdbc.desde(
				"postgresql://ana_owner:cl%40ve-123@ep-algo.us-east-2.aws.neon.tech/cuentasclaras"
						+ "?sslmode=require&channel_binding=require");

		assertThat(conexion.url())
				.isEqualTo("jdbc:postgresql://ep-algo.us-east-2.aws.neon.tech/cuentasclaras?sslmode=require");
		assertThat(conexion.usuario()).isEqualTo("ana_owner");
		assertThat(conexion.clave()).isEqualTo("cl@ve-123");
	}

	@Test
	void conservaElPuertoSiViene() {
		var conexion = BaseDeDatosConfig.ConexionJdbc.desde("postgres://u:p@localhost:5433/base");

		assertThat(conexion.url()).isEqualTo("jdbc:postgresql://localhost:5433/base");
	}

}
