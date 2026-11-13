package br.edu.organizacaorecursos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "security.jwt.identifier-login-enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class AuthLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario administrador;

    @BeforeEach
    void prepararUsuario() {
        usuarioRepository.deleteAll();
        administrador = usuarioRepository.save(new Usuario(
                null, "Admin", "admin@example.com", "MAT-001", Perfil.ADMINISTRADOR));
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin@example.com", "MAT-001"})
    void emiteJwtHs256ComIdentidadePerfilEExpiracao(String identificador) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(identificador))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.id").value(administrador.getId().toString()))
                .andExpect(jsonPath("$.usuario.perfil").value("ADMINISTRADOR"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        var jwt = jwtDecoder.decode(body.get("token").asText());
        org.assertj.core.api.Assertions.assertThat(jwt.getHeaders().get("alg")).isEqualTo("HS256");
        org.assertj.core.api.Assertions.assertThat(jwt.getSubject()).isEqualTo(administrador.getId().toString());
        org.assertj.core.api.Assertions.assertThat(jwt.getClaimAsString("profile")).isEqualTo("ADMINISTRADOR");
        org.assertj.core.api.Assertions.assertThat(jwt.getClaimAsString("name")).isEqualTo(administrador.getNome());
        org.assertj.core.api.Assertions.assertThat(jwt.getClaimAsString("email")).isEqualTo(administrador.getEmail());
        org.assertj.core.api.Assertions.assertThat(jwt.getIssuedAt()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @org.junit.jupiter.api.Test
    void identificadorInexistenteRecebe401SemToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identificador":"ausente@example.com"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @org.junit.jupiter.api.Test
    void documentaLoginDemoComoPublicoNoOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/auth/login'].post.tags[0]").value("Autenticação"))
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").isEmpty());
    }

    private record LoginRequest(String identificador) {
    }
}
