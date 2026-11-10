package br.edu.organizacaorecursos.dto;

import br.edu.organizacaorecursos.domain.StatusReserva;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusReservaRequestDTO {

    @NotNull(message = "O novo status é obrigatório.")
    private StatusReserva status;
}