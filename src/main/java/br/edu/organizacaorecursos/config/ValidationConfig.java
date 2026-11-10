package br.edu.organizacaorecursos.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class ValidationConfig {

    @Bean(name = "defaultValidator")
    public LocalValidatorFactoryBean defaultValidator(Clock clock) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setConfigurationInitializer(configuration -> configuration.clockProvider(() -> clock));
        return validator;
    }
}