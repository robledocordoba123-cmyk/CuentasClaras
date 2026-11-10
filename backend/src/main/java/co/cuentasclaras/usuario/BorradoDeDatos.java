package co.cuentasclaras.usuario;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Borra todo lo de una persona, en el orden que piden las llaves foráneas. Un
 * solo lugar lo sabe: lo usan el derecho de supresión (Ley 1581) y la demo al
 * recrearse. Cuando aparece una tabla nueva, se agrega aquí y en ningún otro lado.
 */
@Component
public class BorradoDeDatos {

	private final JdbcTemplate jdbc;

	public BorradoDeDatos(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void borrarTodoDe(UUID usuarioId) {
		jdbc.update("DELETE FROM abonos WHERE deuda_id IN (SELECT id FROM deudas WHERE usuario_id = ?)", usuarioId);
		jdbc.update("DELETE FROM deudas WHERE usuario_id = ?", usuarioId);
		jdbc.update("DELETE FROM personas WHERE usuario_id = ?", usuarioId);
		jdbc.update("DELETE FROM aportes WHERE meta_id IN (SELECT id FROM metas WHERE usuario_id = ?)", usuarioId);
		jdbc.update("DELETE FROM metas WHERE usuario_id = ?", usuarioId);
		jdbc.update("DELETE FROM movimientos WHERE usuario_id = ?", usuarioId);
		jdbc.update("DELETE FROM pagos_fijos WHERE usuario_id = ?", usuarioId);
		jdbc.update("DELETE FROM presupuestos WHERE usuario_id = ?", usuarioId);
		jdbc.update("DELETE FROM cuentas WHERE usuario_id = ?", usuarioId);
		jdbc.update("DELETE FROM categorias WHERE usuario_id = ?", usuarioId);
		jdbc.update("DELETE FROM usuarios WHERE id = ?", usuarioId);
	}

}
