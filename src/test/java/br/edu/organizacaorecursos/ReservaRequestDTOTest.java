package br.edu.organizacaorecursos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.organizacaorecursos.dto.ReservaRequestDTO;
import jakarta.validation.Validation;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReservaRequestDTOTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);

    @ParameterizedTest
    @CsvSource({
        "dataInicio, -1, false",
        "dataInicio, 0, true",
        "dataInicio, 1, true",
        "dataFim, -1, false",
        "dataFim, 0, true",
        "dataFim, 1, true"
    })
    void validaDatasNaFronteiraDoRelogioInjetado(
            String campo, long deslocamentoEmSegundos, boolean esperadoValido) {
        ReservaRequestDTO request = new ReservaRequestDTO();
        LocalDateTime data = LocalDateTime.now(CLOCK).plusSeconds(deslocamentoEmSegundos);
        if ("dataInicio".equals(campo)) {
            request.setDataInicio(data);
        } else {
            request.setDataFim(data);
        }

        try (var factory = Validation.byDefaultProvider()
                .configure()
                .clockProvider(() -> CLOCK)
                .buildValidatorFactory()) {
            boolean valido = factory.getValidator().validateProperty(request, campo).isEmpty();

            assertEquals(esperadoValido, valido);
        }
    }
}