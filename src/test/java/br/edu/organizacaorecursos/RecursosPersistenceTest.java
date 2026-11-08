package br.edu.organizacaorecursos;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.domain.Manutencao;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
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

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ManutencaoRepository manutencaoRepository;

    @Test
    void persistsResourceUserMaintenanceAndReservationRelationships() {
        var recurso = recursoRepository.save(new Recurso(null, "Sala 1", "SALA"));
        var usuario = usuarioRepository.save(new Usuario(
                null, "Usuário teste", "usuario-teste@example.com", "matricula-teste"));
        var inicio = LocalDateTime.of(2026, 11, 7, 9, 0);
        var fim = inicio.plusHours(1);
        var manutencao = manutencaoRepository.save(new Manutencao(
                null, recurso, inicio, fim, "Reparo"));
        var reserva = reservaRepository.save(new Reserva(
                null,
                recurso,
                inicio.plusHours(2),
                fim.plusHours(2),
                usuario));

        var persistedReserva = reservaRepository.findById(reserva.getId()).orElseThrow();
        var persistedManutencao = manutencaoRepository.findById(manutencao.getId()).orElseThrow();

        assertThat(recurso.getId()).isNotNull();
        assertThat(usuario.getId()).isNotNull();
        assertThat(persistedReserva.getId()).isNotNull();
        assertThat(persistedReserva.getRecurso().getId()).isEqualTo(recurso.getId());
        assertThat(persistedReserva.getUsuario().getId()).isEqualTo(usuario.getId());
        assertThat(persistedManutencao.getRecurso().getId()).isEqualTo(recurso.getId());
        assertThat(manutencaoRepository.isRecursoEmManutencao(
                recurso.getId(), inicio.plusMinutes(30), fim.plusMinutes(30))).isTrue();
        assertThat(manutencaoRepository.isRecursoEmManutencao(
                recurso.getId(), fim, fim.plusHours(1))).isFalse();
    }
}
