package br.edu.organizacaorecursos;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiRequerBearerJwtSemCriarSessaoHttp() throws Exception {
        var result = mockMvc.perform(get("/reservas"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertNull(result.getRequest().getSession(false));
    }

    @Test
    void apiSemTokenBearerExigeProtecaoCsrf() throws Exception {
        mockMvc.perform(post("/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/reservas",
        "/api/reservas",
        "/recursos",
        "/usuarios",
        "/manutencoes"
    })
    void apiComBearerIgnoraCsrfMasAindaExigeJwtValido(String path) throws Exception {
        MockHttpServletRequestBuilder request = post(path)
                .header("Authorization", "Bearer invalido")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }
}