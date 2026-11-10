package br.edu.organizacaorecursos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class RecursoRequestDTO {

    @NotBlank(message = "O nome do recurso é obrigatório.")
    private String nome;

    @NotBlank(message = "O tipo do recurso é obrigatório.")
    private String tipo;

    private boolean restrito;

    private UUID responsavelId;
}