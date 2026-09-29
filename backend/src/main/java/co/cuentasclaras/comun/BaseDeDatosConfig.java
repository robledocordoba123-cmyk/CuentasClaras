package co.cuentasclaras.comun;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

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
@Conditional(BaseDeDatosConfig.EsEnlaceDeProveedor.class)
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

	/**
	 * Al copiar y pegar el enlace es fácil que se cuelen espacios, un salto de
	 * línea o comillas, o que se copie el comando completo {@code psql '...'}.
	 * Se busca el enlace dentro del texto en vez de exigir que empiece
	 * exactamente por "postgresql://" (así falló el primer despliegue).
	 */
	static String extraerEnlace(String texto) {
		// Un enlace JDBC también contiene "postgres" ("jdbc:postgresql://"), pero
		// ese ya está en el formato correcto y no hay que convertirlo.
		if (texto == null || texto.contains("jdbc:")) {
			return null;
		}
		int inicio = texto.indexOf("postgres");
		if (inicio < 0) {
			return null;
		}
		String enlace = texto.substring(inicio).strip();
		int fin = 0;
		while (fin < enlace.length() && !Character.isWhitespace(enlace.charAt(fin))
				&& enlace.charAt(fin) != '\'' && enlace.charAt(fin) != '"') {
			fin++;
		}
		enlace = enlace.substring(0, fin);
		return (enlace.startsWith("postgresql://") || enlace.startsWith("postgres://")) ? enlace : null;
	}

	static class EsEnlaceDeProveedor implements Condition {

		@Override
		public boolean matches(ConditionContext contexto, AnnotatedTypeMetadata metadatos) {
			String valor = contexto.getEnvironment().getProperty("DATABASE_URL");
			// Si ya viene en formato JDBC, lo maneja la configuración normal de Spring.
			return extraerEnlace(valor) != null;
		}

	}

	record ConexionJdbc(String url, String usuario, String clave) {

		static ConexionJdbc desde(String texto) {
			URI uri = URI.create(extraerEnlace(texto));
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
