package br.edu.organizacaorecursos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record ManutencaoRequestDTO(
        UUID recursoId,
        @NotNull(message = "A data de início é obrigatória.") LocalDateTime dataInicio,
        @NotNull(message = "A data de fim é obrigatória.") LocalDateTime dataFim,
        @NotBlank(message = "A descrição é obrigatória.") String descricao) {
}
