package br.edu.organizacaorecursos.repository;

import br.edu.organizacaorecursos.domain.Usuario;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
}
