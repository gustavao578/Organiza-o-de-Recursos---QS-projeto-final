package br.edu.organizacaorecursos.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@Import(CucumberTestClockConfiguration.class)
public class CucumberSpringConfiguration {
}