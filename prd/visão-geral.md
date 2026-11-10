# Visão Geral do Projeto

## Objetivo deste documento

Manter a visão atual e rastreável do projeto **Organização de Recursos**, reunindo seu propósito, escopo confirmado, estado de evolução, contexto técnico e próximos passos. Este documento deve acompanhar o projeto desde a definição dos requisitos até a entrega.

## Visão do produto

O sistema tem como propósito organizar a alocação de salas, professores e materiais para atividades, evitando conflitos de horário. Deve apoiar a consulta de disponibilidade, a criação e o acompanhamento de reservas e a administração dos recursos, respeitando os perfis e responsabilidades definidos para o projeto.

A aplicação é um projeto semestral de qualidade de software. Sua avaliação considera não apenas o funcionamento, mas também evidências objetivas de qualidade, segurança, rastreabilidade e desempenho.

## Objetivos do projeto

- Permitir a organização de reservas de salas, professores e materiais sem conflitos de horário.
- Apoiar os fluxos de solicitantes, responsáveis e administradores descritos na especificação.
- Desenvolver e validar a aplicação com evidências verificáveis de qualidade, segurança, rastreabilidade e desempenho.
- Aplicar ao projeto as técnicas de qualidade estudadas durante as aulas.

## Escopo confirmado

A especificação inicial estabelece os seguintes elementos:

- Consulta de disponibilidade de recursos.
- Criação, alteração e cancelamento de reservas pelo solicitante.
- Aprovação de solicitações especiais pelo responsável.
- Validação da alocação de docentes pelo responsável.
- Acompanhamento de retiradas e devoluções de materiais pelo responsável.
- Gerenciamento de salas, professores, materiais, usuários, bloqueios e períodos de manutenção pelo administrador.
- Prevenção de conflitos de horário envolvendo os recursos alocados.

Documentação complementar adicionada:

- [requisitos-funcionais.md](requisitos-funcionais.md) — especificação detalhada dos requisitos funcionais, regras de negócio e critérios de aceite, revisada e considerada validada.
- [requisitos-não-funcionais.md](requisitos-não-funcionais.md) — requisitos de qualidade, restrições técnicas, evidências de verificação e parâmetros ainda pendentes; não substitui nem altera os requisitos funcionais.
- [arquitetura.md](arquitetura.md) — documento único com o baseline arquitetural, a proposta complementar, diagramas C4, sequências online e batch, análise ATAM e parecer independente do painel revisor. As decisões, ADRs, tecnologias, origem dos dados, uso online de modelo e políticas ainda sem requisito permanecem pendentes; nenhum risco foi formalmente aceito.
- [testes/plano-tdd.md](testes/plano-tdd.md) — inventário rastreável de regras e cenários por RF, regras críticas e RNF, com ambiguidades, matriz regra→caso→teste→status e resultados da fase red. R-TDD-33 (responsividade) e R-TDD-35 (documentação/API) aguardam a criação dessas superfícies.

As personas em [personas.md](personas.md) e os requisitos funcionais foram revisados e considerados validados. As regras críticas e os critérios de qualidade aprovados estão consolidados nesta visão geral; os documentos vinculados mantêm o detalhamento completo.

## Perfis envolvidos

- **Solicitante:** professor ou coordenador que consulta disponibilidade e cria, altera ou cancela suas reservas.
- **Responsável:** aprova solicitações especiais, valida a alocação de docentes e acompanha retiradas e devoluções de material.
- **Administrador:** gerencia salas, professores, materiais, usuários, bloqueios e períodos de manutenção.

Os perfis e suas necessidades estão detalhados em [personas.md](personas.md).

## Contexto técnico e entrega

