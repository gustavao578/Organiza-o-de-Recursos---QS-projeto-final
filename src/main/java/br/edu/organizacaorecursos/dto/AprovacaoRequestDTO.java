package br.edu.organizacaorecursos.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class AprovacaoRequestDTO {

    @NotNull
    private UUID atorId;
}