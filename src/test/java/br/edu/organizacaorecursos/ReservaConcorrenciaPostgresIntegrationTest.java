package br.edu.organizacaorecursos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
class ReservaConcorrenciaPostgresIntegrationTest {

    private static final int SOLICITACOES = 20;
    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 5, 1, 10, 0);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurarPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ManutencaoRepository manutencaoRepository;

    @BeforeEach
    void limparBanco() {
        reservaRepository.deleteAll();
        manutencaoRepository.deleteAll();
        recursoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @RepeatedTest(5)
    void aceitaUmaReservaPorSalaEPorAgendaDoProfessorSobConcorrencia() throws Exception {
        Cenario sala = executarConcorrenciaDaSala();
        Cenario agendaProfessor = executarConcorrenciaDaAgendaDoProfessor();

        assertAll(
                () -> assertCenario("sala", sala),
                () -> assertCenario("agenda do professor", agendaProfessor));
    }

    private Cenario executarConcorrenciaDaSala() throws Exception {
        Recurso sala = recursoRepository.save(new Recurso(null, "Sala Concorrente", "SALA"));
        List<String> corpos = criarCorposDeReserva(sala.getId());

        List<Integer> statusHttp = dispararJuntos(corpos);
        int linhas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_reservas WHERE recurso_id = ? AND data_inicio = ? AND data_fim = ?",
                Integer.class,
                sala.getId(),
                Timestamp.valueOf(INICIO),
                Timestamp.valueOf(INICIO.plusHours(1)));

        return new Cenario(statusHttp, linhas);
    }

    private Cenario executarConcorrenciaDaAgendaDoProfessor() throws Exception {
        Usuario professor = new Usuario(null, "Professor Agenda", "professor-agenda@example.com", "matricula-professor-agenda");
        professor.setPerfil(Perfil.RESPONSAVEL);
        professor = usuarioRepository.save(professor);

        List<Recurso> salas = new ArrayList<>();
        for (int indice = 0; indice < SOLICITACOES; indice++) {
            salas.add(recursoRepository.save(new Recurso(
                    null, "Sala do Professor " + indice, "SALA", false, professor.getId())));
        }

        List<String> corpos = new ArrayList<>();
        for (int indice = 0; indice < salas.size(); indice++) {
            corpos.addAll(criarCorposDeReserva(salas.get(indice).getId(), 1, SOLICITACOES + indice));
        }

        List<Integer> statusHttp = dispararJuntos(corpos);
        Integer linhas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_reservas reserva "
                        + "JOIN tb_recursos recurso ON recurso.id = reserva.recurso_id "
                        + "WHERE recurso.responsavel_id = ? AND reserva.data_inicio = ? AND reserva.data_fim = ?",
                Integer.class,
                professor.getId(),
                Timestamp.valueOf(INICIO),
                Timestamp.valueOf(INICIO.plusHours(1)));

        return new Cenario(statusHttp, linhas == null ? 0 : linhas);
    }

    private List<String> criarCorposDeReserva(UUID recursoId) throws Exception {
        return criarCorposDeReserva(recursoId, SOLICITACOES, 0);
    }

    private List<String> criarCorposDeReserva(UUID recursoId, int quantidade, int deslocamento) throws Exception {
        List<String> corpos = new ArrayList<>();
        for (int indice = 0; indice < quantidade; indice++) {
            Usuario solicitante = usuarioRepository.save(new Usuario(
                    null,
                    "Solicitante " + (deslocamento + indice),
                    "solicitante-" + deslocamento + "-" + indice + "@example.com",
                    "matricula-solicitante-" + deslocamento + "-" + indice));
            corpos.add(objectMapper.writeValueAsString(new PedidoReserva(
                    recursoId, solicitante.getId(), INICIO, INICIO.plusHours(1))));
        }
        return corpos;
    }

    private List<Integer> dispararJuntos(List<String> corpos) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(SOLICITACOES);
        CountDownLatch prontos = new CountDownLatch(SOLICITACOES);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            List<Future<Integer>> respostas = new ArrayList<>();
            for (String corpo : corpos) {
                respostas.add(executor.submit(() -> {
                    prontos.countDown();
                    if (!iniciar.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("A barreira concorrente não foi liberada.");
                    }
                    return mockMvc.perform(post("/reservas")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(corpo))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            }

            assertThat(prontos.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            List<Integer> status = new ArrayList<>();
            for (Future<Integer> resposta : respostas) {
                status.add(resposta.get(30, TimeUnit.SECONDS));
            }
            return status;
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void assertCenario(String nome, Cenario cenario) {
        assertThat(cenario.statusHttp())
                .as("HTTP status por solicitação em %s", nome)
                .containsOnly(201, 409);
        assertThat(cenario.statusHttp().stream().filter(status -> status == 201).count())
                .as("reservas aceitas em %s", nome)
                .isEqualTo(1);
        assertThat(cenario.statusHttp().stream().filter(status -> status == 409).count())
                .as("reservas recusadas com 409 em %s", nome)
                .isEqualTo(19);
        assertThat(cenario.linhasNoBanco())
                .as("linhas sobrepostas no banco em %s", nome)
                .isEqualTo(1);
    }

    private record Cenario(List<Integer> statusHttp, int linhasNoBanco) {
    }

    private record PedidoReserva(UUID recursoId, UUID usuarioId, LocalDateTime dataInicio, LocalDateTime dataFim) {
    }
}