package br.edu.organizacaorecursos.controller;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.dto.RecursoRequestDTO;
import br.edu.organizacaorecursos.dto.RecursoResponseDTO;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recursos")
@RequiredArgsConstructor
public class RecursoController {

    private final RecursoRepository recursoRepository;

    @PostMapping
    public ResponseEntity<RecursoResponseDTO> criarRecurso(@Valid @RequestBody RecursoRequestDTO request) {
        Recurso recurso = new Recurso(null, request.getNome(), request.getTipo(),
                request.isRestrito(), request.getResponsavelId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapear(recursoRepository.save(recurso)));
    }

    @GetMapping
    public ResponseEntity<List<RecursoResponseDTO>> listarRecursos() {
        return ResponseEntity.ok(recursoRepository.findAll().stream().map(this::mapear).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecursoResponseDTO> buscarRecurso(@PathVariable UUID id) {
        return ResponseEntity.ok(mapear(recursoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso não encontrado."))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecursoResponseDTO> atualizarRecurso(
            @PathVariable UUID id, @Valid @RequestBody RecursoRequestDTO request) {
        Recurso recurso = recursoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso não encontrado."));
        recurso.setNome(request.getNome());
        recurso.setTipo(request.getTipo());
        recurso.setRestrito(request.isRestrito());
        recurso.setResponsavelId(request.getResponsavelId());
        return ResponseEntity.ok(mapear(recursoRepository.save(recurso)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarRecurso(@PathVariable UUID id) {
        if (!recursoRepository.existsById(id)) {
            throw new EntityNotFoundException("Recurso não encontrado.");
        }
        recursoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private RecursoResponseDTO mapear(Recurso recurso) {
        RecursoResponseDTO response = new RecursoResponseDTO();
        response.setId(recurso.getId());
        response.setNome(recurso.getNome());
        response.setTipo(recurso.getTipo());
        response.setRestrito(recurso.isRestrito());
        response.setResponsavelId(recurso.getResponsavelId());
        return response;
    }
}
