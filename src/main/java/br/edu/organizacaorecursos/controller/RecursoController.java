package br.edu.organizacaorecursos.controller;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.repository.RecursoRepository;
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
@RequestMapping("/recursos")
@RequiredArgsConstructor
public class RecursoController {

    private final RecursoRepository recursoRepository;

    @PostMapping
    public ResponseEntity<Recurso> criarRecurso(@RequestBody Recurso recurso) {
        Recurso recursoSalvo = recursoRepository.save(recurso);
        return ResponseEntity.status(HttpStatus.CREATED).body(recursoSalvo);
    }

    @GetMapping
    public ResponseEntity<List<Recurso>> listarRecursos() {
        return ResponseEntity.ok(recursoRepository.findAll());
    }
}
