package br.edu.organizacaorecursos.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import br.edu.organizacaorecursos.domain.StatusReserva;
import lombok.Data;

@Data
public class ReservaResponseDTO {

    private UUID id;
    private UUID recursoId;
    private String nomeRecurso;
    private UUID usuarioId;
    private String nomeUsuario;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private StatusReserva status;
}
