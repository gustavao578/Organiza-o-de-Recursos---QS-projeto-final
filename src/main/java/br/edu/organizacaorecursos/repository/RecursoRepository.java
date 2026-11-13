package br.edu.organizacaorecursos.repository;

import br.edu.organizacaorecursos.domain.Recurso;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecursoRepository extends JpaRepository<Recurso, UUID> {

    @Query("""
            SELECT r
            FROM Recurso r
            WHERE (:tipo IS NULL OR LOWER(r.tipo) = LOWER(:tipo))
              AND (:restrito IS NULL OR r.restrito = :restrito)
            ORDER BY r.nome
            """)
    List<Recurso> filtrar(
            @Param("tipo") String tipo,
            @Param("restrito") Boolean restrito);

    boolean existsByResponsavelId(UUID responsavelId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Recurso r WHERE r.id = :recursoId")
    Optional<Recurso> buscarPorIdComBloqueio(@Param("recursoId") UUID recursoId);
}
