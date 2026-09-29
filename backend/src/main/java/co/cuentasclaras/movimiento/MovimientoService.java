package co.cuentasclaras.movimiento;

import co.cuentasclaras.categoria.Categoria;
import co.cuentasclaras.categoria.CategoriaRepository;
import co.cuentasclaras.categoria.TipoCategoria;
import co.cuentasclaras.comun.PaginaResponse;
import co.cuentasclaras.comun.error.RecursoEnConflictoException;
import co.cuentasclaras.comun.error.RecursoNoEncontradoException;
import co.cuentasclaras.comun.error.ReglaDeNegocioException;
import co.cuentasclaras.cuenta.Cuenta;
import co.cuentasclaras.cuenta.CuentaRepository;
import co.cuentasclaras.movimiento.MovimientoDtos.CategoriaResumen;
import co.cuentasclaras.movimiento.MovimientoDtos.MovimientoRequest;
import co.cuentasclaras.movimiento.MovimientoDtos.MovimientoResponse;
import co.cuentasclaras.movimiento.MovimientoDtos.Referencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MovimientoService {

	public static final int TAMANO_MAXIMO_PAGINA = 100;

	private final MovimientoRepository movimientoRepository;
	private final CuentaRepository cuentaRepository;
	private final CategoriaRepository categoriaRepository;

	public MovimientoService(MovimientoRepository movimientoRepository, CuentaRepository cuentaRepository,
			CategoriaRepository categoriaRepository) {
		this.movimientoRepository = movimientoRepository;
		this.cuentaRepository = cuentaRepository;
		this.categoriaRepository = categoriaRepository;
	}

	/** HU-05: listado filtrado y paginado, lo más reciente primero. */
	@Transactional(readOnly = true)
	public PaginaResponse<MovimientoResponse> listar(UUID usuarioId, LocalDate desde, LocalDate hasta,
			UUID cuentaId, UUID categoriaId, TipoMovimiento tipo, int pagina, int tamano) {
		if (desde != null && hasta != null && desde.isAfter(hasta)) {
			throw new ReglaDeNegocioException("La fecha 'desde' no puede ser posterior a 'hasta'.");
		}
		PageRequest paginacion = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, TAMANO_MAXIMO_PAGINA),
				Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("creadoEn")));
		Page<Movimiento> resultado = movimientoRepository.findAll(
				MovimientoRepository.Filtros.de(usuarioId, desde, hasta, cuentaId, categoriaId, tipo), paginacion);
		return PaginaResponse.de(resultado, aRespuestas(resultado.getContent()));
	}

	/** HU-09: todos los movimientos de un mes, del más antiguo al más reciente, para exportar. */
	@Transactional(readOnly = true)
	public List<MovimientoResponse> delMes(UUID usuarioId, java.time.YearMonth mes) {
		return aRespuestas(movimientoRepository.findByUsuarioIdAndFechaBetweenOrderByFechaAscCreadoEnAsc(usuarioId,
				mes.atDay(1), mes.atEndOfMonth()));
	}

	@Transactional(readOnly = true)
	public MovimientoResponse obtener(UUID usuarioId, UUID movimientoId) {
		return aRespuestas(List.of(buscarPropio(usuarioId, movimientoId))).getFirst();
	}

	/** HU-03: registrar un ingreso o un gasto. */
	@Transactional
	public MovimientoResponse crear(UUID usuarioId, MovimientoRequest solicitud) {
		validar(usuarioId, solicitud);
		Movimiento movimiento = movimientoRepository.save(Movimiento.ingresoOGasto(usuarioId, solicitud.cuentaId(),
				solicitud.categoriaId(), solicitud.tipo(), solicitud.monto(), solicitud.fecha(),
				solicitud.descripcion()));
		return aRespuestas(List.of(movimiento)).getFirst();
	}

	@Transactional
	public MovimientoResponse actualizar(UUID usuarioId, UUID movimientoId, MovimientoRequest solicitud) {
		Movimiento movimiento = buscarPropio(usuarioId, movimientoId);
		rechazarSiEsTransferencia(movimiento);
		validar(usuarioId, solicitud);
		movimiento.actualizar(solicitud.cuentaId(), solicitud.categoriaId(), solicitud.tipo(), solicitud.monto(),
				solicitud.fecha(), solicitud.descripcion());
		return aRespuestas(List.of(movimiento)).getFirst();
	}

	@Transactional
	public void eliminar(UUID usuarioId, UUID movimientoId) {
		Movimiento movimiento = buscarPropio(usuarioId, movimientoId);
		rechazarSiEsTransferencia(movimiento);
		movimientoRepository.delete(movimiento);
	}

	/**
	 * Reglas de un ingreso o gasto: la cuenta y la categoría deben ser de la
	 * persona (o una categoría por defecto) y estar activas, y el tipo de la
	 * categoría debe coincidir con el del movimiento (RN-07).
	 */
	private void validar(UUID usuarioId, MovimientoRequest solicitud) {
		if (solicitud.tipo().esTransferencia()) {
			throw new ReglaDeNegocioException(
					"Para mover plata entre tus cuentas usa /api/transferencias; aquí solo INGRESO o GASTO.");
		}
		Cuenta cuenta = cuentaRepository.findByIdAndUsuarioId(solicitud.cuentaId(), usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada."));
		if (cuenta.isArchivada()) {
			throw new ReglaDeNegocioException("La cuenta \"" + cuenta.getNombre() + "\" está archivada.");
		}
		Categoria categoria = categoriaRepository.visiblePara(solicitud.categoriaId(), usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada."));
		if (categoria.isArchivada()) {
			throw new ReglaDeNegocioException("La categoría \"" + categoria.getNombre() + "\" está archivada.");
		}
		TipoCategoria esperado = solicitud.tipo() == TipoMovimiento.INGRESO ? TipoCategoria.INGRESO : TipoCategoria.GASTO;
		if (categoria.getTipo() != esperado) {
			throw new ReglaDeNegocioException("La categoría \"" + categoria.getNombre() + "\" es de "
					+ categoria.getTipo().name().toLowerCase() + " y no sirve para un "
					+ solicitud.tipo().name().toLowerCase() + ".");
		}
	}

	private static void rechazarSiEsTransferencia(Movimiento movimiento) {
		if (movimiento.getTipo().esTransferencia()) {
			throw new RecursoEnConflictoException(
					"Este movimiento es parte de una transferencia: edítala o elimínala desde /api/transferencias.");
		}
	}

	private Movimiento buscarPropio(UUID usuarioId, UUID movimientoId) {
		return movimientoRepository.findByIdAndUsuarioId(movimientoId, usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Movimiento no encontrado."));
	}

	/**
	 * Convierte a respuesta con los nombres de cuenta y categoría. Carga todas
	 * las cuentas y categorías de la página en dos consultas, en vez de dos
	 * consultas por movimiento (el problema "N+1").
	 */
	List<MovimientoResponse> aRespuestas(Collection<Movimiento> movimientos) {
		Map<UUID, Cuenta> cuentas = cuentaRepository.findAllById(
						movimientos.stream().map(Movimiento::getCuentaId).collect(Collectors.toSet()))
				.stream().collect(Collectors.toMap(Cuenta::getId, Function.identity()));
		Map<UUID, Categoria> categorias = categoriaRepository.findAllById(
						movimientos.stream().map(Movimiento::getCategoriaId).filter(Objects::nonNull)
								.collect(Collectors.toSet()))
				.stream().collect(Collectors.toMap(Categoria::getId, Function.identity()));

		return movimientos.stream().map(m -> {
			Cuenta cuenta = cuentas.get(m.getCuentaId());
			Categoria categoria = m.getCategoriaId() == null ? null : categorias.get(m.getCategoriaId());
			return new MovimientoResponse(m.getId(), m.getTipo(), m.getMonto(), m.getFecha(), m.getDescripcion(),
					new Referencia(cuenta.getId(), cuenta.getNombre()),
					categoria == null ? null
							: new CategoriaResumen(categoria.getId(), categoria.getNombre(), categoria.getColor()),
					m.getTransferenciaId());
		}).toList();
	}

}