- Linguagem/build: Java 21 no `pom.xml` e no CI; Spring Boot 3.2.4 e Maven Wrapper 3.10.
- API e persistência: Spring Web/MVC, Spring Data JPA, H2 em memória no perfil atual, UUID para IDs, Hibernate `ddl-auto=update`.
- Segurança implementada: Spring Security OAuth2 Resource Server com JWT HS256, segredo externo `JWT_SECRET`, API stateless e CSRF ignorado somente para requisições Bearer nas rotas da API.
- Validação e contrato: Jakarta Bean Validation e Springdoc OpenAPI/Swagger UI.
- Testes e qualidade: JUnit 5, Mockito via Spring Boot Test, Cucumber-JVM 7.18.1, JaCoCo 0.8.15, GitHub Actions e SonarCloud.
- Entrega prevista: semana 46.
- O projeto será desenvolvido por equipes.

Esta é a stack observada no código e no CI, não uma decisão de banco de produção ou implantação. H2 é volátil; banco de produção, migrações, emissor/validação completa de JWT, provisionamento de identidade e implantação continuam sem decisão. O serviço não fornece endpoint de login/ emissão de tokens. Testcontainers, WireMock e JMeter estão nos requisitos, mas ainda não estão configurados/validados no projeto.

## Regras críticas

#### Tempo e concorrência

- O término de uma reserva deve ser posterior ao início.
- Reservas do mesmo recurso não podem se sobrepor, seja sala, material ou professor.
- Duas solicitações simultâneas conflitantes devem produzir uma única reserva aceita.
- Recursos em manutenção não podem ser reservados.

#### Estados e auditoria

- Fluxo atual: reserva restrita nasce `PENDENTE`; reserva não restrita nasce `APROVADA`; a reserva aprovada pode ir para `EM_USO` e `CONCLUÍDA`.
- Estados de decisão/alternativos: `REJEITADA`, `CANCELADA` e `NÃO_COMPARECEU`. `SOLICITADA` permanece como estado legado no enum e em compatibilidade com fluxos anteriores.
- `ADMINISTRADOR` ou o `RESPONSAVEL` associado ao recurso pode decidir uma reserva restrita pendente pelo endpoint de status; autoaprovação é recusada.
- Reservas iniciadas não podem ser apagadas.
- Toda mudança de estado deve gerar registro de auditoria.

**Lacuna atual:** a regra que impede excluir reservas iniciadas ainda não está implementada, e não há persistência de auditoria. Reservas restritas pendentes que se sobrepõem podem ser cadastradas; a colisão é verificada ao aprovar. A política para esses pedidos pendentes precisa ser confirmada antes de declarar integridade completa.

## Estratégia obrigatória de qualidade

#### Testes automatizados

- Testes unitários com JUnit 5, incluindo testes parametrizados.
- Testes de integração, testes de API em caixa-preta e testes end-to-end.
- Teste automatizado de concorrência que comprove a proteção contra dupla reserva.
- Ao menos uma funcionalidade nova deve ser desenvolvida com TDD ou BDD e evidências da técnica.

#### Ambiente realista e integrações

- Usar banco de dados containerizado com Testcontainers.
- Usar WireMock para simular integrações externas.
- Mocks são permitidos em testes unitários isolados. Persistência e integrações críticas devem ser verificadas em ambiente realista, com Testcontainers ou WireMock.

#### CI, análise estática e requisitos não funcionais

- GitHub Actions deve executar verificações em pull requests.
- JaCoCo deve medir a cobertura; o projeto deve estar integrado ao SonarCloud.
- Avaliar desempenho com JMeter, além de autorização, validação de entrada e tratamento seguro de erros.

## Metas mensuráveis

- **80%** de cobertura de linhas.
- **70%** de cobertura de branches.
- **100%** dos requisitos críticos cobertos pela RTM (matriz de rastreabilidade).
- **0** bugs ou vulnerabilidades críticas.

Cobertura alta, isoladamente, não garante qualidade: testes triviais criados apenas para elevar o percentual não asseguram a pontuação máxima. As evidências devem demonstrar comportamento relevante e rastreável.

## Estado atual do projeto

