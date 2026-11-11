package br.edu.organizacaorecursos.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.edu.organizacaorecursos.domain.Manutencao;
import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.StatusReserva;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.dto.ReservaRequestDTO;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public class ReservaApiSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ManutencaoRepository manutencaoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private UUID recursoId;
    private UUID solicitanteId;
    private UUID responsavelId;
    private UUID reservaId;
    private String token;
    private int statusHttp;
    private JsonNode resposta;

    @Before
    public void limparDados() {
        jdbcTemplate.execute("DELETE FROM tb_reserva_auditorias");
        reservaRepository.deleteAll();
        manutencaoRepository.deleteAll();
        usuarioRepository.deleteAll();
        recursoRepository.deleteAll();
        recursoId = null;
        solicitanteId = null;
        responsavelId = null;
        reservaId = null;
        token = null;
        resposta = null;
    }

    @Dado("que o recurso está classificado como restrito")
    public void recursoRestrito() {
        prepararRecursoRestrito();
    }

    @Dado("que o solicitante tem permissão para solicitar uma reserva")
    public void solicitanteTemPermissao() {
        usarToken("SOLICITANTE", solicitanteId);
    }

    @Quando("o solicitante envia uma solicitação válida para esse recurso")
    public void solicitanteEnviaSolicitacao() throws Exception {
        criarSolicitacao(inicioFuturo(), inicioFuturo().plusHours(1));
    }

    @Entao("a reserva é registrada como PENDENTE e não como confirmação final")
    public void reservaPermaneceSolicitada() throws Exception {
        assertEquals(201, statusHttp);
        assertEquals("PENDENTE", resposta.path("status").asText());
        assertEquals(StatusReserva.PENDENTE, reservaRepository.findById(reservaId).orElseThrow().getStatus());
    }

    @Dado("que existe uma reserva restrita no estado PENDENTE")
    public void existeReservaRestritaSolicitada() throws Exception {
        prepararRecursoRestrito();
        criarSolicitacao(inicioFuturo(), inicioFuturo().plusHours(1));
        assertEquals(201, statusHttp);
    }

    @Dado("que o ator é o responsável autorizado pelo recurso")
    public void atorResponsavel() {
        usarToken("ADMIN", responsavelId);
    }

    @Dado("que o ator é o solicitante dessa reserva")
    public void atorSolicitante() {
        responsavelId = solicitanteId;
        Usuario solicitante = usuarioRepository.findById(solicitanteId).orElseThrow();
        solicitante.setPerfil(Perfil.ADMINISTRADOR);
        usuarioRepository.save(solicitante);
        Recurso recurso = recursoRepository.findById(recursoId).orElseThrow();
        recurso.setResponsavelId(solicitanteId);
        recursoRepository.save(recurso);
        usarToken("ADMIN", solicitanteId);
    }

    @Dado("que o ator não é o responsável autorizado pelo recurso")
    public void atorNaoResponsavel() {
        Usuario administrador = criarUsuario("administrador");
        administrador.setPerfil(Perfil.SOLICITANTE);
        usuarioRepository.save(administrador);
        usarToken("ADMIN", administrador.getId());
    }

    @Quando("o ator aprova a reserva")
    public void atorAprovaReserva() throws Exception {
        aprovarReserva();
    }

    @Quando("o ator tenta aprovar a reserva")
    public void atorTentaAprovarReserva() throws Exception {
        aprovarReserva();
    }

    @Entao("a reserva passa para o estado APROVADA")
    public void reservaPassaParaAprovada() throws Exception {
        assertEquals(200, statusHttp);
        assertEquals("APROVADA", resposta.path("status").asText());
    }

    @Entao("a operação é rejeitada e a reserva permanece PENDENTE")
    public void operacaoRejeitadaReservaSolicitada() throws Exception {
        assertTrue(statusHttp >= 400 && statusHttp < 500);
        assertEquals(StatusReserva.PENDENTE,
                reservaRepository.findById(reservaId).orElseThrow().getStatus());
    }

    @Dado("^que o recurso tem manutenção de (.+) até (.+)$")
    public void recursoTemManutencao(String inicio, String fim) {
        prepararSolicitante();
        usarToken("SOLICITANTE", solicitanteId);
        Recurso recurso = recursoRepository.save(new Recurso(null, "Sala Manutenção", "SALA"));
        recursoId = recurso.getId();
        manutencaoRepository.save(new Manutencao(
                null,
                recurso,
                LocalDateTime.parse(inicio),
                LocalDateTime.parse(fim),
                "Manutenção programada"));
    }

    @Quando("^o solicitante cria uma reserva de (.+) até (.+)$")
    public void solicitanteCriaReservaNoPeriodo(String inicio, String fim) throws Exception {
        criarSolicitacao(LocalDateTime.parse(inicio), LocalDateTime.parse(fim));
    }

    @Entao("^a resposta da API é (\\d+) e a reserva (.+)$")
    public void respostaApiEEstadoPersistencia(int statusEsperado, String persistencia) {
        assertEquals(statusEsperado, statusHttp);
        assertEquals("é persistida".equals(persistencia), reservaRepository.count() == 1);
    }

    @Dado("^que a reserva está no estado (.+)$")
    public void reservaEstaNoEstado(String estado) throws Exception {
        prepararRecursoRestrito();
        criarSolicitacao(inicioFuturo(), inicioFuturo().plusHours(1));
        if ("APROVADA".equals(estado) || "EM_USO".equals(estado) || "CONCLUÍDA".equals(estado)) {
            usarToken("ADMIN", responsavelId);
            aprovarReserva();
        }
        if ("EM_USO".equals(estado) || "CONCLUÍDA".equals(estado)) {
            usarToken("RESPONSAVEL", responsavelId);
            enviarPost("/reservas/" + reservaId + "/iniciar", "{}");
        }
        if ("CONCLUÍDA".equals(estado)) {
            usarToken("RESPONSAVEL", responsavelId);
            enviarPost("/reservas/" + reservaId + "/concluir", "{}");
        }
        assertEquals(estado, reservaRepository.findById(reservaId).orElseThrow().getStatus().name());
    }

    @Quando("^um ator autorizado solicita a transição para (.+)$")
    public void atorAutorizadoSolicitaTransicao(String destino) throws Exception {
        atorSolicitaTransicao(destino);
    }

    @Quando("^um ator solicita a transição para (.+)$")
    public void atorSolicitaTransicao(String destino) throws Exception {
        switch (destino) {
            case "APROVADA" -> {
                usarToken("ADMIN", responsavelId);
                aprovarReserva();
            }
            case "EM_USO" -> {
                usarToken("RESPONSAVEL", responsavelId);
                enviarPost("/reservas/" + reservaId + "/iniciar", "{}");
            }
            case "CONCLUÍDA" -> {
                usarToken("RESPONSAVEL", responsavelId);
                enviarPost("/reservas/" + reservaId + "/concluir", "{}");
            }
            case "REJEITADA" -> {
                usarToken("ADMIN", responsavelId);
                enviarPostComAtor("/reservas/" + reservaId + "/rejeitar");
            }
            case "CANCELADA" -> {
                usarToken("SOLICITANTE", solicitanteId);
                enviarPostComAtor("/reservas/" + reservaId + "/cancelar");
            }
            case "NÃO_COMPARECEU" -> {
                usarToken("RESPONSAVEL", responsavelId);
                enviarPostComAtor("/reservas/" + reservaId + "/nao-compareceu");
            }
            case "PENDENTE" -> {
                usarToken("ADMIN", responsavelId);
                enviarPatchStatus("PENDENTE");
            }
            default -> throw new IllegalArgumentException("Estado de destino não suportado: " + destino);
        }
    }

    @Entao("^a operação é aceita e o estado persistido passa a (.+)$")
    public void operacaoAceitaEstado(String estado) {
        assertEquals(200, statusHttp);
        assertEquals(estado, resposta.path("status").asText());
        assertEquals(estado, reservaRepository.findById(reservaId).orElseThrow().getStatus().name());
    }

    @Entao("^a operação é rejeitada e a reserva permanece em (.+)$")
    public void operacaoRejeitadaEstado(String estado) {
        assertTrue(statusHttp >= 400 && statusHttp < 500);
        assertEquals(estado, reservaRepository.findById(reservaId).orElseThrow().getStatus().name());
    }

    private void prepararRecursoRestrito() {
        prepararSolicitante();
        Usuario responsavel = criarUsuario("responsavel");
        responsavel.setPerfil(Perfil.RESPONSAVEL);
        usuarioRepository.save(responsavel);
        responsavelId = responsavel.getId();
        Recurso recurso = recursoRepository.save(
                new Recurso(null, "Sala Restrita", "SALA", true, responsavelId));
        recursoId = recurso.getId();
        usarToken("SOLICITANTE", solicitanteId);
    }

    private void prepararSolicitante() {
        Usuario solicitante = criarUsuario("solicitante");
        solicitanteId = solicitante.getId();
    }

    private Usuario criarUsuario(String nome) {
        String sufixo = UUID.randomUUID().toString();
        return usuarioRepository.save(new Usuario(
                null,
                nome,
                nome + "-" + sufixo + "@example.com",
                nome + "-" + sufixo));
    }

    private void criarSolicitacao(LocalDateTime inicio, LocalDateTime fim) throws Exception {
        ReservaRequestDTO request = new ReservaRequestDTO();
        request.setRecursoId(recursoId);
        request.setDataInicio(inicio);
        request.setDataFim(fim);
        request.setUsuarioId(solicitanteId);
        MvcResult result = mockMvc.perform(post("/reservas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        guardarResposta(result);
        if (statusHttp == 201) {
            reservaId = UUID.fromString(resposta.path("id").asText());
        }
    }

    private void aprovarReserva() throws Exception {
        enviarPostComAtor("/reservas/" + reservaId + "/aprovar");
    }

    private void enviarPostComAtor(String caminho) throws Exception {
        enviarPost(caminho, "{}");
    }

    private void enviarPost(String caminho, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(caminho)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        guardarResposta(result);
    }

    private void enviarPatchStatus(String status) throws Exception {
        MvcResult result = mockMvc.perform(patch("/reservas/" + reservaId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"%s"}
                                """.formatted(status)))
                .andReturn();
        guardarResposta(result);
    }

    private void guardarResposta(MvcResult result) throws Exception {
        statusHttp = result.getResponse().getStatus();
        byte[] body = result.getResponse().getContentAsByteArray();
        resposta = body.length == 0 ? objectMapper.createObjectNode() : objectMapper.readTree(body);
    }

    private void usarToken(String perfil, UUID id) {
        token = gerarToken(perfil, id);
    }

    private String gerarToken(String perfil, UUID id) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(id.toString())
                    .claim("profile", perfil)
                    .issueTime(Date.from(clock.instant()))
                    .expirationTime(Date.from(clock.instant().plusSeconds(3600)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(jwtSecret.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar o token de teste.", exception);
        }
    }

    private LocalDateTime inicioFuturo() {
        return LocalDateTime.now(clock).plusDays(2).withSecond(0).withNano(0);
    }
}