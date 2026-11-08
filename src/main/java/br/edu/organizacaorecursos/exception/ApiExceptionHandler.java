package br.edu.organizacaorecursos.exception;

import jakarta.persistence.EntityNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(
            EntityNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", exception.getMessage()));
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, String>> handleRegraNegocio(
            RegraNegocioException exception) {
        return ResponseEntity.badRequest().body(Map.of("erro", exception.getMessage()));
    }

    @ExceptionHandler(ConflitoReservaException.class)
    public ResponseEntity<Map<String, String>> handleConflitoReserva(
            ConflitoReservaException exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "erro", exception.getMessage(),
                "detalhes", exception.getDetalhes()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("erro", "O identificador informado possui formato inválido."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException exception) {
        Map<String, String> erros = new LinkedHashMap<>();
        exception.getBindingResult().getAllErrors().forEach(error -> {
            String nomeCampo = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : error.getObjectName();
            String mensagem = error.getDefaultMessage() == null
                    ? "O valor informado é inválido."
                    : error.getDefaultMessage();
            erros.put(nomeCampo, mensagem);
        });

        if (erros.isEmpty()) {
            erros.put("erro", "A requisição contém dados inválidos.");
        }

        return ResponseEntity.badRequest().body(erros);
    }
}