**Fase:** backend parcial, não pronto para ser considerado concluído. A aplicação implementa CRUD básico de recursos, usuários e manutenções; criação, leitura, listagem, atualização pendente, cancelamento e transições de reservas; classificação de recursos restritos; e aprovação/rejeição. A API usa UUID, DTOs e OpenAPI. Reservas não restritas são aprovadas na criação; restritas ficam pendentes. Há validação de períodos, manutenção e conflitos de reservas aprovadas, além de locking pessimista por recurso.

**Segurança atual e limites:** rotas exigem JWT Bearer válido e a aplicação é stateless. O serviço verifica o perfil persistido e o responsável do recurso ao decidir reservas. A autorização de perfil para os demais endpoints CRUD, leitura/filtragem de reservas por proprietário e matriz completa de permissões não está implementada. O usuário é provisionado por operações de cadastro; não existe fluxo de login/emissão de token no backend. Isso impede ligar uma UI a um contrato de produção sem antes definir autenticação/provisionamento e acesso por endpoint.

**Verificação reproduzida em 2026-11-10:** `./mvnw -B verify` passou com 92 testes (JUnit, integração MockMvc e 16 invocações Cucumber), sem falhas ou erros. JaCoCo reportou 87% de linhas e 71% de branches, acima dos gates locais de 80%/70%. O GitHub Actions run `42859680966` no commit `eb2bced` passou por Java 21, Maven verify, SonarCloud e publicação do relatório JaCoCo. O shell local executou Java 25.0.4; runtime Java 21 é verificado pelo CI, não pelo ambiente local. A evidência de concorrência/persistência local usa H2, não Testcontainers.

**Backend ainda necessário antes do frontend funcional:**

1. Definir e aplicar a matriz de autorização por perfil em cada endpoint; restringir operações administrativas e escopo de leitura; resolver emissão/provisionamento e validação de identidade/JWT.
2. Formalizar e documentar o contrato da API, incluindo transições canônicas, autorização, campos/status, erros e consultas. Resolver a sobreposição de pedidos pendentes e estados extras/legados.
3. Implementar pesquisa de disponibilidade/filtros e completar os modelos/regras de professores, materiais, estoque e agendas previstos em RF-02/RF-03/RF-05.
4. Implementar auditoria durável, impedir exclusão de reservas iniciadas e fechar os requisitos de movimentação de materiais, notificações e relatórios (RF-09–RF-12).
5. Decidir banco de integração/produção e estratégia de migração; validar concorrência com Testcontainers. Executar JMeter após aprovar perfil de carga e limites; completar E2E/WireMock quando aplicável.
6. Atualizar a RTM até os requisitos críticos possuírem evidência executável adequada. A cobertura de código aprovada não significa que esses requisitos estejam implementados.

**Prontidão para frontend:** não iniciar a implementação funcional do frontend ainda. O CI e a base de reserva já permitem prototipar fluxos em paralelo, mas falta um contrato estável para autenticação, permissões, disponibilidade, materiais/professores, auditoria e erros. Recomenda-se começar o frontend após os itens 1–3 e a publicação do contrato correspondente; itens de qualidade operacional restantes podem continuar em paralelo conforme o plano acordado.

**Arquitetura/escopo:** banco de produção, implantação, integrações e ADRs continuam pendentes. O cron/treinamento incremental descrito em [arquitetura.md](arquitetura.md) não está nos RF/RNF validados e não deve ser assumido como backend necessário sem confirmação de escopo.

## Registro de evolução

A visão geral deve ser atualizada sempre que uma implementação for iniciada ou concluída, ou quando arquivos do projeto forem criados, alterados ou removidos. Registrar a mudança de modo conciso, mantendo o estado atual coerente com o repositório. Quando aplicável, relacionar a mudança ao requisito, decisão ou evidência correspondente.

