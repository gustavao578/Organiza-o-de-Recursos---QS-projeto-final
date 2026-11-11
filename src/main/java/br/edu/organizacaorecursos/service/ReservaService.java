package br.edu.organizacaorecursos.service;

import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.Reserva;
import br.edu.organizacaorecursos.domain.StatusReserva;
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
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        StatusReserva statusInicial = recurso.isRestrito()
                ? StatusReserva.PENDENTE
                : StatusReserva.APROVADA;
        boolean temConflito = statusInicial == StatusReserva.APROVADA
                && (reservaRepository.existeConflitoDeHorario(
                                recurso.getId(), dto.getDataInicio(), dto.getDataFim())
                        || responsavelId != null && reservaRepository.existeConflitoDeAgendaResponsavel(
                                responsavelId, null, dto.getDataInicio(), dto.getDataFim()));
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
                return reservaRepository.findAll().stream().map(this::mapearParaResponseDTO).toList();
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
                if (novoStatus == StatusReserva.APROVADA
                                && (reservaRepository.existeConflitoDeHorarioAprovado(
                                                recurso.getId(), reserva.getId(), dto.getDataInicio(), dto.getDataFim())
                                        || responsavelId != null && reservaRepository.existeConflitoDeAgendaResponsavel(
                                                responsavelId, reserva.getId(), dto.getDataInicio(), dto.getDataFim()))) {
                        throw new ConflitoReservaException(
                                        "O recurso já está reservado neste período.",
                                        recurso.getNome() + ": período conflitante de "
                                                        + dto.getDataInicio() + " a " + dto.getDataFim() + ".");
                }

                reserva.setRecurso(recurso);
                reserva.setDataInicio(dto.getDataInicio());
                reserva.setDataFim(dto.getDataFim());
                reserva.setStatus(novoStatus);
                return mapearParaResponseDTO(reservaRepository.save(reserva));
        }

        @Transactional
        public ReservaResponseDTO atualizarStatus(UUID reservaId, StatusReserva novoStatus, UUID atorId) {
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
                return persistirStatus(reserva, novoStatus);
        }

        @Transactional
        public ReservaResponseDTO aprovarReserva(UUID reservaId, UUID atorId) {
                return atualizarStatus(reservaId, StatusReserva.APROVADA, atorId);
        }

        @Transactional
        public ReservaResponseDTO iniciarReserva(UUID reservaId) {
                return transicionarReserva(
                                reservaId,
                                StatusReserva.APROVADA,
                                StatusReserva.EM_USO,
                                "Somente reservas aprovadas podem ser iniciadas.");
        }

        @Transactional
        public ReservaResponseDTO iniciarReserva(UUID reservaId, UUID atorId) {
                return transicionarComoResponsavel(
                                reservaId,
                                atorId,
                                StatusReserva.APROVADA,
                                StatusReserva.EM_USO,
                                "Somente reservas aprovadas podem ser iniciadas.");
        }

        @Transactional
        public ReservaResponseDTO concluirReserva(UUID reservaId) {
                return transicionarReserva(
                                reservaId,
                                StatusReserva.EM_USO,
                                StatusReserva.CONCLUÍDA,
                                "Somente reservas em uso podem ser concluídas.");
        }

        @Transactional
        public ReservaResponseDTO concluirReserva(UUID reservaId, UUID atorId) {
                return transicionarComoResponsavel(
                                reservaId,
                                atorId,
                                StatusReserva.EM_USO,
                                StatusReserva.CONCLUÍDA,
                                "Somente reservas em uso podem ser concluídas.");
        }

        @Transactional
        public ReservaResponseDTO rejeitarReserva(UUID reservaId, UUID atorId) {
                Reserva reserva = buscarReserva(reservaId);
                if (atorId != null) {
                        validarResponsavel(reserva, atorId);
                }
                validarStatusPendente(reserva);
                return persistirStatus(reserva, StatusReserva.REJEITADA);
        }

        @Transactional
        public ReservaResponseDTO cancelarReserva(UUID reservaId, UUID atorId) {
                Reserva reserva = buscarReserva(reservaId);
                if (!reserva.getUsuario().getId().equals(atorId)) {
                        throw new RegraNegocioException("Somente o solicitante pode cancelar a reserva.");
                }
                validarStatusPendente(reserva);
                return persistirStatus(reserva, StatusReserva.CANCELADA);
        }

        @Transactional
        public ReservaResponseDTO marcarNaoComparecimento(UUID reservaId, UUID atorId) {
                return transicionarComoResponsavel(
                        reservaId,
                        atorId,
                                StatusReserva.APROVADA,
                                StatusReserva.NÃO_COMPARECEU,
                                "Somente reservas aprovadas podem ser marcadas como não comparecimento.");
        }

            private ReservaResponseDTO transicionarComoResponsavel(
                    UUID reservaId,
                    UUID atorId,
                    StatusReserva origem,
                    StatusReserva destino,
                    String mensagemInvalida) {
                Reserva reserva = buscarReserva(reservaId);
                if (atorId != null) {
                        validarResponsavel(reserva, atorId);
                }
                return transicionarReserva(reserva, origem, destino, mensagemInvalida);
            }

        private ReservaResponseDTO transicionarReserva(
                        UUID reservaId, StatusReserva origem, StatusReserva destino, String mensagemInvalida) {
                return transicionarReserva(buscarReserva(reservaId), origem, destino, mensagemInvalida);
        }

        private ReservaResponseDTO transicionarReserva(
                        Reserva reserva, StatusReserva origem, StatusReserva destino, String mensagemInvalida) {
                if (reserva.getStatus() != origem) {
                        throw new RegraNegocioException(mensagemInvalida);
                }

                return persistirStatus(reserva, destino);
        }

        private Reserva buscarReserva(UUID reservaId) {
                return reservaRepository.findById(reservaId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Reserva não encontrada para o ID informado."));
        }

        private void validarStatusPendente(Reserva reserva) {
                if (reserva.getStatus() != StatusReserva.PENDENTE
                                && reserva.getStatus() != StatusReserva.SOLICITADA) {
                        throw new RegraNegocioException("Somente solicitações pendentes podem ser alteradas.");
                }
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

        private ReservaResponseDTO persistirStatus(Reserva reserva, StatusReserva status) {
                reserva.setStatus(status);
                return mapearParaResponseDTO(reservaRepository.save(reserva));
        }
}
