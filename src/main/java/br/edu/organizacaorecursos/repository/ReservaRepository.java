package br.edu.organizacaorecursos.repository;

import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.domain.StatusReserva;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    @Query("""
            SELECT r
            FROM Reserva r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:recursoId IS NULL OR r.recurso.id = :recursoId)
            ORDER BY r.dataInicio
            """)
    List<Reserva> filtrar(
            @Param("status") StatusReserva status,
            @Param("recursoId") UUID recursoId);

    boolean existsByUsuario_Id(UUID usuarioId);

    boolean existsByUsuario_IdAndStatusIn(
            UUID usuarioId, Collection<StatusReserva> statuses);

    boolean existsByRecurso_Id(UUID recursoId);

    boolean existsByRecurso_IdAndStatusIn(
            UUID recursoId, Collection<StatusReserva> statuses);

    @Query("""
            SELECT r
            FROM Reserva r
            WHERE r.dataInicio < :fim
              AND r.dataFim > :inicio
              AND r.status <> br.edu.organizacaorecursos.domain.StatusReserva.CANCELADA
            """)
    List<Reserva> buscarReservasSobrepondoPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("""
            SELECT COUNT(r) > 0
            FROM Reserva r
            WHERE r.recurso.id = :recursoId
              AND r.dataInicio < :dataFim
              AND r.dataFim > :dataInicio
              AND r.status IN (br.edu.organizacaorecursos.domain.StatusReserva.PENDENTE,
                               br.edu.organizacaorecursos.domain.StatusReserva.APROVADA,
                               br.edu.organizacaorecursos.domain.StatusReserva.EM_USO)
            """)
    boolean existeConflitoDeHorario(
            @Param("recursoId") UUID recursoId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query("""
            SELECT COUNT(r) > 0
            FROM Reserva r
            WHERE r.recurso.responsavelId = :responsavelId
              AND (:reservaId IS NULL OR r.id <> :reservaId)
              AND r.dataInicio < :dataFim
              AND r.dataFim > :dataInicio
              AND r.status IN (br.edu.organizacaorecursos.domain.StatusReserva.PENDENTE,
                               br.edu.organizacaorecursos.domain.StatusReserva.APROVADA,
                               br.edu.organizacaorecursos.domain.StatusReserva.EM_USO)
            """)
    boolean existeConflitoDeAgendaResponsavel(
            @Param("responsavelId") UUID responsavelId,
            @Param("reservaId") UUID reservaId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query("""
            SELECT r
            FROM Reserva r
            WHERE r.recurso.id = :recursoId
              AND r.dataInicio < :dataFim
              AND r.dataFim > :dataInicio
              AND r.status IN (br.edu.organizacaorecursos.domain.StatusReserva.PENDENTE,
                               br.edu.organizacaorecursos.domain.StatusReserva.APROVADA,
                               br.edu.organizacaorecursos.domain.StatusReserva.EM_USO)
            ORDER BY r.dataInicio
            """)
    List<Reserva> buscarConflitosDeHorario(
            @Param("recursoId") UUID recursoId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query("""
            SELECT COUNT(r) > 0
            FROM Reserva r
            WHERE r.recurso.id = :recursoId
              AND r.id <> :reservaId
              AND r.dataInicio < :dataFim
              AND r.dataFim > :dataInicio
              AND r.status IN (br.edu.organizacaorecursos.domain.StatusReserva.PENDENTE,
                               br.edu.organizacaorecursos.domain.StatusReserva.APROVADA,
                               br.edu.organizacaorecursos.domain.StatusReserva.EM_USO)
            """)
    boolean existeConflitoDeHorarioAprovado(
            @Param("recursoId") UUID recursoId,
            @Param("reservaId") UUID reservaId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);
}
