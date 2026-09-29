package co.cuentasclaras.categoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

	/** Visibles para una persona: las por defecto (sin dueña) más las suyas. */
	@Query("""
			SELECT c FROM Categoria c
			WHERE c.usuarioId IS NULL OR c.usuarioId = :usuarioId
			ORDER BY c.tipo, c.nombre
			""")
	List<Categoria> visiblesPara(@Param("usuarioId") UUID usuarioId);

	/** Una categoría solo se encuentra si es por defecto o de la persona (RN-01). */
	@Query("""
			SELECT c FROM Categoria c
			WHERE c.id = :id AND (c.usuarioId IS NULL OR c.usuarioId = :usuarioId)
			""")
	Optional<Categoria> visiblePara(@Param("id") UUID id, @Param("usuarioId") UUID usuarioId);

	/**
	 * ¿Ya hay una categoría activa visible con ese nombre y tipo? Incluye las por
	 * defecto: no tiene sentido crear un segundo "Mercado" de gasto.
	 */
	@Query("""
			SELECT COUNT(c) > 0 FROM Categoria c
			WHERE (c.usuarioId IS NULL OR c.usuarioId = :usuarioId)
			  AND LOWER(c.nombre) = LOWER(:nombre)
			  AND c.tipo = :tipo
			  AND c.archivada = FALSE
			  AND c.id <> :excluirId
			""")
	boolean existeNombre(@Param("usuarioId") UUID usuarioId, @Param("nombre") String nombre,
			@Param("tipo") TipoCategoria tipo, @Param("excluirId") UUID excluirId);

	@Query(value = """
			SELECT EXISTS (SELECT 1 FROM movimientos WHERE categoria_id = :id)
			    OR EXISTS (SELECT 1 FROM presupuestos WHERE categoria_id = :id)
			""", nativeQuery = true)
	boolean estaEnUso(@Param("id") UUID id);

}
