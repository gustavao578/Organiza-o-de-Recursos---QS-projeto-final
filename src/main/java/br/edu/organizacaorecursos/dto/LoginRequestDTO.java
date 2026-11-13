package br.edu.organizacaorecursos.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "O e-mail ou a matrícula é obrigatório.") String identificador) {
}
