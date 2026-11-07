package br.edu.organizacaorecursos.repository;

import br.edu.organizacaorecursos.domain.Recurso;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecursoRepository extends JpaRepository<Recurso, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Recurso r WHERE r.id = :recursoId")
    Optional<Recurso> buscarPorIdComBloqueio(@Param("recursoId") UUID recursoId);
}
