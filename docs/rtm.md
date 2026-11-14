# Matriz de Rastreabilidade de Requisitos

**Atualizada em:** 2026-11-14
**Base de código:** commit `6106cc5` na branch `fix/verify-profile-isolation`. A execução local de `./mvnw -B verify` passou com 235 testes, 0 falhas/erros e JaCoCo gerado; Java CI e Quality Gate SonarCloud também passaram nesse commit. A revisão por pares humana continua pendente.

Esta matriz distingue requisito documentado de requisito implementado e validado. **COBERTO** significa evidência automatizada no escopo descrito; **PARCIAL** indica lacunas de funcionalidade, ambiente ou critério; **BLOQUEADO** significa que a funcionalidade ou decisão não está disponível. Os status não são inferidos do percentual global JaCoCo.

## Requisitos funcionais

| ID | Requisito | Evidência principal | Status e limite conhecido |
|---|---|---|---|
| RF-01 | Autenticação e autorização por perfil | `SecurityConfigTest`, `MatrizAutorizacaoTest`, `AuthLoginIntegrationTest` | **PARCIAL** — JWT Bearer, autorização por perfil e login de demonstração são exercitados; login por identificador não autentica a posse do identificador e permanece restrito a `local`/`demo`. |
| RF-02 | Cadastro e consulta de salas, professores e materiais | `ReservaApiIntegrationTest`, `MatrizAutorizacaoTest` | **PARCIAL** — CRUD de recursos e usuários existe; modelagem completa de professores/materiais e todos os critérios de filtro não existem. |
| RF-03 | Pesquisa por tipo, capacidade, localização, competência e disponibilidade | Nenhum endpoint de pesquisa seletiva demonstrado | **BLOQUEADO** — `GET /reservas` é uma listagem, não pesquisa de disponibilidade por recurso e intervalo. |
| RF-04 | Criar, alterar e cancelar reservas | `ReservaApiIntegrationTest`, `ReservaServiceTest` | **PARCIAL** — fluxos principais e restrições de estado são testados; regras de edição e justificativa fora dos cenários implementados continuam pendentes. |
| RF-05 | Detectar sobreposição, inclusive na agenda de professor | `ReservaApiIntegrationTest.rejeitaPeriodosSobrepostosParaMesmoRecurso`, `permiteReservasAdjacentes`; `ReservaService` | **PARCIAL** — conflito de recurso e agenda vinculada são verificados; agenda/modelo de professor, materiais e busca de disponibilidade não estão completos. |
| RF-06 | Impedir dupla reserva concorrente | `ReservaConcorrenciaPostgresIntegrationTest`; testes de conflito | **PARCIAL** — há teste concorrente e de persistência; a validação de carga não substitui a garantia em ambiente PostgreSQL de produção. |
| RF-07 | Aprovação obrigatória de recursos restritos | Feature `aprovacao-bloqueio.feature`, `ReservaApiIntegrationTest` | **COBERTO** — criação pendente, decisão e autorização de responsável/administrador têm evidência automatizada. |
| RF-08 | Bloquear reservas durante manutenção | Feature `aprovacao-bloqueio.feature`, `ManutencaoServiceTest` | **PARCIAL** — bloqueio de criação é testado; comportamento completo de busca/calendário e reservas já existentes não está coberto. |
| RF-09 | Retirada e devolução de materiais/equipamentos | Sem modelo/fluxo correspondente | **BLOQUEADO** — não implementado. |
| RF-10 | Histórico auditável de mudanças | `ReservaAuditoriaPostgresIntegrationTest`, `ReservaApiIntegrationTest` | **PARCIAL** — auditoria transacional de mudanças de estado de reservas existe; alterações de outros agregados e política de retenção não estão cobertas. |
| RF-11 | Notificação simulada ou integração externa | Nenhum adaptador/fluxo encontrado | **BLOQUEADO** — canal, eventos e destinatários não definidos/implementados. |
| RF-12 | Relatório de utilização, carga horária e conflitos evitados | `RelatorioUtilizacaoApiTest`, `/relatorios/utilizacao` | **PARCIAL** — agrega horas e recusas por conflito no período; exportação, visualização e demais dimensões do requisito não estão implementadas. |
| RF-13 | Interface responsiva e mensagens compreensíveis | SPA em `src/main/resources/static/`; testes de API/Node | **PARCIAL** — fluxos e mensagens da SPA existem; validação visual, acessibilidade e viewports não foram comprovadas nesta execução. |
| RF-14 | Documentar API ou fluxos públicos | OpenAPI em `/v3/api-docs`, Swagger UI; `publicaDocumentacaoOpenApiEEndpointsDeReserva` | **PARCIAL** — contrato OpenAPI é publicado; cobertura completa de erros/exemplos e sincronismo não estão verificados. |

## Requisitos não funcionais

