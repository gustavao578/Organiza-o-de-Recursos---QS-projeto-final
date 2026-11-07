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

- Linguagem prevista: Java 21.
- Framework previsto: Spring Boot 3.x.
- Entrega prevista: semana 46.
- O projeto será desenvolvido por equipes.

A arquitetura, o banco de dados, os mecanismos de autenticação e as integrações ainda precisam de definição técnica. A estratégia e as ferramentas de qualidade estão definidas abaixo.

## Regras críticas

#### Tempo e concorrência

- O término de uma reserva deve ser posterior ao início.
- Reservas do mesmo recurso não podem se sobrepor, seja sala, material ou professor.
- Duas solicitações simultâneas conflitantes devem produzir uma única reserva aceita.
- Recursos em manutenção não podem ser reservados.

#### Estados e auditoria

- Fluxo principal: `SOLICITADA → APROVADA → EM_USO → CONCLUÍDA`.
- Estados alternativos: `REJEITADA`, `CANCELADA` e `NÃO_COMPARECEU`.
- Somente responsáveis podem aprovar reservas de recursos restritos.
- Reservas iniciadas não podem ser apagadas.
- Toda mudança de estado deve gerar registro de auditoria.

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

**Fase:** requisitos funcionais e personas revisados e validados; requisitos não funcionais registrados; baseline arquitetural, proposta complementar e parecer ATAM/painel elaborados. O plano TDD cobre os RFs, as regras críticas e os RNFs, com ambiguidades identificadas. A base inicial de persistência foi implementada em Maven com Java 21 e Spring Boot 3.2.4: configuração H2 em memória, entidades JPA `Recurso` e `Reserva`, repositórios e teste de persistência. A aplicação ainda não contém endpoints, regras de negócio, autenticação ou UI; os respectivos casos permanecem sem implementação.

**Baseline:** a especificação define o propósito, os perfis, os requisitos funcionais validados, a stack prevista e as evidências de qualidade exigidas. O contrato placeholder `OrganizationResourceService` e sua suíte red foram removidos de `src`; o inventário de casos permanece em `prd/testes/plano-tdd.md`. A nova suíte contém um teste que inicia o contexto Spring e persiste/recarrega recurso e reserva em H2, verificando os IDs UUID e a associação entre elas. `mvn clean verify` passou com 1 teste, sem falhas ou erros. A compilação usa release 21, mas o ambiente executa Java 25; a execução com runtime Java 21 ainda não foi verificada. O teste atual não comprova concorrência, integração com banco containerizado ou as demais regras de negócio.

**Próximas definições técnicas:** resolver as perguntas e lacunas de [arquitetura.md](arquitetura.md); confirmar se o job de ML integra o escopo e, se sim, definir fonte e semântica de “dados novos”, watermark/checkpoint, avaliação, gates e governança; decidir a persistência de integração/produção e a estratégia de concorrência de reservas; implementar regras de negócio e validar os contratos do plano TDD; criar e documentar os endpoints da API; e configurar CI e ferramentas de qualidade. Os casos de responsividade e consistência documental permanecem sem teste até existirem UI e contrato/API. Os ADRs estão propostos e aguardam decisão. Essas definições devem implementar os requisitos validados, sem alterá-los.

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

## Próximos passos

1. **Concluir arquitetura e dados:** resolver as perguntas abertas de [arquitetura.md](arquitetura.md) e decidir arquitetura, persistência e contratos necessários para implementar os requisitos validados, sem presumir decisões ainda não aprovadas.
2. **Implementar as regras de negócio:** definir/confirmar contratos e implementar os comportamentos funcionais a partir dos requisitos validados e dos casos do plano TDD, iterando no ciclo red-green-refactor.
3. **Completar a estratégia de teste:** adicionar integração com persistência/Testcontainers, testes de API e E2E; quando houver UI e contrato público, cobrir responsividade (R-TDD-33) e consistência da documentação/API (R-TDD-35).
4. **Planejar a implementação da qualidade:** configurar CI, cobertura, análise estática, ambientes de teste e medição de desempenho conforme a estratégia obrigatória.
5. **Construir a RTM:** vincular todos os requisitos críticos a implementação e evidências de teste, atingindo 100% de cobertura na matriz.
6. **Verificar as metas de qualidade:** medir cobertura de linhas e branches, executar os testes de concorrência e desempenho e confirmar ausência de bugs ou vulnerabilidades críticas.

## Histórico de atualização

- **2026-10-19:** criação do documento com base exclusivamente na especificação inicial e no estado visível do repositório.
- **2026-10-26:** atualização da visão geral para incluir a documentação de requisitos funcionais adicionada em `prd/requisitos-funcionais.md`.
- **2026-10-26:** registro da validação das personas e dos requisitos funcionais e inclusão das regras críticas e metas de qualidade acordadas.
