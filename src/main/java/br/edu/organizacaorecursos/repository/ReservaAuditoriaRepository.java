package br.edu.organizacaorecursos.repository;

import br.edu.organizacaorecursos.domain.ReservaAuditoria;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaAuditoriaRepository extends JpaRepository<ReservaAuditoria, UUID> {

    List<ReservaAuditoria> findByReservaIdOrderByInstanteAscIdAsc(UUID reservaId);

    boolean existsByReservaId(UUID reservaId);
}
