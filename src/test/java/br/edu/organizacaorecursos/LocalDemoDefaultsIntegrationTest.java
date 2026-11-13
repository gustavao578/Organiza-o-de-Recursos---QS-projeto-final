package br.edu.organizacaorecursos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.config.location=file:src/main/resources/",
        "spring.datasource.url=jdbc:h2:mem:local-default-test"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LocalDemoDefaultsIntegrationTest {

    @Autowired private UsuarioRepository usuarios;
    @Autowired private RecursoRepository recursos;
    @Autowired private MockMvc mockMvc;

    @Test
    void perfilLocalPadraoInicializaDadosEPermiteLogin() throws Exception {
        assertThat(usuarios.count()).isEqualTo(3);
        assertThat(recursos.count()).isEqualTo(2);
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"ADM001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.perfil").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