| Data | Mudança | Arquivos ou área | Requisito, decisão ou evidência relacionada |
|---|---|---|---|
| 2026-10-19 | Criação do baseline de visão geral e personas; sem implementação de aplicação registrada. | `prd/visão-geral.md`, `prd/personas.md` | Especificação inicial fornecida para o projeto. |
| 2026-10-26 | Adição da especificação funcional detalhada, com regras de negócio, critérios de aceite, validações e próximos passos para continuidade. | `prd/requisitos-funcionais.md` | Requisitos funcionais obrigatórios e base de validação do projeto. |
| 2026-10-26 | Registro da validação das personas e dos requisitos funcionais; inclusão das regras críticas, estratégia obrigatória de qualidade e metas mensuráveis. | `prd/visão-geral.md`, `prd/personas.md`, `prd/requisitos-funcionais.md` | Revisão confirmada; cobertura de requisitos críticos, concorrência, auditoria, testes, CI e metas de qualidade. |
| 2026-11-01 | Inclusão dos requisitos não funcionais, com atributos de qualidade, evidências, restrições técnicas e parâmetros ainda pendentes. | `prd/requisitos-não-funcionais.md` | RNF-01 a RNF-09; metas de cobertura e rastreabilidade; desempenho sem limites quantitativos aprovados. |
| 2026-11-01 | Criação do baseline de análise arquitetural; registro da restrição do cron job diário usando somente dados novos, sem escolher arquitetura ou alterar RF/RNF. | `prd/arquitetura.md` | ASR-01 a ASR-07; fontes e prioridades indicadas no documento; escopo e detalhes da rotina diária permanecem como perguntas abertas. |
| 2026-11-02 | Inclusão da proposta complementar de arquitetura e do parecer independente do painel, com diagramas C4, análise ATAM, ADRs propostos, matriz de cobertura, riscos e experimentos; decisões e limiares permanecem pendentes. | `prd/arquitetura-proposta.md`, `prd/arquitetura.md`, `prd/painel-revisor-arquitetura.md` | Incrementalidade/ML permanece condicionada à confirmação de escopo e dados; divergências de checkpoint, auditoria, fonte e promoção registradas; nenhum risco formalmente aceito. |
| 2026-11-04 | Consolidação do baseline, da proposta complementar e do parecer independente em um único documento; remoção dos documentos separados. | `prd/arquitetura.md` | Conteúdo e decisões pendentes preservados em um documento de referência. |
| 2026-11-04 | Criação do plano TDD com inventário de regras, cenários por RF, regras críticas e RNF, rastreabilidade e registro de ambiguidades; início da fase red com projeto Maven/JUnit 5, API placeholder e 40 testes, sem lógica de negócio. | `prd/testes/plano-tdd.md`, `pom.xml`, `src/main/java/br/edu/organizacaorecursos/OrganizationResourceService.java`, `src/test/java/br/edu/organizacaorecursos/OrganizationResourceServiceRedTest.java`, `.gitignore` | `mvn -DskipTests package` compilou; `mvn test`: 40 testes, 40 falhas de asserção, 0 erros. R-TDD-33 (responsividade) e R-TDD-35 (documentação/API) sem teste por falta da UI/contrato; integração real, CI, desempenho e demais gates ainda pendentes. |
| 2026-11-07 | Implementação da base inicial de persistência: Maven configurado com Java 21/Spring Boot 3.2.4 e dependências Web, Data JPA, H2, Validation e Lombok; criação da aplicação, pacotes `domain`, `repository`, `service`, `controller` e `dto`, entidades `Recurso`/`Reserva`, repositórios e teste de persistência H2. Remoção do placeholder e da antiga suíte red de `src`; plano TDD preservado. Namespace alinhado para `br.edu.organizacaorecursos`. | `pom.xml`, `src/main/java/br/edu/organizacaorecursos/`, `src/main/resources/application.properties`, `src/test/java/br/edu/organizacaorecursos/`, remoção de `src/main/java/br/edu/organizacaorecursos/OrganizationResourceService.java` e `src/test/java/br/edu/organizacaorecursos/OrganizationResourceServiceRedTest.java` | Story 1.1; `mvn clean verify`: 1 teste executado, 0 falhas/erros. Evidência cobre inicialização e persistência básica em H2, não concorrência, regras do plano TDD, Testcontainers ou runtime Java 21. |
| 2026-11-07 | Fase 2 da API de reservas: DTOs de entrada/saída com validação, `ReservaService` transacional, query de sobreposição, bloqueio pessimista por recurso, exceções com respostas 400/404 e endpoint `POST /api/reservas`. A regra fornecida permite períodos adjacentes. | `src/main/java/br/edu/organizacaorecursos/dto/`, `exception/`, `service/ReservaService.java`, `controller/ReservaController.java`, `repository/RecursoRepository.java`, `repository/ReservaRepository.java`, `src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java` | RF-04, RF-05, RF-06, RF-13; critérios da story de DTO e regras de negócio. `mvn clean verify`: 11 testes, 0 falhas/erros, incluindo uma corrida concorrente em H2; Testcontainers e runtime Java 21 não verificados. |
| 2026-11-07 | Fase 3 da API REST: criação/listagem de recursos em `/recursos`, criação de reservas em `/reservas` (mantido alias `/api/reservas`) e padronização do `@RestControllerAdvice` para erros de regra e validação. | `src/main/java/br/edu/organizacaorecursos/controller/RecursoController.java`, `controller/ReservaController.java`, `exception/ApiExceptionHandler.java`, `src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java` | Story aprovada “Exposição da API REST e Tratamento de Erros Global”; `mvn clean verify`: 13 testes, 0 falhas/erros. |
| 2026-11-07 | Expansão do ciclo de vida da reserva: busca por UUID, exclusão, respostas 404 para reservas inexistentes e 400 para identificadores malformados; inclusão da dependência Springdoc e interface Swagger UI com metadados OpenAPI. | `pom.xml`, `src/main/java/br/edu/organizacaorecursos/config/OpenApiConfig.java`, `controller/ReservaController.java`, `service/ReservaService.java`, `exception/ApiExceptionHandler.java`, `src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java` | Story aprovada “Expansão do CRUD e Documentação Swagger (OpenAPI)”; `mvn clean verify`: 19 testes, 0 falhas/erros, incluindo GET/DELETE, não encontrados, UUID inválido e geração de `/v3/api-docs`. |
| 2026-11-08 | Evolução do domínio com `Usuario` e `Manutencao`, repositórios JPA e consulta de sobreposição do período de manutenção. `Reserva` agora referencia `Usuario`; o request de criação passou a receber `usuarioId` e o serviço resolve o usuário antes de persistir. | `src/main/java/br/edu/organizacaorecursos/domain/Usuario.java`, `domain/Manutencao.java`, `domain/Reserva.java`, `repository/UsuarioRepository.java`, `repository/ManutencaoRepository.java`, `dto/ReservaRequestDTO.java`, `service/ReservaService.java`, testes de persistência/API | Story aprovada “Evolução do Domínio (Usuários e Manutenções)”; `mvn clean verify`: 19 testes, 0 falhas/erros. A query de manutenção foi verificada; a validação que bloqueia a reserva durante manutenção permanece fora desta implementação, conforme a sequência indicada na solicitação. |
| 2026-11-08 | Integração do usuário e validação efetiva de manutenção na criação da reserva; resposta padronizada com `usuarioId`/`nomeUsuario`; criação de endpoints POST/GET para usuários e manutenções. | `src/main/java/br/edu/organizacaorecursos/controller/UsuarioController.java`, `controller/ManutencaoController.java`, `dto/ReservaResponseDTO.java`, `service/ReservaService.java`, `src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java` | Story aprovada “Integração de Usuários e Validação de Manutenções”; `mvn clean verify`: 22 testes, 0 falhas/erros, cobrindo usuário inexistente, bloqueio durante manutenção e endpoints auxiliares. |
| 2026-11-08 | Inclusão de testes unitários isolados do `ReservaService` com JUnit 5 e Mockito: datas inválidas, manutenção ativa, choque de horário e criação válida com mapeamento do DTO. | `src/test/java/br/edu/organizacaorecursos/service/ReservaServiceTest.java` | Story aprovada “Testes Automatizados da Regra de Negócio”; execução direcionada: 4 testes, 0 falhas/erros; `mvn clean verify`: 26 testes, 0 falhas/erros. |
| 2026-11-09 | Ciclo BDD/TDD para aprovação de recursos restritos, autoaprovação e ator não autorizado; implementação das transições de reserva, rejeição de reaprovação, regras de manutenção nas fronteiras temporais e injeção de `Clock`. | `src/main/java/br/edu/organizacaorecursos/service/ReservaService.java`, `ReservaController.java`, `StatusReserva.java`, `ClockConfig.java`; `src/test/java/`, `src/test/resources/features/aprovacao-bloqueio.feature`, `docs/testes/evidencia-tdd.md` | Commits `5980602`, `8594f9d`, `a2dd4f7`, `30338c0`, `16ac2fb`, `8c2dbb1`, `2898c0d`; ciclos e resultados históricos estão registrados em `docs/testes/evidencia-tdd.md`. |
| 2026-11-10 | Integração do Maven Wrapper/Cucumber/CI; autenticação JWT stateless e política CSRF limitada a requests Bearer; incremento de testes e correções da análise SonarCloud. | `.github/workflows/ci.yml`, `pom.xml`, `.mvn/`, `SecurityConfig.java`, `SecurityConfigTest.java`, `ApiExceptionHandlerTest.java`, `prd/rtm.md`, `README.md` | Commits `76559b1`, `dc61774`, `884c38d`, `92abc6e`, `102e283`; workflow passou em `42859680966`, incluindo SonarCloud. |
| 2026-11-10 | Implementação US-05: perfis, indicador `restrito`, criação restrita `PENDENTE`, aprovação/rejeição por administrador/responsável, endpoint PATCH de status, validação de conflito na aprovação e testes de serviço/API/BDD. | `Perfil.java`, `Usuario.java`, DTOs, `ReservaService.java`, `ReservaController.java`, `ReservaServiceTest.java`, `ReservaApiIntegrationTest.java`, `ReservaApiSteps.java`, feature de aprovação | Commit `eb2bced`; `./mvnw -B verify`: 92 testes verdes; JaCoCo 87% linhas/71% branches; CI/SonarCloud verdes. As lacunas de RF não cobertas permanecem no RTM. |

