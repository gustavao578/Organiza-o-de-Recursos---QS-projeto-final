package br.edu.organizacaorecursos;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.domain.StatusReserva;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RelatorioUtilizacaoApiTest {

    private static final LocalDateTime PERIODO_INICIO = LocalDateTime.of(2026, 3, 15, 10, 0);
    private static final LocalDateTime PERIODO_FIM = LocalDateTime.of(2026, 3, 15, 15, 0);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private SecretKey jwtSigningKey;

    @BeforeEach
    void limparDados() {
        jdbcTemplate.execute("DELETE FROM tb_conflitos_reserva");
        jdbcTemplate.execute("DELETE FROM tb_reserva_auditorias");
        reservaRepository.deleteAll();
        manutencaoRepository.deleteAll();
        usuarioRepository.deleteAll();
        recursoRepository.deleteAll();
    }

    @Test
    void agregaHorasPorRecursoEUsuarioComInterseccaoDoPeriodo() throws Exception {
        Usuario professoraA = criarUsuario("Professora A");
        Usuario professoraB = criarUsuario("Professor B");
        Recurso sala = criarRecurso("Sala A");
        Recurso laboratorio = criarRecurso("Laboratório B");

        criarReserva(sala, professoraA, LocalDateTime.of(2026, 3, 15, 9, 0),
                LocalDateTime.of(2026, 3, 15, 11, 0), StatusReserva.PENDENTE);
        criarReserva(sala, professoraA, LocalDateTime.of(2026, 3, 15, 14, 0),
                LocalDateTime.of(2026, 3, 15, 16, 0), StatusReserva.SOLICITADA);
        criarReserva(sala, professoraA, LocalDateTime.of(2026, 3, 15, 11, 0),
                LocalDateTime.of(2026, 3, 15, 12, 0), StatusReserva.APROVADA);
        criarReserva(sala, professoraA, LocalDateTime.of(2026, 3, 15, 12, 0),
                LocalDateTime.of(2026, 3, 15, 13, 0), StatusReserva.EM_USO);
        criarReserva(sala, professoraB, LocalDateTime.of(2026, 3, 15, 10, 0),
                LocalDateTime.of(2026, 3, 15, 14, 0), StatusReserva.CANCELADA);
        criarReserva(laboratorio, professoraB, LocalDateTime.of(2026, 3, 15, 13, 0),
                LocalDateTime.of(2026, 3, 15, 14, 0), StatusReserva.CONCLUÍDA);
        criarReserva(laboratorio, professoraA, LocalDateTime.of(2026, 3, 15, 10, 0),
                LocalDateTime.of(2026, 3, 15, 11, 0), StatusReserva.REJEITADA);
        criarReserva(laboratorio, professoraB, LocalDateTime.of(2026, 3, 15, 11, 0),
                LocalDateTime.of(2026, 3, 15, 12, 0), StatusReserva.NÃO_COMPARECEU);

        // Conta manual no período [10:00,15:00): Sala A = 1h (9–11) + 1h (14–16)
        // + 1h APROVADA + 1h EM_USO = 4h; a CANCELADA não conta.
        // Laboratório B = 1h CONCLUÍDA + 1h NÃO_COMPARECEU = 2h; REJEITADA não conta.
        // Professora A totaliza 4h e Professor B 2h; total geral = 6h.
        ResponseEntity<JsonNode> response = consultarRelatorio(
                PERIODO_INICIO, PERIODO_FIM, "RESPONSAVEL", UUID.randomUUID());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode relatorio = response.getBody();
        assertThat(relatorio).isNotNull();
        assertThat(relatorio.path("porRecurso").size()).isEqualTo(2);
        assertThat(relatorio.path("porUsuario").size()).isEqualTo(2);
        assertThat(relatorio.path("conflitosEvitados").asLong()).isZero();
        assertThat(linhaPorId(relatorio.path("porRecurso"), sala.getId()).path("horas").asDouble())
                .isEqualTo(4.0);
        assertThat(linhaPorId(relatorio.path("porRecurso"), laboratorio.getId()).path("horas").asDouble())
                .isEqualTo(2.0);
        assertThat(linhaPorId(relatorio.path("porUsuario"), professoraA.getId()).path("horas").asDouble())
                .isEqualTo(4.0);
        assertThat(linhaPorId(relatorio.path("porUsuario"), professoraB.getId()).path("horas").asDouble())
                .isEqualTo(2.0);
    }

    @Test
    void periodoSemReservasRetornaListasVaziasEZeroConflitos() throws Exception {
        ResponseEntity<JsonNode> response = consultarRelatorio(
                LocalDateTime.of(2030, 1, 1, 0, 0),
                LocalDateTime.of(2030, 1, 2, 0, 0),
                "RESPONSAVEL",
                UUID.randomUUID());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("porRecurso").size()).isZero();
        assertThat(response.getBody().path("porUsuario").size()).isZero();
        assertThat(response.getBody().path("conflitosEvitados").asLong()).isZero();
    }

    @Test
    void rejeitaPeriodoVazioOuInvertido() throws Exception {
        LocalDateTime inicio = LocalDateTime.of(2026, 3, 15, 10, 0);

        assertThat(consultarRelatorio(inicio, inicio, "ADMINISTRADOR", UUID.randomUUID())
                .getStatusCode().value()).isEqualTo(400);
        assertThat(consultarRelatorio(inicio.plusHours(1), inicio, "ADMINISTRADOR", UUID.randomUUID())
                .getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void rejeitaDataHoraMalformadaComMensagemDoPeriodo() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token(UUID.randomUUID(), "ADMINISTRADOR"));
        var uri = UriComponentsBuilder.fromHttpUrl(baseUrl() + "/relatorios/utilizacao")
                .queryParam("inicio", "nao-e-data")
                .queryParam("fim", PERIODO_FIM)
                .build()
                .encode()
                .toUri();

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("erro").asText())
                .isEqualTo("As datas do período devem usar formato ISO-8601.");
    }

    @Test
    void exigeTokenEPermiteSomenteResponsavelOuAdministrador() throws Exception {
        LocalDateTime inicio = LocalDateTime.of(2030, 1, 1, 0, 0);
        LocalDateTime fim = inicio.plusDays(1);

        assertThat(consultarRelatorio(inicio, fim, null, null).getStatusCode().value()).isEqualTo(401);
        assertThat(consultarRelatorio(inicio, fim, "SOLICITANTE", UUID.randomUUID())
                .getStatusCode().value()).isEqualTo(403);
        assertThat(consultarRelatorio(inicio, fim, "RESPONSAVEL", UUID.randomUUID())
                .getStatusCode().value()).isEqualTo(200);
        assertThat(consultarRelatorio(inicio, fim, "ADMINISTRADOR", UUID.randomUUID())
                .getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void registraTentativa409EContaPeloInstanteDaRecusa() throws Exception {
        Usuario solicitante = criarUsuario("Solicitante de teste");
        Recurso sala = criarRecurso("Sala com conflito");
        LocalDateTime dataSolicitada = LocalDateTime.now().plusDays(30).withSecond(0).withNano(0);
        criarReserva(sala, solicitante, dataSolicitada, dataSolicitada.plusHours(1), StatusReserva.APROVADA);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token(solicitante.getId(), "SOLICITANTE"));
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "recursoId", sala.getId(),
                "usuarioId", solicitante.getId(),
                "dataInicio", dataSolicitada.plusMinutes(15).toString(),
                "dataFim", dataSolicitada.plusMinutes(45).toString()));
        ResponseEntity<JsonNode> tentativa = restTemplate.postForEntity(
                baseUrl() + "/reservas",
                new HttpEntity<>(requestBody, headers),
                JsonNode.class);

        assertThat(tentativa.getStatusCode().value()).isEqualTo(409);

        LocalDateTime inicioTentativa = LocalDateTime.now().minusMinutes(1);
        ResponseEntity<JsonNode> relatorio = consultarRelatorio(
                inicioTentativa,
                inicioTentativa.plusDays(1),
                "ADMINISTRADOR",
                UUID.randomUUID());

        assertThat(relatorio.getStatusCode().value()).isEqualTo(200);
        assertThat(relatorio.getBody()).isNotNull();
        assertThat(relatorio.getBody().path("conflitosEvitados").asLong()).isEqualTo(1);
    }

    @Test
    void publicaEndpointNoOpenApi() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(baseUrl() + "/v3/api-docs", JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("paths").has("/relatorios/utilizacao")).isTrue();
        JsonNode operacao = response.getBody().path("paths").path("/relatorios/utilizacao").path("get");
        assertThat(operacao.path("security").get(0).has("bearerAuth")).isTrue();
        assertThat(operacao.path("parameters").findValuesAsText("name"))
                .contains("inicio", "fim");
        assertThat(operacao.path("parameters").findValuesAsText("description").toString())
                .contains("inclusivo", "exclusivo");
        for (JsonNode parametro : operacao.path("parameters")) {
            if (parametro.path("name").asText().equals("inicio")
                    || parametro.path("name").asText().equals("fim")) {
                assertThat(parametro.path("required").asBoolean()).isTrue();
            }
        }
        assertThat(response.getBody()
                        .at("/components/schemas/RelatorioUtilizacaoResponseDTO/properties/porUsuario")
                        .path("description")
                        .asText())
                .contains("Reserva.usuario");
    }

    private Usuario criarUsuario(String nome) {
        String sufixo = UUID.randomUUID().toString();
        return usuarioRepository.save(new Usuario(
                null,
                nome,
                sufixo + "@example.test",
                sufixo,
                Perfil.SOLICITANTE));
    }

    private Recurso criarRecurso(String nome) {
        return recursoRepository.save(new Recurso(null, nome, "SALA", false, null));
    }

    private void criarReserva(
            Recurso recurso,
            Usuario usuario,
            LocalDateTime inicio,
            LocalDateTime fim,
            StatusReserva status) {
        reservaRepository.save(new Reserva(null, recurso, inicio, fim, status, usuario));
    }

    private ResponseEntity<JsonNode> consultarRelatorio(
            LocalDateTime inicio, LocalDateTime fim, String perfil, UUID sujeito) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        if (perfil != null) {
            headers.setBearerAuth(token(sujeito, perfil));
        }
        var uri = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host("localhost")
                .port(port)
                .path("/relatorios/utilizacao")
                .queryParam("inicio", inicio)
                .queryParam("fim", fim)
                .build()
                .encode()
                .toUri();
        return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    }

    private String token(UUID sujeito, String perfil) throws Exception {
        long agora = java.time.Instant.now().getEpochSecond();
        String cabecalho = base64Url(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
        String claims = base64Url(objectMapper.writeValueAsBytes(Map.of(
                "sub", sujeito.toString(),
                "profile", perfil,
                "iat", agora,
                "exp", agora + 3600)));
        String conteudo = cabecalho + "." + claims;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(jwtSigningKey);
        return conteudo + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(conteudo.getBytes(StandardCharsets.US_ASCII)));
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private JsonNode linhaPorId(JsonNode linhas, UUID id) {
        for (JsonNode linha : linhas) {
            if (id.toString().equals(linha.path("id").asText())) {
                return linha;
            }
        }
        throw new AssertionError("Linha de relatório não encontrada para o ID " + id);
    }
}
