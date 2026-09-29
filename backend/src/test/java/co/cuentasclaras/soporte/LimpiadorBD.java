package co.cuentasclaras.soporte;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Deja la base limpia antes de cada prueba. El orden respeta las llaves
 * foráneas (primero lo que depende de otras tablas) y conserva las categorías
 * por defecto, que vienen de la migración V1.
 */
@Component
public class LimpiadorBD {

	private final JdbcTemplate jdbc;

	public LimpiadorBD(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public void limpiar() {
		jdbc.execute("DELETE FROM movimientos");
		jdbc.execute("DELETE FROM presupuestos");
		jdbc.execute("DELETE FROM cuentas");
		jdbc.execute("DELETE FROM categorias WHERE usuario_id IS NOT NULL");
		jdbc.execute("DELETE FROM usuarios");
	}

}
