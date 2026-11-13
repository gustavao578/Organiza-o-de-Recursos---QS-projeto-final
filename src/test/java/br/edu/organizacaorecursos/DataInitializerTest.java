package br.edu.organizacaorecursos;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.organizacaorecursos.config.DataInitializer;
import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = {
        "app.seed-demo-data=true",
        "spring.datasource.url=jdbc:h2:mem:seedtest"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataInitializerTest {

    @Autowired private DataInitializer initializer;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private RecursoRepository recursos;

    @BeforeEach
    void limpar() {
        recursos.deleteAll();
        usuarios.deleteAll();
    }

    @Test
    void criaDadosDeDemonstracaoUmaUnicaVez() {
        initializer.run();
        initializer.run();

        assertThat(usuarios.count()).isEqualTo(3);
        assertThat(recursos.count()).isEqualTo(2);
        var responsavel = usuarios.findByMatricula("RESP001").orElseThrow();
        assertThat(responsavel.getPerfil()).isEqualTo(Perfil.RESPONSAVEL);
        assertThat(recursos.findAll()).anySatisfy(recurso -> {
            assertThat(recurso.isRestrito()).isTrue();
            assertThat(recurso.getResponsavelId()).isEqualTo(responsavel.getId());
        });
        assertThat(recursos.findAll()).anySatisfy(recurso -> assertThat(recurso.isRestrito()).isFalse());
    }

    @Test
    void naoCriaRecursosOuUsuariosQuandoResponsavelNaoExiste() {
        usuarios.save(new Usuario(null, "Existente", "existente@example.com", "EX001", Perfil.SOLICITANTE));
        initializer.run();

        assertThat(usuarios.count()).isEqualTo(1);
        assertThat(recursos.count()).isZero();
    }
}
