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
    public ResponseEntity<Manutencao> criarManutencao(@Valid @RequestBody Manutencao manutencao) {
        if (manutencao.getRecurso() == null || manutencao.getRecurso().getId() == null) {
            throw new RegraNegocioException("O ID do recurso é obrigatório.");
        }
        manutencao.setRecurso(recursoRepository.findById(manutencao.getRecurso().getId())
                .orElseThrow(() -> new EntityNotFoundException("Recurso não encontrado.")));
        return ResponseEntity.status(HttpStatus.CREATED).body(manutencaoRepository.save(manutencao));
    }

    @GetMapping
    public ResponseEntity<List<Manutencao>> listarManutencoes() {
        return ResponseEntity.ok(manutencaoRepository.findAll());
    }
}
