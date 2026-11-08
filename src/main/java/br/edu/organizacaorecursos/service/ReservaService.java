package br.edu.organizacaorecursos.service;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.dto.ReservaRequestDTO;
import br.edu.organizacaorecursos.dto.ReservaResponseDTO;
import br.edu.organizacaorecursos.exception.ConflitoReservaException;
import br.edu.organizacaorecursos.exception.RegraNegocioException;
import br.edu.organizacaorecursos.exception.RecursoNaoEncontradoException;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final RecursoRepository recursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ManutencaoRepository manutencaoRepository;

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

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));

        boolean emManutencao = manutencaoRepository.isRecursoEmManutencao(
                recurso.getId(), dto.getDataInicio(), dto.getDataFim());
        if (emManutencao) {
            throw new RegraNegocioException(
                    "O recurso não pode ser reservado pois estará em manutenção neste período.");
        }

        boolean temConflito = reservaRepository.existeConflitoDeHorario(
                recurso.getId(), dto.getDataInicio(), dto.getDataFim());
        if (temConflito) {
            Reserva conflito = reservaRepository.buscarConflitosDeHorario(
                            recurso.getId(), dto.getDataInicio(), dto.getDataFim())
                    .stream()
                    .findFirst()
                    .orElse(null);
            LocalDateTime inicioConflitante = conflito == null
                    ? dto.getDataInicio()
                    : conflito.getDataInicio();
            LocalDateTime fimConflitante = conflito == null
                    ? dto.getDataFim()
                    : conflito.getDataFim();
            String detalhes = recurso.getNome() + ": período conflitante de "
                    + inicioConflitante + " a " + fimConflitante + ".";
            throw new ConflitoReservaException(
                    "O recurso já está reservado neste período.", detalhes);
        }

        Reserva novaReserva = new Reserva();
        novaReserva.setRecurso(recurso);
        novaReserva.setDataInicio(dto.getDataInicio());
        novaReserva.setDataFim(dto.getDataFim());
        novaReserva.setUsuario(usuario);

        Reserva reservaSalva = reservaRepository.save(novaReserva);
        return mapearParaResponseDTO(reservaSalva);
    }

    @Transactional(readOnly = true)
    public ReservaResponseDTO buscarReservaPorId(UUID id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Reserva não encontrada para o ID informado."));
        return mapearParaResponseDTO(reserva);
    }

    @Transactional
    public void deletarReserva(UUID id) {
        if (!reservaRepository.existsById(id)) {
            throw new EntityNotFoundException("Reserva não encontrada para exclusão.");
        }
        reservaRepository.deleteById(id);
    }

    private ReservaResponseDTO mapearParaResponseDTO(Reserva reserva) {
        ReservaResponseDTO response = new ReservaResponseDTO();
        response.setId(reserva.getId());
        response.setRecursoId(reserva.getRecurso().getId());
        response.setNomeRecurso(reserva.getRecurso().getNome());
        response.setUsuarioId(reserva.getUsuario().getId());
        response.setNomeUsuario(reserva.getUsuario().getNome());
        response.setDataInicio(reserva.getDataInicio());
        response.setDataFim(reserva.getDataFim());
        return response;
    }
}
