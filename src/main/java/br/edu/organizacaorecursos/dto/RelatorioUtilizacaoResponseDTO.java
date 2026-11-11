package br.edu.organizacaorecursos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

public record RelatorioUtilizacaoResponseDTO(
        List<PorRecurso> porRecurso,
        @Schema(description = "Horas agrupadas pelo usuário associado a Reserva.usuario; o perfil Professor não é inferido.")
        List<PorUsuario> porUsuario,
        long conflitosEvitados) {

    public record PorRecurso(UUID id, String nome, double horas) {}

    public record PorUsuario(UUID id, String nome, double horas) {}
}
