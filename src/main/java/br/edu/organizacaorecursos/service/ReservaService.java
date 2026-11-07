package br.edu.organizacaorecursos.service;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.dto.ReservaRequestDTO;
import br.edu.organizacaorecursos.dto.ReservaResponseDTO;
import br.edu.organizacaorecursos.exception.RegraNegocioException;
import br.edu.organizacaorecursos.exception.RecursoNaoEncontradoException;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final RecursoRepository recursoRepository;

    @Transactional
    public ReservaResponseDTO criarReserva(ReservaRequestDTO dto) {
        if (dto.getDataInicio() == null || dto.getDataFim() == null) {
            throw new RegraNegocioException("As datas de início e fim são obrigatórias.");
        }

        if (!dto.getDataFim().isAfter(dto.getDataInicio())) {
            throw new RegraNegocioException(
                    "A data de término deve ser posterior à data de início.");
        }

        Recurso recurso = recursoRepository.buscarPorIdComBloqueio(dto.getRecursoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recurso não encontrado."));

        boolean temConflito = reservaRepository.existeConflitoDeHorario(
                recurso.getId(), dto.getDataInicio(), dto.getDataFim());
        if (temConflito) {
            throw new RegraNegocioException("O recurso já está reservado neste período.");
        }

        Reserva novaReserva = new Reserva();
        novaReserva.setRecurso(recurso);
        novaReserva.setDataInicio(dto.getDataInicio());
        novaReserva.setDataFim(dto.getDataFim());
        novaReserva.setUsuarioResponsavel(dto.getUsuarioResponsavel());

        Reserva reservaSalva = reservaRepository.save(novaReserva);
        return mapearParaResponseDTO(reservaSalva);
    }

    private ReservaResponseDTO mapearParaResponseDTO(Reserva reserva) {
        ReservaResponseDTO response = new ReservaResponseDTO();
        response.setId(reserva.getId());
        response.setRecursoId(reserva.getRecurso().getId());
        response.setNomeRecurso(reserva.getRecurso().getNome());
        response.setDataInicio(reserva.getDataInicio());
        response.setDataFim(reserva.getDataFim());
        response.setUsuarioResponsavel(reserva.getUsuarioResponsavel());
        return response;
    }
}
