package co.cuentasclaras.movimiento;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
