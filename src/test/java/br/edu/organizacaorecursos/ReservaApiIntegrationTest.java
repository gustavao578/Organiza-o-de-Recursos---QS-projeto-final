package br.edu.organizacaorecursos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.dto.ReservaRequestDTO;
import br.edu.organizacaorecursos.exception.RegraNegocioException;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
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
    private ReservaService reservaService;

    @BeforeEach
    void limparDados() {
        reservaRepository.deleteAll();
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
                .andExpect(jsonPath("$.usuarioResponsavel").value("usuario-api"));

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
        request.setUsuarioResponsavel("usuario-api");

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
                .andExpect(jsonPath("$.usuarioResponsavel").isNotEmpty());
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
    void rejeitaResponsavelEmBranco() throws Exception {
        Recurso recurso = criarRecurso();
        ReservaRequestDTO request = reserva(recurso.getId(), inicioFuturo(), " ");

        enviarReserva(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.usuarioResponsavel")
                        .value("O nome do responsável é obrigatório."));
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

    private ReservaRequestDTO reserva(UUID recursoId, LocalDateTime inicio, String usuario) {
        ReservaRequestDTO request = new ReservaRequestDTO();
        request.setRecursoId(recursoId);
        request.setDataInicio(inicio);
        request.setDataFim(inicio.plusHours(1));
        request.setUsuarioResponsavel(usuario);
        return request;
    }

    private LocalDateTime inicioFuturo() {
        return LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
    }
}
