package co.cuentasclaras.transferencia;

import co.cuentasclaras.comun.error.RecursoNoEncontradoException;
import co.cuentasclaras.comun.error.ReglaDeNegocioException;
import co.cuentasclaras.cuenta.Cuenta;
import co.cuentasclaras.cuenta.CuentaRepository;
import co.cuentasclaras.movimiento.Movimiento;
import co.cuentasclaras.movimiento.MovimientoDtos.Referencia;
import co.cuentasclaras.movimiento.MovimientoRepository;
import co.cuentasclaras.movimiento.TipoMovimiento;
import co.cuentasclaras.transferencia.TransferenciaDtos.TransferenciaRequest;
import co.cuentasclaras.transferencia.TransferenciaDtos.TransferenciaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * HU-04: mover plata entre cuentas propias. Una transferencia son dos
 * movimientos unidos por el mismo transferenciaId: una salida en la cuenta de
 * origen y una entrada en la de destino.
 */
@Service
public class TransferenciaService {

	private final MovimientoRepository movimientoRepository;
	private final CuentaRepository cuentaRepository;

	public TransferenciaService(MovimientoRepository movimientoRepository, CuentaRepository cuentaRepository) {
		this.movimientoRepository = movimientoRepository;
		this.cuentaRepository = cuentaRepository;
	}

	/**
	 * RN-05: las dos partes se guardan en UNA transacción. Si la segunda falla
	 * (se cae la conexión, el servidor se reinicia…), la base deshace también la
	 * primera: nunca queda plata que "sale" de una cuenta sin "entrar" a la otra.
	 */
	@Transactional
	public TransferenciaResponse crear(UUID usuarioId, TransferenciaRequest solicitud) {
		if (solicitud.cuentaOrigenId().equals(solicitud.cuentaDestinoId())) {
			throw new ReglaDeNegocioException("La cuenta de origen y la de destino deben ser distintas.");
		}
		Cuenta origen = cuentaActivaPropia(usuarioId, solicitud.cuentaOrigenId());
		Cuenta destino = cuentaActivaPropia(usuarioId, solicitud.cuentaDestinoId());

		UUID transferenciaId = UUID.randomUUID();
		movimientoRepository.save(Movimiento.parteDeTransferencia(usuarioId, origen.getId(),
				TipoMovimiento.TRANSFERENCIA_SALIDA, solicitud.monto(), solicitud.fecha(), solicitud.descripcion(),
				transferenciaId));
		movimientoRepository.save(Movimiento.parteDeTransferencia(usuarioId, destino.getId(),
				TipoMovimiento.TRANSFERENCIA_ENTRADA, solicitud.monto(), solicitud.fecha(), solicitud.descripcion(),
				transferenciaId));

		return new TransferenciaResponse(transferenciaId, solicitud.monto(), solicitud.fecha(),
				solicitud.descripcion(), referencia(origen), referencia(destino));
	}

	@Transactional(readOnly = true)
	public TransferenciaResponse obtener(UUID usuarioId, UUID transferenciaId) {
		List<Movimiento> partes = partesPropias(usuarioId, transferenciaId);
		Movimiento salida = parte(partes, TipoMovimiento.TRANSFERENCIA_SALIDA);
		Movimiento entrada = parte(partes, TipoMovimiento.TRANSFERENCIA_ENTRADA);
		Cuenta origen = cuentaRepository.findByIdAndUsuarioId(salida.getCuentaId(), usuarioId).orElseThrow();
		Cuenta destino = cuentaRepository.findByIdAndUsuarioId(entrada.getCuentaId(), usuarioId).orElseThrow();
		return new TransferenciaResponse(transferenciaId, salida.getMonto(), salida.getFecha(),
				salida.getDescripcion(), referencia(origen), referencia(destino));
	}

	/** Se eliminan las dos partes juntas, también en una sola transacción. */
	@Transactional
	public void eliminar(UUID usuarioId, UUID transferenciaId) {
		movimientoRepository.deleteAll(partesPropias(usuarioId, transferenciaId));
	}

	private List<Movimiento> partesPropias(UUID usuarioId, UUID transferenciaId) {
		List<Movimiento> partes = movimientoRepository.findByTransferenciaIdAndUsuarioId(transferenciaId, usuarioId);
		if (partes.isEmpty()) {
			throw new RecursoNoEncontradoException("Transferencia no encontrada.");
		}
		return partes;
	}

	private static Movimiento parte(List<Movimiento> partes, TipoMovimiento tipo) {
		return partes.stream().filter(m -> m.getTipo() == tipo).findFirst().orElseThrow();
	}

	private Cuenta cuentaActivaPropia(UUID usuarioId, UUID cuentaId) {
		Cuenta cuenta = cuentaRepository.findByIdAndUsuarioId(cuentaId, usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada."));
		if (cuenta.isArchivada()) {
			throw new ReglaDeNegocioException("La cuenta \"" + cuenta.getNombre() + "\" está archivada.");
		}
		return cuenta;
	}

	private static Referencia referencia(Cuenta cuenta) {
		return new Referencia(cuenta.getId(), cuenta.getNombre());
	}

}
