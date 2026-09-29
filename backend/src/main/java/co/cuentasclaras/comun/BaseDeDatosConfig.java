package co.cuentasclaras.comun;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Proveedores como Neon entregan la conexión en formato
 * {@code postgresql://usuario:clave@servidor/base?sslmode=require}, pero el
 * driver JDBC de Java espera {@code jdbc:postgresql://servidor/base} con el
 * usuario y la clave aparte. Esta configuración hace la conversión, así en
 * producción basta con pegar el enlace tal como lo da Neon en DATABASE_URL.
 *
 * <p>Solo se activa si DATABASE_URL tiene ese formato. En desarrollo y en las
 * pruebas se usa la configuración normal de Spring Boot.
 */
@Configuration
@ConditionalOnExpression("'${DATABASE_URL:}'.startsWith('postgresql://') or '${DATABASE_URL:}'.startsWith('postgres://')")
public class BaseDeDatosConfig {

	@Bean
	DataSource dataSource(Environment entorno) {
		ConexionJdbc conexion = ConexionJdbc.desde(entorno.getRequiredProperty("DATABASE_URL"));
		return DataSourceBuilder.create()
				.url(conexion.url())
				.username(conexion.usuario())
				.password(conexion.clave())
				.build();
	}

	record ConexionJdbc(String url, String usuario, String clave) {

		static ConexionJdbc desde(String enlace) {
			URI uri = URI.create(enlace);
			String[] credenciales = uri.getRawUserInfo().split(":", 2);
			// channel_binding no es un parámetro del driver JDBC; sslmode sí.
			String parametros = uri.getRawQuery() == null ? "" : Arrays.stream(uri.getRawQuery().split("&"))
					.filter(p -> !p.startsWith("channel_binding="))
					.collect(Collectors.joining("&"));
			String puerto = uri.getPort() == -1 ? "" : ":" + uri.getPort();
			String url = "jdbc:postgresql://" + uri.getHost() + puerto + uri.getRawPath()
					+ (parametros.isEmpty() ? "" : "?" + parametros);
			return new ConexionJdbc(url, decodificar(credenciales[0]),
					credenciales.length > 1 ? decodificar(credenciales[1]) : "");
		}

		private static String decodificar(String texto) {
			return URLDecoder.decode(texto, StandardCharsets.UTF_8);
		}

	}

}
