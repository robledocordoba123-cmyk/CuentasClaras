package co.cuentasclaras.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data genera la implementación a partir del nombre de cada método:
 * findByEmail se convierte en "SELECT ... WHERE email = ?".
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

	Optional<Usuario> findByEmail(String email);

	boolean existsByEmail(String email);

}
