package br.edu.organizacaorecursos.service;

import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.dto.RelatorioUtilizacaoResponseDTO;
import br.edu.organizacaorecursos.exception.RegraNegocioException;
import br.edu.organizacaorecursos.repository.ConflitoReservaRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RelatorioUtilizacaoService {

    private final ReservaRepository reservaRepository;
    private final ConflitoReservaRepository conflitoReservaRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public RelatorioUtilizacaoResponseDTO gerar(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null || !fim.isAfter(inicio)) {
            throw new RegraNegocioException("O fim do período deve ser posterior ao início.");
        }

        Map<UUID, Acumulador> porRecurso = new TreeMap<>();
        Map<UUID, Acumulador> porUsuario = new TreeMap<>();

        for (Reserva reserva : reservaRepository.buscarReservasSobrepondoPeriodo(inicio, fim)) {
            LocalDateTime inicioInterseccao = reserva.getDataInicio().isAfter(inicio)
                    ? reserva.getDataInicio()
                    : inicio;
            LocalDateTime fimInterseccao = reserva.getDataFim().isBefore(fim)
                    ? reserva.getDataFim()
                    : fim;
            Duration duracao = Duration.between(
                    inicioInterseccao.atZone(clock.getZone()).toInstant(),
                    fimInterseccao.atZone(clock.getZone()).toInstant());

            somar(porRecurso, reserva.getRecurso().getId(), reserva.getRecurso().getNome(), duracao);
            somar(porUsuario, reserva.getUsuario().getId(), reserva.getUsuario().getNome(), duracao);
        }

        long conflitosEvitados = conflitoReservaRepository
                .countByInstanteTentativaGreaterThanEqualAndInstanteTentativaLessThan(inicio, fim);

        return new RelatorioUtilizacaoResponseDTO(
                porRecurso.values().stream()
                        .map(acumulador -> new RelatorioUtilizacaoResponseDTO.PorRecurso(
                                acumulador.id(), acumulador.nome(), horas(acumulador.duracao())))
                        .toList(),
                porUsuario.values().stream()
                        .map(acumulador -> new RelatorioUtilizacaoResponseDTO.PorUsuario(
                                acumulador.id(), acumulador.nome(), horas(acumulador.duracao())))
                        .toList(),
                conflitosEvitados);
    }

    private void somar(
            Map<UUID, Acumulador> acumuladores, UUID id, String nome, Duration duracao) {
        acumuladores.compute(id, (chave, atual) -> atual == null
                ? new Acumulador(id, nome, duracao)
                : atual.comDuracao(duracao));
    }

    private static double horas(Duration duracao) {
        return duracao.getSeconds() / 3600.0 + duracao.getNano() / 3_600_000_000_000.0;
    }

    private record Acumulador(UUID id, String nome, Duration duracao) {

        private Acumulador comDuracao(Duration adicional) {
            return new Acumulador(id, nome, duracao.plus(adicional));
        }
    }
}
