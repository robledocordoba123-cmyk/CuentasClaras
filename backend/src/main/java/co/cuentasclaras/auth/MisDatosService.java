package co.cuentasclaras.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ley 1581, derecho de acceso: la persona descarga en un archivo todo lo que la
 * aplicación guarda de ella. Nunca incluye el hash de la contraseña.
 */
@Service
public class MisDatosService {

	private final JdbcTemplate jdbc;
	private final Clock reloj;

	public MisDatosService(JdbcTemplate jdbc, Clock reloj) {
		this.jdbc = jdbc;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> exportar(UUID usuarioId) {
		Map<String, Object> datos = new LinkedHashMap<>();
		datos.put("generadoEn", reloj.instant().toString());
		datos.put("persona", jdbc.queryForMap("""
				SELECT id, nombre, email, creado_en, autorizo_datos_en, version_politica
				FROM usuarios WHERE id = ?""", usuarioId));
		datos.put("cuentas", lista("""
				SELECT id, nombre, tipo, saldo_inicial, archivada, creado_en
				FROM cuentas WHERE usuario_id = ? ORDER BY creado_en""", usuarioId));
		datos.put("categoriasPropias", lista("""
				SELECT id, nombre, tipo, color, archivada
				FROM categorias WHERE usuario_id = ? ORDER BY nombre""", usuarioId));
		datos.put("movimientos", lista("""
				SELECT id, cuenta_id, categoria_id, tipo, monto, fecha, descripcion, transferencia_id, creado_en
				FROM movimientos WHERE usuario_id = ? ORDER BY fecha, creado_en""", usuarioId));
		datos.put("presupuestos", lista("""
				SELECT id, categoria_id, mes, monto_limite
				FROM presupuestos WHERE usuario_id = ? ORDER BY mes""", usuarioId));
		return datos;
	}

	private List<Map<String, Object>> lista(String sql, UUID usuarioId) {
		return jdbc.queryForList(sql, usuarioId);
	}

}
