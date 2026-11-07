package br.edu.organizacaorecursos.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class ReservaRequestDTO {

    @NotNull(message = "O ID do recurso é obrigatório.")
    private UUID recursoId;

    @NotNull(message = "A data de início é obrigatória.")
    @FutureOrPresent(message = "A data de início deve ser no presente ou futuro.")
    private LocalDateTime dataInicio;

    @NotNull(message = "A data de fim é obrigatória.")
    @FutureOrPresent(message = "A data de fim deve ser no presente ou futuro.")
    private LocalDateTime dataFim;

    @NotBlank(message = "O nome do responsável é obrigatório.")
    private String usuarioResponsavel;
}
