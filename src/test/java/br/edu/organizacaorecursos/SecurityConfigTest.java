package br.edu.organizacaorecursos;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.organizacaorecursos.config.SecurityConfig;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey jwtSigningKey;

    @Test
    void jwtDecoderRejeitaSegredoComMenosDe256Bits() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SecurityConfig().jwtSigningKey("curto"));

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("32 bytes"));
    }

    @Test
    void geraChaveJwtAleatoriaDe256BitsQuandoNaoHaSegredoConfigurado() {
        SecretKey first = new SecurityConfig().jwtSigningKey("");
        SecretKey second = new SecurityConfig().jwtSigningKey(null);

        org.assertj.core.api.Assertions.assertThat(first.getEncoded()).hasSize(32);
        org.assertj.core.api.Assertions.assertThat(second.getEncoded()).hasSize(32);
        org.assertj.core.api.Assertions.assertThat(first.getEncoded()).isNotEqualTo(second.getEncoded());
    }

    @Test
    void usaChaveConfiguradaQuandoVariavelDeAmbienteEstaDisponivel() {
        String configuredSecret = "x".repeat(32);

        org.assertj.core.api.Assertions.assertThat(
                        new SecurityConfig().jwtSigningKey(configuredSecret).getEncoded())
                .isEqualTo(configuredSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void jwtDecoderRejeitaTokenExpirado() throws Exception {
        Instant now = Instant.parse("2026-11-12T12:00:00Z");
        SignedJWT expired = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                new JWTClaimsSet.Builder()
                        .subject("usuario")
                        .issueTime(Date.from(now.minusSeconds(120)))
                        .expirationTime(Date.from(now.minusSeconds(61)))
                        .build());
        expired.sign(new MACSigner(jwtSigningKey.getEncoded()));
        JwtDecoder decoder = new SecurityConfig().jwtDecoder(
                jwtSigningKey, Clock.fixed(now, ZoneOffset.UTC));

        assertThrows(JwtException.class, () -> decoder.decode(expired.serialize()));
    }

    @Test
    void apiRequerBearerJwtSemCriarSessaoHttp() throws Exception {
        var result = mockMvc.perform(get("/reservas"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertNull(result.getRequest().getSession(false));
    }

    @Test
    void loginPorIdentificadorNaoFicaDisponivelForaDoPerfilDemo() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identificador":"admin@example.com"}
                                """))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/", "/index.html", "/styles.css",
        "/js/api.js", "/js/auth.js", "/js/app.js", "/js/recursos.js", "/js/reservas.js",
        "/js/usuarios.js", "/js/manutencoes.js", "/js/relatorios.js"
    })
    void arquivosDaSpaSaoPublicosSemLiberarRotasDaApi(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/recursos", "/usuarios", "/manutencoes", "/h2-console"})
    void rotasPrivadasContinuamExigindoAutenticacao(String path) throws Exception {
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