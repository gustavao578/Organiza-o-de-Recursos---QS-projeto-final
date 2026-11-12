package br.edu.organizacaorecursos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.repository.ConflitoReservaRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RelatorioUtilizacaoServiceTest {

    @Test
    void calculaDuracaoRealDeIntersecaoDuranteMudancaDeHorario() {
        ZoneId zone = ZoneId.of("America/New_York");
        Clock clock = Clock.fixed(Instant.parse("2026-03-08T12:00:00Z"), zone);
        ReservaRepository reservas = mock(ReservaRepository.class);
        ConflitoReservaRepository conflitos = mock(ConflitoReservaRepository.class);
        UUID recursoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Reserva reserva = new Reserva(
                UUID.randomUUID(),
                new Recurso(recursoId, "Sala", "SALA"),
                LocalDateTime.of(2026, 3, 8, 1, 0),
                LocalDateTime.of(2026, 3, 8, 4, 0),
                new Usuario(usuarioId, "Usuário", "user@example.com", "MAT001"));
        when(reservas.buscarReservasSobrepondoPeriodo(
                reserva.getDataInicio(), reserva.getDataFim()))
                .thenReturn(List.of(reserva));
        when(conflitos.countByInstanteTentativaGreaterThanEqualAndInstanteTentativaLessThan(
                reserva.getDataInicio(), reserva.getDataFim()))
                .thenReturn(0L);

        var report = new RelatorioUtilizacaoService(reservas, conflitos, clock)
                .gerar(reserva.getDataInicio(), reserva.getDataFim());

        assertThat(report.porRecurso()).singleElement()
                .extracting("horas")
                .isEqualTo(2.0);
    }
}
