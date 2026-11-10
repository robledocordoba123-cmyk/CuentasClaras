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
		datos.put("personas", lista("SELECT id, nombre, creado_en FROM personas WHERE usuario_id = ? ORDER BY nombre", usuarioId));
		datos.put("deudas", lista("""
				SELECT d.id, d.persona_id, d.direccion, d.monto, d.descripcion, d.fecha, d.saldada_en,
				       (SELECT json_agg(json_build_object('monto', a.monto, 'fecha', a.fecha) ORDER BY a.fecha)::text
				        FROM abonos a WHERE a.deuda_id = d.id) AS abonos
				FROM deudas d WHERE d.usuario_id = ? ORDER BY d.fecha""", usuarioId));
		datos.put("pagosFijos", lista("""
				SELECT id, nombre, monto, dia_del_mes, categoria_id, cuenta_id, activo
				FROM pagos_fijos WHERE usuario_id = ? ORDER BY dia_del_mes""", usuarioId));
		datos.put("metas", lista("""
				SELECT m.id, m.nombre, m.monto_objetivo, m.fecha_objetivo,
				       (SELECT json_agg(json_build_object('monto', a.monto, 'fecha', a.fecha) ORDER BY a.fecha)::text
				        FROM aportes a WHERE a.meta_id = m.id) AS aportes
				FROM metas m WHERE m.usuario_id = ? ORDER BY m.creado_en""", usuarioId));
		datos.put("presupuestos", lista("""
				SELECT id, categoria_id, mes, monto_limite
				FROM presupuestos WHERE usuario_id = ? ORDER BY mes""", usuarioId));
		return datos;
	}

	private List<Map<String, Object>> lista(String sql, UUID usuarioId) {
		return jdbc.queryForList(sql, usuarioId);
	}

}
