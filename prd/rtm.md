# Matriz de Rastreabilidade de Requisitos (RTM)

**Data da fotografia:** 2026-11-10
**Fontes:** [Requisitos funcionais](requisitos-funcionais.md), [Requisitos não funcionais](requisitos-não-funcionais.md), [Visão geral](visão-geral.md) e [Plano TDD](testes/plano-tdd.md).
**Baseline de implementação:** `main`, commit `4c27bb8` (merge do PR [#2](https://github.com/gustavao578/Organiza-o-de-Recursos---QS-projeto-final/pull/2)); a correção do Quality Gate está em acompanhamento neste PR de follow-up.

## 1. Objetivo e convenções

Esta matriz liga cada requisito oficial RF/RNF e cada regra crítica da visão geral às evidências disponíveis e ao estado observado. Ela distingue rastreabilidade documental de cobertura comportamental: listar um requisito na RTM não significa que esteja implementado ou validado.

| Status | Significado |
|---|---|
| **COBERTO** | O escopo implementado descrito no requisito tem evidência de teste adequada; limitações residuais são explicitadas. |
| **PARCIAL** | Há evidência de parte do requisito, mas critérios, categorias ou ambientes relevantes continuam sem cobertura. |
| **BLOQUEADO** | A funcionalidade não existe ou faltam decisões, ferramentas ou critérios para testar o requisito. |

Os nomes de testes referem-se aos métodos das classes em `src/test/java/br/edu/organizacaorecursos/`. Evidências Cucumber indicam também o cenário literal no arquivo `.feature`, o runner e os steps. Os identificadores `RC-01` a `RC-08` são rótulos editoriais desta RTM para as regras da seção **Regras críticas** da visão geral; não substituem IDs oficiais.

## 2. Requisitos funcionais

| ID | Requisito | Evidência/testes ligados | Status e lacuna atual |
|---|---|---|---|
| RF-01 | Autenticação e autorização por perfil | BDD-APR-01–04 em `src/test/resources/features/aprovacao-bloqueio.feature`; `src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java` — `patchStatusExigeAdminPersistidoEImpedeAprovacoesSobrepostas`, `rejeitaAprovacaoPorUsuarioQueNaoEResponsavel`; `src/test/java/br/edu/organizacaorecursos/SecurityConfigTest.java` — `apiRequerBearerJwtSemCriarSessaoHttp`; commits `76559b1`, `a60b61d`, `dc61774`. | **PARCIAL** — JWT, autorização de decisão e política stateless validadas; matriz completa para todos os endpoints e auditoria não cobertas. |
| RF-02 | Cadastro e consulta de salas, professores e materiais | `src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java` — `criaEListaRecursosPelosEndpointsRest`, `atualizaConsultaERemoveRecursoPelosEndpointsRest`, `criaEListaUsuariosPelaApi`, `retornaNotFoundAoExcluirUsuarioInexistente`; commits `76559b1`, `a60b61d`, `7505d8d`. | **PARCIAL** — CRUD de Recurso/Usuário e manutenção; modelo de professores, materiais, atributos completos e regra de duplicidade não cobertos/implementados. |
| RF-03 | Pesquisa por tipo, capacidade, localização, competência e disponibilidade | Nenhum teste de pesquisa com filtros. | **BLOQUEADO** — consulta de disponibilidade e filtros de domínio não implementados. |
| RF-04 | Criação, alteração e cancelamento de reservas | `src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java` — `criaReservaEDevolveDtoSemExporEntidades`, `atualizaReservaPendenteSomentePeloSolicitante`; feature BDD-EST-02 `Registrar um estado alternativo permitido`; commit `76559b1`. | **PARCIAL** — criação, atualização de pendentes e cancelamento cobertos; justificativa e edição de reservas aprovadas não definidos. |
| RF-05 | Detecção de sobreposição, incluindo agenda de professor | `src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java` — `rejeitaPeriodosSobrepostosParaMesmoRecurso`, `permiteReservasAdjacentes`, `patchStatusExigeAdminPersistidoEImpedeAprovacoesSobrepostas`; commit `76559b1`. | **PARCIAL** — conflitos por Recurso, inclusive na aprovação, cobertos; agenda de professor, materiais/equipamentos e disponibilidade pesquisável ausentes. |
| RF-06 | Proteção contra dupla reserva simultânea | `aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas`; integração com H2. | **PARCIAL** — cenário local concorrente aceita exatamente uma; Testcontainers e engine de produção não verificados. |
| RF-07 | Aprovação obrigatória de recursos restritos | `src/test/resources/features/aprovacao-bloqueio.feature` — BDD-APR-01 `Solicitação de recurso restrito aguarda aprovação`, BDD-APR-02 `Responsável aprova uma solicitação restrita`, BDD-APR-03 `Solicitante não pode aprovar a própria reserva`, BDD-APR-04 `Usuário sem papel de responsável não aprova uma reserva restrita`; runner `src/test/java/br/edu/organizacaorecursos/bdd/CucumberTest.java`; steps `src/test/java/br/edu/organizacaorecursos/bdd/ReservaApiSteps.java`; commits `76559b1`, `a60b61d`. | **COBERTO** — estado inicial, perfil ADMIN, vínculo com responsável, autoaprovação e ator não responsável executados via MockMvc/JWT. |
| RF-08 | Bloqueio de recursos em manutenção | BDD-MAN-01 `Criar reserva na fronteira do período de manutenção` em `src/test/resources/features/aprovacao-bloqueio.feature`; runner `src/test/java/br/edu/organizacaorecursos/bdd/CucumberTest.java`; steps `ReservaApiSteps.recursoTemManutencao`, `ReservaApiSteps.solicitanteCriaReservaNoPeriodo`; commit `76559b1`. | **PARCIAL** — três exemplos de fronteira executados; calendário/busca, manutenção passada e reservas já afetadas não implementados/definidos. |
| RF-09 | Retirada e devolução de materiais/equipamentos | Nenhuma evidência executável. | **BLOQUEADO** — modelo, estoque e fluxo de empréstimo não implementados. |
| RF-10 | Histórico auditável de mudanças | Nenhuma evidência executável de auditoria. | **BLOQUEADO** — eventos, ator, antes/depois, retenção e consulta autorizada não implementados. |
| RF-11 | Notificação simulada ou integração externa | Nenhuma evidência executável. | **BLOQUEADO** — canal, eventos, destinatários e comportamento de falha pendentes. |
| RF-12 | Relatórios de utilização, carga horária e conflitos evitados | Nenhuma evidência executável. | **BLOQUEADO** — fórmulas, autorização, filtros e exportação não implementados/definidos. |
| RF-13 | Interface responsiva e mensagens compreensíveis | `validaCamposObrigatoriosDoRequest`, `informaRecursoEIntervaloQuandoExisteConflitoDeReserva`, `retornaBadRequestQuandoRecursoNaoExiste`, `rejeitaUsuarioInexistente`, `rejeitaDataDeInicioNoPassado`. | **PARCIAL** — respostas HTTP e erros de API têm evidência; não há UI responsiva, nem critérios de viewport/usabilidade validados. |
| RF-14 | Documentação de API ou fluxos públicos | `publicaDocumentacaoOpenApiEEndpointsDeReserva`. | **PARCIAL** — OpenAPI/Swagger básico validado; autenticação, erros/exemplos completos e sincronismo contínuo não verificados. |

## 3. Requisitos não funcionais

| ID | Requisito | Evidência/testes ligados | Status e lacuna atual |
|---|---|---|---|
| RNF-01 | Segurança e autorização | BDD-APR-03/04; `src/test/java/br/edu/organizacaorecursos/SecurityConfigTest.java` — `apiRequerBearerJwtSemCriarSessaoHttp`, `apiSemTokenBearerExigeProtecaoCsrf`, `apiComBearerIgnoraCsrfMasAindaExigeJwtValido`; commits `76559b1`, `a60b61d`, `dc61774`. O SonarCloud reportou `java:S4502` no commit `4c27bb8`; a correção mantém CSRF ativo e o ignora apenas em rotas da API com cabeçalho Bearer, mantendo a autenticação JWT obrigatória. | **PARCIAL** — política e testes locais atualizados; confirmação após nova análise SonarCloud pendente. |
| RNF-02 | Integridade e concorrência | `rejeitaPeriodosSobrepostosParaMesmoRecurso`, `aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas`; H2 local. | **PARCIAL** — conflitos e corrida exercitados; atomicidade no engine de destino/Testcontainers pendente. |
| RNF-03 | Desempenho mensurável com JMeter | Nenhum relatório JMeter. | **BLOQUEADO** — ferramenta, perfil de carga e limites quantitativos não fornecidos/aprovados. |
| RNF-04 | Estratégia e evidência de testes | JUnit 5 + Cucumber-JVM; `src/test/java/br/edu/organizacaorecursos/bdd/CucumberTest.java` seleciona `features/aprovacao-bloqueio.feature`; `./mvnw -B verify` executou feature, integração e JaCoCo; commit `76559b1`. | **PARCIAL** — BDD via MockMvc executado; Testcontainers, E2E externo e WireMock permanecem pendentes. |
| RNF-05 | Cobertura, RTM e análise de qualidade | `./mvnw -B verify` local — JaCoCo 312/353 linhas (88,4%) e 92/126 branches (73,0%); `src/test/java/br/edu/organizacaorecursos/exception/ApiExceptionHandlerTest.java` — `converteViolacaoDeIntegridadeEmConflito`, `informaErroGenericoQuandoValidacaoNaoContemErros`, `usaMensagemPadraoParaErroDeValidacaoSemMensagem`; commits funcionais `76559b1`, `a60b61d`, `7505d8d`, `dc61774`. No run `42731780666`, commit `4c27bb8`, Maven `verify` passou, mas SonarCloud reprovou a cobertura nova (79,202% < 80%; 253 linhas a cobrir, 41 descobertas) e manteve `java:S4502` aberto. | **PARCIAL** — JaCoCo mantém as metas de 80%/70%; a aprovação SonarCloud depende de nova análise no PR de correção. |
| RNF-06 | Integração contínua em pull requests | `.github/workflows/ci.yml` executa `./mvnw -B verify` e scanner SonarCloud; wrapper `mvnw`, `.mvn/wrapper/`; commit `76559b1`. Run `42731780666` no merge `4c27bb8`: passo Maven `Verify with Maven` passou e passo `SonarCloud analysis` falhou. | **PARCIAL** — pipeline executou e apontou corretamente o Quality Gate reprovado; resultado da reanálise após a correção está pendente. |
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
| RC-05 | Fluxo principal `SOLICITADA → APROVADA → EM_USO → CONCLUÍDA` e estados alternativos declarados. | `src/test/resources/features/aprovacao-bloqueio.feature` — BDD-EST-01 `Executar uma transição do fluxo principal`, BDD-EST-02 `Registrar um estado alternativo permitido`, BDD-EST-03 `Rejeitar transição inválida`; runner `src/test/java/br/edu/organizacaorecursos/bdd/CucumberTest.java`; commit `76559b1`. | **COBERTO** — transições e preservação do estado em rejeições exercitadas via API. |
| RC-06 | Somente responsáveis aprovam reservas de recursos restritos. | BDD-APR-02/04 em `src/test/resources/features/aprovacao-bloqueio.feature`; `src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java` — `aprovaSolicitacaoRestritaQuandoResponsavelExecuta`, `rejeitaAprovacaoPorUsuarioQueNaoEResponsavel`; commits `76559b1`, `a60b61d`. | **COBERTO** — exige perfil ADMIN, vínculo como responsável, impede autoaprovação e ator ADMIN não responsável. |
| RC-07 | Reserva iniciada não pode ser apagada. | `excluiReservaPorId` cobre apenas exclusão física de reserva existente. | **BLOQUEADO** — estado/início da reserva não são modelados; teste atual não comprova a proibição. |
| RC-08 | Mudanças de estado geram registro de auditoria. | Nenhuma evidência executável. | **BLOQUEADO** — estados e auditoria não implementados. |

## 5. Resumo da rastreabilidade crítica

Todos os RF-01–RF-14, RNF-01–RNF-09 e RC-01–RC-08 estão enumerados nesta RTM e ligados a evidências ou à ausência explícita delas. Isso significa **100% de rastreabilidade documental**, não 100% de cobertura executável dos requisitos críticos. RC-01, RC-05 e RC-06 têm evidência adequada nos escopos declarados; RC-02–RC-04 são parciais; RC-07 e RC-08 permanecem bloqueados. Portanto, a meta de cobertura de 100% dos requisitos críticos **não está atendida**. O run `42731780666` do commit `4c27bb8` reprovou o Quality Gate por cobertura nova de 79,202% (mínimo 80%) e pela pendência `java:S4502`; a correção local mantém CSRF protegido e adiciona testes de exceção, mas o novo resultado SonarCloud ainda depende da execução do PR.

A cobertura JaCoCo é uma medida agregada de código executado, não substitui rastreabilidade, qualidade de asserções, SonarCloud ou testes em infraestrutura representativa. No run `42731780666`, a compilação, os testes e o JaCoCo passaram, mas o passo SonarCloud reprovou; a validação local da correção passou e aguarda reexecução remota para confirmar o Quality Gate.

## 6. Atualização da RTM

Atualizar esta matriz quando um requisito mudar, um teste/evidência for acrescentado ou o status de implementação/validação se alterar. Ao concluir uma evidência, registrar o identificador do teste, ambiente (ex.: H2 ou Testcontainers), comando/run e resultado; não promover **PARCIAL** ou **BLOQUEADO** a **COBERTO** apenas com base no percentual global de cobertura.

## 7. Evidência BDD executável — Aprovação e bloqueio

O arquivo abaixo preserva os nomes e os textos dos cenários de `docs/testes/bdd-aprovacao-bloqueio.md`. Todos os passos fazem requests à aplicação com MockMvc; os passos de aprovação usam JWT assinado com o perfil e o sujeito do ator. O runner JUnit Platform inclui explicitamente a feature, portanto `./mvnw -B verify` executa estes exemplos no CI.

| ID | Cenário/esquema literal | Feature e execução | Steps relevantes | Commit de código |
|---|---|---|---|---|
| BDD-APR-01 | `Solicitação de recurso restrito aguarda aprovação` | `src/test/resources/features/aprovacao-bloqueio.feature`; `src/test/java/br/edu/organizacaorecursos/bdd/CucumberTest.java` | `ReservaApiSteps.recursoRestrito`, `solicitanteTemPermissao`, `solicitanteEnviaSolicitacao`, `reservaPermaneceSolicitada` | `76559b1` |
| BDD-APR-02 | `Responsável aprova uma solicitação restrita` | Mesma feature e runner | `ReservaApiSteps.existeReservaRestritaSolicitada`, `atorResponsavel`, `atorAprovaReserva`, `reservaPassaParaAprovada` | `76559b1`, `a60b61d` |
| BDD-APR-03 | `Solicitante não pode aprovar a própria reserva` | Mesma feature e runner | `ReservaApiSteps.atorSolicitante`, `atorTentaAprovarReserva`, `operacaoRejeitadaReservaSolicitada` | `76559b1`, `a60b61d` |
| BDD-APR-04 | `Usuário sem papel de responsável não aprova uma reserva restrita` | Mesma feature e runner | `ReservaApiSteps.atorNaoResponsavel`, `atorTentaAprovarReserva`, `operacaoRejeitadaReservaSolicitada` | `76559b1`, `a60b61d` |
| BDD-MAN-01 | `Criar reserva na fronteira do período de manutenção` — três exemplos da tabela `Exemplos` | Mesma feature e runner | `ReservaApiSteps.recursoTemManutencao`, `solicitanteCriaReservaNoPeriodo`, `respostaApiEEstadoPersistencia` | `76559b1` |
| BDD-EST-01 | `Executar uma transição do fluxo principal` — três linhas de `Exemplos` | Mesma feature e runner | `ReservaApiSteps.reservaEstaNoEstado`, `atorAutorizadoSolicitaTransicao`, `operacaoAceitaEstado` | `76559b1` |
| BDD-EST-02 | `Registrar um estado alternativo permitido` — três linhas candidatas mantidas como especificadas | Mesma feature e runner | `ReservaApiSteps.reservaEstaNoEstado`, `atorAutorizadoSolicitaTransicao`, `operacaoAceitaEstado` | `76559b1` |
| BDD-EST-03 | `Rejeitar transição inválida` — três linhas de `Exemplos` | Mesma feature e runner | `ReservaApiSteps.reservaEstaNoEstado`, `atorSolicitaTransicao`, `operacaoRejeitadaEstado` | `76559b1` |

**Validação local após correção:** `./mvnw -B verify` passou: 84 testes, zero falhas/erros; JaCoCo: 312/353 linhas (88,4%) e 92/126 branches (73,0%). **CI/SonarCloud do merge:** run `42731780666`, commit `4c27bb8`; o passo Maven passou e o passo SonarCloud falhou: cobertura nova 79,202% (meta ≥80%) e `java:S4502` aberto em `SecurityConfig.java:28`, responsável pelo rating D de segurança nova. O código agora mantém CSRF ativo, dispensando-o apenas para rotas da API que trazem Bearer, e mantém JWT obrigatório; `SecurityConfigTest` exercita a rejeição sem Bearer e as rotas com Bearer inválido. A aprovação do novo Quality Gate depende da reanálise remota no PR.
