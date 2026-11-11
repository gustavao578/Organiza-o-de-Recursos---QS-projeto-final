package br.edu.organizacaorecursos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "tb_reserva_auditorias")
@Getter
@NoArgsConstructor
public class ReservaAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, updatable = false)
    private Reserva reserva;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(nullable = false, updatable = false)
    private Instant instante;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, updatable = false)
    private StatusReserva estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_novo", nullable = false, updatable = false)
    private StatusReserva estadoNovo;

    @Column(nullable = false, updatable = false, length = 1000)
    private String motivo;

    public ReservaAuditoria(
            Reserva reserva,
            UUID usuarioId,
            Instant instante,
            StatusReserva estadoAnterior,
            StatusReserva estadoNovo,
            String motivo) {
        this.reserva = reserva;
        this.usuarioId = usuarioId;
        this.instante = instante;
        this.estadoAnterior = estadoAnterior;
        this.estadoNovo = estadoNovo;
        this.motivo = motivo;
    }

    @PreUpdate
    @PreRemove
    private void impedirAlteracaoOuExclusao() {
        throw new UnsupportedOperationException("Registros de auditoria são imutáveis.");
    }
}
