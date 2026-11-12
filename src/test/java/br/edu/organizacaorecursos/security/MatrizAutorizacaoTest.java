package br.edu.organizacaorecursos.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.domain.StatusReserva;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.dto.ReservaRequestDTO;
import br.edu.organizacaorecursos.dto.RecursoRequestDTO;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Teste parametrizado de autorização baseado em matriz-autorizacao.md.
 * Valida cada célula (operação, perfil, status esperado) contra a API com MockMvc.
 * Testes incluem:
 * - Cada célula da matriz de autorização
 * - Regras de posse (Solicitante só altera/cancela próprias reservas)
 * - Divergências marcadas com [DIVERGÊNCIA]
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MatrizAutorizacaoTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RecursoRepository recursoRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Usuario solicitante1;
    private Usuario solicitante2;
    private Usuario responsavel;
    private Usuario administrador;

    private Recurso recursoPublico;

    private Reserva reservaSolicitante1;
    private Reserva reservaSolicitante2;

    @BeforeEach
    void setup() {
        jdbcTemplate.execute("DELETE FROM tb_reserva_auditorias");
        reservaRepository.deleteAll();
        usuarioRepository.deleteAll();
        recursoRepository.deleteAll();

        solicitante1 = usuarioRepository.save(
            new Usuario(null, "Solicitante 1", "sol1@test.com", "MAT001", Perfil.SOLICITANTE)
        );
        solicitante2 = usuarioRepository.save(
            new Usuario(null, "Solicitante 2", "sol2@test.com", "MAT002", Perfil.SOLICITANTE)
        );
        responsavel = usuarioRepository.save(
            new Usuario(null, "Responsável", "resp@test.com", "MAT003", Perfil.RESPONSAVEL)
        );
        administrador = usuarioRepository.save(
            new Usuario(null, "Administrador", "admin@test.com", "MAT004", Perfil.ADMINISTRADOR)
        );

        recursoPublico = recursoRepository.save(
            new Recurso(null, "Sala Pública", "SALA")
        );

        reservaSolicitante1 = new Reserva(
            null, recursoPublico,
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now().plusHours(2),
            StatusReserva.PENDENTE, solicitante1
        );
        reservaSolicitante1 = reservaRepository.save(reservaSolicitante1);

        reservaSolicitante2 = new Reserva(
            null, recursoPublico,
            LocalDateTime.now().plusHours(3),
            LocalDateTime.now().plusHours(4),
            StatusReserva.PENDENTE, solicitante2
        );
        reservaSolicitante2 = reservaRepository.save(reservaSolicitante2);
    }

    // ==================== RESERVAS ====================

    @ParameterizedTest(name = "GET /reservas - {0} espera {1}")
    @CsvSource({"Anônimo,200,null", "Solicitante,200,SOLICITANTE", "Responsável,200,RESPONSAVEL", "Administrador,200,ADMINISTRADOR"})
    void testarGetReservas(String descricao, int statusEsperado, String perfilStr) throws Exception {
        if ("null".equals(perfilStr)) {
            mockMvc.perform(get("/reservas"))
                .andExpect(status().is(statusEsperado));
        } else {
            UUID usuarioId = obterUsuarioId(perfilStr);
            mockMvc.perform(get("/reservas")
                .with(jwtContexto(usuarioId, perfilStr)))
                .andExpect(status().is(statusEsperado));
        }
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "POST /reservas - {0} espera {1}")
    @CsvSource({"Anônimo,401,null", "Solicitante,201,SOLICITANTE", "Responsável,201,RESPONSAVEL", "Administrador,201,ADMINISTRADOR"})
    void testarCriarReserva(String descricao, int statusEsperado, String perfilStr) throws Exception {
        ReservaRequestDTO request = new ReservaRequestDTO();
        request.setRecursoId(recursoPublico.getId());
        request.setUsuarioId(solicitante1.getId());
        request.setDataInicio(LocalDateTime.now().plusHours(5));
        request.setDataFim(LocalDateTime.now().plusHours(6));

        if ("null".equals(perfilStr)) {
            mockMvc.perform(post("/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(statusEsperado));
        } else {
            UUID usuarioId = obterUsuarioId(perfilStr);
            mockMvc.perform(post("/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(jwtContexto(usuarioId, perfilStr)))
                .andExpect(status().is(statusEsperado));
        }
        SecurityContextHolder.clearContext();
    }

    // ==================== POSSE (OWNERSHIP) ====================

    @ParameterizedTest(name = "PUT /reservas/{id} - {0} espera {1}")
    @CsvSource({"Solicitante altera própria,200,proprio", "Solicitante altera de outro,403,outro"})
    void testarOwnershipReservaAlteracao(String descricao, int statusEsperado, String tipo) throws Exception {
        ReservaRequestDTO request = new ReservaRequestDTO();
        request.setRecursoId(recursoPublico.getId());
        request.setUsuarioId(solicitante1.getId());
        request.setDataInicio(LocalDateTime.now().plusHours(7));
        request.setDataFim(LocalDateTime.now().plusHours(8));

        UUID reservaId = "proprio".equals(tipo) ? reservaSolicitante1.getId() : reservaSolicitante2.getId();

        mockMvc.perform(put("/reservas/{id}", reservaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .with(jwtContexto(solicitante1.getId(), "SOLICITANTE")))
            .andExpect(status().is(statusEsperado));
        
        SecurityContextHolder.clearContext();
    }

    // ==================== RECURSOS DESPROTEGIDOS [DIVERGÊNCIA] ====================

    @ParameterizedTest(name = "POST /recursos - {0} espera {1} [DIVERGÊNCIA]")
    @CsvSource({"Anônimo,401,null", "Solicitante,401,SOLICITANTE", "Responsável,401,RESPONSAVEL", "Administrador,201,ADMINISTRADOR"})
    void testarCriarRecursoDesprotegido(String descricao, int statusEsperado, String perfilStr) throws Exception {
        RecursoRequestDTO request = new RecursoRequestDTO();
        request.setNome("Sala Nova");
        request.setTipo("SALA");

        if ("null".equals(perfilStr)) {
            mockMvc.perform(post("/recursos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(statusEsperado));
        } else {
            UUID usuarioId = obterUsuarioId(perfilStr);
            mockMvc.perform(post("/recursos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(jwtContexto(usuarioId, perfilStr)))
                .andExpect(status().is(statusEsperado));
        }
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "DELETE /recursos - {0} espera {1} [DIVERGÊNCIA]")
    @CsvSource({"Anônimo,401,null", "Solicitante,401,SOLICITANTE", "Responsável,401,RESPONSAVEL", "Administrador,204,ADMINISTRADOR"})
    void testarDeletarRecursoDesprotegido(String descricao, int statusEsperado, String perfilStr) throws Exception {
        if ("null".equals(perfilStr)) {
            mockMvc.perform(delete("/recursos/{id}", recursoPublico.getId()))
                .andExpect(status().is(statusEsperado));
        } else {
            UUID usuarioId = obterUsuarioId(perfilStr);
            mockMvc.perform(delete("/recursos/{id}", recursoPublico.getId())
                .with(jwtContexto(usuarioId, perfilStr)))
                .andExpect(status().is(statusEsperado));
        }
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "PUT /recursos - {0} espera {1} [DIVERGÊNCIA]")
    @CsvSource({"Anônimo,401,null", "Solicitante,401,SOLICITANTE", "Responsável,401,RESPONSAVEL", "Administrador,200,ADMINISTRADOR"})
    void testarAtualizarRecursoDesprotegido(String descricao, int statusEsperado, String perfilStr) throws Exception {
        RecursoRequestDTO request = new RecursoRequestDTO();
        request.setNome("Sala Atualizada");
        request.setTipo("SALA");

        if ("null".equals(perfilStr)) {
            mockMvc.perform(put("/recursos/{id}", recursoPublico.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(statusEsperado));
        } else {
            UUID usuarioId = obterUsuarioId(perfilStr);
            mockMvc.perform(put("/recursos/{id}", recursoPublico.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(jwtContexto(usuarioId, perfilStr)))
                .andExpect(status().is(statusEsperado));
        }
        SecurityContextHolder.clearContext();
    }

    // ==================== USUÁRIOS DESPROTEGIDOS [DIVERGÊNCIA] ====================

    @ParameterizedTest(name = "GET /usuarios - {0} espera {1} [DIVERGÊNCIA]")
    @CsvSource({"Anônimo,401,null", "Solicitante,200,SOLICITANTE", "Responsável,200,RESPONSAVEL", "Administrador,200,ADMINISTRADOR"})
    void testarListarUsuariosDesprotegido(String descricao, int statusEsperado, String perfilStr) throws Exception {
        if ("null".equals(perfilStr)) {
            mockMvc.perform(get("/usuarios"))
                .andExpect(status().is(statusEsperado));
        } else {
            UUID usuarioId = obterUsuarioId(perfilStr);
            mockMvc.perform(get("/usuarios")
                .with(jwtContexto(usuarioId, perfilStr)))
                .andExpect(status().is(statusEsperado));
        }
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "DELETE /usuarios - {0} espera {1} [DIVERGÊNCIA]")
    @CsvSource({"Anônimo,401,null", "Solicitante,403,SOLICITANTE", "Responsável,403,RESPONSAVEL", "Administrador,204,ADMINISTRADOR"})
    void testarDeletarUsuarioDesprotegido(String descricao, int statusEsperado, String perfilStr) throws Exception {
        if ("null".equals(perfilStr)) {
            mockMvc.perform(delete("/usuarios/{id}", solicitante2.getId()))
                .andExpect(status().is(statusEsperado));
        } else {
            UUID usuarioId = obterUsuarioId(perfilStr);
            mockMvc.perform(delete("/usuarios/{id}", solicitante2.getId())
                .with(jwtContexto(usuarioId, perfilStr)))
                .andExpect(status().is(statusEsperado));
        }
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "GET /usuarios/{id} - {0} espera {1} [DIVERGÊNCIA]")
    @CsvSource({"Anônimo,401,null", "Solicitante,200,SOLICITANTE", "Responsável,200,RESPONSAVEL", "Administrador,200,ADMINISTRADOR"})
    void testarBuscarUsuarioDesprotegido(String descricao, int statusEsperado, String perfilStr) throws Exception {
        if ("null".equals(perfilStr)) {
            mockMvc.perform(get("/usuarios/{id}", solicitante1.getId()))
                .andExpect(status().is(statusEsperado));
        } else {
            UUID usuarioId = obterUsuarioId(perfilStr);
            mockMvc.perform(get("/usuarios/{id}", solicitante1.getId())
                .with(jwtContexto(usuarioId, perfilStr)))
                .andExpect(status().is(statusEsperado));
        }
        SecurityContextHolder.clearContext();
    }

    // ==================== MANUTENÇÕES DESPROTEGIDAS [DIVERGÊNCIA] ====================

    @ParameterizedTest(name = "POST /manutencoes - {0} espera {1} [DIVERGÊNCIA]")
    @CsvSource({"Anônimo,401,null", "Solicitante,401,SOLICITANTE", "Responsável,201,RESPONSAVEL", "Administrador,201,ADMINISTRADOR"})
    void testarCriarManutencaoDesprotegido(String descricao, int statusEsperado, String perfilStr) throws Exception {
        var request = new java.util.HashMap<String, Object>();
        request.put("recursoId", recursoPublico.getId().toString());
        request.put("dataInicio", LocalDateTime.now().plusHours(1).toString());
        request.put("dataFim", LocalDateTime.now().plusHours(2).toString());

        if ("null".equals(perfilStr)) {
            mockMvc.perform(post("/manutencoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(statusEsperado));
        } else {
            UUID usuarioId = obterUsuarioId(perfilStr);
            mockMvc.perform(post("/manutencoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(jwtContexto(usuarioId, perfilStr)))
                .andExpect(status().is(statusEsperado));
        }
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "GET /manutencoes - {0} espera {1} [DIVERGÊNCIA]")
    @CsvSource({"Anônimo,401,null", "Solicitante,403,SOLICITANTE", "Responsável,200,RESPONSAVEL", "Administrador,200,ADMINISTRADOR"})
    void testarListarManutencaoDesprotegido(String descricao, int statusEsperado, String perfilStr) throws Exception {
        if ("null".equals(perfilStr)) {
            mockMvc.perform(get("/manutencoes"))
                .andExpect(status().is(statusEsperado));
        } else {
            UUID usuarioId = obterUsuarioId(perfilStr);
            mockMvc.perform(get("/manutencoes")
                .with(jwtContexto(usuarioId, perfilStr)))
                .andExpect(status().is(statusEsperado));
        }
        SecurityContextHolder.clearContext();
    }

    // ==================== HELPER METHODS ====================

    private UUID obterUsuarioId(String perfil) {
        return switch(perfil) {
            case "SOLICITANTE" -> solicitante1.getId();
            case "RESPONSAVEL" -> responsavel.getId();
            case "ADMINISTRADOR" -> administrador.getId();
            default -> null;
        };
    }

    private RequestPostProcessor jwtContexto(UUID subject, String perfil) {
        return request -> {
            Jwt jwt = Jwt.withTokenValue("token-de-teste")
                .header("alg", "none")
                .subject(subject.toString())
                .claim("profile", perfil)
                .build();
            var contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(new JwtAuthenticationToken(jwt));
            SecurityContextHolder.setContext(contexto);
            return request;
        };
    }
}
