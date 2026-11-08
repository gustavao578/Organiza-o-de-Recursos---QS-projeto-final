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
import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.dto.ReservaRequestDTO;
import br.edu.organizacaorecursos.dto.ReservaResponseDTO;
import br.edu.organizacaorecursos.exception.RegraNegocioException;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private ReservaService reservaService;

    private ReservaRequestDTO request;
    private Recurso recurso;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
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
    void rejeitaReservaQuandoExisteChoqueDeHorario() {
        prepararRecursoEUsuario();
        when(manutencaoRepository.isRecursoEmManutencao(
                recurso.getId(), request.getDataInicio(), request.getDataFim()))
                .thenReturn(false);
        when(reservaRepository.existeConflitoDeHorario(
                recurso.getId(), request.getDataInicio(), request.getDataFim()))
                .thenReturn(true);

        RegraNegocioException exception = assertThrows(
                RegraNegocioException.class,
                () -> reservaService.criarReserva(request));

        assertEquals("O recurso já está reservado neste período.", exception.getMessage());
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void criaReservaQuandoDadosSaoValidosERecursoEstaDisponivel() {
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

        assertNotNull(response.getId());
        assertEquals(recurso.getId(), response.getRecursoId());
        assertEquals(recurso.getNome(), response.getNomeRecurso());
        assertEquals(usuario.getId(), response.getUsuarioId());
        assertEquals(usuario.getNome(), response.getNomeUsuario());
        assertEquals(request.getDataInicio(), response.getDataInicio());
        assertEquals(request.getDataFim(), response.getDataFim());
        verify(reservaRepository).save(any(Reserva.class));
    }

    private void prepararRecursoEUsuario() {
        when(recursoRepository.buscarPorIdComBloqueio(request.getRecursoId()))
                .thenReturn(Optional.of(recurso));
        when(usuarioRepository.findById(request.getUsuarioId()))
                .thenReturn(Optional.of(usuario));
    }
}
