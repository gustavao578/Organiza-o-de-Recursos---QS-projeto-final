package br.edu.organizacaorecursos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.dto.ReservaRequestDTO;
import br.edu.organizacaorecursos.exception.RegraNegocioException;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.service.ReservaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ReservaApiIntegrationTest {

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
    void mantemSolicitacaoRestritaPendenteAteAprovacao() throws Exception {
                UUID recursoId = criarRecursoRestrito(null);

        enviarReserva(reserva(recursoId, inicioFuturo(), "solicitante-restrito"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SOLICITADA"));
    }

    @Test
    void patchStatusExigeAdminPersistidoEImpedeAprovacoesSobrepostas() throws Exception {
        Usuario admin = criarUsuario("responsavel-patch");
        UUID recursoId = criarRecursoRestrito(admin.getId());
        LocalDateTime inicio = inicioFuturo();
        enviarReserva(reserva(recursoId, inicio, "solicitante-patch-1"))
                .andExpect(status().isCreated());
        enviarReserva(reserva(recursoId, inicio, "solicitante-patch-2"))
                .andExpect(status().isCreated());
        UUID primeiraReservaId = reservaRepository.findAll().get(0).getId();
        UUID segundaReservaId = reservaRepository.findAll().get(1).getId();

        atualizarStatusReserva(primeiraReservaId, admin.getId(), "APROVADA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"));
        atualizarStatusReserva(segundaReservaId, admin.getId(), "APROVADA")
                .andExpect(status().isBadRequest());

        Usuario usuarioComum = criarUsuario("usuario-patch-comum");
        UUID terceiraReservaId = reservaRepository.save(new br.edu.organizacaorecursos.domain.Reserva(
                null, recursoRepository.findById(recursoId).orElseThrow(), inicio.plusHours(2),
                inicio.plusHours(3), usuarioComum)).getId();
        atualizarStatusReserva(terceiraReservaId, usuarioComum.getId(), "APROVADA")
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/reservas/{id}/status", terceiraReservaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"APROVADA"}
                                """))
                .andExpect(status().isForbidden());

        enviarReserva(reserva(recursoId, inicio.plusHours(4), "solicitante-status-invalido"))
                .andExpect(status().isCreated());
        UUID quartaReservaId = reservaRepository.findAll().stream()
                .filter(reservaSalva -> reservaSalva.getDataInicio().equals(inicio.plusHours(4)))
                .findFirst().orElseThrow().getId();
        atualizarStatusReserva(quartaReservaId, admin.getId(), "SOLICITADA")
                .andExpect(status().isBadRequest());
    }

    @Test
    void atualizaReservaPendenteSomentePeloSolicitante() throws Exception {
        Usuario solicitante = criarUsuario("solicitante-put");
        Recurso recurso = recursoRepository.save(new Recurso(null, "Sala PUT", "SALA", true, null));
        LocalDateTime inicio = inicioFuturo();
        ReservaRequestDTO criacao = new ReservaRequestDTO();
        criacao.setRecursoId(recurso.getId());
        criacao.setUsuarioId(solicitante.getId());
        criacao.setDataInicio(inicio);
        criacao.setDataFim(inicio.plusHours(1));
        enviarReserva(criacao).andExpect(status().isCreated());
        UUID reservaId = reservaRepository.findAll().get(0).getId();

        ReservaRequestDTO atualizacao = new ReservaRequestDTO();
        atualizacao.setRecursoId(recurso.getId());
        atualizacao.setUsuarioId(solicitante.getId());
        atualizacao.setDataInicio(inicio.plusHours(2));
        atualizacao.setDataFim(inicio.plusHours(3));
        putReserva(reservaId, solicitante.getId(), "COMUM", atualizacao)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLICITADA"))
                .andExpect(jsonPath("$.dataInicio").value(inicio.plusHours(2) + ":00"));

        Usuario outroUsuario = criarUsuario("outro-put");
        putReserva(reservaId, outroUsuario.getId(), "COMUM", atualizacao)
                .andExpect(status().isForbidden());
    }

    @Test
    void aprovaSolicitacaoRestritaQuandoResponsavelExecuta() throws Exception {
        Usuario responsavel = criarUsuario("responsavel-aprovador");
        UUID recursoId = criarRecursoRestrito(responsavel.getId());

        enviarReserva(reserva(recursoId, inicioFuturo(), "solicitante-aprovacao"))
                .andExpect(status().isCreated());
        UUID reservaId = reservaRepository.findAll().get(0).getId();

        aprovarReserva(reservaId, responsavel.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"));
    }

    @Test
    void rejeitaAutoAprovacaoEPreservaSolicitacao() throws Exception {
        Usuario solicitante = criarUsuario("solicitante-autorizado");
                solicitante.setPerfil(Perfil.ADMIN);
                usuarioRepository.save(solicitante);
        UUID recursoId = criarRecursoRestrito(solicitante.getId());
        LocalDateTime inicio = inicioFuturo();
        ReservaRequestDTO request = new ReservaRequestDTO();
        request.setRecursoId(recursoId);
        request.setDataInicio(inicio);
        request.setDataFim(inicio.plusHours(1));
        request.setUsuarioId(solicitante.getId());
        enviarReserva(request).andExpect(status().isCreated());
        UUID reservaId = reservaRepository.findAll().get(0).getId();

        aprovarReserva(reservaId, solicitante.getId())
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/reservas/{id}", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLICITADA"));
    }

    @Test
    void rejeitaAprovacaoPorUsuarioQueNaoEResponsavel() throws Exception {
        Usuario responsavel = criarUsuario("responsavel-registrado");
        Usuario outroUsuario = criarUsuario("outro-usuario");
        outroUsuario.setPerfil(Perfil.ADMIN);
        usuarioRepository.save(outroUsuario);
        UUID recursoId = criarRecursoRestrito(responsavel.getId());
        enviarReserva(reserva(recursoId, inicioFuturo(), "solicitante-outro"))
                .andExpect(status().isCreated());
        UUID reservaId = reservaRepository.findAll().get(0).getId();

        aprovarReserva(reservaId, outroUsuario.getId())
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/reservas/{id}", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLICITADA"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
                        "termina no início da manutenção, TERMINA_NO_INICIO, 201, true",
                        "começa no fim da manutenção, COMECA_NO_FIM, 201, true",
                        "cruza um segundo da manutenção, CRUZA_UM_SEGUNDO, 400, false"
    })
    void aplicaFronteirasDoIntervaloDeManutencao(
            String caso,
                        String fronteira,
            int statusEsperado,
            boolean devePersistir) throws Exception {
        Recurso recurso = criarRecurso();
                LocalDateTime inicioManutencao = LocalDateTime.now(clock).plusDays(2).withHour(10)
                                .withMinute(0).withSecond(0).withNano(0);
                LocalDateTime fimManutencao = inicioManutencao.plusHours(1);
        manutencaoRepository.save(new br.edu.organizacaorecursos.domain.Manutencao(
                                null, recurso, inicioManutencao, fimManutencao, "Reparo"));
        ReservaRequestDTO request = new ReservaRequestDTO();
        request.setRecursoId(recurso.getId());
                switch (fronteira) {
                        case "TERMINA_NO_INICIO" -> {
                                request.setDataInicio(inicioManutencao.minusHours(1));
                                request.setDataFim(inicioManutencao);
                        }
                        case "COMECA_NO_FIM" -> {
                                request.setDataInicio(fimManutencao);
                                request.setDataFim(fimManutencao.plusHours(1));
                        }
                        case "CRUZA_UM_SEGUNDO" -> {
                                request.setDataInicio(inicioManutencao.minusHours(1));
                                request.setDataFim(inicioManutencao.plusSeconds(1));
                        }
                        default -> throw new IllegalArgumentException("Fronteira não suportada.");
                }
        request.setUsuarioId(criarUsuario("usuario-" + caso.replace(' ', '-')).getId());

        enviarReserva(request)
                .andExpect(status().is(statusEsperado));

        org.assertj.core.api.Assertions.assertThat(reservaRepository.count())
                .isEqualTo(devePersistir ? 1 : 0);
    }

    @Test
    void iniciaReservaAprovadaETransicionaParaEmUso() throws Exception {
        Usuario responsavel = criarUsuario("responsavel-inicio");
        UUID recursoId = criarRecursoRestrito(responsavel.getId());
        enviarReserva(reserva(recursoId, inicioFuturo(), "solicitante-inicio"))
                .andExpect(status().isCreated());
        UUID reservaId = reservaRepository.findAll().get(0).getId();
        aprovarReserva(reservaId, responsavel.getId())
                .andExpect(status().isOk());

        mockMvc.perform(post("/reservas/{id}/iniciar", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_USO"));
    }

    @Test
    void concluiReservaEmUsoETransicionaParaConcluida() throws Exception {
        Usuario responsavel = criarUsuario("responsavel-conclusao");
        UUID recursoId = criarRecursoRestrito(responsavel.getId());
        enviarReserva(reserva(recursoId, inicioFuturo(), "solicitante-conclusao"))
                .andExpect(status().isCreated());
        UUID reservaId = reservaRepository.findAll().get(0).getId();
        aprovarReserva(reservaId, responsavel.getId()).andExpect(status().isOk());
        mockMvc.perform(post("/reservas/{id}/iniciar", reservaId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/reservas/{id}/concluir", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUÍDA"));
    }

    @Test
    void rejeitaSaltoDeSolicitadaParaEmUsoEPreservaEstado() throws Exception {
        UUID recursoId = criarRecursoRestrito(criarUsuario("responsavel-salto").getId());
        enviarReserva(reserva(recursoId, inicioFuturo(), "solicitante-salto"))
                .andExpect(status().isCreated());
        UUID reservaId = reservaRepository.findAll().get(0).getId();

        mockMvc.perform(post("/reservas/{id}/iniciar", reservaId))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/reservas/{id}", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLICITADA"));
    }

        @ParameterizedTest(name = "{0} por {1} a partir de {2}")
        @CsvSource({
                        "REJEITADA, responsavel, SOLICITADA",
                        "CANCELADA, solicitante, SOLICITADA",
                        "NÃO_COMPARECEU, responsavel, APROVADA"
        })
        void aplicaTransicoesAlternativasPermitidas(
                        String destino, String papelAtor, String origem) throws Exception {
                Usuario responsavel = criarUsuario("responsavel-alternativo-" + destino);
                Usuario solicitante = criarUsuario("solicitante-alternativo-" + destino);
                UUID recursoId = criarRecursoRestrito(responsavel.getId());
                ReservaRequestDTO request = new ReservaRequestDTO();
                request.setRecursoId(recursoId);
                request.setDataInicio(inicioFuturo());
                request.setDataFim(request.getDataInicio().plusHours(1));
                request.setUsuarioId(solicitante.getId());
                enviarReserva(request).andExpect(status().isCreated());
                UUID reservaId = reservaRepository.findAll().get(0).getId();
                if ("APROVADA".equals(origem)) {
                        aprovarReserva(reservaId, responsavel.getId()).andExpect(status().isOk());
                }

                UUID atorId = "solicitante".equals(papelAtor) ? solicitante.getId() : responsavel.getId();
                String acao = switch (destino) {
                        case "REJEITADA" -> "rejeitar";
                        case "CANCELADA" -> "cancelar";
                        case "NÃO_COMPARECEU" -> "nao-compareceu";
                        default -> throw new IllegalArgumentException("Estado alternativo não suportado.");
                };
                String perfil = switch (destino) {
                        case "REJEITADA" -> "ADMIN";
                        case "CANCELADA" -> "SOLICITANTE";
                        case "NÃO_COMPARECEU" -> "RESPONSAVEL";
                        default -> throw new IllegalArgumentException("Estado alternativo não suportado.");
                };
                postComo(reservaId, acao, atorId, perfil)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value(destino));
        }

                    @Test
                    void rejeitaAprovacaoDeReservaJaRejeitadaEPreservaEstado() throws Exception {
                        Usuario responsavel = criarUsuario("responsavel-reaprovacao");
                        UUID recursoId = criarRecursoRestrito(responsavel.getId());
                        enviarReserva(reserva(recursoId, inicioFuturo(), "solicitante-reaprovacao"))
                                .andExpect(status().isCreated());
                        UUID reservaId = reservaRepository.findAll().get(0).getId();
                        postComo(reservaId, "rejeitar", responsavel.getId(), "ADMIN")
                                .andExpect(status().isOk());

                        aprovarReserva(reservaId, responsavel.getId())
                                .andExpect(status().isBadRequest());

                        mockMvc.perform(get("/reservas/{id}", reservaId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("REJEITADA"));
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
    void atualizaConsultaERemoveRecursoPelosEndpointsRest() throws Exception {
        Recurso recurso = criarRecurso();
        String atualizacao = """
                {"nome":"Sala Atualizada","tipo":"SALA","restrito":true}
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/recursos/{id}", recurso.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(atualizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Sala Atualizada"))
                .andExpect(jsonPath("$.restrito").value(true));

        mockMvc.perform(get("/recursos/{id}", recurso.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restrito").value(true));

        mockMvc.perform(delete("/recursos/{id}", recurso.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/recursos/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
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
                ReservaRequestDTO request = criarReserva();
        UUID reservaId = reservaRepository.findAll().get(0).getId();

                try {
                        mockMvc.perform(delete("/reservas/{id}", reservaId)
                                                        .with(jwtContexto(request.getUsuarioId(), "COMUM")))
                                        .andExpect(status().isNoContent());
                } finally {
                        SecurityContextHolder.clearContext();
                }

        org.assertj.core.api.Assertions.assertThat(reservaRepository.existsById(reservaId)).isFalse();
    }

    @Test
    void retornaNotFoundAoExcluirReservaInexistente() throws Exception {
                try {
                        mockMvc.perform(delete("/reservas/{id}", UUID.randomUUID())
                                                        .with(jwtContexto(UUID.randomUUID(), "COMUM")))
                                        .andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.erro").value("Reserva não encontrada para exclusão."));
                } finally {
                        SecurityContextHolder.clearContext();
                }
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
                                {"nome":"Usuário API","email":"usuario.api@example.com","matricula":"api-001","perfil":"ADMIN"}
                """;

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Usuário API"))
                .andExpect(jsonPath("$.perfil").value("COMUM"));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("usuario.api@example.com"));

        mockMvc.perform(delete("/usuarios/{id}", usuarioRepository.findAll().get(0).getId()))
                .andExpect(status().isNoContent());
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
    void rejeitaManutencaoSemRecursoNoCorpo() throws Exception {
        LocalDateTime inicio = inicioFuturo();
        String request = """
                {"dataInicio":"%s","dataFim":"%s","descricao":"Reparo"}
                """.formatted(inicio, inicio.plusHours(1));

        mockMvc.perform(post("/manutencoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.recurso").value("O recurso é obrigatório."));

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

    private org.springframework.test.web.servlet.ResultActions aprovarReserva(UUID reservaId, UUID atorId)
            throws Exception {
        try {
            return mockMvc.perform(post("/reservas/{id}/aprovar", reservaId)
                    .with(jwtContexto(atorId, "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"atorId":"%s"}
                            """.formatted(atorId)));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private org.springframework.test.web.servlet.ResultActions atualizarStatusReserva(
            UUID reservaId, UUID atorId, String novoStatus) throws Exception {
                try {
                        return mockMvc.perform(patch("/reservas/{id}/status", reservaId)
                                        .with(jwtContexto(atorId, "ADMIN"))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"status":"%s"}
                                                        """.formatted(novoStatus)));
                } finally {
                        SecurityContextHolder.clearContext();
                }
    }

        private org.springframework.test.web.servlet.ResultActions putReserva(
                        UUID reservaId, UUID atorId, String perfil, ReservaRequestDTO request) throws Exception {
                try {
                        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                        .put("/reservas/{id}", reservaId)
                                        .with(jwtContexto(atorId, perfil))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));
                } finally {
                        SecurityContextHolder.clearContext();
                }
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

    private Recurso criarRecurso() {
        return recursoRepository.save(new Recurso(null, "Sala 1", "SALA"));
    }

        private org.springframework.test.web.servlet.ResultActions postComo(
                        UUID reservaId, String acao, UUID atorId, String perfil) throws Exception {
                try {
                        return mockMvc.perform(post("/reservas/{id}/{acao}", reservaId, acao)
                                        .with(jwtContexto(atorId, perfil))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"atorId":"%s"}
                                                        """.formatted(atorId)));
                } finally {
                        SecurityContextHolder.clearContext();
                }
        }

    private UUID criarRecursoRestrito(UUID responsavelId) throws Exception {
        String responsavelJson = responsavelId == null
                ? ""
                : ",\"responsavelId\":\"" + responsavelId + "\"";
        String recursoJson = """
                {"nome":"Sala Restrita","tipo":"SALA","restrito":true%s}
                """.formatted(responsavelJson);
        String recursoResponse = mockMvc.perform(post("/recursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recursoJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(recursoResponse).get("id").asText());
    }

    private Usuario criarUsuario(String nome) {
                Usuario usuario = new Usuario(
                null,
                nome,
                nome + "@example.com",
                                "matricula-" + nome);
                if (nome.startsWith("responsavel")) {
                        usuario.setPerfil(Perfil.ADMIN);
                }
                return usuarioRepository.save(usuario);
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
                return LocalDateTime.now(clock).plusDays(2).withSecond(0).withNano(0);
    }
}
