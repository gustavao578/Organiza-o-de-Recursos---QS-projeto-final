package br.edu.organizacaorecursos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI organizacaoRecursosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Organização de Recursos")
                        .version("v1")
                        .description("API REST para cadastro e reserva de recursos."));
    }
}
