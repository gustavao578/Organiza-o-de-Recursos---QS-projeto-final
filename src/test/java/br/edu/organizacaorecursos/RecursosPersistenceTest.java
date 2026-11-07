package br.edu.organizacaorecursos;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RecursosPersistenceTest {

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Test
    void persistsResourceAndReservationWithGeneratedIdentifiers() {
        var recurso = recursoRepository.save(new Recurso(null, "Sala 1", "SALA"));
        var reserva = reservaRepository.save(new Reserva(
                null,
                recurso,
                LocalDateTime.of(2026, 11, 7, 9, 0),
                LocalDateTime.of(2026, 11, 7, 10, 0),
                "usuario-teste"));

        var persistedReserva = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(recurso.getId()).isNotNull();
        assertThat(persistedReserva.getId()).isNotNull();
        assertThat(persistedReserva.getRecurso().getId()).isEqualTo(recurso.getId());
        assertThat(persistedReserva.getUsuarioResponsavel()).isEqualTo("usuario-teste");
    }
}
