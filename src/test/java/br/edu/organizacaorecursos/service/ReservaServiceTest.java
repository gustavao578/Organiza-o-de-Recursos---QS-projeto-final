package br.edu.organizacaorecursos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.domain.StatusReserva;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.dto.ReservaRequestDTO;
import br.edu.organizacaorecursos.dto.ReservaResponseDTO;
import br.edu.organizacaorecursos.exception.ConflitoReservaException;
import br.edu.organizacaorecursos.exception.RegraNegocioException;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.repository.ReservaAuditoriaRepository;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private RecursoRepository recursoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ManutencaoRepository manutencaoRepository;

    @Mock
    private ReservaAuditoriaRepository reservaAuditoriaRepository;

    private ReservaService reservaService;

    private ReservaRequestDTO request;
    private Recurso recurso;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        reservaService = new ReservaService(
                reservaRepository,
                recursoRepository,
                usuarioRepository,
                manutencaoRepository,
                reservaAuditoriaRepository,
                Clock.fixed(Instant.parse("2026-11-11T12:00:00Z"), java.time.ZoneOffset.UTC));
        UUID recursoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)
                .withSecond(0).withNano(0);

        recurso = new Recurso(recursoId, "Sala de Reunião A", "SALA");
        usuario = new Usuario(usuarioId, "Gustavo", "gustavo@example.com", "MAT123");
        request = new ReservaRequestDTO();
        request.setRecursoId(recursoId);
        request.setUsuarioId(usuarioId);
        request.setDataInicio(inicio);
        request.setDataFim(inicio.plusHours(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"inicio", "fim"})
    void rejeitaReservaQuandoDataObrigatoriaEstaAusente(String campo) {
        if ("inicio".equals(campo)) {
            request.setDataInicio(null);
        } else {
            request.setDataFim(null);
        }

        RegraNegocioException exception = assertThrows(
                RegraNegocioException.class,
                () -> reservaService.criarReserva(request));

        assertEquals("As datas de início e fim são obrigatórias.", exception.getMessage());
        verifyNoInteractions(
                reservaRepository, recursoRepository, usuarioRepository, manutencaoRepository);
    }

    @Test
    void rejeitaDataFimAnteriorAoInicio() {
        request.setDataFim(request.getDataInicio().minusHours(1));

        RegraNegocioException exception = assertThrows(
                RegraNegocioException.class,
                () -> reservaService.criarReserva(request));

        assertEquals(
                "A data de término deve ser posterior à data de início.",
                exception.getMessage());
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void rejeitaReservaQuandoRecursoEstaEmManutencao() {
        prepararRecursoEUsuario();
        when(manutencaoRepository.isRecursoEmManutencao(
                recurso.getId(), request.getDataInicio(), request.getDataFim()))
                .thenReturn(true);

        RegraNegocioException exception = assertThrows(
                RegraNegocioException.class,
                () -> reservaService.criarReserva(request));

        assertEquals(
                "O recurso não pode ser reservado pois estará em manutenção neste período.",
                exception.getMessage());
        verify(reservaRepository, never()).save(any(Reserva.class));
        verify(reservaRepository, never()).existeConflitoDeHorario(
                any(), any(), any());
    }

    @Test
    void rejeitaSobreposicaoMesmoQuandoRecursoRestritoGerariaStatusPendente() {
        recurso.setRestrito(true);
        prepararRecursoEUsuario();
        when(manutencaoRepository.isRecursoEmManutencao(
                recurso.getId(), request.getDataInicio(), request.getDataFim()))
                .thenReturn(false);
        when(reservaRepository.existeConflitoDeHorario(
                recurso.getId(), request.getDataInicio(), request.getDataFim()))
                .thenReturn(true);
        when(reservaRepository.buscarConflitosDeHorario(
                recurso.getId(), request.getDataInicio(), request.getDataFim()))
                .thenReturn(List.of());

        ConflitoReservaException exception = assertThrows(
                ConflitoReservaException.class,
                () -> reservaService.criarReserva(request));

        assertEquals("O recurso já está reservado neste período.", exception.getMessage());
        verify(reservaRepository, never()).save(any(Reserva.class));
        verify(reservaRepository).existeConflitoDeHorario(
                recurso.getId(), request.getDataInicio(), request.getDataFim());
    }

    @Test
    void criaReservaQuandoDadosSaoValidosERecursoEstaDisponivel() {
        prepararRecursoEUsuario();
        request.setStatus(StatusReserva.PENDENTE);
        when(manutencaoRepository.isRecursoEmManutencao(
                recurso.getId(), request.getDataInicio(), request.getDataFim()))
                .thenReturn(false);
        when(reservaRepository.existeConflitoDeHorario(
                recurso.getId(), request.getDataInicio(), request.getDataFim()))
                .thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacao -> {
            Reserva reservaSalva = invocacao.getArgument(0);
            reservaSalva.setId(UUID.randomUUID());
            return reservaSalva;
        });

        ReservaResponseDTO response = reservaService.criarReserva(request);

        assertNotNull(response.getId());
        assertEquals(recurso.getId(), response.getRecursoId());
        assertEquals(recurso.getNome(), response.getNomeRecurso());
        assertEquals(usuario.getId(), response.getUsuarioId());
        assertEquals(usuario.getNome(), response.getNomeUsuario());
        assertEquals(request.getDataInicio(), response.getDataInicio());
        assertEquals(request.getDataFim(), response.getDataFim());
        assertEquals(StatusReserva.APROVADA, response.getStatus());
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    void criaReservaRestritaComStatusPendente() {
        recurso.setRestrito(true);
        prepararRecursoEUsuario();
        when(manutencaoRepository.isRecursoEmManutencao(
                recurso.getId(), request.getDataInicio(), request.getDataFim()))
                .thenReturn(false);
        when(reservaRepository.existeConflitoDeHorario(
                recurso.getId(), request.getDataInicio(), request.getDataFim()))
                .thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacao -> {
            Reserva reservaSalva = invocacao.getArgument(0);
            reservaSalva.setId(UUID.randomUUID());
            return reservaSalva;
        });

        ReservaResponseDTO response = reservaService.criarReserva(request);

        assertEquals(StatusReserva.PENDENTE, response.getStatus());
        verify(reservaRepository).existeConflitoDeHorario(
                recurso.getId(), request.getDataInicio(), request.getDataFim());
    }

    @Test
    void aprovaReservaPendenteQuandoAtorEhResponsavelDoRecurso() {
        Reserva reserva = criarReservaPendente();
        Usuario responsavel = criarResponsavelDoRecurso();
        prepararAtualizacaoStatus(reserva, responsavel);
        when(recursoRepository.buscarPorIdComBloqueio(recurso.getId()))
                .thenReturn(Optional.of(recurso));
        when(reservaRepository.existeConflitoDeHorarioAprovado(
                recurso.getId(), reserva.getId(), reserva.getDataInicio(), reserva.getDataFim()))
                .thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ReservaResponseDTO response = reservaService.atualizarStatus(
                reserva.getId(), StatusReserva.APROVADA, responsavel.getId());

        assertEquals(StatusReserva.APROVADA, response.getStatus());
        verify(reservaRepository).save(reserva);
    }

    @Test
    void rejeitaReservaPendenteQuandoAtorEhResponsavelDoRecurso() {
        Reserva reserva = criarReservaPendente();
        Usuario responsavel = criarResponsavelDoRecurso();
        prepararAtualizacaoStatus(reserva, responsavel);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ReservaResponseDTO response = reservaService.atualizarStatus(
                reserva.getId(), StatusReserva.REJEITADA, responsavel.getId());

        assertEquals(StatusReserva.REJEITADA, response.getStatus());
        verify(reservaRepository).save(reserva);
    }

    @ParameterizedTest
    @EnumSource(value = StatusReserva.class, names = {"PENDENTE", "SOLICITADA", "APROVADA"})
    void solicitantePodeCancelarReservaEmEstadosPermitidos(StatusReserva status) {
        Reserva reserva = criarReservaPendente();
        reserva.setStatus(status);
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ReservaResponseDTO response = reservaService.cancelarReserva(reserva.getId(), usuario.getId());

        assertEquals(StatusReserva.CANCELADA, response.getStatus());
        verify(reservaRepository).save(reserva);
    }

    @ParameterizedTest
    @EnumSource(value = StatusReserva.class, names = {
        "EM_USO", "CONCLUÍDA", "REJEITADA", "CANCELADA", "NÃO_COMPARECEU"
    })
    void solicitanteNaoPodeCancelarReservaEmEstadosBloqueados(StatusReserva status) {
        Reserva reserva = criarReservaPendente();
        reserva.setStatus(status);
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        assertThrows(RegraNegocioException.class,
                () -> reservaService.cancelarReserva(reserva.getId(), usuario.getId()));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @ParameterizedTest
    @EnumSource(value = StatusReserva.class, names = {"EM_USO", "CONCLUÍDA", "NÃO_COMPARECEU"})
    void impedeExcluirReservaQueJaFoiIniciada(StatusReserva status) {
        Reserva reserva = criarReservaPendente();
        reserva.setStatus(status);
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        assertThrows(
                RegraNegocioException.class,
                () -> reservaService.deletarReserva(reserva.getId(), usuario.getId()));

        verify(reservaRepository, never()).delete(reserva);
    }

    @Test
    void naoAprovaReservaPendenteComConflitoDeHorario() {
        Reserva reserva = criarReservaPendente();
        Usuario responsavel = criarResponsavelDoRecurso();
        prepararAtualizacaoStatus(reserva, responsavel);
        when(recursoRepository.buscarPorIdComBloqueio(recurso.getId()))
                .thenReturn(Optional.of(recurso));
        when(reservaRepository.existeConflitoDeHorarioAprovado(
                recurso.getId(), reserva.getId(), reserva.getDataInicio(), reserva.getDataFim()))
                .thenReturn(true);

        assertThrows(
                br.edu.organizacaorecursos.exception.ConflitoReservaException.class,
                () -> reservaService.atualizarStatus(
                        reserva.getId(), StatusReserva.APROVADA, responsavel.getId()));

        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    private Reserva criarReservaPendente() {
        return new Reserva(
                UUID.randomUUID(),
                recurso,
                request.getDataInicio(),
                request.getDataFim(),
                StatusReserva.PENDENTE,
                usuario);
    }

    private Usuario criarResponsavelDoRecurso() {
        UUID responsavelId = UUID.randomUUID();
        recurso.setRestrito(true);
        recurso.setResponsavelId(responsavelId);
        Usuario responsavel = new Usuario(
                responsavelId, "Responsável", "responsavel@example.com", "MAT999");
        responsavel.setPerfil(Perfil.RESPONSAVEL);
        return responsavel;
    }

    private void prepararAtualizacaoStatus(Reserva reserva, Usuario responsavel) {
        when(usuarioRepository.findById(responsavel.getId())).thenReturn(Optional.of(responsavel));
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
    }

    private void prepararRecursoEUsuario() {
        when(recursoRepository.buscarPorIdComBloqueio(request.getRecursoId()))
                .thenReturn(Optional.of(recurso));
        when(usuarioRepository.findById(request.getUsuarioId()))
                .thenReturn(Optional.of(usuario));
    }
}
