package co.cuentasclaras.resumen;

import co.cuentasclaras.categoria.Categoria;
import co.cuentasclaras.categoria.CategoriaRepository;
import co.cuentasclaras.movimiento.MovimientoDtos.CategoriaResumen;
import co.cuentasclaras.movimiento.MovimientoRepository;
import co.cuentasclaras.presupuesto.EstadoPresupuesto;
import co.cuentasclaras.presupuesto.PresupuestoDtos.PresupuestoResponse;
import co.cuentasclaras.presupuesto.PresupuestoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** HU-08: resumen del mes y tendencia de los últimos meses. */
@Service
public class ResumenService {

	private static final BigDecimal CIEN = new BigDecimal("100");

	private final MovimientoRepository movimientoRepository;
	private final CategoriaRepository categoriaRepository;
	private final PresupuestoService presupuestoService;

	public ResumenService(MovimientoRepository movimientoRepository, CategoriaRepository categoriaRepository,
			PresupuestoService presupuestoService) {
		this.movimientoRepository = movimientoRepository;
		this.categoriaRepository = categoriaRepository;
		this.presupuestoService = presupuestoService;
	}

	@Transactional(readOnly = true)
	public ResumenMensual delMes(UUID usuarioId, YearMonth mes) {
		MovimientoRepository.TotalesDelMes totales = movimientoRepository
				.totalesPorMes(usuarioId, mes.atDay(1), mes.atEndOfMonth()).stream().findFirst().orElse(null);
		BigDecimal ingresos = totales == null ? BigDecimal.ZERO : totales.getIngresos();
		BigDecimal gastos = totales == null ? BigDecimal.ZERO : totales.getGastos();

		List<MovimientoRepository.TotalPorCategoria> porCategoria = movimientoRepository
				.gastosPorCategoria(usuarioId, mes.atDay(1), mes.atEndOfMonth());
		Map<UUID, Categoria> categorias = categoriaRepository
				.findAllById(porCategoria.stream().map(MovimientoRepository.TotalPorCategoria::getCategoriaId).toList())
				.stream().collect(Collectors.toMap(Categoria::getId, Function.identity()));

		List<GastoPorCategoria> gastosPorCategoria = porCategoria.stream()
				.map(t -> {
					Categoria c = categorias.get(t.getCategoriaId());
					return new GastoPorCategoria(new CategoriaResumen(c.getId(), c.getNombre(), c.getColor()),
							t.getTotal(), porcentaje(t.getTotal(), gastos));
				})
				.sorted(Comparator.comparing(GastoPorCategoria::total).reversed())
				.toList();

		// HU-07: las alertas son los presupuestos que ya pasaron del 80 %.
		List<PresupuestoResponse> alertas = presupuestoService.delMes(usuarioId, mes).stream()
				.filter(p -> p.estado() != EstadoPresupuesto.EN_CONTROL)
				.toList();

		return new ResumenMensual(mes.toString(), ingresos, gastos, ingresos.subtract(gastos), gastosPorCategoria,
				alertas);
	}

	/** Ingresos y gastos de los últimos N meses (incluido el actual), también los meses sin movimientos. */
	@Transactional(readOnly = true)
	public List<TotalesMes> tendencia(UUID usuarioId, YearMonth hasta, int meses) {
		YearMonth desde = hasta.minusMonths(meses - 1L);
		Map<String, MovimientoRepository.TotalesDelMes> conDatos = movimientoRepository
				.totalesPorMes(usuarioId, desde.atDay(1), hasta.atEndOfMonth()).stream()
				.collect(Collectors.toMap(MovimientoRepository.TotalesDelMes::getMes, Function.identity()));

		List<TotalesMes> resultado = new ArrayList<>();
		for (YearMonth mes = desde; !mes.isAfter(hasta); mes = mes.plusMonths(1)) {
			MovimientoRepository.TotalesDelMes t = conDatos.get(mes.toString());
			BigDecimal ingresos = t == null ? BigDecimal.ZERO : t.getIngresos();
			BigDecimal gastos = t == null ? BigDecimal.ZERO : t.getGastos();
			resultado.add(new TotalesMes(mes.toString(), ingresos, gastos, ingresos.subtract(gastos)));
		}
		return resultado;
	}

	private static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
		return total.signum() == 0 ? BigDecimal.ZERO : parte.multiply(CIEN).divide(total, 1, RoundingMode.HALF_UP);
	}

	public record GastoPorCategoria(CategoriaResumen categoria, BigDecimal total, BigDecimal porcentaje) {
	}

	public record ResumenMensual(String mes, BigDecimal totalIngresos, BigDecimal totalGastos, BigDecimal balance,
			List<GastoPorCategoria> gastosPorCategoria, List<PresupuestoResponse> alertas) {
	}

	public record TotalesMes(String mes, BigDecimal ingresos, BigDecimal gastos, BigDecimal balance) {
	}

}
