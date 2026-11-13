package br.edu.organizacaorecursos.config;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final RequestMatcher API_REQUEST = new OrRequestMatcher(List.of(
            new AntPathRequestMatcher("/reservas/**"),
            new AntPathRequestMatcher("/recursos/**"),
            new AntPathRequestMatcher("/usuarios/**"),
            new AntPathRequestMatcher("/manutencoes/**"),
            new AntPathRequestMatcher("/relatorios/**"),
            new AntPathRequestMatcher("/auth/login")));

    // API clients authenticate only with Bearer tokens, so API routes do not use cookie-based CSRF tokens.
    @SuppressWarnings("java:S4502")
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers(API_REQUEST))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/", "/index.html", "/styles.css",
                                "/js/api.js", "/js/auth.js", "/js/app.js",
                                "/js/recursos.js", "/js/reservas.js",
                                "/js/usuarios.js", "/js/manutencoes.js", "/js/relatorios.js")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        if (origins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalArgumentException("As origens CORS devem ser explícitas, sem curingas.");
        }
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecretKey jwtSigningKey(@Value("${security.jwt.secret:}") String secret) {
        return createSigningKey(secret);
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator();
        timestamps.setClock(clock);
        decoder.setJwtValidator(timestamps);
        return decoder;
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(jwtSigningKey)
                .algorithm(JWSAlgorithm.HS256)
                .build();
        return new NimbusJwtEncoder(
                new ImmutableJWKSet<SecurityContext>(new JWKSet(jwk)));
    }

    static SecretKey createSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            try {
                KeyGenerator generator = KeyGenerator.getInstance("HmacSHA256");
                generator.init(256);
                return generator.generateKey();
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException("Não foi possível gerar uma chave JWT efêmera.", exception);
            }
        }

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException(
                    "security.jwt.secret deve ter pelo menos 32 bytes para HS256.");
        }
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String profile = jwt.getClaimAsString("profile");
            String normalized = switch (profile == null ? "" : profile) {
                case "ADMIN" -> "ADMINISTRADOR";
                case "COMUM" -> "SOLICITANTE";
                case "SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR" -> profile;
                default -> null;
            };
            return normalized == null
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + normalized));
        });
        return converter;
    }
}