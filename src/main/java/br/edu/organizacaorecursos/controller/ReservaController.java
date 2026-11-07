package br.edu.organizacaorecursos.controller;

import br.edu.organizacaorecursos.dto.ReservaRequestDTO;
import br.edu.organizacaorecursos.dto.ReservaResponseDTO;
import br.edu.organizacaorecursos.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/reservas", "/api/reservas"})
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    public ResponseEntity<ReservaResponseDTO> criarReserva(
            @Valid @RequestBody ReservaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.criarReserva(request));
    }
}
