package br.edu.organizacaorecursos.repository;

import br.edu.organizacaorecursos.domain.ConflitoReserva;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConflitoReservaRepository extends JpaRepository<ConflitoReserva, UUID> {

    long countByInstanteTentativaGreaterThanEqualAndInstanteTentativaLessThan(
            LocalDateTime inicio, LocalDateTime fim);
}
