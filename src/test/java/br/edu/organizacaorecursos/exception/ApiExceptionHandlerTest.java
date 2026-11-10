package br.edu.organizacaorecursos.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void converteViolacaoDeIntegridadeEmConflito() {
        var response = handler.handleIntegrityViolation(
                new DataIntegrityViolationException("duplicate value"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(
                Map.of("erro", "Os dados informados conflitam com um registro existente."),
                response.getBody());
    }

    @Test
    void informaErroGenericoQuandoValidacaoNaoContemErros() throws NoSuchMethodException {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        var response = handler.handleValidationExceptions(
                validationException(bindingResult));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("erro", "A requisição contém dados inválidos."), response.getBody());
    }

    @Test
    void usaMensagemPadraoParaErroDeValidacaoSemMensagem() throws NoSuchMethodException {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new ObjectError("request", (String) null));

        var response = handler.handleValidationExceptions(
                validationException(bindingResult));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("request", "O valor informado é inválido."), response.getBody());
    }

    private MethodArgumentNotValidException validationException(BindingResult bindingResult)
            throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("validRequest", Object.class);
        return new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);
    }

    private void validRequest(Object request) {
    }
}
