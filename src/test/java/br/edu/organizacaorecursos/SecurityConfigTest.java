package br.edu.organizacaorecursos;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import br.edu.organizacaorecursos.config.SecurityConfig;
import java.time.Clock;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void jwtDecoderRejeitaSegredoComMenosDe256Bits() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SecurityConfig().jwtDecoder("curto", Clock.systemUTC()));

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("32 bytes"));
    }

    @Test
    void apiRequerBearerJwtSemCriarSessaoHttp() throws Exception {
        var result = mockMvc.perform(get("/reservas"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertNull(result.getRequest().getSession(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/", "/index.html", "/styles.css",
        "/js/api.js", "/js/auth.js", "/js/app.js", "/js/recursos.js", "/js/reservas.js"
    })
    void arquivosDaSpaSaoPublicosSemLiberarRotasDaApi(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/recursos", "/usuarios", "/manutencoes"})
    void rotasApiContinuamExigindoAutenticacaoMesmoComSpaPublica(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiSemTokenBearerRespondeNaoAutorizadoSemDependerDeCsrf() throws Exception {
        mockMvc.perform(post("/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
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