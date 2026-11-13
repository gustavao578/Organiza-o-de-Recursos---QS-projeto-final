package br.edu.organizacaorecursos.controller;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.StatusReserva;
import br.edu.organizacaorecursos.dto.RecursoRequestDTO;
import br.edu.organizacaorecursos.dto.RecursoResponseDTO;
import br.edu.organizacaorecursos.exception.RegraNegocioException;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recursos")
@RequiredArgsConstructor
public class RecursoController {

    private static final List<StatusReserva> STATUS_RESERVA_ATIVA =
            List.of(StatusReserva.PENDENTE, StatusReserva.APROVADA, StatusReserva.EM_USO);

    private final RecursoRepository recursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final ManutencaoRepository manutencaoRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<RecursoResponseDTO> criarRecurso(
            @Valid @RequestBody RecursoRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        exigirJwt(jwt);
        validarResponsavel(request.getResponsavelId());
        Recurso recurso = new Recurso(null, request.getNome(), request.getTipo(),
                request.isRestrito(), request.getResponsavelId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapear(recursoRepository.save(recurso)));
    }

    @GetMapping
    public ResponseEntity<List<RecursoResponseDTO>> listarRecursos(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Boolean restrito) {
        return ResponseEntity.ok(recursoRepository.filtrar(tipo, restrito).stream().map(this::mapear).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecursoResponseDTO> buscarRecurso(@PathVariable UUID id) {
        return ResponseEntity.ok(mapear(recursoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso não encontrado."))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<RecursoResponseDTO> atualizarRecurso(
            @PathVariable UUID id,
            @Valid @RequestBody RecursoRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        exigirJwt(jwt);
        validarResponsavel(request.getResponsavelId());
        Recurso recurso = recursoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recurso não encontrado."));
        recurso.setNome(request.getNome());
        recurso.setTipo(request.getTipo());
        recurso.setRestrito(request.isRestrito());
        recurso.setResponsavelId(request.getResponsavelId());
        return ResponseEntity.ok(mapear(recursoRepository.save(recurso)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> deletarRecurso(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        exigirJwt(jwt);
        if (!recursoRepository.existsById(id)) {
            throw new EntityNotFoundException("Recurso não encontrado.");
        }
        if (reservaRepository.existsByRecurso_IdAndStatusIn(id, STATUS_RESERVA_ATIVA)) {
            throw new RegraNegocioException("Não é possível excluir um recurso com reservas ativas.");
        }
        if (reservaRepository.existsByRecurso_Id(id) || manutencaoRepository.existsByRecurso_Id(id)) {
            throw new RegraNegocioException(
                    "Não é possível excluir um recurso associado a reservas ou manutenções.");
        }
        recursoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void validarResponsavel(UUID responsavelId) {
        if (responsavelId == null) {
            return;
        }
        var responsavel = usuarioRepository.findById(responsavelId)
                .orElseThrow(() -> new EntityNotFoundException("Responsável não encontrado."));
        if (responsavel.getPerfil() != Perfil.RESPONSAVEL) {
            throw new RegraNegocioException("O usuário informado não possui perfil RESPONSAVEL.");
        }
    }

    private void exigirJwt(Jwt jwt) {
        if (jwt == null) {
            throw new AccessDeniedException("A autenticação é obrigatória para gerenciar recursos.");
        }
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
