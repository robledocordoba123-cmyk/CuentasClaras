package co.cuentasclaras.categoria;

import co.cuentasclaras.categoria.CategoriaDtos.CategoriaRequest;
import co.cuentasclaras.categoria.CategoriaDtos.CategoriaResponse;
import co.cuentasclaras.comun.error.AccionNoPermitidaException;
import co.cuentasclaras.comun.error.RecursoEnConflictoException;
import co.cuentasclaras.comun.error.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoriaService {

	/** Id que no existe: para "excluir ninguna" en la validación de nombre repetido. */
	private static final UUID NINGUNA = new UUID(0, 0);

	private final CategoriaRepository categoriaRepository;

	public CategoriaService(CategoriaRepository categoriaRepository) {
		this.categoriaRepository = categoriaRepository;
	}

	@Transactional(readOnly = true)
	public List<CategoriaResponse> listar(UUID usuarioId, TipoCategoria tipo, boolean incluirArchivadas) {
		return categoriaRepository.visiblesPara(usuarioId).stream()
				.filter(c -> tipo == null || c.getTipo() == tipo)
				.filter(c -> incluirArchivadas || !c.isArchivada())
				.map(CategoriaService::aRespuesta)
				.toList();
	}

	@Transactional
	public CategoriaResponse crear(UUID usuarioId, CategoriaRequest solicitud) {
		validarNombreLibre(usuarioId, solicitud, NINGUNA);
		Categoria categoria = categoriaRepository.save(
				new Categoria(usuarioId, solicitud.nombre(), solicitud.tipo(), solicitud.color()));
		return aRespuesta(categoria);
	}

	@Transactional
	public CategoriaResponse actualizar(UUID usuarioId, UUID categoriaId, CategoriaRequest solicitud) {
		Categoria categoria = buscarEditable(usuarioId, categoriaId);
		// RN-07: si ya clasifica movimientos, cambiarle el tipo dejaría gastos en
		// una categoría de ingreso (o al revés).
		if (categoria.getTipo() != solicitud.tipo() && categoriaRepository.estaEnUso(categoriaId)) {
			throw new RecursoEnConflictoException(
					"No puedes cambiar el tipo de una categoría que ya tiene movimientos o presupuestos.");
		}
		validarNombreLibre(usuarioId, solicitud, categoriaId);
		categoria.actualizar(solicitud.nombre(), solicitud.tipo(), solicitud.color());
		return aRespuesta(categoria);
	}

	/**
	 * RN-10: si la categoría está en uso se archiva; si no, se elimina.
	 *
	 * @return la categoría archivada, o null si se eliminó
	 */
	@Transactional
	public CategoriaResponse eliminar(UUID usuarioId, UUID categoriaId) {
		Categoria categoria = buscarEditable(usuarioId, categoriaId);
		if (categoriaRepository.estaEnUso(categoriaId)) {
			categoria.archivar();
			return aRespuesta(categoria);
		}
		categoriaRepository.delete(categoria);
		return null;
	}

	@Transactional
	public CategoriaResponse restaurar(UUID usuarioId, UUID categoriaId) {
		Categoria categoria = buscarEditable(usuarioId, categoriaId);
		validarNombreLibre(usuarioId,
				new CategoriaRequest(categoria.getNombre(), categoria.getTipo(), categoria.getColor()), categoriaId);
		categoria.restaurar();
		return aRespuesta(categoria);
	}

	/**
	 * Solo se editan las propias. Una ajena da 404 (no se revela que existe);
	 * una por defecto se puede ver, pero modificarla da 403.
	 */
	private Categoria buscarEditable(UUID usuarioId, UUID categoriaId) {
		Categoria categoria = categoriaRepository.visiblePara(categoriaId, usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada."));
		if (categoria.esPorDefecto()) {
			throw new AccionNoPermitidaException("Las categorías por defecto no se pueden modificar ni eliminar.");
		}
		return categoria;
	}

	private void validarNombreLibre(UUID usuarioId, CategoriaRequest solicitud, UUID excluirId) {
		if (categoriaRepository.existeNombre(usuarioId, solicitud.nombre(), solicitud.tipo(), excluirId)) {
			throw new RecursoEnConflictoException(
					"Ya existe una categoría de " + solicitud.tipo().name().toLowerCase()
							+ " llamada \"" + solicitud.nombre() + "\".");
		}
	}

	private static CategoriaResponse aRespuesta(Categoria c) {
		return new CategoriaResponse(c.getId(), c.getNombre(), c.getTipo(), c.getColor(), c.esPorDefecto(),
				c.isArchivada());
	}

}
