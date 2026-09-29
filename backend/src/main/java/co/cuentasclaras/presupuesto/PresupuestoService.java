package co.cuentasclaras.presupuesto;

import co.cuentasclaras.categoria.Categoria;
import co.cuentasclaras.categoria.CategoriaRepository;
import co.cuentasclaras.categoria.TipoCategoria;
import co.cuentasclaras.comun.error.RecursoNoEncontradoException;
import co.cuentasclaras.comun.error.ReglaDeNegocioException;
import co.cuentasclaras.movimiento.MovimientoDtos.CategoriaResumen;
import co.cuentasclaras.movimiento.MovimientoRepository;
import co.cuentasclaras.presupuesto.PresupuestoDtos.PresupuestoRequest;
import co.cuentasclaras.presupuesto.PresupuestoDtos.PresupuestoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PresupuestoService {

	private static final BigDecimal CIEN = new BigDecimal("100");

	private final PresupuestoRepository presupuestoRepository;
	private final CategoriaRepository categoriaRepository;
	private final MovimientoRepository movimientoRepository;

	public PresupuestoService(PresupuestoRepository presupuestoRepository, CategoriaRepository categoriaRepository,
			MovimientoRepository movimientoRepository) {
		this.presupuestoRepository = presupuestoRepository;
		this.categoriaRepository = categoriaRepository;
		this.movimientoRepository = movimientoRepository;
	}

	/** Presupuestos del mes con lo gastado, lo disponible y el semáforo; los más usados primero. */
	@Transactional(readOnly = true)
	public List<PresupuestoResponse> delMes(UUID usuarioId, YearMonth mes) {
		List<Presupuesto> presupuestos = presupuestoRepository.findByUsuarioIdAndMes(usuarioId, mes.toString());
		if (presupuestos.isEmpty()) {
			return List.of();
		}
		Map<UUID, BigDecimal> gastado = movimientoRepository
				.gastosPorCategoria(usuarioId, mes.atDay(1), mes.atEndOfMonth()).stream()
				.collect(Collectors.toMap(MovimientoRepository.TotalPorCategoria::getCategoriaId,
						MovimientoRepository.TotalPorCategoria::getTotal));
		Map<UUID, Categoria> categorias = categoriaRepository
				.findAllById(presupuestos.stream().map(Presupuesto::getCategoriaId).toList()).stream()
				.collect(Collectors.toMap(Categoria::getId, Function.identity()));

		return presupuestos.stream()
				.map(p -> aRespuesta(p, categorias.get(p.getCategoriaId()),
						gastado.getOrDefault(p.getCategoriaId(), BigDecimal.ZERO)))
				.sorted(Comparator.comparing(PresupuestoResponse::porcentajeUsado).reversed())
				.toList();
	}

	/**
	 * HU-06. PUT idempotente: si ya hay presupuesto para esa categoría y mes se
	 * actualiza el límite, si no se crea. Así nunca hay dos (RN-08) y el cliente
	 * no tiene que saber cuál de los dos casos es.
	 */
	@Transactional
	public PresupuestoResponse definir(UUID usuarioId, PresupuestoRequest solicitud) {
		YearMonth mes = YearMonth.parse(solicitud.mes());
		Categoria categoria = categoriaRepository.visiblePara(solicitud.categoriaId(), usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada."));
		if (categoria.getTipo() != TipoCategoria.GASTO) {
			throw new ReglaDeNegocioException("Solo se pueden presupuestar categorías de gasto.");
		}
		if (categoria.isArchivada()) {
			throw new ReglaDeNegocioException("La categoría \"" + categoria.getNombre() + "\" está archivada.");
		}

		Presupuesto presupuesto = presupuestoRepository
				.findByUsuarioIdAndCategoriaIdAndMes(usuarioId, categoria.getId(), mes.toString())
				.orElseGet(() -> new Presupuesto(usuarioId, categoria.getId(), mes, solicitud.montoLimite()));
		presupuesto.cambiarLimite(solicitud.montoLimite());
		presupuestoRepository.save(presupuesto);

		BigDecimal gastado = movimientoRepository.gastosPorCategoria(usuarioId, mes.atDay(1), mes.atEndOfMonth())
				.stream().filter(t -> t.getCategoriaId().equals(categoria.getId()))
				.map(MovimientoRepository.TotalPorCategoria::getTotal).findFirst().orElse(BigDecimal.ZERO);
		return aRespuesta(presupuesto, categoria, gastado);
	}

	@Transactional
	public void eliminar(UUID usuarioId, UUID presupuestoId) {
		Presupuesto presupuesto = presupuestoRepository.findByIdAndUsuarioId(presupuestoId, usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Presupuesto no encontrado."));
		presupuestoRepository.delete(presupuesto);
	}

	private static PresupuestoResponse aRespuesta(Presupuesto p, Categoria categoria, BigDecimal gastado) {
		// Porcentaje con 1 decimal, redondeo comercial (HALF_UP): 79,95 % → 80,0 %.
		BigDecimal porcentaje = gastado.multiply(CIEN).divide(p.getMontoLimite(), 1, RoundingMode.HALF_UP);
		return new PresupuestoResponse(p.getId(),
				new CategoriaResumen(categoria.getId(), categoria.getNombre(), categoria.getColor()),
				p.getMes().toString(), p.getMontoLimite(), gastado, p.getMontoLimite().subtract(gastado), porcentaje,
				EstadoPresupuesto.segun(porcentaje));
	}

}
