package br.edu.organizacaorecursos.dto;

import br.edu.organizacaorecursos.domain.ReservaAuditoria;
import br.edu.organizacaorecursos.domain.StatusReserva;
import java.time.Instant;
import java.util.UUID;

public record ReservaAuditoriaResponseDTO(
        UUID id,
        UUID reservaId,
        UUID usuarioId,
        Instant instante,
        StatusReserva estadoAnterior,
        StatusReserva estadoNovo,
        String motivo) {

    public static ReservaAuditoriaResponseDTO de(ReservaAuditoria auditoria) {
        return new ReservaAuditoriaResponseDTO(
                auditoria.getId(),
                auditoria.getReserva().getId(),
                auditoria.getUsuarioId(),
                auditoria.getInstante(),
                auditoria.getEstadoAnterior(),
                auditoria.getEstadoNovo(),
                auditoria.getMotivo());
    }
}
