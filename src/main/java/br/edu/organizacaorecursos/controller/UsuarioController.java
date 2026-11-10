package br.edu.organizacaorecursos.controller;

import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.dto.UsuarioRequestDTO;
import br.edu.organizacaorecursos.dto.UsuarioResponseDTO;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
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
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criarUsuario(@Valid @RequestBody UsuarioRequestDTO request) {
        Usuario usuario = new Usuario(null, request.getNome(), request.getEmail(),
                request.getMatricula(), Perfil.COMUM);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapear(usuarioRepository.save(usuario)));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioRepository.findAll().stream().map(this::mapear).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarUsuario(@PathVariable UUID id) {
        return ResponseEntity.ok(mapear(usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizarUsuario(
            @PathVariable UUID id, @Valid @RequestBody UsuarioRequestDTO request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setMatricula(request.getMatricula());
        return ResponseEntity.ok(mapear(usuarioRepository.save(usuario)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable UUID id) {
        if (!usuarioRepository.existsById(id)) {
            throw new EntityNotFoundException("Usuário não encontrado.");
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
}