## Próximos passos

1. **Fechar identidade e contrato:** definir emissor/provisionamento de JWT, publicar matriz de permissões de todos os endpoints e filtrar consultas por proprietário/perfil.
2. **Completar o domínio principal:** implementar pesquisa/disponibilidade e fechar os modelos e regras de professores, materiais, estoque e agendas conforme os RFs.
3. **Completar integridade operacional:** implementar auditoria persistida e proteção contra exclusão de reservas iniciadas; decidir regra para pedidos pendentes sobrepostos.
4. **Decidir persistência de produção:** registrar banco/migrações e validar concorrência com Testcontainers no engine escolhido.
5. **Finalizar evidências operacionais:** fechar lacunas da RTM crítica, definir limites de carga aprovados e executar JMeter; manter CI/SonarCloud verde.
6. **Iniciar o frontend funcional** somente após os itens 1–2 e a publicação do contrato correspondente. A cobertura de código/CI atual não substitui esses pré-requisitos.

## Histórico de atualização

- **2026-10-19:** criação do documento com base exclusivamente na especificação inicial e no estado visível do repositório.
- **2026-10-26:** atualização da visão geral para incluir a documentação de requisitos funcionais adicionada em `prd/requisitos-funcionais.md`.
- **2026-10-26:** registro da validação das personas e dos requisitos funcionais e inclusão das regras críticas e metas de qualidade acordadas.
