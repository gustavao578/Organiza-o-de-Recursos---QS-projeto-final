package br.edu.organizacaorecursos.repository;

import br.edu.organizacaorecursos.domain.Reserva;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    @Query("""
            SELECT COUNT(r) > 0
            FROM Reserva r
            WHERE r.recurso.id = :recursoId
              AND r.dataInicio < :dataFim
              AND r.dataFim > :dataInicio
            """)
    boolean existeConflitoDeHorario(
            @Param("recursoId") UUID recursoId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query("""
            SELECT r
            FROM Reserva r
            WHERE r.recurso.id = :recursoId
              AND r.dataInicio < :dataFim
              AND r.dataFim > :dataInicio
            ORDER BY r.dataInicio
            """)
    List<Reserva> buscarConflitosDeHorario(
            @Param("recursoId") UUID recursoId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);
}
