package br.edu.organizacaorecursos.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class ReservaResponseDTO {

    private UUID id;
    private UUID recursoId;
    private String nomeRecurso;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private String usuarioResponsavel;
}
