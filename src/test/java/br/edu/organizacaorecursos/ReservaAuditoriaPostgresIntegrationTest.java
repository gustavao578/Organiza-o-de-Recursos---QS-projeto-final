package br.edu.organizacaorecursos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.domain.StatusReserva;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import br.edu.organizacaorecursos.service.ReservaService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Testcontainers
@Import(ReservaAuditoriaPostgresIntegrationTest.ClockTestConfiguration.class)
class ReservaAuditoriaPostgresIntegrationTest {

    private static final Instant INSTANTE_TESTE = Instant.parse("2026-11-11T12:34:56Z");
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
    private ReservaService reservaService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ManutencaoRepository manutencaoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void limparDados() {
        jdbcTemplate.execute("DELETE FROM tb_reserva_auditorias");
        reservaRepository.deleteAll();
        manutencaoRepository.deleteAll();
        recursoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void persisteHistoricoComMudancaDeEstadoNaMesmaTransacao() {
        Cenario cenario = criarReservaPendente();

        reservaService.atualizarStatus(
                cenario.reserva().getId(), StatusReserva.APROVADA, cenario.responsavel().getId());

        assertThat(reservaRepository.findById(cenario.reserva().getId()).orElseThrow().getStatus())
                .isEqualTo(StatusReserva.APROVADA);
        var registro = jdbcTemplate.queryForMap(
                """
                SELECT reserva_id, usuario_id, instante, estado_anterior, estado_novo, motivo
                FROM tb_reserva_auditorias
                WHERE reserva_id = ?
                """,
                cenario.reserva().getId());
        assertThat(registro.get("reserva_id")).isEqualTo(cenario.reserva().getId());
        assertThat(registro.get("usuario_id")).isEqualTo(cenario.responsavel().getId());
        assertThat(((OffsetDateTime) registro.get("instante")).toInstant()).isEqualTo(INSTANTE_TESTE);
        assertThat(registro.get("estado_anterior")).isEqualTo("PENDENTE");
        assertThat(registro.get("estado_novo")).isEqualTo("APROVADA");
        assertThat(registro.get("motivo")).isNotNull().isNotEqualTo("");
    }

    @Test
    void rollbackDaMudancaDeEstadoTambemRemoveAuditoria() {
        Cenario cenario = criarReservaPendente();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
            reservaService.atualizarStatus(
                    cenario.reserva().getId(), StatusReserva.APROVADA, cenario.responsavel().getId());
            throw new IllegalStateException("forçar rollback");
        }));

        assertThat(reservaRepository.findById(cenario.reserva().getId()).orElseThrow().getStatus())
                .isEqualTo(StatusReserva.PENDENTE);
        Integer registros = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_reserva_auditorias WHERE reserva_id = ?",
                Integer.class,
                cenario.reserva().getId());
        assertThat(registros).isZero();
    }

    private Cenario criarReservaPendente() {
        Usuario responsavel = new Usuario(
                null, "Responsável", "responsavel-auditoria@example.com", "matricula-responsavel-auditoria");
        responsavel.setPerfil(Perfil.RESPONSAVEL);
        responsavel = usuarioRepository.save(responsavel);

        Usuario solicitante = usuarioRepository.save(new Usuario(
                null, "Solicitante", "solicitante-auditoria@example.com", "matricula-solicitante-auditoria"));
        Recurso recurso = recursoRepository.save(new Recurso(
                null, "Sala Auditada", "SALA", true, responsavel.getId()));
        Reserva reserva = reservaRepository.save(new Reserva(
                null,
                recurso,
                INICIO,
                INICIO.plusHours(1),
                StatusReserva.PENDENTE,
                solicitante));
        return new Cenario(reserva, responsavel);
    }

    private record Cenario(Reserva reserva, Usuario responsavel) {
    }

    @TestConfiguration
    static class ClockTestConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(INSTANTE_TESTE, ZoneOffset.UTC);
        }
    }
}
