package co.cuentasclaras.comun;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación interactiva en /swagger-ui.html. El botón "Authorize" permite
 * pegar el token del login y probar las rutas protegidas desde el navegador.
 */
@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI documentacion() {
		return new OpenAPI()
				.info(new Info()
						.title("CuentasClaras API")
						.version("1.0")
						.description("Finanzas personales: cuentas, movimientos, transferencias, presupuestos y "
								+ "resumen mensual. Primero regístrate o inicia sesión en /api/auth y pega el token "
								+ "en \"Authorize\"."))
				.components(new Components().addSecuritySchemes("jwt",
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList("jwt"));
	}

}
