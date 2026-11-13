package br.edu.organizacaorecursos.service;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.domain.ReservaAuditoria;
import br.edu.organizacaorecursos.domain.StatusReserva;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.dto.ReservaAuditoriaResponseDTO;
import br.edu.organizacaorecursos.dto.ReservaRequestDTO;
import br.edu.organizacaorecursos.dto.ReservaResponseDTO;
import br.edu.organizacaorecursos.exception.ConflitoReservaException;
import br.edu.organizacaorecursos.exception.RegraNegocioException;
import br.edu.organizacaorecursos.exception.RecursoNaoEncontradoException;
import br.edu.organizacaorecursos.repository.ManutencaoRepository;
import br.edu.organizacaorecursos.repository.ReservaAuditoriaRepository;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.ReservaRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final RecursoRepository recursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ManutencaoRepository manutencaoRepository;
    private final ReservaAuditoriaRepository reservaAuditoriaRepository;
    private final Clock clock;

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
        UUID responsavelId = bloquearAgendaDoResponsavel(recurso);

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));

        boolean emManutencao = manutencaoRepository.isRecursoEmManutencao(
                recurso.getId(), dto.getDataInicio(), dto.getDataFim());
        if (emManutencao) {
            throw new RegraNegocioException(
                    "O recurso não pode ser reservado pois estará em manutenção neste período.");
        }

        boolean temConflito = reservaRepository.existeConflitoDeHorario(
                        recurso.getId(), dto.getDataInicio(), dto.getDataFim())
                || responsavelId != null && reservaRepository.existeConflitoDeAgendaResponsavel(
                        responsavelId, null, dto.getDataInicio(), dto.getDataFim());
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

        StatusReserva statusInicial = recurso.isRestrito()
                ? StatusReserva.PENDENTE
                : StatusReserva.APROVADA;
        Reserva novaReserva = new Reserva();
        novaReserva.setRecurso(recurso);
        novaReserva.setDataInicio(dto.getDataInicio());
        novaReserva.setDataFim(dto.getDataFim());
        novaReserva.setUsuario(usuario);
        novaReserva.setStatus(statusInicial);

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

        @Transactional(readOnly = true)
        public List<ReservaResponseDTO> listarReservas() {
                return listarReservas(null, null);
        }

        @Transactional(readOnly = true)
        public List<ReservaResponseDTO> listarReservas(StatusReserva status, UUID recursoId) {
                return reservaRepository.filtrar(status, recursoId).stream()
                        .map(this::mapearParaResponseDTO)
                        .toList();
        }

        @Transactional(readOnly = true)
        public List<ReservaAuditoriaResponseDTO> historicoReserva(UUID reservaId) {
                if (!reservaRepository.existsById(reservaId)) {
                        throw new EntityNotFoundException("Reserva não encontrada para o ID informado.");
                }
                return reservaAuditoriaRepository.findByReservaIdOrderByInstanteAscIdAsc(reservaId).stream()
                                .map(ReservaAuditoriaResponseDTO::de)
                                .toList();
        }

        @Transactional
        public ReservaResponseDTO atualizarReserva(UUID reservaId, ReservaRequestDTO dto, UUID atorId) {
                if (dto.getDataInicio() == null || dto.getDataFim() == null) {
                        throw new RegraNegocioException("As datas de início e fim são obrigatórias.");
                }
                if (!dto.getDataFim().isAfter(dto.getDataInicio())) {
                        throw new RegraNegocioException("A data de término deve ser posterior à data de início.");
                }

                Reserva reserva = buscarReserva(reservaId);
                if (!reserva.getUsuario().getId().equals(atorId)) {
                        throw new AccessDeniedException("Somente o solicitante pode atualizar a própria reserva.");
                }
                if (reserva.getStatus() != StatusReserva.PENDENTE
                                && reserva.getStatus() != StatusReserva.SOLICITADA) {
                        throw new RegraNegocioException("Somente reservas pendentes podem ser atualizadas.");
                }

                Recurso recurso = recursoRepository.buscarPorIdComBloqueio(dto.getRecursoId())
                                .orElseThrow(() -> new RecursoNaoEncontradoException("Recurso não encontrado."));
                UUID responsavelId = bloquearAgendaDoResponsavel(recurso);
                if (manutencaoRepository.isRecursoEmManutencao(
                                recurso.getId(), dto.getDataInicio(), dto.getDataFim())) {
                        throw new RegraNegocioException(
                                        "O recurso não pode ser reservado pois estará em manutenção neste período.");
                }

                StatusReserva novoStatus = recurso.isRestrito()
                                ? StatusReserva.PENDENTE
                                : StatusReserva.APROVADA;
                if (reservaRepository.existeConflitoDeHorarioAprovado(
                                                recurso.getId(), reserva.getId(), dto.getDataInicio(), dto.getDataFim())
                                || responsavelId != null && reservaRepository.existeConflitoDeAgendaResponsavel(
                                                responsavelId, reserva.getId(), dto.getDataInicio(), dto.getDataFim())) {
                        throw new ConflitoReservaException(
                                        "O recurso já está reservado neste período.",
                                        recurso.getNome() + ": período conflitante de "
                                                        + dto.getDataInicio() + " a " + dto.getDataFim() + ".");
                }

                StatusReserva estadoAnterior = reserva.getStatus();
                reserva.setRecurso(recurso);
                reserva.setDataInicio(dto.getDataInicio());
                reserva.setDataFim(dto.getDataFim());
                reserva.setStatus(novoStatus);
                Reserva reservaSalva = reservaRepository.save(reserva);
                registrarMudancaDeEstado(
                                reservaSalva,
                                estadoAnterior,
                                novoStatus,
                                atorId,
                                "Reserva atualizada e confirmada.");
                return mapearParaResponseDTO(reservaSalva);
        }

        @Transactional
        public ReservaResponseDTO atualizarStatus(UUID reservaId, StatusReserva novoStatus, UUID atorId) {
                return atualizarStatus(reservaId, novoStatus, atorId, null);
        }

        @Transactional
        public ReservaResponseDTO atualizarStatus(
                        UUID reservaId, StatusReserva novoStatus, UUID atorId, String motivo) {
                Usuario ator = usuarioRepository.findById(atorId)
                        .orElseThrow(() -> new RegraNegocioException("Usuário autenticado não encontrado."));
                if (novoStatus != StatusReserva.APROVADA && novoStatus != StatusReserva.REJEITADA) {
                        throw new RegraNegocioException("O status deve ser APROVADA ou REJEITADA.");
                }

                Reserva reserva = buscarReserva(reservaId);
                if (reserva.getStatus() != StatusReserva.PENDENTE
                        && reserva.getStatus() != StatusReserva.SOLICITADA) {
                        throw new RegraNegocioException("Somente reservas pendentes podem ser decididas.");
                }
                if (!reserva.getRecurso().isRestrito()) {
                        throw new RegraNegocioException("Somente reservas de recursos restritos exigem decisão.");
                }
                UUID responsavelId = reserva.getRecurso().getResponsavelId();
                boolean administrador = ator.getPerfil() == Perfil.ADMINISTRADOR;
                boolean responsavelDoRecurso = ator.getPerfil() == Perfil.RESPONSAVEL
                                && responsavelId != null && responsavelId.equals(atorId);
                if (!administrador && !responsavelDoRecurso) {
                        throw new AccessDeniedException(
                                        "Somente administradores ou o responsável cadastrado podem decidir a reserva.");
                }
                if (reserva.getUsuario().getId().equals(atorId)) {
                        throw new RegraNegocioException("O solicitante não pode aprovar a própria reserva.");
                }

                if (novoStatus == StatusReserva.APROVADA) {
                                                Recurso recurso = recursoRepository.buscarPorIdComBloqueio(reserva.getRecurso().getId())
                                .orElseThrow(() -> new RecursoNaoEncontradoException("Recurso não encontrado."));
                                                responsavelId = bloquearAgendaDoResponsavel(recurso);
                        boolean temConflito = reservaRepository.existeConflitoDeHorarioAprovado(
                                reserva.getRecurso().getId(), reserva.getId(),
                                reserva.getDataInicio(), reserva.getDataFim());
                        temConflito = temConflito || responsavelId != null
                                && reservaRepository.existeConflitoDeAgendaResponsavel(
                                        responsavelId, reserva.getId(),
                                        reserva.getDataInicio(), reserva.getDataFim());
                        if (temConflito) {
                                throw new ConflitoReservaException(
                                        "O recurso já está reservado neste período.",
                                        reserva.getRecurso().getNome() + ": período conflitante de "
                                                + reserva.getDataInicio() + " a " + reserva.getDataFim() + ".");
                        }
                }
                String motivoEfetivo = motivoOuPadrao(
                                motivo, novoStatus == StatusReserva.APROVADA
                                                ? "Reserva aprovada."
                                                : "Reserva rejeitada.");
                return persistirStatus(reserva, novoStatus, atorId, motivoEfetivo);
        }

        @Transactional
        public ReservaResponseDTO aprovarReserva(UUID reservaId, UUID atorId) {
                return atualizarStatus(reservaId, StatusReserva.APROVADA, atorId);
        }

        @Transactional
        public ReservaResponseDTO aprovarReserva(UUID reservaId, UUID atorId, String motivo) {
                return atualizarStatus(reservaId, StatusReserva.APROVADA, atorId, motivo);
        }

        @Transactional
        public ReservaResponseDTO iniciarReserva(UUID reservaId, UUID atorId) {
                return transicionarComoResponsavel(
                                reservaId,
                                atorId,
                                StatusReserva.APROVADA,
                                StatusReserva.EM_USO,
                                "Reserva iniciada.");
        }

        @Transactional
        public ReservaResponseDTO concluirReserva(UUID reservaId, UUID atorId) {
                return transicionarComoResponsavel(
                                reservaId,
                                atorId,
                                StatusReserva.EM_USO,
                                StatusReserva.CONCLUÍDA,
                                "Reserva concluída.");
        }

        @Transactional
        public ReservaResponseDTO cancelarReserva(UUID reservaId, UUID atorId) {
                return cancelarReserva(reservaId, atorId, null);
        }

        @Transactional
        public ReservaResponseDTO cancelarReserva(UUID reservaId, UUID atorId, String motivo) {
                Reserva reserva = buscarReserva(reservaId);
                if (!reserva.getUsuario().getId().equals(atorId)) {
                        throw new RegraNegocioException("Somente o solicitante pode cancelar a reserva.");
                }
                if (reserva.getStatus() != StatusReserva.PENDENTE
                        && reserva.getStatus() != StatusReserva.SOLICITADA
                        && reserva.getStatus() != StatusReserva.APROVADA) {
                    throw new RegraNegocioException(
                            "Somente reservas pendentes, solicitadas ou aprovadas podem ser canceladas.");
                }
                return persistirStatus(
                                reserva,
                                StatusReserva.CANCELADA,
                                atorId,
                                motivoOuPadrao(motivo, "Reserva cancelada pelo solicitante."));
        }

        @Transactional
        public ReservaResponseDTO marcarNaoComparecimento(UUID reservaId, UUID atorId) {
                return marcarNaoComparecimento(reservaId, atorId, null);
        }

        @Transactional
        public ReservaResponseDTO marcarNaoComparecimento(UUID reservaId, UUID atorId, String motivo) {
                return transicionarComoResponsavel(
                        reservaId,
                        atorId,
                                StatusReserva.APROVADA,
                                StatusReserva.NÃO_COMPARECEU,
                                motivoOuPadrao(motivo, "Não comparecimento registrado."));
        }

            private ReservaResponseDTO transicionarComoResponsavel(
                    UUID reservaId,
                    UUID atorId,
                    StatusReserva origem,
                    StatusReserva destino,
                        String motivo) {
                Reserva reserva = buscarReserva(reservaId);
                    if (atorId == null) {
                            throw new AccessDeniedException("A identidade do responsável é obrigatória.");
                }
                    validarResponsavel(reserva, atorId);
                    return transicionarReserva(reserva, origem, destino, atorId, motivo);
            }

            private ReservaResponseDTO transicionarReserva(
                            Reserva reserva,
                            StatusReserva origem,
                            StatusReserva destino,
                            UUID atorId,
                            String motivo) {
                if (reserva.getStatus() != origem) {
                            throw new RegraNegocioException("A reserva não está no estado necessário para esta ação.");
                }

                    return persistirStatus(reserva, destino, atorId, motivo);
        }

        private Reserva buscarReserva(UUID reservaId) {
                return reservaRepository.findById(reservaId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Reserva não encontrada para o ID informado."));
        }

        private void validarResponsavel(Reserva reserva, UUID atorId) {
                UUID responsavelId = reserva.getRecurso().getResponsavelId();
                if (responsavelId == null || !responsavelId.equals(atorId)) {
                        throw new RegraNegocioException("Somente o responsável pode alterar esta reserva.");
                }
        }

    @Transactional
        public void deletarReserva(UUID id, UUID atorId) {
                Reserva reserva = reservaRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada para exclusão."));
                Usuario ator = usuarioRepository.findById(atorId)
                                .orElseThrow(() -> new AccessDeniedException("Usuário autenticado não encontrado."));
                if (ator.getPerfil() != Perfil.ADMINISTRADOR
                                && !reserva.getUsuario().getId().equals(atorId)) {
                        throw new AccessDeniedException(
                                        "Somente o solicitante ou um ADMINISTRADOR pode excluir a reserva.");
                }
                if (reserva.getStatus() == StatusReserva.EM_USO
                                || reserva.getStatus() == StatusReserva.CONCLUÍDA
                                || reserva.getStatus() == StatusReserva.NÃO_COMPARECEU) {
                        throw new RegraNegocioException("Reservas que já foram iniciadas não podem ser excluídas.");
                }
                if (reservaAuditoriaRepository.existsByReservaId(id)) {
                        throw new RegraNegocioException(
                                        "Reservas com histórico de auditoria não podem ser excluídas.");
                }
                reservaRepository.delete(reserva);
    }

        private UUID bloquearAgendaDoResponsavel(Recurso recurso) {
                UUID responsavelId = recurso.getResponsavelId();
                if (responsavelId != null) {
                        usuarioRepository.buscarPorIdComBloqueio(responsavelId);
                }
                return responsavelId;
        }

        private ReservaResponseDTO mapearParaResponseDTO(Reserva reserva) {
        ReservaResponseDTO response = new ReservaResponseDTO();
        response.setId(reserva.getId());
        response.setRecursoId(reserva.getRecurso().getId());
        response.setNomeRecurso(reserva.getRecurso().getNome());
        response.setRecursoRestrito(reserva.getRecurso().isRestrito());
        response.setUsuarioId(reserva.getUsuario().getId());
        response.setNomeUsuario(reserva.getUsuario().getNome());
        response.setDataInicio(reserva.getDataInicio());
        response.setDataFim(reserva.getDataFim());
        response.setStatus(reserva.getStatus());
        return response;
    }

        private ReservaResponseDTO persistirStatus(
                        Reserva reserva, StatusReserva status, UUID atorId, String motivo) {
                StatusReserva estadoAnterior = reserva.getStatus();
                reserva.setStatus(status);
                Reserva reservaSalva = reservaRepository.save(reserva);
                registrarMudancaDeEstado(reservaSalva, estadoAnterior, status, atorId, motivo);
                return mapearParaResponseDTO(reservaSalva);
        }

        private void registrarMudancaDeEstado(
                        Reserva reserva,
                        StatusReserva estadoAnterior,
                        StatusReserva estadoNovo,
                        UUID atorId,
                        String motivo) {
                if (estadoAnterior == estadoNovo) {
                        return;
                }
                if (atorId == null) {
                        throw new AccessDeniedException("A identidade do usuário é obrigatória para auditar a mudança.");
                }
                reservaAuditoriaRepository.save(new ReservaAuditoria(
                                reserva,
                                atorId,
                                Instant.now(clock),
                                estadoAnterior,
                                estadoNovo,
                                motivoOuPadrao(motivo, "Estado alterado para " + estadoNovo + ".")));
        }

        private String motivoOuPadrao(String motivo, String padrao) {
                return motivo == null || motivo.isBlank() ? padrao : motivo.trim();
        }
}
