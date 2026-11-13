package br.edu.organizacaorecursos.config;

import br.edu.organizacaorecursos.domain.Perfil;
import br.edu.organizacaorecursos.domain.Recurso;
import br.edu.organizacaorecursos.domain.Usuario;
import br.edu.organizacaorecursos.repository.RecursoRepository;
import br.edu.organizacaorecursos.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local & !prod")
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);
    private final UsuarioRepository usuarios;
    private final RecursoRepository recursos;

    public DataInitializer(UsuarioRepository usuarios, RecursoRepository recursos) {
        this.usuarios = usuarios;
        this.recursos = recursos;
    }

    @Override
    @Transactional
    public void run(String... args) {
        criarUsuarioSeAusente("Administrador", "admin@organizacao.edu.br", "ADM001", Perfil.ADMINISTRADOR);
        criarUsuarioSeAusente("Responsável Laboratório", "responsavel@organizacao.edu.br", "RESP001",
                Perfil.RESPONSAVEL);
        criarUsuarioSeAusente("Aluno Solicitante", "solicitante@organizacao.edu.br", "SOL001",
                Perfil.SOLICITANTE);
        if (!recursos.existsByNomeIgnoreCase("Sala de estudos")) {
            recursos.save(new Recurso(null, "Sala de estudos", "SALA", false, null));
        }
        if (!recursos.existsByNomeIgnoreCase("Laboratório")) {
            var responsavel = usuarios.findByMatricula("RESP001")
                    .filter(usuario -> usuario.getPerfil() == Perfil.RESPONSAVEL);
            if (responsavel.isEmpty()) {
                LOGGER.warn("Laboratório de demonstração não foi criado: RESP001 não é um responsável válido.");
                return;
            }
            recursos.save(new Recurso(null, "Laboratório", "LABORATORIO", true, responsavel.get().getId()));
        }
    }

    private void criarUsuarioSeAusente(String nome, String email, String matricula, Perfil perfil) {
        var porMatricula = usuarios.findByMatricula(matricula);
        var porEmail = usuarios.findByEmailIgnoreCase(email);
        if (porMatricula.isPresent() || porEmail.isPresent()) {
            Usuario usuario = porMatricula.orElseGet(porEmail::orElseThrow);
            if (!email.equalsIgnoreCase(usuario.getEmail()) || !matricula.equals(usuario.getMatricula())
                    || usuario.getPerfil() != perfil
                    || (porEmail.isPresent() && !porEmail.get().getId().equals(usuario.getId()))) {
                LOGGER.warn("Usuário de demonstração {} não foi criado: matrícula ou e-mail em uso com dados diferentes.",
                        matricula);
            }
            return;
        }
        usuarios.save(new Usuario(null, nome, email, matricula, perfil));
    }
}
