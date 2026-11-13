package br.edu.organizacaorecursos.service;

import br.edu.organizacaorecursos.domain.Manutencao;
import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.dto.ManutencaoRequestDTO;
import br.edu.organizacaorecursos.dto.ManutencaoResponseDTO;
import br.edu.organizacaorecursos.exception.RegraNegocioException;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManutencaoService {

    private final ManutencaoRepository manutencaoRepository;
    private final RecursoRepository recursoRepository;
    private final ReservaRepository reservaRepository;

    @Transactional
    public ManutencaoResponseDTO criar(ManutencaoRequestDTO request) {
        validarPeriodo(request);
        Recurso recurso = buscarRecurso(request.recursoId());
        validarConflitoComReserva(request);
        validarConflitoComManutencao(request, null);
        return toResponse(manutencaoRepository.save(new Manutencao(
                null, recurso, request.dataInicio(), request.dataFim(), request.descricao())));
    }

    @Transactional(readOnly = true)
    public List<ManutencaoResponseDTO> listar(UUID recursoId) {
        return manutencaoRepository.filtrar(recursoId).stream()
                .map(ManutencaoService::toResponse)
                .toList();
    }

    @Transactional
    public ManutencaoResponseDTO atualizar(UUID id, ManutencaoRequestDTO request) {
        Manutencao manutencao = manutencaoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Manutenção não encontrada."));
        validarPeriodo(request);
        Recurso recurso = buscarRecurso(request.recursoId());
        validarConflitoComReserva(request);
        validarConflitoComManutencao(request, id);
        manutencao.setRecurso(recurso);
        manutencao.setDataInicio(request.dataInicio());
        manutencao.setDataFim(request.dataFim());
        manutencao.setDescricao(request.descricao());
        return toResponse(manutencaoRepository.save(manutencao));
    }

    @Transactional
    public void deletar(UUID id) {
        if (!manutencaoRepository.existsById(id)) {
            throw new EntityNotFoundException("Manutenção não encontrada.");
        }
        manutencaoRepository.deleteById(id);
    }

    private Recurso buscarRecurso(UUID recursoId) {
        if (recursoId == null) {
            throw new RegraNegocioException("O ID do recurso é obrigatório.");
        }
        return recursoRepository.buscarPorIdComBloqueio(recursoId)
                .orElseThrow(() -> new EntityNotFoundException("Recurso não encontrado."));
    }

    private void validarPeriodo(ManutencaoRequestDTO request) {
        if (request.dataInicio() == null || request.dataFim() == null) {
            throw new RegraNegocioException("As datas de início e fim são obrigatórias.");
        }
        if (!request.dataFim().isAfter(request.dataInicio())) {
            throw new RegraNegocioException("A data de fim deve ser posterior à data de início.");
        }
    }

    private void validarConflitoComReserva(ManutencaoRequestDTO request) {
        if (reservaRepository.existeConflitoDeHorario(
                request.recursoId(), request.dataInicio(), request.dataFim())) {
            throw new RegraNegocioException(
                    "A manutenção não pode sobrepor uma reserva ativa do recurso.");
        }
    }

    private void validarConflitoComManutencao(ManutencaoRequestDTO request, UUID id) {
        if (manutencaoRepository.existeConflito(
                request.recursoId(), id, request.dataInicio(), request.dataFim())) {
            throw new RegraNegocioException(
                    "A manutenção não pode sobrepor outra manutenção do recurso.");
        }
    }

    private static ManutencaoResponseDTO toResponse(Manutencao manutencao) {
        return new ManutencaoResponseDTO(
                manutencao.getId(),
                manutencao.getRecurso().getId(),
                manutencao.getRecurso().getNome(),
                manutencao.getDataInicio(),
                manutencao.getDataFim(),
                manutencao.getDescricao());
    }
}
