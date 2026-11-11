package br.edu.organizacaorecursos.service;

import br.edu.organizacaorecursos.domain.ConflitoReserva;
import br.edu.organizacaorecursos.repository.ConflitoReservaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConflitoReservaService {

    private final ConflitoReservaRepository conflitoReservaRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarRecusaPorSobreposicao() {
        conflitoReservaRepository.save(new ConflitoReserva(LocalDateTime.now(clock)));
    }
}
