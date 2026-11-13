package br.edu.organizacaorecursos.repository;

import br.edu.organizacaorecursos.domain.Manutencao;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManutencaoRepository extends JpaRepository<Manutencao, UUID> {

    @Query("""
            SELECT m
            FROM Manutencao m
            WHERE :recursoId IS NULL OR m.recurso.id = :recursoId
            ORDER BY m.dataInicio
            """)
    List<Manutencao> filtrar(@Param("recursoId") UUID recursoId);

    boolean existsByRecurso_Id(UUID recursoId);

    @Query("""
            SELECT COUNT(m) > 0
            FROM Manutencao m
            WHERE m.recurso.id = :recursoId
              AND m.dataInicio < :dataFim
              AND m.dataFim > :dataInicio
            """)
    boolean isRecursoEmManutencao(
            @Param("recursoId") UUID recursoId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);
}
