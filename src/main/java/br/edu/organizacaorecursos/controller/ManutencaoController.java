package br.edu.organizacaorecursos.controller;

import br.edu.organizacaorecursos.domain.Manutencao;
import br.edu.organizacaorecursos.exception.RegraNegocioException;
import jakarta.validation.Valid;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manutencoes")
@RequiredArgsConstructor
public class ManutencaoController {

    private final ManutencaoRepository manutencaoRepository;
    private final RecursoRepository recursoRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSAVEL')")
    public ResponseEntity<Manutencao> criarManutencao(
            @Valid @RequestBody Manutencao manutencao,
            @AuthenticationPrincipal Jwt jwt) {
        exigirJwt(jwt);
        if (manutencao.getRecurso() == null || manutencao.getRecurso().getId() == null) {
            throw new RegraNegocioException("O ID do recurso é obrigatório.");
        }
        manutencao.setRecurso(recursoRepository.findById(manutencao.getRecurso().getId())
                .orElseThrow(() -> new EntityNotFoundException("Recurso não encontrado.")));
        return ResponseEntity.status(HttpStatus.CREATED).body(manutencaoRepository.save(manutencao));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RESPONSAVEL')")
    public ResponseEntity<List<Manutencao>> listarManutencoes() {
        return ResponseEntity.ok(manutencaoRepository.findAll());
    }

    private void exigirJwt(Jwt jwt) {
        if (jwt == null) {
            throw new AccessDeniedException("A autenticação é obrigatória para gerenciar manutenções.");
        }
    }
}
