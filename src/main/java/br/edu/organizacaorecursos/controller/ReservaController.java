package br.edu.organizacaorecursos.controller;

import br.edu.organizacaorecursos.dto.AprovacaoRequestDTO;
import br.edu.organizacaorecursos.dto.ReservaAuditoriaResponseDTO;
import br.edu.organizacaorecursos.dto.ReservaRequestDTO;
import br.edu.organizacaorecursos.dto.ReservaResponseDTO;
import br.edu.organizacaorecursos.dto.StatusReservaRequestDTO;
import br.edu.organizacaorecursos.domain.StatusReserva;
import br.edu.organizacaorecursos.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/reservas", "/api/reservas"})
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    public ResponseEntity<ReservaResponseDTO> criarReserva(
            @Valid @RequestBody ReservaRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        exigirUmDosPerfis(jwt, "COMUM", "SOLICITANTE");
        if (jwt != null) {
            request.setUsuarioId(UUID.fromString(jwt.getSubject()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.criarReserva(request));
    }

    @PostMapping("/{id}/aprovar")
    public ResponseEntity<ReservaResponseDTO> aprovarReserva(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) AprovacaoRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        exigirAdmin(jwt);
        return ResponseEntity.ok(reservaService.aprovarReserva(
                id, atorId(jwt), request == null ? null : request.getMotivo()));
    }

    @PostMapping("/{id}/iniciar")
    public ResponseEntity<ReservaResponseDTO> iniciarReserva(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        exigirPerfil(jwt, "RESPONSAVEL");
        return ResponseEntity.ok(reservaService.iniciarReserva(id, atorId(jwt)));
    }

    @PostMapping("/{id}/concluir")
    public ResponseEntity<ReservaResponseDTO> concluirReserva(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        exigirPerfil(jwt, "RESPONSAVEL");
        return ResponseEntity.ok(reservaService.concluirReserva(id, atorId(jwt)));
    }

    @PostMapping("/{id}/rejeitar")
    public ResponseEntity<ReservaResponseDTO> rejeitarReserva(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) AprovacaoRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        exigirAdmin(jwt);
        return ResponseEntity.ok(reservaService.atualizarStatus(
                id, StatusReserva.REJEITADA, atorId(jwt), request == null ? null : request.getMotivo()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReservaResponseDTO> atualizarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusReservaRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new AccessDeniedException("A autenticação é obrigatória para decidir reservas.");
        }
        exigirUmDosPerfis(jwt, "ADMINISTRADOR", "RESPONSAVEL");
        return ResponseEntity.ok(reservaService.atualizarStatus(
                id, request.getStatus(), atorId(jwt), request.getMotivo()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservaResponseDTO> atualizarReserva(
            @PathVariable UUID id,
            @Valid @RequestBody ReservaRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        exigirUmDosPerfis(jwt, "COMUM", "SOLICITANTE");
        if (jwt == null) {
            throw new AccessDeniedException("A identidade do solicitante é obrigatória.");
        }
        UUID solicitanteId = UUID.fromString(jwt.getSubject());
        request.setUsuarioId(solicitanteId);
        return ResponseEntity.ok(reservaService.atualizarReserva(id, request, solicitanteId));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponseDTO> cancelarReserva(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) AprovacaoRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        exigirUmDosPerfis(jwt, "COMUM", "SOLICITANTE");
        return ResponseEntity.ok(reservaService.cancelarReserva(
                id, atorId(request, jwt), request == null ? null : request.getMotivo()));
    }

    @PostMapping("/{id}/nao-compareceu")
    public ResponseEntity<ReservaResponseDTO> marcarNaoComparecimento(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) AprovacaoRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        exigirPerfil(jwt, "RESPONSAVEL");
        return ResponseEntity.ok(reservaService.marcarNaoComparecimento(
                id, atorId(request, jwt), request == null ? null : request.getMotivo()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(reservaService.buscarReservaPorId(id));
    }

    @GetMapping
    public ResponseEntity<java.util.List<ReservaResponseDTO>> listarReservas() {
        return ResponseEntity.ok(reservaService.listarReservas());
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<List<ReservaAuditoriaResponseDTO>> historico(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new AccessDeniedException("A autenticação é obrigatória para consultar o histórico.");
        }
        exigirUmDosPerfis(jwt, "RESPONSAVEL", "ADMINISTRADOR");
        return ResponseEntity.ok(reservaService.historicoReserva(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarReserva(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new AccessDeniedException("A identidade do solicitante é obrigatória.");
        }
        reservaService.deletarReserva(id, atorId(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID atorId(AprovacaoRequestDTO request, Jwt jwt) {
        if (jwt != null) {
            return UUID.fromString(jwt.getSubject());
        }
        if (request != null && request.getAtorId() != null) {
            return request.getAtorId();
        }
        throw new IllegalArgumentException("O ator autenticado é obrigatório.");
    }

    private UUID atorId(Jwt jwt) {
        return jwt == null ? null : UUID.fromString(jwt.getSubject());
    }

    private void exigirPerfil(Jwt jwt, String perfilEsperado) {
        if (jwt != null && !perfilEsperado.equals(perfilNormalizado(jwt.getClaimAsString("profile")))) {
            throw new AccessDeniedException("Perfil não autorizado para esta operação.");
        }
    }

    private void exigirAdmin(Jwt jwt) {
        if (jwt == null || !"ADMINISTRADOR".equals(perfilNormalizado(jwt.getClaimAsString("profile")))) {
            throw new AccessDeniedException("Somente usuários ADMINISTRADOR podem decidir reservas.");
        }
    }

    private void exigirUmDosPerfis(Jwt jwt, String... perfisAceitos) {
        if (jwt != null && java.util.Arrays.stream(perfisAceitos)
                .noneMatch(perfil -> perfil.equals(perfilNormalizado(jwt.getClaimAsString("profile"))))) {
            throw new AccessDeniedException("Perfil não autorizado para esta operação.");
        }
    }

    private String perfilNormalizado(String perfil) {
        return switch (perfil == null ? "" : perfil) {
            case "ADMIN" -> "ADMINISTRADOR";
            case "COMUM" -> "SOLICITANTE";
            default -> perfil;
        };
    }
}
