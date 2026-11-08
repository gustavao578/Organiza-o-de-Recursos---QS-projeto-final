package br.edu.organizacaorecursos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.dto.ReservaRequestDTO;
import br.edu.organizacaorecursos.exception.RegraNegocioException;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.service.ReservaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReservaApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private ReservaService reservaService;

    @BeforeEach
    void limparDados() {
        reservaRepository.deleteAll();
        manutencaoRepository.deleteAll();
        usuarioRepository.deleteAll();
        recursoRepository.deleteAll();
    }

    @Test
    void criaReservaEDevolveDtoSemExporEntidades() throws Exception {
        Recurso recurso = criarRecurso();
        ReservaRequestDTO request = reserva(recurso.getId(), inicioFuturo(), "usuario-api");

        mockMvc.perform(post("/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.recursoId").value(recurso.getId().toString()))
                .andExpect(jsonPath("$.nomeRecurso").value("Sala 1"))
                .andExpect(jsonPath("$.usuarioId").value(request.getUsuarioId().toString()))
                .andExpect(jsonPath("$.nomeUsuario").value("usuario-api"));

        org.assertj.core.api.Assertions.assertThat(reservaRepository.count()).isEqualTo(1);
    }

    @Test
    void criaEListaRecursosPelosEndpointsRest() throws Exception {
        String request = """
                {"nome":"Sala de Reunião A","tipo":"SALA"}
                """;

        mockMvc.perform(post("/recursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Sala de Reunião A"))
                .andExpect(jsonPath("$.tipo").value("SALA"));

        mockMvc.perform(get("/recursos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Sala de Reunião A"));

        org.assertj.core.api.Assertions.assertThat(recursoRepository.count()).isEqualTo(1);
    }

    @Test
    void rejeitaRecursoSemNomeOuTipoObrigatorios() throws Exception {
        mockMvc.perform(post("/recursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nome").exists())
                .andExpect(jsonPath("$.tipo").exists());

        org.assertj.core.api.Assertions.assertThat(recursoRepository.count()).isZero();
    }

    @Test
    void mantemAliasAnteriorParaCriacaoDeReserva() throws Exception {
        Recurso recurso = criarRecurso();
        ReservaRequestDTO request = reserva(recurso.getId(), inicioFuturo(), "usuario-api");

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recursoId").value(recurso.getId().toString()));
    }

    @Test
    void buscaReservaPorId() throws Exception {
        ReservaRequestDTO request = criarReserva();

        mockMvc.perform(get("/reservas/{id}", reservaRepository.findAll().get(0).getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recursoId").value(request.getRecursoId().toString()))
                .andExpect(jsonPath("$.usuarioId").value(request.getUsuarioId().toString()))
                .andExpect(jsonPath("$.nomeUsuario").value("usuario-api"));
    }

    @Test
    void retornaNotFoundAoBuscarReservaInexistente() throws Exception {
        mockMvc.perform(get("/reservas/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro")
                        .value("Reserva não encontrada para o ID informado."));
    }

    @Test
    void excluiReservaPorId() throws Exception {
        criarReserva();
        UUID reservaId = reservaRepository.findAll().get(0).getId();

        mockMvc.perform(delete("/reservas/{id}", reservaId))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(reservaRepository.existsById(reservaId)).isFalse();
    }

    @Test
    void retornaNotFoundAoExcluirReservaInexistente() throws Exception {
        mockMvc.perform(delete("/reservas/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Reserva não encontrada para exclusão."));
    }

    @Test
    void rejeitaIdentificadorDeReservaComFormatoInvalido() throws Exception {
        mockMvc.perform(get("/reservas/{id}", "id-invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro")
                        .value("O identificador informado possui formato inválido."));
    }

    @Test
    void publicaDocumentacaoOpenApiEEndpointsDeReserva() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API de Organização de Recursos"))
                .andExpect(jsonPath("$.paths['/reservas/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/reservas/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/recursos'].post").exists())
                .andExpect(jsonPath("$.paths['/usuarios'].post").exists())
                .andExpect(jsonPath("$.paths['/manutencoes'].post").exists());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void rejeitaPeriodosSobrepostosParaMesmoRecurso() throws Exception {
        Recurso recurso = criarRecurso();
        LocalDateTime inicio = inicioFuturo();

        enviarReserva(reserva(recurso.getId(), inicio, "primeiro"))
                .andExpect(status().isCreated());

        enviarReserva(reserva(recurso.getId(), inicio.plusMinutes(30), "segundo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro")
                        .value("O recurso já está reservado neste período."));

        org.assertj.core.api.Assertions.assertThat(reservaRepository.count()).isEqualTo(1);
    }

    @Test
    void informaRecursoEIntervaloQuandoExisteConflitoDeReserva() throws Exception {
        Recurso recurso = criarRecurso();
        LocalDateTime inicioExistente = inicioFuturo();
        ReservaRequestDTO reservaExistente = reserva(recurso.getId(), inicioExistente, "primeiro");
        enviarReserva(reservaExistente).andExpect(status().isCreated());

        enviarReserva(reserva(recurso.getId(), inicioExistente.plusMinutes(30), "segundo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro")
                        .value("O recurso já está reservado neste período."))
                .andExpect(jsonPath("$.detalhes").value(allOf(
                        containsString(recurso.getNome()),
                        containsString(inicioExistente.toString()),
                        containsString(reservaExistente.getDataFim().toString()))));
    }

    @Test
    void permiteReservasAdjacentes() throws Exception {
        Recurso recurso = criarRecurso();
        LocalDateTime inicio = inicioFuturo();

        enviarReserva(reserva(recurso.getId(), inicio, "primeiro"))
                .andExpect(status().isCreated());
        enviarReserva(reserva(recurso.getId(), inicio.plusHours(1), "segundo"))
                .andExpect(status().isCreated());

        org.assertj.core.api.Assertions.assertThat(reservaRepository.count()).isEqualTo(2);
    }

    @Test
    void rejeitaDataFimIgualAoInicio() throws Exception {
        Recurso recurso = criarRecurso();
        LocalDateTime inicio = inicioFuturo();
        ReservaRequestDTO request = new ReservaRequestDTO();
        request.setRecursoId(recurso.getId());
        request.setDataInicio(inicio);
        request.setDataFim(inicio);
        request.setUsuarioId(criarUsuario("usuario-api").getId());

        enviarReserva(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro")
                        .value("A data de término deve ser posterior à data de início."));

        org.assertj.core.api.Assertions.assertThat(reservaRepository.count()).isZero();
    }

    @Test
    void rejeitaDataFimAnteriorAoInicio() throws Exception {
        Recurso recurso = criarRecurso();
        LocalDateTime inicio = inicioFuturo();
        ReservaRequestDTO request = reserva(recurso.getId(), inicio, "usuario-api");
        request.setDataFim(inicio.minusMinutes(1));

        enviarReserva(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro")
                        .value("A data de término deve ser posterior à data de início."));

        org.assertj.core.api.Assertions.assertThat(reservaRepository.count()).isZero();
    }

    @Test
    void validaCamposObrigatoriosDoRequest() throws Exception {
        mockMvc.perform(post("/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.recursoId").isNotEmpty())
                .andExpect(jsonPath("$.dataInicio").isNotEmpty())
                .andExpect(jsonPath("$.dataFim").isNotEmpty())
                .andExpect(jsonPath("$.usuarioId").isNotEmpty());
    }

    @Test
    void rejeitaDataDeInicioNoPassado() throws Exception {
        Recurso recurso = criarRecurso();
        ReservaRequestDTO request = reserva(
                recurso.getId(), LocalDateTime.now().minusDays(1), "usuario-api");
        request.setDataFim(LocalDateTime.now().plusDays(2));

        enviarReserva(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dataInicio")
                        .value("A data de início deve ser no presente ou futuro."));
    }

    @Test
    void rejeitaUsuarioInexistente() throws Exception {
        Recurso recurso = criarRecurso();
        ReservaRequestDTO request = reserva(recurso.getId(), inicioFuturo(), "usuario-api");
        request.setUsuarioId(UUID.randomUUID());

        enviarReserva(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Usuário não encontrado."));
    }

    @Test
    void impedeReservaDuranteManutencao() throws Exception {
        Recurso recurso = criarRecurso();
        LocalDateTime inicio = inicioFuturo();
        var manutencao = new br.edu.organizacaorecursos.domain.Manutencao(
                null,
                recurso,
                inicio,
                inicio.plusHours(2),
                "Reparo");

        mockMvc.perform(post("/manutencoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manutencao)))
                .andExpect(status().isCreated());

        enviarReserva(reserva(recurso.getId(), inicio.plusMinutes(30), "usuario-api"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(
                        "O recurso não pode ser reservado pois estará em manutenção neste período."));

        org.assertj.core.api.Assertions.assertThat(reservaRepository.count()).isZero();
    }

    @Test
    void criaEListaUsuariosPelaApi() throws Exception {
        String request = """
                {"nome":"Usuário API","email":"usuario.api@example.com","matricula":"api-001"}
                """;

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Usuário API"));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("usuario.api@example.com"));
    }

    @Test
    void listaManutencoesPelaApi() throws Exception {
        Recurso recurso = criarRecurso();
        LocalDateTime inicio = inicioFuturo();
        var manutencao = new br.edu.organizacaorecursos.domain.Manutencao(
                null, recurso, inicio, inicio.plusHours(1), "Reparo");

        mockMvc.perform(post("/manutencoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manutencao)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value("Reparo"));

        mockMvc.perform(get("/manutencoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recurso.id").value(recurso.getId().toString()));
    }

    @Test
    void rejeitaManutencaoSemDatasOuDescricaoObrigatorias() throws Exception {
        Recurso recurso = criarRecurso();
        String request = """
                {"recurso":{"id":"%s"}}
                """.formatted(recurso.getId());

        mockMvc.perform(post("/manutencoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dataInicio").exists())
                .andExpect(jsonPath("$.dataFim").exists())
                .andExpect(jsonPath("$.descricao").exists());

        org.assertj.core.api.Assertions.assertThat(manutencaoRepository.count()).isZero();
    }

    @Test
    void rejeitaManutencaoSemIdentificadorDeRecurso() throws Exception {
        LocalDateTime inicio = inicioFuturo();
        String request = """
                {"recurso":{},"dataInicio":"%s","dataFim":"%s","descricao":"Reparo"}
                """.formatted(inicio, inicio.plusHours(1));

        mockMvc.perform(post("/manutencoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O ID do recurso é obrigatório."));

        org.assertj.core.api.Assertions.assertThat(manutencaoRepository.count()).isZero();
    }

    @Test
    void retornaNotFoundAoCriarManutencaoParaRecursoInexistente() throws Exception {
        LocalDateTime inicio = inicioFuturo();
        String request = """
                {"recurso":{"id":"%s"},"dataInicio":"%s","dataFim":"%s","descricao":"Reparo"}
                """.formatted(UUID.randomUUID(), inicio, inicio.plusHours(1));

        mockMvc.perform(post("/manutencoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Recurso não encontrado."));

        org.assertj.core.api.Assertions.assertThat(manutencaoRepository.count()).isZero();
    }

    @Test
    void retornaBadRequestQuandoRecursoNaoExiste() throws Exception {
        enviarReserva(reserva(UUID.randomUUID(), inicioFuturo(), "usuario-api"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Recurso não encontrado."));
    }

    @Test
    void aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas() throws Exception {
        Recurso recurso = criarRecurso();
        LocalDateTime inicio = inicioFuturo().plusDays(1);
        ReservaRequestDTO primeiro = reserva(recurso.getId(), inicio, "primeiro");
        ReservaRequestDTO segundo = reserva(recurso.getId(), inicio, "segundo");
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> tentarCriar(primeiro, prontas, iniciar));
            var segunda = executor.submit(() -> tentarCriar(segundo, prontas, iniciar));

            org.assertj.core.api.Assertions.assertThat(prontas.await(5, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();

            int aceitas = (primeira.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (segunda.get(10, TimeUnit.SECONDS) ? 1 : 0);

            org.assertj.core.api.Assertions.assertThat(aceitas).isEqualTo(1);
            org.assertj.core.api.Assertions.assertThat(reservaRepository.count()).isEqualTo(1);
        }
    }

    private boolean tentarCriar(
            ReservaRequestDTO request, CountDownLatch prontas, CountDownLatch iniciar)
            throws InterruptedException {
        prontas.countDown();
        if (!iniciar.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("A barreira concorrente não foi liberada.");
        }

        try {
            reservaService.criarReserva(request);
            return true;
        } catch (RegraNegocioException exception) {
            return false;
        }
    }

    private org.springframework.test.web.servlet.ResultActions enviarReserva(
            ReservaRequestDTO request) throws Exception {
        return mockMvc.perform(post("/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private Recurso criarRecurso() {
        return recursoRepository.save(new Recurso(null, "Sala 1", "SALA"));
    }

    private Usuario criarUsuario(String nome) {
        return usuarioRepository.save(new Usuario(
                null,
                nome,
                nome + "@example.com",
                "matricula-" + nome));
    }

    private ReservaRequestDTO criarReserva() throws Exception {
        Recurso recurso = criarRecurso();
        ReservaRequestDTO request = reserva(recurso.getId(), inicioFuturo(), "usuario-api");
        mockMvc.perform(post("/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        return request;
    }

    private ReservaRequestDTO reserva(UUID recursoId, LocalDateTime inicio, String usuario) {
        ReservaRequestDTO request = new ReservaRequestDTO();
        request.setRecursoId(recursoId);
        request.setDataInicio(inicio);
        request.setDataFim(inicio.plusHours(1));
        request.setUsuarioId(criarUsuario(usuario).getId());
        return request;
    }

    private LocalDateTime inicioFuturo() {
        return LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
    }
}
