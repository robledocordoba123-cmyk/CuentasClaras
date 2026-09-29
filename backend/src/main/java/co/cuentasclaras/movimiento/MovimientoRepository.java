package co.cuentasclaras.movimiento;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JpaSpecificationExecutor permite armar la consulta según los filtros que
 * lleguen (fecha, cuenta, categoría, tipo) sin escribir una consulta distinta
 * por cada combinación.
 */
public interface MovimientoRepository extends JpaRepository<Movimiento, UUID>, JpaSpecificationExecutor<Movimiento> {

	Optional<Movimiento> findByIdAndUsuarioId(UUID id, UUID usuarioId);

	List<Movimiento> findByTransferenciaIdAndUsuarioId(UUID transferenciaId, UUID usuarioId);

	List<Movimiento> findByUsuarioIdAndFechaBetweenOrderByFechaAscCreadoEnAsc(UUID usuarioId, LocalDate desde,
			LocalDate hasta);

	/**
	 * Total gastado por categoría en un rango de fechas. Solo GASTO: las
	 * transferencias no son gasto (RN-06).
	 */
	@Query(value = """
			SELECT categoria_id AS categoriaId, SUM(monto) AS total
			FROM movimientos
			WHERE usuario_id = :usuarioId AND tipo = 'GASTO' AND fecha BETWEEN :desde AND :hasta
			GROUP BY categoria_id
			""", nativeQuery = true)
	List<TotalPorCategoria> gastosPorCategoria(@Param("usuarioId") UUID usuarioId, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta);

	/** Ingresos y gastos por mes (AAAA-MM), sin transferencias (RN-06). Una fila por mes con datos. */
	@Query(value = """
			SELECT to_char(fecha, 'YYYY-MM') AS mes,
			       COALESCE(SUM(monto) FILTER (WHERE tipo = 'INGRESO'), 0) AS ingresos,
			       COALESCE(SUM(monto) FILTER (WHERE tipo = 'GASTO'), 0) AS gastos
			FROM movimientos
			WHERE usuario_id = :usuarioId AND fecha BETWEEN :desde AND :hasta
			GROUP BY to_char(fecha, 'YYYY-MM')
			""", nativeQuery = true)
	List<TotalesDelMes> totalesPorMes(@Param("usuarioId") UUID usuarioId, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta);

	interface TotalPorCategoria {

		UUID getCategoriaId();

		BigDecimal getTotal();

	}

	interface TotalesDelMes {

		String getMes();

		BigDecimal getIngresos();

		BigDecimal getGastos();

	}

	/** Filtros del listado. Cada uno se aplica solo si viene; la dueña, siempre (RN-01). */
	final class Filtros {

		private Filtros() {
		}

		static Specification<Movimiento> de(UUID usuarioId, LocalDate desde, LocalDate hasta, UUID cuentaId,
				UUID categoriaId, TipoMovimiento tipo) {
			Specification<Movimiento> spec = (raiz, consulta, cb) -> cb.equal(raiz.get("usuarioId"), usuarioId);
			if (desde != null) {
				spec = spec.and((raiz, consulta, cb) -> cb.greaterThanOrEqualTo(raiz.get("fecha"), desde));
			}
			if (hasta != null) {
				spec = spec.and((raiz, consulta, cb) -> cb.lessThanOrEqualTo(raiz.get("fecha"), hasta));
			}
			if (cuentaId != null) {
				spec = spec.and((raiz, consulta, cb) -> cb.equal(raiz.get("cuentaId"), cuentaId));
			}
			if (categoriaId != null) {
				spec = spec.and((raiz, consulta, cb) -> cb.equal(raiz.get("categoriaId"), categoriaId));
			}
			if (tipo != null) {
				spec = spec.and((raiz, consulta, cb) -> cb.equal(raiz.get("tipo"), tipo));
			}
			return spec;
		}

	}

}
