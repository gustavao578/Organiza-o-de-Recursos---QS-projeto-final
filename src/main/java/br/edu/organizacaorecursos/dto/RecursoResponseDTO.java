package br.edu.organizacaorecursos.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class RecursoResponseDTO {

    private UUID id;
    private String nome;
    private String tipo;
    private boolean restrito;
    private UUID responsavelId;
}