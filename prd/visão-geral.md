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
- [arquitetura.md](arquitetura.md) — baseline arquitetural inicial com contexto, personas, requisitos arquiteturalmente significativos, atributos de qualidade, drivers priorizados, riscos e perguntas abertas. Registra a restrição explícita desta solicitação de executar diariamente ao final do dia um cron job de treinamento ou retreinamento usando somente dados novos; seu escopo e detalhes operacionais permanecem pendentes.

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

**Fase:** requisitos funcionais e personas revisados e validados; requisitos não funcionais registrados; baseline de análise arquitetural inicial elaborado. Decisões detalhadas de arquitetura e implementação permanecem pendentes.

**Baseline:** a especificação define o propósito, os perfis, os requisitos funcionais validados, a stack prevista e as evidências de qualidade exigidas. No momento da criação deste documento, o README do repositório contém apenas o título do projeto e não há implementação da aplicação registrada na estrutura visível do repositório.

**Próximas definições técnicas:** arquitetura e persistência; estratégia de implementação das regras de concorrência; configuração da CI e integração das ferramentas de qualidade. Essas definições devem implementar os requisitos validados, sem alterá-los.

## Registro de evolução

A visão geral deve ser atualizada sempre que uma implementação for iniciada ou concluída, ou quando arquivos do projeto forem criados, alterados ou removidos. Registrar a mudança de modo conciso, mantendo o estado atual coerente com o repositório. Quando aplicável, relacionar a mudança ao requisito, decisão ou evidência correspondente.

| Data | Mudança | Arquivos ou área | Requisito, decisão ou evidência relacionada |
|---|---|---|---|
| 2026-10-19 | Criação do baseline de visão geral e personas; sem implementação de aplicação registrada. | `prd/visão-geral.md`, `prd/personas.md` | Especificação inicial fornecida para o projeto. |
| 2026-10-26 | Adição da especificação funcional detalhada, com regras de negócio, critérios de aceite, validações e próximos passos para continuidade. | `prd/requisitos-funcionais.md` | Requisitos funcionais obrigatórios e base de validação do projeto. |
| 2026-10-26 | Registro da validação das personas e dos requisitos funcionais; inclusão das regras críticas, estratégia obrigatória de qualidade e metas mensuráveis. | `prd/visão-geral.md`, `prd/personas.md`, `prd/requisitos-funcionais.md` | Revisão confirmada; cobertura de requisitos críticos, concorrência, auditoria, testes, CI e metas de qualidade. |
| 2026-11-01 | Inclusão dos requisitos não funcionais, com atributos de qualidade, evidências, restrições técnicas e parâmetros ainda pendentes. | `prd/requisitos-não-funcionais.md` | RNF-01 a RNF-09; metas de cobertura e rastreabilidade; desempenho sem limites quantitativos aprovados. |
| 2026-11-01 | Criação do baseline de análise arquitetural; registro da restrição do cron job diário usando somente dados novos, sem escolher arquitetura ou alterar RF/RNF. | `prd/arquitetura.md` | ASR-01 a ASR-07; fontes e prioridades indicadas no documento; escopo e detalhes da rotina diária permanecem como perguntas abertas. |

## Próximos passos

1. **Concluir arquitetura e dados:** resolver as perguntas abertas de [arquitetura.md](arquitetura.md) e decidir arquitetura, persistência e contratos necessários para implementar os requisitos validados, sem presumir decisões ainda não aprovadas.
2. **Planejar a implementação da qualidade:** configurar CI, cobertura, análise estática, ambientes de teste e medição de desempenho conforme a estratégia obrigatória.
3. **Construir a RTM:** vincular todos os requisitos críticos a implementação e evidências de teste, atingindo 100% de cobertura na matriz.
4. **Implementar incrementalmente:** associar cada incremento aos requisitos e testes correspondentes e manter este documento alinhado até a entrega da semana 46.
5. **Verificar as metas de qualidade:** medir cobertura de linhas e branches, executar os testes de concorrência e desempenho e confirmar ausência de bugs ou vulnerabilidades críticas.

## Histórico de atualização

- **2026-10-19:** criação do documento com base exclusivamente na especificação inicial e no estado visível do repositório.
- **2026-10-26:** atualização da visão geral para incluir a documentação de requisitos funcionais adicionada em `prd/requisitos-funcionais.md`.
- **2026-10-26:** registro da validação das personas e dos requisitos funcionais e inclusão das regras críticas e metas de qualidade acordadas.
