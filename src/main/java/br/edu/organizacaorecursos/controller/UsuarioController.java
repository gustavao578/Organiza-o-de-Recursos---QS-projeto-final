package br.edu.organizacaorecursos.controller;

import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.dto.UsuarioRequestDTO;
import br.edu.organizacaorecursos.dto.UsuarioResponseDTO;
import br.edu.organizacaorecursos.domain.StatusReserva;
import br.edu.organizacaorecursos.exception.RegraNegocioException;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private static final List<StatusReserva> STATUS_RESERVA_ATIVA =
            List.of(StatusReserva.PENDENTE, StatusReserva.APROVADA, StatusReserva.EM_USO);

    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final RecursoRepository recursoRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponseDTO> criarUsuario(
            @Valid @RequestBody UsuarioRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        exigirJwt(jwt);
        Perfil perfil = request.getPerfil() != null && isAdministrador(jwt)
                ? request.getPerfil()
                : Perfil.SOLICITANTE;
        Usuario usuario = new Usuario(null, request.getNome(), request.getEmail(),
                request.getMatricula(), perfil);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapear(usuarioRepository.save(usuario)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioRepository.findAll().stream().map(this::mapear).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarUsuario(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        exigirProprietarioOuAdministrador(id, jwt);
        return ResponseEntity.ok(mapear(usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizarUsuario(
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        exigirProprietarioOuAdministrador(id, jwt);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setMatricula(request.getMatricula());
        if (request.getPerfil() != null && isAdministrador(jwt)) {
            usuario.setPerfil(request.getPerfil());
        }
        return ResponseEntity.ok(mapear(usuarioRepository.save(usuario)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> deletarUsuario(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new AccessDeniedException("A autenticação é obrigatória para excluir usuários.");
        }
        if (!usuarioRepository.existsById(id)) {
            throw new EntityNotFoundException("Usuário não encontrado.");
        }
        if (reservaRepository.existsByUsuario_IdAndStatusIn(id, STATUS_RESERVA_ATIVA)) {
            throw new RegraNegocioException("Não é possível excluir um usuário com reservas ativas.");
        }
        if (reservaRepository.existsByUsuario_Id(id)) {
            throw new RegraNegocioException("Não é possível excluir um usuário associado a reservas.");
        }
        if (recursoRepository.existsByResponsavelId(id)) {
            throw new RegraNegocioException("Não é possível excluir um usuário responsável por recursos.");
        }
        usuarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private UsuarioResponseDTO mapear(Usuario usuario) {
        UsuarioResponseDTO response = new UsuarioResponseDTO();
        response.setId(usuario.getId());
        response.setNome(usuario.getNome());
        response.setEmail(usuario.getEmail());
        response.setMatricula(usuario.getMatricula());
        response.setPerfil(usuario.getPerfil());
        return response;
    }

    private boolean isAdministrador(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        String perfil = perfilNormalizado(jwt.getClaimAsString("profile"));
        return "ADMINISTRADOR".equals(perfil) || "ADMIN".equals(perfil);
    }

    private void exigirProprietarioOuAdministrador(UUID id, Jwt jwt) {
        if (jwt == null) {
            throw new AccessDeniedException("A autenticação é obrigatória para editar usuários.");
        }
        if (isAdministrador(jwt)) {
            return;
        }
        try {
            if (id.equals(UUID.fromString(jwt.getSubject()))) {
                return;
            }
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("A identidade do usuário autenticado é inválida.");
        }
        throw new AccessDeniedException("Somente o próprio usuário ou um administrador pode editar o cadastro.");
    }

    private void exigirJwt(Jwt jwt) {
        if (jwt == null) {
            throw new AccessDeniedException("A autenticação é obrigatória para gerenciar usuários.");
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
