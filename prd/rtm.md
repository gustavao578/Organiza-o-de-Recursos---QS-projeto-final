# Matriz de Rastreabilidade de Requisitos (RTM)

**Data da fotografia:** 2026-11-08
**Fontes:** [Requisitos funcionais](requisitos-funcionais.md), [Requisitos não funcionais](requisitos-não-funcionais.md), [Visão geral](visão-geral.md) e [Plano TDD](testes/plano-tdd.md).
**Baseline de implementação:** branch `ci/coverage-sonar-quality-gates`, commit `f291354` (PR [#1](https://github.com/gustavao578/Organiza-o-de-Recursos---QS-projeto-final/pull/1)); ainda não integrado à `main`.

## 1. Objetivo e convenções

Esta matriz liga cada requisito oficial RF/RNF e cada regra crítica da visão geral às evidências disponíveis e ao estado observado. Ela distingue rastreabilidade documental de cobertura comportamental: listar um requisito na RTM não significa que esteja implementado ou validado.

| Status | Significado |
|---|---|
| **COBERTO** | O escopo implementado descrito no requisito tem evidência de teste adequada; limitações residuais são explicitadas. |
| **PARCIAL** | Há evidência de parte do requisito, mas critérios, categorias ou ambientes relevantes continuam sem cobertura. |
| **BLOQUEADO** | A funcionalidade não existe ou faltam decisões, ferramentas ou critérios para testar o requisito. |

Os nomes de testes referem-se às classes em `src/test/java/br/edu/organizacaorecursos/`. Os identificadores `RC-01` a `RC-08` são rótulos editoriais desta RTM para as regras da seção **Regras críticas** da visão geral; não substituem IDs oficiais.

## 2. Requisitos funcionais

| ID | Requisito | Evidência/testes ligados | Status e lacuna atual |
|---|---|---|---|
| RF-01 | Autenticação e autorização por perfil | Nenhuma evidência de autenticação/autorização. Validação de DTO não comprova controle de acesso. | **BLOQUEADO** — autenticação, matriz de permissões e auditoria de tentativas não implementadas. |
| RF-02 | Cadastro e consulta de salas, professores e materiais | `criaEListaRecursosPelosEndpointsRest`, `criaEListaUsuariosPelaApi`, `rejeitaRecursoSemNomeOuTipoObrigatorios`, `listaManutencoesPelaApi`. | **PARCIAL** — CRUD básico de Recurso/Usuário e manutenção; modelo de professores, materiais, atributos completos, atualização e regra de duplicidade não cobertos/implementados. |
| RF-03 | Pesquisa por tipo, capacidade, localização, competência e disponibilidade | Nenhum teste de pesquisa com filtros. | **BLOQUEADO** — consulta de disponibilidade e filtros de domínio não implementados. |
| RF-04 | Criação, alteração e cancelamento de reservas | `criaReservaEDevolveDtoSemExporEntidades`, `criaReservaQuandoDadosSaoValidosERecursoEstaDisponivel`, `rejeitaReservaQuandoDataObrigatoriaEstaAusente`, `rejeitaDataFimIgualAoInicio`, `rejeitaDataFimAnteriorAoInicio`, `rejeitaUsuarioInexistente`. | **PARCIAL** — criação e validações cobertas; alteração, cancelamento, autorização e justificativa não implementados. |
| RF-05 | Detecção de sobreposição, incluindo agenda de professor | `rejeitaPeriodosSobrepostosParaMesmoRecurso`, `permiteReservasAdjacentes`, `informaRecursoEIntervaloQuandoExisteConflitoDeReserva`, `impedeReservaDuranteManutencao`. | **PARCIAL** — conflito do Recurso atual e manutenção cobertos; agenda de professor, materiais/equipamentos e disponibilidade pesquisável ausentes. |
| RF-06 | Proteção contra dupla reserva simultânea | `aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas`; integração com H2. | **PARCIAL** — cenário local concorrente aceita exatamente uma; Testcontainers e engine de produção não verificados. |
| RF-07 | Aprovação obrigatória de recursos restritos | Nenhuma evidência executável. | **BLOQUEADO** — catálogo de recursos restritos, responsável, estados e fluxo de aprovação não implementados/definidos. |
| RF-08 | Bloqueio de recursos em manutenção | `rejeitaReservaQuandoRecursoEstaEmManutencao`, `impedeReservaDuranteManutencao`, `rejeitaManutencaoSemDatasOuDescricaoObrigatorias`, `rejeitaManutencaoSemIdentificadorDeRecurso`, `retornaNotFoundAoCriarManutencaoParaRecursoInexistente`. | **PARCIAL** — cadastro e bloqueio no fluxo de criação cobertos; calendário/busca, estados de manutenção, manutenção passada e reservas já afetadas não implementados/definidos. |
| RF-09 | Retirada e devolução de materiais/equipamentos | Nenhuma evidência executável. | **BLOQUEADO** — modelo, estoque e fluxo de empréstimo não implementados. |
| RF-10 | Histórico auditável de mudanças | Nenhuma evidência executável de auditoria. | **BLOQUEADO** — eventos, ator, antes/depois, retenção e consulta autorizada não implementados. |
| RF-11 | Notificação simulada ou integração externa | Nenhuma evidência executável. | **BLOQUEADO** — canal, eventos, destinatários e comportamento de falha pendentes. |
| RF-12 | Relatórios de utilização, carga horária e conflitos evitados | Nenhuma evidência executável. | **BLOQUEADO** — fórmulas, autorização, filtros e exportação não implementados/definidos. |
| RF-13 | Interface responsiva e mensagens compreensíveis | `validaCamposObrigatoriosDoRequest`, `informaRecursoEIntervaloQuandoExisteConflitoDeReserva`, `retornaBadRequestQuandoRecursoNaoExiste`, `rejeitaUsuarioInexistente`, `rejeitaDataDeInicioNoPassado`. | **PARCIAL** — respostas HTTP e erros de API têm evidência; não há UI responsiva, nem critérios de viewport/usabilidade validados. |
| RF-14 | Documentação de API ou fluxos públicos | `publicaDocumentacaoOpenApiEEndpointsDeReserva`. | **PARCIAL** — OpenAPI/Swagger básico validado; autenticação, erros/exemplos completos e sincronismo contínuo não verificados. |

## 3. Requisitos não funcionais

| ID | Requisito | Evidência/testes ligados | Status e lacuna atual |
|---|---|---|---|
| RNF-01 | Segurança e autorização | Validação de entrada e tratamento de erros na suíte API; nenhum teste de autenticação/perfil. | **PARCIAL** — validações presentes, mas autenticação, autorização, auditoria de negativas e scan de segurança ausentes. |
| RNF-02 | Integridade e concorrência | `rejeitaPeriodosSobrepostosParaMesmoRecurso`, `aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas`; H2 local. | **PARCIAL** — conflitos e corrida exercitados; atomicidade no engine de destino/Testcontainers pendente. |
| RNF-03 | Desempenho mensurável com JMeter | Nenhum relatório JMeter. | **BLOQUEADO** — ferramenta, perfil de carga e limites quantitativos não fornecidos/aprovados. |
| RNF-04 | Estratégia e evidência de testes | 39 execuções: 12 unitárias/validação, 26 de API e 1 de persistência; JUnit 5 e parametrização temporal com `Clock.fixed`. | **PARCIAL** — Testcontainers, E2E, WireMock e evidência formal TDD/BDD permanecem pendentes. |
| RNF-05 | Cobertura, RTM e análise de qualidade | JaCoCo local: 101/105 linhas (96,2%) e 22/26 branches (84,6%); gates globais 80%/70% passaram. Artifact JaCoCo publicado no PR #1. | **PARCIAL** — metas locais de cobertura atendidas; RTM ainda não comprova testes para todos os requisitos críticos. SonarCloud não iniciou: check falhou antes do scanner por ausência de `SONAR_TOKEN`, `SONAR_ORGANIZATION` e `SONAR_PROJECT_KEY`; sem relatório, issues ou Quality Gate. |
| RNF-06 | Integração contínua em pull requests | PR #1 executou checks de push e PR. Maven verify e upload `jacoco-report` passaram; etapa Sonar falhou na validação de configuração. | **PARCIAL** — execução remota existe, mas o workflow não conclui verde até configurar SonarCloud; proteção de branch não verificada. |
| RNF-07 | Auditoria e rastreabilidade de alterações | Nenhuma evidência executável de estado/auditoria. | **BLOQUEADO** — estados, auditoria, identidade e retenção não implementados/definidos. |
| RNF-08 | Responsividade e clareza da interface | Testes de mensagens/validação na API. | **PARCIAL** — erros de API cobertos parcialmente; UI e testes desktop/tablet/mobile ausentes. |
| RNF-09 | Documentação dos contratos e fluxos | `publicaDocumentacaoOpenApiEEndpointsDeReserva`. | **PARCIAL** — documentação OpenAPI presente; cobertura e sincronismo dos fluxos/erros completos não verificados. |

## 4. Regras críticas da visão geral

| ID editorial | Regra crítica | Evidência/testes ligados | Status e lacuna atual |
|---|---|---|---|
| RC-01 | Término da reserva posterior ao início. | `rejeitaDataFimIgualAoInicio`, `rejeitaDataFimAnteriorAoInicio`, `ReservaRequestDTOTest.validaDatasNaFronteiraDoRelogioInjetado`. | **COBERTO** para validação temporal declarada: campos exercitados em -1/0/+1 segundo com relógio fixo; duração mínima/máxima não definida. |
| RC-02 | Reservas do mesmo recurso não se sobrepõem (sala/material/professor). | `rejeitaPeriodosSobrepostosParaMesmoRecurso`, `permiteReservasAdjacentes`. | **PARCIAL** — Recurso atualmente modelado é testado; professor/material e respectivas agendas/estoque não existem no domínio. |
| RC-03 | Solicitações simultâneas conflitantes produzem uma única reserva aceita. | `aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas`. | **PARCIAL** — evidência em H2; engine de destino não validado. |
| RC-04 | Recurso em manutenção não pode ser reservado. | `rejeitaReservaQuandoRecursoEstaEmManutencao`, `impedeReservaDuranteManutencao`. | **PARCIAL** — criação bloqueada; busca/calendário e bordas temporais de manutenção não cobertos. |
| RC-05 | Fluxo principal `SOLICITADA → APROVADA → EM_USO → CONCLUÍDA` e estados alternativos declarados. | Nenhum teste de transição. | **BLOQUEADO** — modelo e pré-condições de estado ausentes. |
| RC-06 | Somente responsáveis aprovam reservas de recursos restritos. | Nenhuma evidência executável. | **BLOQUEADO** — autorização, recursos restritos e aprovação não implementados/definidos. |
| RC-07 | Reserva iniciada não pode ser apagada. | `excluiReservaPorId` cobre apenas exclusão física de reserva existente. | **BLOQUEADO** — estado/início da reserva não são modelados; teste atual não comprova a proibição. |
| RC-08 | Mudanças de estado geram registro de auditoria. | Nenhuma evidência executável. | **BLOQUEADO** — estados e auditoria não implementados. |

## 5. Resumo da rastreabilidade crítica

Todos os RF-01–RF-14, RNF-01–RNF-09 e RC-01–RC-08 estão enumerados nesta RTM e ligados a evidências ou à ausência explícita delas. Isso significa **100% de rastreabilidade documental**, não 100% de cobertura executável dos requisitos críticos. RC-01 tem evidência adequada no escopo declarado; RC-02–RC-04 são parciais; RC-05–RC-08 permanecem bloqueados. Portanto, a meta de cobertura de 100% dos requisitos críticos **não está atendida**.

A cobertura JaCoCo é uma medida agregada de código executado, não substitui rastreabilidade, qualidade de asserções, SonarCloud ou testes em infraestrutura representativa. A análise SonarCloud e o Quality Gate permanecem pendentes no PR #1 até a configuração segura do secret e das variáveis Actions.

## 6. Atualização da RTM

Atualizar esta matriz quando um requisito mudar, um teste/evidência for acrescentado ou o status de implementação/validação se alterar. Ao concluir uma evidência, registrar o identificador do teste, ambiente (ex.: H2 ou Testcontainers), comando/run e resultado; não promover **PARCIAL** ou **BLOQUEADO** a **COBERTO** apenas com base no percentual global de cobertura.
