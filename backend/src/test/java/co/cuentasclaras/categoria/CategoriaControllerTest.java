package co.cuentasclaras.categoria;

import co.cuentasclaras.soporte.PruebaDeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoriaControllerTest extends PruebaDeApi {

	private String tokenAna;

	@BeforeEach
	void crearUsuaria() throws Exception {
		tokenAna = registrar("ana@correo.com");
	}

	private ResultActions crearCategoria(String token, String nombre, String tipo) throws Exception {
		return pedir(POST, "/api/categorias", token, """
				{"nombre": "%s", "tipo": "%s", "color": "#abcdef"}
				""".formatted(nombre, tipo));
	}

	@Test
	void unaPersonaNuevaVeLas12CategoriasPorDefectoYPuedeFiltrarPorTipo() throws Exception {
		pedir(GET, "/api/categorias", tokenAna)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(12)))
				.andExpect(jsonPath("$[*].nombre", hasItem("Domicilios")));

		pedir(GET, "/api/categorias?tipo=INGRESO", tokenAna)
				.andExpect(jsonPath("$", hasSize(3)));
	}

	@Test
	void creaUnaCategoriaPropiaQueOtraPersonaNoVe() throws Exception {
		crearCategoria(tokenAna, "Mascotas", "GASTO")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.porDefecto").value(false))
				.andExpect(jsonPath("$.color").value("#ABCDEF"));

		String tokenBeto = registrar("beto@correo.com");
		pedir(GET, "/api/categorias", tokenBeto)
				.andExpect(jsonPath("$", hasSize(12)))
				.andExpect(jsonPath("$[*].nombre", not(hasItem("Mascotas"))));
	}

	@Test
	void noPermiteRepetirElNombreDeUnaCategoriaDelMismoTipoNiSiquieraUnaPorDefecto() throws Exception {
		crearCategoria(tokenAna, "mercado", "GASTO").andExpect(status().isConflict());

		// El mismo nombre en el otro tipo sí es válido.
		crearCategoria(tokenAna, "Mercado", "INGRESO").andExpect(status().isCreated());
	}

	@Test
	void lasCategoriasPorDefectoNoSePuedenModificarNiEliminar() throws Exception {
		String ruta = "/api/categorias/" + CATEGORIA_MERCADO;

		pedir(PUT, ruta, tokenAna, """
				{"nombre": "Mi mercado", "tipo": "GASTO"}
				""").andExpect(status().isForbidden());
		pedir(DELETE, ruta, tokenAna).andExpect(status().isForbidden());
	}

	@Test
	void otraPersonaRecibe404AlModificarUnaCategoriaAjena() throws Exception {
		String categoria = idDe(crearCategoria(tokenAna, "Mascotas", "GASTO"));
		String tokenBeto = registrar("beto@correo.com");

		pedir(PUT, "/api/categorias/" + categoria, tokenBeto, """
				{"nombre": "Hackeada", "tipo": "GASTO"}
				""").andExpect(status().isNotFound());
		pedir(DELETE, "/api/categorias/" + categoria, tokenBeto).andExpect(status().isNotFound());
	}

	@Test
	void noPermiteCambiarElTipoDeUnaCategoriaConMovimientosYAlEliminarlaLaArchiva() throws Exception {
		String categoria = idDe(crearCategoria(tokenAna, "Mascotas", "GASTO"));
		String cuenta = idDe(pedir(POST, "/api/cuentas", tokenAna, """
				{"nombre": "Efectivo", "tipo": "EFECTIVO", "saldoInicial": 0}
				"""));
		insertarMovimiento(cuenta, java.util.UUID.fromString(categoria), "GASTO", "35000");

		pedir(PUT, "/api/categorias/" + categoria, tokenAna, """
				{"nombre": "Mascotas", "tipo": "INGRESO"}
				""").andExpect(status().isConflict());

		pedir(DELETE, "/api/categorias/" + categoria, tokenAna)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.archivada").value(true));
	}

	@Test
	void unaCategoriaSinUsoSeEliminaYUnColorInvalidoSeRechaza() throws Exception {
		String categoria = idDe(crearCategoria(tokenAna, "Temporal", "GASTO"));
		pedir(DELETE, "/api/categorias/" + categoria, tokenAna).andExpect(status().isNoContent());

		pedir(POST, "/api/categorias", tokenAna, """
				{"nombre": "Colorida", "tipo": "GASTO", "color": "rojo"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.color").exists());
	}

}
