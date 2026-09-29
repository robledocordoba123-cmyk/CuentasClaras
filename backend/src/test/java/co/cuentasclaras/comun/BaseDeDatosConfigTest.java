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
	void toleraEspaciosSaltosDeLineaComillasYElComandoPsqlAlCopiarYPegar() {
		String esperado = "jdbc:postgresql://ep-algo.neon.tech/cuentasclaras?sslmode=require";
		String enlace = "postgresql://u:p@ep-algo.neon.tech/cuentasclaras?sslmode=require&channel_binding=require";

		for (String pegado : new String[] { "  " + enlace + "\n", "'" + enlace + "'", "\"" + enlace + "\"",
				"psql '" + enlace + "'" }) {
			assertThat(BaseDeDatosConfig.ConexionJdbc.desde(pegado).url()).as(pegado).isEqualTo(esperado);
		}
	}

	@Test
	void unEnlaceJdbcOTextoSinEnlaceNoSeConvierte() {
		assertThat(BaseDeDatosConfig.extraerEnlace("jdbc:postgresql://localhost:5433/base")).isNull();
		assertThat(BaseDeDatosConfig.extraerEnlace("")).isNull();
	}

	@Test
	void conservaElPuertoSiViene() {
		var conexion = BaseDeDatosConfig.ConexionJdbc.desde("postgres://u:p@localhost:5433/base");

		assertThat(conexion.url()).isEqualTo("jdbc:postgresql://localhost:5433/base");
	}

}
