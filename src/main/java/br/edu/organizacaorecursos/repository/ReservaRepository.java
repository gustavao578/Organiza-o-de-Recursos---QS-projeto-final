package br.edu.organizacaorecursos.repository;

import br.edu.organizacaorecursos.domain.Reserva;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {}
