package br.edu.organizacaorecursos.dto;

import br.edu.organizacaorecursos.domain.Perfil;
import lombok.Data;

import java.util.UUID;

@Data
public class UsuarioResponseDTO {

    private UUID id;
    private String nome;
    private String email;
    private String matricula;
    private Perfil perfil;
}