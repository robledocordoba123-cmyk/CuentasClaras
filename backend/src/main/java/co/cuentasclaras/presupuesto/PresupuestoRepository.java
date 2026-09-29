package co.cuentasclaras.presupuesto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, UUID> {

	Optional<Presupuesto> findByIdAndUsuarioId(UUID id, UUID usuarioId);

	Optional<Presupuesto> findByUsuarioIdAndCategoriaIdAndMes(UUID usuarioId, UUID categoriaId, String mes);

	List<Presupuesto> findByUsuarioIdAndMes(UUID usuarioId, String mes);

}
