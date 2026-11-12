package br.edu.organizacaorecursos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
class ReservaExclusionPostgresIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @SpyBean
    private ReservaRepository reservaRepository;

    @BeforeEach
    void prepararSchemaECriarMigracao() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.tb_reservas')", String.class))
                .as("tabela criada e gerenciada pelo Hibernate")
                .isEqualTo("tb_reservas");

        Integer constraintCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname = ?",
                Integer.class,
                "ex_tb_reservas_sem_sobreposicao_ativa");
        if (constraintCount == null || constraintCount == 0) {
            new ResourceDatabasePopulator(
                    new ClassPathResource("db/migration/V1__reservas_sem_sobreposicao_ativa.sql"))
                    .execute(dataSource);
        }
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname = ?",
                Integer.class,
                "ex_tb_reservas_sem_sobreposicao_ativa"))
                .isEqualTo(1);
        jdbcTemplate.update("DELETE FROM tb_reservas");
        recursoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDENTE", "APROVADA", "EM_USO"})
    void rejeitaFisicamenteSobreposicaoDosStatusAtivos(String status) {
        Fixture fixture = criarFixture();
        inserir(fixture, status, INICIO, INICIO.plusHours(1));

        assertThatThrownBy(() -> inserir(
                fixture, status, INICIO.plusMinutes(30), INICIO.plusHours(2)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("ex_tb_reservas_sem_sobreposicao_ativa");
    }

    @Test
    void permitePeriodosAdjacentesNoMesmoRecurso() {
        Fixture fixture = criarFixture();

        inserir(fixture, "PENDENTE", INICIO, INICIO.plusHours(1));
        inserir(fixture, "APROVADA", INICIO.plusHours(1), INICIO.plusHours(2));

        assertThat(contar(fixture.recurso().getId())).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CANCELADA", "REJEITADA", "CONCLUÍDA", "NÃO_COMPARECEU", "SOLICITADA"})
    void naoAplicaRestricaoAStatusForaDaListaAtiva(String statusInativo) {
        Fixture fixture = criarFixture();

        inserir(fixture, statusInativo, INICIO, INICIO.plusHours(1));
        inserir(fixture, "PENDENTE", INICIO.plusMinutes(30), INICIO.plusHours(2));

        assertThat(contar(fixture.recurso().getId())).isEqualTo(2);
    }

    @Test
    void endpointRetorna409QuandoExclusionConstraintRejeitaReserva() throws Exception {
        Fixture fixture = criarFixture();
        inserir(fixture, "PENDENTE", INICIO, INICIO.plusHours(1));

        // Deixa a constraint PostgreSQL ser o árbitro sem criar uma corrida temporal:
        // a verificação preventiva é stubada apenas neste teste e a escrita é real.
        doReturn(false).when(reservaRepository).existeConflitoDeHorario(
                eq(fixture.recurso().getId()),
                eq(INICIO.plusMinutes(30)),
                eq(INICIO.plusHours(2)));

        String corpo = objectMapper.writeValueAsString(new PedidoReserva(
                fixture.recurso().getId(),
                fixture.usuario().getId(),
                INICIO.plusMinutes(30),
                INICIO.plusHours(2)));

        try {
            mockMvc.perform(post("/reservas")
                            .with(jwtContexto(fixture.usuario().getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.erro")
                            .value("Os dados informados conflitam com um registro existente."));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(reservaRepository).save(any(Reserva.class));
        assertThat(contar(fixture.recurso().getId())).isEqualTo(1);
    }

    private Fixture criarFixture() {
        Usuario usuario = usuarioRepository.saveAndFlush(new Usuario(
                null,
                "Solicitante constraint",
                "solicitante-" + UUID.randomUUID() + "@example.com",
                "matricula-" + UUID.randomUUID()));
        Recurso recurso = recursoRepository.saveAndFlush(new Recurso(
                null, "Recurso constraint", "SALA"));
        return new Fixture(recurso, usuario);
    }

    private void inserir(Fixture fixture, String status, LocalDateTime inicio, LocalDateTime fim) {
        jdbcTemplate.update(
                """
                INSERT INTO tb_reservas
                    (id, recurso_id, data_inicio, data_fim, status, usuario_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                fixture.recurso().getId(),
                Timestamp.valueOf(inicio),
                Timestamp.valueOf(fim),
                status,
                fixture.usuario().getId());
    }

    private int contar(UUID recursoId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_reservas WHERE recurso_id = ?",
                Integer.class,
                recursoId);
    }

    private RequestPostProcessor jwtContexto(UUID subject) {
        return request -> {
            Jwt jwt = Jwt.withTokenValue("token-de-teste")
                    .header("alg", "none")
                    .subject(subject.toString())
                    .claim("profile", "SOLICITANTE")
                    .build();
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new JwtAuthenticationToken(
                    jwt, List.of(new SimpleGrantedAuthority("ROLE_SOLICITANTE"))));
            SecurityContextHolder.setContext(context);
            return request;
        };
    }

    private record Fixture(Recurso recurso, Usuario usuario) {
    }

    private record PedidoReserva(
            UUID recursoId, UUID usuarioId, LocalDateTime dataInicio, LocalDateTime dataFim) {
    }
}
