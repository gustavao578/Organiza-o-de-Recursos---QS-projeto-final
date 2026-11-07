package br.edu.organizacaorecursos.repository;

import br.edu.organizacaorecursos.domain.Recurso;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecursoRepository extends JpaRepository<Recurso, UUID> {}
