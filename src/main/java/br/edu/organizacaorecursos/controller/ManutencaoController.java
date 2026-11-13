package br.edu.organizacaorecursos.controller;

import br.edu.organizacaorecursos.dto.ManutencaoRequestDTO;
import br.edu.organizacaorecursos.dto.ManutencaoResponseDTO;
import br.edu.organizacaorecursos.service.ManutencaoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manutencoes")
@RequiredArgsConstructor
public class ManutencaoController {

    private final ManutencaoService manutencaoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSAVEL')")
    public ResponseEntity<ManutencaoResponseDTO> criarManutencao(
            @Valid @RequestBody ManutencaoRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        exigirJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(manutencaoService.criar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSAVEL')")
    public ResponseEntity<List<ManutencaoResponseDTO>> listarManutencoes(
            @RequestParam(required = false) UUID recursoId) {
        return ResponseEntity.ok(manutencaoService.listar(recursoId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSAVEL')")
    public ResponseEntity<ManutencaoResponseDTO> atualizarManutencao(
            @PathVariable UUID id,
            @Valid @RequestBody ManutencaoRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        exigirJwt(jwt);
        return ResponseEntity.ok(manutencaoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSAVEL')")
    public ResponseEntity<Void> deletarManutencao(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        exigirJwt(jwt);
        manutencaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    private void exigirJwt(Jwt jwt) {
        if (jwt == null) {
            throw new AccessDeniedException("A autenticação é obrigatória para gerenciar manutenções.");
        }
    }
}
