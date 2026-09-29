package co.cuentasclaras.cuenta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CuentaRepository extends JpaRepository<Cuenta, UUID> {

	/**
	 * Busca por id Y dueña a la vez (RN-01). Si la cuenta es de otra persona el
	 * resultado es vacío, igual que si no existiera: nunca se consulta solo por id.
	 */
	Optional<Cuenta> findByIdAndUsuarioId(UUID id, UUID usuarioId);

	List<Cuenta> findByUsuarioIdOrderByCreadoEnAsc(UUID usuarioId);

	List<Cuenta> findByUsuarioIdAndArchivadaFalseOrderByCreadoEnAsc(UUID usuarioId);

	boolean existsByUsuarioIdAndNombreIgnoreCaseAndArchivadaFalse(UUID usuarioId, String nombre);

	boolean existsByUsuarioIdAndNombreIgnoreCaseAndArchivadaFalseAndIdNot(UUID usuarioId, String nombre, UUID id);

	@Query(value = "SELECT EXISTS (SELECT 1 FROM movimientos WHERE cuenta_id = :cuentaId)", nativeQuery = true)
	boolean tieneMovimientos(@Param("cuentaId") UUID cuentaId);

	/**
	 * RN-04: efecto neto de los movimientos en cada cuenta de la persona. Lo que
	 * entra (ingresos y transferencias recibidas) suma; lo que sale, resta.
	 * Saldo actual = saldo inicial + neto. Una sola consulta para todas las
	 * cuentas, en vez de una por cuenta.
	 */
	@Query(value = """
			SELECT m.cuenta_id AS cuentaId,
			       SUM(CASE WHEN m.tipo IN ('INGRESO', 'TRANSFERENCIA_ENTRADA') THEN m.monto
			                ELSE -m.monto END) AS neto
			FROM movimientos m
			WHERE m.usuario_id = :usuarioId
			GROUP BY m.cuenta_id
			""", nativeQuery = true)
	List<NetoPorCuenta> netoPorCuenta(@Param("usuarioId") UUID usuarioId);

	interface NetoPorCuenta {

		UUID getCuentaId();

		BigDecimal getNeto();

	}

}
