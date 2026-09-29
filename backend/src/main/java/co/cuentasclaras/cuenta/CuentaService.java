package co.cuentasclaras.cuenta;

import co.cuentasclaras.comun.error.RecursoEnConflictoException;
import co.cuentasclaras.comun.error.RecursoNoEncontradoException;
import co.cuentasclaras.cuenta.CuentaDtos.CuentaRequest;
import co.cuentasclaras.cuenta.CuentaDtos.CuentaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Todas las operaciones reciben el id de la dueña (sacado del JWT) y lo usan en
 * cada consulta. No existe ningún método que busque una cuenta solo por su id.
 */
@Service
public class CuentaService {

	private final CuentaRepository cuentaRepository;

	public CuentaService(CuentaRepository cuentaRepository) {
		this.cuentaRepository = cuentaRepository;
	}

	@Transactional(readOnly = true)
	public List<CuentaResponse> listar(UUID usuarioId, boolean incluirArchivadas) {
		List<Cuenta> cuentas = incluirArchivadas
				? cuentaRepository.findByUsuarioIdOrderByCreadoEnAsc(usuarioId)
				: cuentaRepository.findByUsuarioIdAndArchivadaFalseOrderByCreadoEnAsc(usuarioId);
		Map<UUID, BigDecimal> netos = netosPorCuenta(usuarioId);
		return cuentas.stream().map(c -> aRespuesta(c, netos)).toList();
	}

	@Transactional(readOnly = true)
	public CuentaResponse obtener(UUID usuarioId, UUID cuentaId) {
		return aRespuesta(buscarPropia(usuarioId, cuentaId), netosPorCuenta(usuarioId));
	}

	@Transactional
	public CuentaResponse crear(UUID usuarioId, CuentaRequest solicitud) {
		if (cuentaRepository.existsByUsuarioIdAndNombreIgnoreCaseAndArchivadaFalse(usuarioId, solicitud.nombre())) {
			throw nombreRepetido(solicitud.nombre());
		}
		Cuenta cuenta = cuentaRepository.save(
				new Cuenta(usuarioId, solicitud.nombre(), solicitud.tipo(), solicitud.saldoInicial()));
		return aRespuesta(cuenta, Map.of());
	}

	@Transactional
	public CuentaResponse actualizar(UUID usuarioId, UUID cuentaId, CuentaRequest solicitud) {
		Cuenta cuenta = buscarPropia(usuarioId, cuentaId);
		if (cuentaRepository.existsByUsuarioIdAndNombreIgnoreCaseAndArchivadaFalseAndIdNot(
				usuarioId, solicitud.nombre(), cuentaId)) {
			throw nombreRepetido(solicitud.nombre());
		}
		cuenta.actualizar(solicitud.nombre(), solicitud.tipo(), solicitud.saldoInicial());
		return aRespuesta(cuenta, netosPorCuenta(usuarioId));
	}

	/**
	 * RN-10: si la cuenta tiene movimientos no se borra, se archiva (deja de
	 * aparecer en los formularios pero el historial y los reportes se conservan).
	 * Si nunca se usó, se borra de verdad.
	 *
	 * @return true si se archivó, false si se eliminó
	 */
	@Transactional
	public boolean eliminar(UUID usuarioId, UUID cuentaId) {
		Cuenta cuenta = buscarPropia(usuarioId, cuentaId);
		if (cuentaRepository.tieneMovimientos(cuentaId)) {
			cuenta.archivar();
			return true;
		}
		cuentaRepository.delete(cuenta);
		return false;
	}

	@Transactional
	public CuentaResponse restaurar(UUID usuarioId, UUID cuentaId) {
		Cuenta cuenta = buscarPropia(usuarioId, cuentaId);
		if (cuentaRepository.existsByUsuarioIdAndNombreIgnoreCaseAndArchivadaFalseAndIdNot(
				usuarioId, cuenta.getNombre(), cuentaId)) {
			throw nombreRepetido(cuenta.getNombre());
		}
		cuenta.restaurar();
		return aRespuesta(cuenta, netosPorCuenta(usuarioId));
	}

	private Cuenta buscarPropia(UUID usuarioId, UUID cuentaId) {
		return cuentaRepository.findByIdAndUsuarioId(cuentaId, usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada."));
	}

	private Map<UUID, BigDecimal> netosPorCuenta(UUID usuarioId) {
		return cuentaRepository.netoPorCuenta(usuarioId).stream()
				.collect(Collectors.toMap(CuentaRepository.NetoPorCuenta::getCuentaId,
						CuentaRepository.NetoPorCuenta::getNeto));
	}

	private static CuentaResponse aRespuesta(Cuenta cuenta, Map<UUID, BigDecimal> netos) {
		BigDecimal saldoActual = cuenta.getSaldoInicial().add(netos.getOrDefault(cuenta.getId(), BigDecimal.ZERO));
		return new CuentaResponse(cuenta.getId(), cuenta.getNombre(), cuenta.getTipo(),
				cuenta.getSaldoInicial(), saldoActual, cuenta.isArchivada());
	}

	private static RecursoEnConflictoException nombreRepetido(String nombre) {
		return new RecursoEnConflictoException("Ya tienes una cuenta activa llamada \"" + nombre + "\".");
	}

}
