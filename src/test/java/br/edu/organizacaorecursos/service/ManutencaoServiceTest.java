package br.edu.organizacaorecursos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.edu.organizacaorecursos.domain.Manutencao;
import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.dto.ManutencaoRequestDTO;
import br.edu.organizacaorecursos.exception.RegraNegocioException;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManutencaoServiceTest {

    @Mock private ManutencaoRepository manutencaoRepository;
    @Mock private RecursoRepository recursoRepository;
    @Mock private ReservaRepository reservaRepository;

    private ManutencaoService service;
    private Recurso resource;
    private LocalDateTime start;

    @BeforeEach
    void setUp() {
        service = new ManutencaoService(manutencaoRepository, recursoRepository, reservaRepository);
        resource = new Recurso(UUID.randomUUID(), "Sala", "SALA");
        start = LocalDateTime.of(2030, 1, 1, 10, 0);
    }

    @Test
    void criaManutencaoQuandoPeriodoEstaDisponivel() {
        ManutencaoRequestDTO request = request(resource.getId(), start, start.plusHours(1));
        when(recursoRepository.buscarPorIdComBloqueio(resource.getId())).thenReturn(Optional.of(resource));
        when(reservaRepository.existeConflitoDeHorario(
                resource.getId(), request.dataInicio(), request.dataFim())).thenReturn(false);
        when(manutencaoRepository.save(any(Manutencao.class))).thenAnswer(invocation -> {
            Manutencao saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var response = service.criar(request);

        assertEquals(resource.getId(), response.recursoId());
        assertEquals("Sala", response.nomeRecurso());
        assertEquals("Reparo", response.descricao());
    }

    @Test
    void rejeitaRecursoAusenteOuInexistente() {
        assertThrows(RegraNegocioException.class,
                () -> service.criar(request(null, start, start.plusHours(1))));

        when(recursoRepository.buscarPorIdComBloqueio(resource.getId())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> service.criar(request(resource.getId(), start, start.plusHours(1))));
    }

    @Test
    void rejeitaDatasObrigatoriasEPeriodoInvertido() {
        assertThrows(RegraNegocioException.class,
                () -> service.criar(request(resource.getId(), null, start)));

        assertThrows(RegraNegocioException.class,
                () -> service.criar(request(resource.getId(), start, start)));
        verify(reservaRepository, never()).existeConflitoDeHorario(any(), any(), any());
    }

    @Test
    void rejeitaSobreposicaoComReservaAtiva() {
        when(recursoRepository.buscarPorIdComBloqueio(resource.getId())).thenReturn(Optional.of(resource));
        when(reservaRepository.existeConflitoDeHorario(
                resource.getId(), start, start.plusHours(1))).thenReturn(true);

        assertThrows(RegraNegocioException.class,
                () -> service.criar(request(resource.getId(), start, start.plusHours(1))));
        verify(manutencaoRepository, never()).save(any(Manutencao.class));
    }

    @Test
    void listaAtualizaEExcluiManutencoes() {
        Manutencao existing = new Manutencao(
                UUID.randomUUID(), resource, start, start.plusHours(1), "Original");
        when(manutencaoRepository.filtrar(resource.getId())).thenReturn(List.of(existing));
        assertEquals("Original", service.listar(resource.getId()).get(0).descricao());

        when(manutencaoRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(recursoRepository.buscarPorIdComBloqueio(resource.getId())).thenReturn(Optional.of(resource));
        when(reservaRepository.existeConflitoDeHorario(
                resource.getId(), start.plusHours(2), start.plusHours(3))).thenReturn(false);
        when(manutencaoRepository.save(any(Manutencao.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals("Atualizada", service.atualizar(existing.getId(),
                new ManutencaoRequestDTO(
                        resource.getId(), start.plusHours(2), start.plusHours(3), "Atualizada"))
                .descricao());

        when(manutencaoRepository.existsById(existing.getId())).thenReturn(true);
        service.deletar(existing.getId());
        verify(manutencaoRepository).deleteById(existing.getId());
    }

    @Test
    void atualizacaoEExclusaoDeManutencaoInexistenteRetornamNotFound() {
        UUID missingId = UUID.randomUUID();
        when(manutencaoRepository.findById(missingId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> service.atualizar(missingId, request(resource.getId(), start, start.plusHours(1))));

        when(manutencaoRepository.existsById(missingId)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, () -> service.deletar(missingId));
    }

    private ManutencaoRequestDTO request(UUID recursoId, LocalDateTime inicio, LocalDateTime fim) {
        return new ManutencaoRequestDTO(recursoId, inicio, fim, "Reparo");
    }
}
