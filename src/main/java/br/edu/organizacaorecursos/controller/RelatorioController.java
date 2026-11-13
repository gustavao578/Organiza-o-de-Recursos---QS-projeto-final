package br.edu.organizacaorecursos.controller;

import br.edu.organizacaorecursos.dto.RelatorioUtilizacaoResponseDTO;
import br.edu.organizacaorecursos.service.RelatorioUtilizacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private static final Set<String> PERFIS_AUTORIZADOS = Set.of("RESPONSAVEL", "ADMINISTRADOR");

    private final RelatorioUtilizacaoService relatorioUtilizacaoService;

    @GetMapping("/utilizacao")
    @Operation(
            summary = "Relatório de utilização por período",
            description = "Retorna horas por recurso, carga horária agrupada por usuário associado à reserva "
                    + "e tentativas recusadas por sobreposição no período. Reservas CANCELADA não contam. "
                    + "Apenas perfis RESPONSAVEL e ADMINISTRADOR podem consultar.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Relatório calculado."),
        @ApiResponse(responseCode = "400", description = "Período ausente, inválido ou com fim não posterior ao início."),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido."),
        @ApiResponse(responseCode = "403", description = "Perfil diferente de RESPONSAVEL ou ADMINISTRADOR.")
    })
    public ResponseEntity<RelatorioUtilizacaoResponseDTO> obterUtilizacao(
            @Parameter(
                    name = "inicio",
                    in = ParameterIn.QUERY,
                    required = true,
                    description = "Início inclusivo do período ISO-8601.",
                    example = "2026-03-15T10:00:00")
            @RequestParam("inicio") LocalDateTime inicio,
            @Parameter(
                    name = "fim",
                    in = ParameterIn.QUERY,
                    required = true,
                    description = "Fim exclusivo do período ISO-8601.",
                    example = "2026-03-15T15:00:00")
            @RequestParam("fim") LocalDateTime fim,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || !PERFIS_AUTORIZADOS.contains(perfilNormalizado(jwt.getClaimAsString("profile")))) {
            throw new AccessDeniedException("Perfil não autorizado para consultar relatórios.");
        }
        return ResponseEntity.ok(relatorioUtilizacaoService.gerar(inicio, fim));
    }

    private String perfilNormalizado(String perfil) {
        return switch (perfil == null ? "" : perfil) {
            case "ADMIN" -> "ADMINISTRADOR";
            case "COMUM" -> "SOLICITANTE";
            default -> perfil;
        };
    }
}
