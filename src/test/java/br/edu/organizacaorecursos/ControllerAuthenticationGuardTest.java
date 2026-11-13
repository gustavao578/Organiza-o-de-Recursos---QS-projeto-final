package br.edu.organizacaorecursos;

import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.organizacaorecursos.controller.ManutencaoController;
import br.edu.organizacaorecursos.controller.RecursoController;
import br.edu.organizacaorecursos.controller.ReservaController;
import br.edu.organizacaorecursos.controller.UsuarioController;
import br.edu.organizacaorecursos.dto.ManutencaoRequestDTO;
import br.edu.organizacaorecursos.dto.RecursoRequestDTO;
import br.edu.organizacaorecursos.dto.ReservaRequestDTO;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

class ControllerAuthenticationGuardTest {

    @Test
    void manutencaoRecusaPrincipalJwtAusente() {
        var controller = new ManutencaoController(null);

        assertThrows(AccessDeniedException.class,
                () -> controller.criarManutencao(
                        new ManutencaoRequestDTO(null, null, null, null), null));
    }

    @Test
    void recursoRecusaPrincipalJwtAusente() {
        var controller = new RecursoController(null, null, null, null);

        assertThrows(AccessDeniedException.class,
                () -> controller.criarRecurso(new RecursoRequestDTO(), null));
    }

    @Test
    void usuarioRecusaPrincipalJwtAusente() {
        var controller = new UsuarioController(null, null, null);

        assertThrows(AccessDeniedException.class,
                () -> controller.deletarUsuario(UUID.randomUUID(), null));
    }

    @Test
    void reservaRecusaPrincipalJwtAusente() {
        var controller = new ReservaController(null);

        assertThrows(AccessDeniedException.class,
                () -> controller.criarReserva(new ReservaRequestDTO(), null));
    }

    @Test
    void reservaRecusaClaimDePerfilDiferenteDeSolicitante() {
        var controller = new ReservaController(null);
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .claim("profile", "ADMINISTRADOR")
                .build();

        assertThrows(AccessDeniedException.class,
                () -> controller.criarReserva(new ReservaRequestDTO(), jwt));
    }
}
