package br.edu.organizacaorecursos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.organizacaorecursos.domain.Manutencao;
import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.domain.StatusReserva;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class Story22ApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RecursoRepository recursoRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private ManutencaoRepository manutencaoRepository;
    @Autowired private JwtEncoder jwtEncoder;

    private Usuario admin;
    private Usuario requester;
    private Usuario responsible;
    private Recurso resource;

    @BeforeEach
    void setUp() {
        manutencaoRepository.deleteAll();
        reservaRepository.deleteAll();
        recursoRepository.deleteAll();
        usuarioRepository.deleteAll();
        requester = saveUser("requester", Perfil.SOLICITANTE);
        responsible = saveUser("responsible", Perfil.RESPONSAVEL);
        admin = saveUser("admin", Perfil.ADMINISTRADOR);
        resource = recursoRepository.save(new Recurso(
                null, "Sala A", "SALA", true, responsible.getId()));
    }

    @AfterEach
    void clearContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void crudManutencaoRetornaDtoEFiltraPorRecurso() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        String request = manutencaoJson(resource.getId(), start, start.plusHours(1), "Reparo");

        String id = mockMvc.perform(post("/manutencoes")
                        .with(jwt(admin.getId(), "ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recursoId").value(resource.getId().toString()))
                .andExpect(jsonPath("$.nomeRecurso").value("Sala A"))
                .andExpect(jsonPath("$.recurso").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID maintenanceId = UUID.fromString(
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(id).get("id").asText());

        mockMvc.perform(get("/manutencoes")
                        .param("recursoId", resource.getId().toString())
                        .with(jwt(responsible.getId(), "RESPONSAVEL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(maintenanceId.toString()));

        mockMvc.perform(put("/manutencoes/{id}", maintenanceId)
                        .with(jwt(responsible.getId(), "RESPONSAVEL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manutencaoJson(resource.getId(), start.plusHours(2),
                                start.plusHours(3), "Reparo atualizado")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Reparo atualizado"));

        mockMvc.perform(delete("/manutencoes/{id}", maintenanceId)
                        .with(jwt(admin.getId(), "ADMINISTRADOR")))
                .andExpect(status().isNoContent());
    }

    @Test
    void manutencaoRejeitaPeriodoInvalidoESobreposicaoAtiva() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        mockMvc.perform(post("/manutencoes")
                        .with(jwt(admin.getId(), "ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manutencaoJson(resource.getId(), start, start, "Período inválido")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(
                        "A data de fim deve ser posterior à data de início."));

        reservaRepository.save(new Reserva(
                null, resource, start, start.plusHours(1), StatusReserva.EM_USO, requester));
        mockMvc.perform(post("/manutencoes")
                        .with(jwt(admin.getId(), "ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manutencaoJson(
                                resource.getId(), start.plusMinutes(30), start.plusHours(2), "Conflito")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(
                        "A manutenção não pode sobrepor uma reserva ativa do recurso."));
    }

    @ParameterizedTest
    @EnumSource(value = StatusReserva.class, names = {"PENDENTE", "APROVADA", "EM_USO"})
    void bloqueiaExclusaoDeUsuarioERecursoComReservaAtiva(StatusReserva statusAtivo) throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(4);
        reservaRepository.save(new Reserva(
                null, resource, start, start.plusHours(1), statusAtivo, requester));

        mockMvc.perform(delete("/usuarios/{id}", requester.getId())
                        .with(jwt(admin.getId(), "ADMINISTRADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(
                        "Não é possível excluir um usuário com reservas ativas."));

        mockMvc.perform(delete("/recursos/{id}", resource.getId())
                        .with(jwt(admin.getId(), "ADMINISTRADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(
                        "Não é possível excluir um recurso com reservas ativas."));
    }

    @Test
    void validaResponsavelDoRecursoEFiltraRecursos() throws Exception {
        Usuario invalidResponsible = saveUser("invalid", Perfil.SOLICITANTE);
        String invalidRequest = """
                {"nome":"Sala inválida","tipo":"SALA","restrito":true,"responsavelId":"%s"}
                """.formatted(invalidResponsible.getId());

        mockMvc.perform(post("/recursos")
                        .with(jwt(admin.getId(), "ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(
                        "O usuário informado não possui perfil RESPONSAVEL."));

        mockMvc.perform(get("/recursos")
                        .param("tipo", "SALA")
                        .param("restrito", "true")
                        .with(jwt(requester.getId(), "SOLICITANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(resource.getId().toString()));

        mockMvc.perform(get("/recursos")
                        .param("tipo", "PROJETOR")
                        .with(jwt(requester.getId(), "SOLICITANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void edicaoDeUsuarioExigeProprietarioOuAdministrador() throws Exception {
        String update = """
                {"nome":"Nome atualizado","email":"updated@example.com",
                 "matricula":"updated-001","perfil":"ADMINISTRADOR"}
                """;

        mockMvc.perform(get("/usuarios")
                        .with(jwt(requester.getId(), "SOLICITANTE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/usuarios/{id}", requester.getId())
                        .with(jwt(responsible.getId(), "RESPONSAVEL")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/usuarios/{id}", requester.getId())
                        .with(jwt(requester.getId(), "SOLICITANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requester.getId().toString()));

        mockMvc.perform(put("/usuarios/{id}", requester.getId())
                        .with(jwt(responsible.getId(), "RESPONSAVEL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/usuarios/{id}", requester.getId())
                        .with(jwt(requester.getId(), "SOLICITANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("SOLICITANTE"))
                .andExpect(jsonPath("$.nome").value("Nome atualizado"));
    }

    @Test
    void filtrosDeReservaPreservamListaSemFiltro() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        Recurso otherResource = recursoRepository.save(new Recurso(null, "Projetor", "PROJETOR"));
        reservaRepository.save(new Reserva(
                null, resource, start, start.plusHours(1), StatusReserva.PENDENTE, requester));
        reservaRepository.save(new Reserva(
                null, otherResource, start.plusHours(2), start.plusHours(3),
                StatusReserva.APROVADA, requester));

        mockMvc.perform(get("/reservas").with(jwt(admin.getId(), "ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/reservas")
                        .param("status", "PENDENTE")
                        .param("recursoId", resource.getId().toString())
                        .with(jwt(admin.getId(), "ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"));
    }

    private Usuario saveUser(String suffix, Perfil profile) {
        return usuarioRepository.save(new Usuario(
                null, suffix, suffix + "@example.com", suffix + "-001", profile));
    }

    private String manutencaoJson(UUID resourceId, LocalDateTime start, LocalDateTime end, String description) {
        return """
                {"recursoId":"%s","dataInicio":"%s","dataFim":"%s","descricao":"%s"}
                """.formatted(resourceId, start, end, description);
    }

    private RequestPostProcessor jwt(UUID subject, String profile) {
        Instant issuedAt = Instant.now();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                        .subject(subject.toString())
                        .claim("profile", profile)
                        .issuedAt(issuedAt)
                        .expiresAt(issuedAt.plus(Duration.ofMinutes(5)))
                        .build()))
                .getTokenValue();
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }
}
