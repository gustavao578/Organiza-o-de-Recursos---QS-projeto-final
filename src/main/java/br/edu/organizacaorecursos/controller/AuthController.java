package br.edu.organizacaorecursos.controller;

import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.dto.LoginRequestDTO;
import br.edu.organizacaorecursos.dto.LoginResponseDTO;
import br.edu.organizacaorecursos.dto.UsuarioResponseDTO;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação")
@Profile({"local", "demo"})
@ConditionalOnProperty(name = "security.jwt.identifier-login-enabled", havingValue = "true")
public class AuthController {

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);

    private final UsuarioRepository usuarioRepository;
    private final JwtEncoder jwtEncoder;

    public AuthController(UsuarioRepository usuarioRepository, JwtEncoder jwtEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.jwtEncoder = jwtEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        String identificador = request.identificador().trim();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCaseOrMatricula(
                        identificador, identificador)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Identificador ou credenciais inválidos."));

        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getId().toString())
                .claim("profile", usuario.getPerfil().name())
                .claim("name", usuario.getNome())
                .claim("email", usuario.getEmail())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(TOKEN_LIFETIME))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        return ResponseEntity.ok(new LoginResponseDTO(token, toResponse(usuario)));
    }

    private UsuarioResponseDTO toResponse(Usuario usuario) {
        UsuarioResponseDTO response = new UsuarioResponseDTO();
        response.setId(usuario.getId());
        response.setNome(usuario.getNome());
        response.setEmail(usuario.getEmail());
        response.setMatricula(usuario.getMatricula());
        response.setPerfil(usuario.getPerfil());
        return response;
    }
}
