package br.edu.organizacaorecursos.repository;

import br.edu.organizacaorecursos.domain.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailIgnoreCaseOrMatricula(String email, String matricula);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT u FROM Usuario u WHERE u.id = :usuarioId")
	Optional<Usuario> buscarPorIdComBloqueio(@Param("usuarioId") UUID usuarioId);
}