| ID | Requisito | Evidência principal | Status e limite conhecido |
|---|---|---|---|
| RNF-01 | Segurança e autorização | `SecurityConfigTest`, `MatrizAutorizacaoTest`; relatório SonarCloud requerido pela CI | **PARCIAL** — controles de JWT/perfil testados; não há Quality Gate atual para esta revisão nem auditoria de segurança completa. |
| RNF-02 | Integridade e concorrência | Testes de conflitos e integração PostgreSQL/Testcontainers | **PARCIAL** — regras e transações têm testes; carga PostgreSQL em ambiente representativo não foi executada. |
| RNF-03 | Desempenho mensurável | [Plano](performance/plano-01.md), [resultado histórico](performance/resultado-01.md), [rodada atual](performance/resultado-02.md) e `perf/reservas.jmx` | **PARCIAL** — as rodadas locais são inconclusivas para produção; `GET /reservas` é proxy, H2 não representa PostgreSQL e os limites propostos aguardam aprovação. |
| RNF-04 | Estratégia e evidência de testes | [Plano de testes](testes/plano-testes.md), [relatório](testes/relatorio-testes.md), JUnit/Cucumber/Testcontainers | **PARCIAL** — suite automatizada passa localmente; E2E externo, acessibilidade e WireMock não estão demonstrados. |
| RNF-05 | Cobertura, RTM e análise de qualidade | [JaCoCo](qualidade/relatorio-jacoco.md), matriz neste documento e CI | **PARCIAL** — metas locais de linha/branch passam; Quality Gate atual e validação independente da RTM estão pendentes. |
| RNF-06 | Integração contínua | `.github/workflows/ci.yml` | **PARCIAL** — workflow está configurado; execução verde na revisão publicada ainda não foi comprovada. |
| RNF-07 | Auditoria e rastreabilidade | `ReservaAuditoriaPostgresIntegrationTest` | **PARCIAL** — mudanças de estado de reserva são registradas; outros agregados e retenção não estão cobertos. |
| RNF-08 | Responsividade e clareza da interface | SPA e testes estáticos/Node; inspeção visual pendente | **PARCIAL** — não há evidência visual ou de acessibilidade nesta validação. |
| RNF-09 | Documentação de contratos e fluxos | OpenAPI/Swagger, README e [diagramas](arquitetura/diagramas.md) | **PARCIAL** — artefatos existem; revisão de completude e sincronismo permanece pendente. |

## Regras críticas

Os IDs RC são editoriais e referenciam as regras críticas da [visão geral](../prd/visão-geral.md).

| ID | Regra | Evidência | Status |
|---|---|---|---|
| RC-01 | Término posterior ao início | `ReservaRequestDTOTest`, `ReservaServiceTest` | **COBERTO** |
| RC-02 | Reservas conflitantes do mesmo recurso são rejeitadas, inclusive quando a nova solicitação seria `PENDENTE` | `ReservaApiIntegrationTest.rejeitaSobreposicaoDePendentesEmRecursoRestrito`; verificação antes da atribuição do status em `ReservaService.criarReserva` | **COBERTO**. A documentação antiga de mutação que marcava a regra como lacuna foi superada; ver [DEF-002](defeitos/DEF-002-sobreposicao-pendentes.md). |
| RC-03 | Solicitações simultâneas conflitantes não geram múltiplas reservas aceitas | `ReservaConcorrenciaPostgresIntegrationTest` | **COBERTO** no teste PostgreSQL automatizado; carga/staging de produção não avaliada. |
| RC-04 | Recurso em manutenção não pode ser reservado | `ManutencaoServiceTest`, `ReservaApiIntegrationTest`, feature de manutenção | **COBERTO** para criação de reserva. |
| RC-05 | Transições de estado respeitam o ciclo permitido | Feature `aprovacao-bloqueio.feature`, `ReservaServiceTest` | **COBERTO** nos estados e transições exercitados. |
| RC-06 | Decisão de reserva exige administrador/responsável autorizado | `MatrizAutorizacaoTest`, `ReservaApiIntegrationTest` | **COBERTO** nos endpoints/atores testados. |
| RC-07 | Reserva iniciada não pode ser apagada | `ReservaServiceTest.impedeExcluirReservaQueJaFoiIniciada` | **COBERTO** |
| RC-08 | Mudanças de estado são auditadas atomicamente | `ReservaAuditoriaPostgresIntegrationTest` | **COBERTO** para transições de estado de reserva; auditoria de outras entidades permanece parcial em RF-10/RNF-07. |

## Evidência e atualização

- Execução local: `./mvnw -B verify`, 235 testes, 0 falhas, 0 erros, 0 ignorados; JaCoCo: 93,14% de linhas, 76,77% de branches e 92,21% de instruções.
- A execução local não substitui CI; para `6106cc5`, Java CI e Quality Gate SonarCloud passaram. A revisão por pares humana continua pendente.
- O documento histórico [prd/rtm.md](../prd/rtm.md) foi substituído por esta matriz como fonte atual; auditorias de mutação anteriores continuam preservadas como evidência histórica, não como estado vigente.
- Atualize esta RTM quando requisitos ou evidências mudarem. Registre teste, ambiente, comando/run e limitações; não use cobertura agregada para promover status.
