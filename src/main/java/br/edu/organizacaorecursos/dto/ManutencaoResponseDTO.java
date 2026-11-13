package br.edu.organizacaorecursos.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ManutencaoResponseDTO(
        UUID id,
        UUID recursoId,
        String nomeRecurso,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        String descricao) {
}
