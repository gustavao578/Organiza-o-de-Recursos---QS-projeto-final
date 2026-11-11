# Matriz de Rastreabilidade de Requisitos (RTM)

**Data da fotografia:** 2026-11-10
**Fontes:** [Requisitos funcionais](requisitos-funcionais.md), [Requisitos não funcionais](requisitos-não-funcionais.md), [Visão geral](visão-geral.md) e [Plano TDD](testes/plano-tdd.md).
**Baseline de implementação:** `main`, commit `eb2bced` (2026-11-10). CI no commit `eb2bced` passou em `42859680966`, incluindo Maven verify, SonarCloud e publicação do relatório JaCoCo.

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
| RF-01 | Autenticação e autorização por perfil | BDD-APR-01–04; `ReservaApiIntegrationTest` — `patchStatusAceitaResponsavelEPreservaConflitos`, `patchStatusPermiteAdministradorMesmoSemSerResponsavelDoRecurso`, `rejeitaAprovacaoPorUsuarioQueNaoEResponsavel`; `SecurityConfigTest` — `apiRequerBearerJwtSemCriarSessaoHttp`; commits `76559b1`, `a60b61d`, `dc61774`, `eb2bced`. | **PARCIAL** — JWT, autorização de decisão e API stateless validados; matriz para demais endpoints, escopo de leitura por proprietário e auditoria de acessos não cobertos. |
| RF-02 | Cadastro e consulta de salas, professores e materiais | `ReservaApiIntegrationTest` — `criaEListaRecursosPelosEndpointsRest`, `atualizaConsultaERemoveRecursoPelosEndpointsRest`, `criaEListaUsuariosPelaApi`, `retornaNotFoundAoExcluirUsuarioInexistente`; commits `76559b1`, `a60b61d`, `7505d8d`. | **PARCIAL** — CRUD básico de Recurso/Usuário e manutenção; enum Perfil está disponível, mas não há entidades específicas de professor/material, atributos completos ou regra de duplicidade. |
| RF-03 | Pesquisa por tipo, capacidade, localização, competência e disponibilidade | Nenhum teste de pesquisa com filtros. | **BLOQUEADO** — consulta de disponibilidade e filtros de domínio não implementados. |
| RF-04 | Criação, alteração e cancelamento de reservas | `ReservaApiIntegrationTest` — `criaReservaEDevolveDtoSemExporEntidades`, `atualizaReservaPendenteSomentePeloSolicitante`; feature BDD-EST-02; commits `76559b1`, `eb2bced`. | **PARCIAL** — criação, atualização de pendentes, cancelamento e estados cobertos; justificativas, edição de aprovadas e regra para apagar reservas iniciadas continuam ausentes/indefinidas. |
| RF-05 | Detecção de sobreposição, incluindo agenda de professor | `ReservaApiIntegrationTest` — `rejeitaPeriodosSobrepostosParaMesmoRecurso`, `permiteReservasAdjacentes`, `patchStatusAceitaResponsavelEPreservaConflitos`; commits `76559b1`, `eb2bced`. | **PARCIAL** — conflitos de Recurso são validados na criação/aprovação; agenda de professor, materiais/equipamentos e disponibilidade pesquisável ausentes. Pedidos pendentes sobrepostos podem coexistir até a decisão. |
| RF-06 | Proteção contra dupla reserva simultânea | `aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas`; integração com H2. | **PARCIAL** — cenário local concorrente aceita exatamente uma; Testcontainers e engine de produção não verificados. |
| RF-07 | Aprovação obrigatória de recursos restritos | `aprovacao-bloqueio.feature` — BDD-APR-01–04; `ReservaApiIntegrationTest` — `criaReservaRestritaComStatusPendente`, `patchStatusAceitaResponsavelEPreservaConflitos`, `patchStatusPermiteAdministradorMesmoSemSerResponsavelDoRecurso`, `rejeitaAprovacaoPorUsuarioQueNaoEResponsavel`; runner `CucumberTest`; commits `76559b1`, `a60b61d`, `eb2bced`. | **COBERTO** — criação restrita `PENDENTE`, aprovação/rejeição e autorização por responsável/admin têm evidência API/JWT. A política para pedidos pendentes sobrepostos permanece uma lacuna de produto/integração. |
| RF-08 | Bloqueio de recursos em manutenção | BDD-MAN-01 `Criar reserva na fronteira do período de manutenção` em `src/test/resources/features/aprovacao-bloqueio.feature`; runner `src/test/java/br/edu/organizacaorecursos/bdd/CucumberTest.java`; steps `ReservaApiSteps.recursoTemManutencao`, `ReservaApiSteps.solicitanteCriaReservaNoPeriodo`; commit `76559b1`. | **PARCIAL** — três exemplos de fronteira executados; calendário/busca, manutenção passada e reservas já afetadas não implementados/definidos. |
| RF-09 | Retirada e devolução de materiais/equipamentos | Nenhuma evidência executável. | **BLOQUEADO** — modelo, estoque e fluxo de empréstimo não implementados. |
| RF-10 | Histórico auditável de mudanças | Nenhuma evidência executável de auditoria. | **BLOQUEADO** — eventos, ator, antes/depois, retenção e consulta autorizada não implementados. |
| RF-11 | Notificação simulada ou integração externa | Nenhuma evidência executável. | **BLOQUEADO** — canal, eventos, destinatários e comportamento de falha pendentes. |
| RF-12 | Relatórios de utilização, carga horária e conflitos evitados | Nenhuma evidência executável. | **BLOQUEADO** — fórmulas, autorização, filtros e exportação não implementados/definidos. |
| RF-13 | Interface responsiva e mensagens compreensíveis | `validaCamposObrigatoriosDoRequest`, `informaRecursoEIntervaloQuandoExisteConflitoDeReserva`, `retornaBadRequestQuandoRecursoNaoExiste`, `rejeitaUsuarioInexistente`, `rejeitaDataDeInicioNoPassado`. | **PARCIAL** — respostas HTTP e erros de API têm evidência; não há UI responsiva, nem critérios de viewport/usabilidade validados. |
| RF-14 | Documentação de API ou fluxos públicos | `publicaDocumentacaoOpenApiEEndpointsDeReserva`. | **PARCIAL** — OpenAPI/Swagger básico validado; autenticação, erros/exemplos completos e sincronismo contínuo não verificados. |

## 3. Requisitos não funcionais

**Atualização 2026-11-10:** a evidência posterior confirma SonarCloud verde no run `42859680966`, JaCoCo em 87% de linhas e 71% de branches, e 92 testes executados com Maven Wrapper. Estes dados substituem os resultados de cobertura, CI e análise Sonar antigos registrados em algumas linhas abaixo. A autorização ainda é parcial; auditoria, Testcontainers, E2E externo, WireMock e JMeter continuam pendentes.

| ID | Requisito | Evidência/testes ligados | Status e lacuna atual |
|---|---|---|---|
| RNF-01 | Segurança e autorização | BDD-APR-03/04; `src/test/java/br/edu/organizacaorecursos/SecurityConfigTest.java` — `apiRequerBearerJwtSemCriarSessaoHttp`, `apiSemTokenBearerExigeProtecaoCsrf`, `apiComBearerIgnoraCsrfMasAindaExigeJwtValido`; commits `76559b1`, `a60b61d`, `dc61774`. O SonarCloud reportou `java:S4502` no commit `4c27bb8`. A exceção limitada às rotas da API é justificada porque a aplicação autentica essas requisições exclusivamente por JWT Bearer explicitamente enviado no cabeçalho `Authorization`, não por cookies ou sessão; requisições sem Bearer continuam protegidas por CSRF e tokens inválidos continuam rejeitados. A supressão `java:S4502` está restrita à configuração que aplica esse matcher e exige revisão manual dessa justificativa. | **PARCIAL** — justificativa explícita e testes locais; confirmação após nova análise SonarCloud pendente. |
| RNF-02 | Integridade e concorrência | `rejeitaPeriodosSobrepostosParaMesmoRecurso`, `aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas`; H2 local. | **PARCIAL** — conflitos e corrida exercitados; atomicidade no engine de destino/Testcontainers pendente. |
| RNF-03 | Desempenho mensurável com JMeter | Proposta em [docs/performance/rnf.md](../docs/performance/rnf.md); plano [perf/reservas.jmx](../perf/reservas.jmx) e dados fictícios [perf/usuarios.csv](../perf/usuarios.csv); medição e SQL em [docs/performance/resultado-01.md](../docs/performance/resultado-01.md). | **PARCIAL** — p95/erros medidos em loopback com H2; `GET /reservas` é proxy sem filtro, o RNF ainda aguarda validação e PostgreSQL/ambiente representativo não foram medidos. |
| RNF-04 | Estratégia e evidência de testes | JUnit 5 + Cucumber-JVM; `CucumberTest` seleciona `features/aprovacao-bloqueio.feature`; `./mvnw -B verify` executa feature, integração e JaCoCo; commit `eb2bced`. | **PARCIAL** — BDD via MockMvc executado; Testcontainers, E2E externo e WireMock permanecem pendentes. |
| RNF-05 | Cobertura, RTM e análise de qualidade | `./mvnw -B verify` e run `42859680966` — JaCoCo 87% linhas/71% branches; `ApiExceptionHandlerTest` cobre os mapeamentos de erro; commits funcionais `76559b1`, `a60b61d`, `7505d8d`, `dc61774`, `eb2bced`. | **PARCIAL** — metas globais e SonarCloud passaram; RTM ainda tem requisitos críticos sem implementação/evidência, portanto a meta de 100% de cobertura crítica não foi alcançada. |
| RNF-06 | Integração contínua em pull requests | `.github/workflows/ci.yml` executa `./mvnw -B verify`, SonarCloud e publicação JaCoCo; `mvnw`, `.mvn/wrapper/`; run `42859680966`, commit `eb2bced`. | **COBERTO** para execução verificada do workflow; não é evidência de regras de negócio ausentes. |
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
| RC-05 | Fluxo `PENDENTE → APROVADA → EM_USO → CONCLUÍDA`, estados alternativos e transições válidas. | `aprovacao-bloqueio.feature` — BDD-EST-01–03; runner `CucumberTest`; commits `76559b1`, `eb2bced`. | **COBERTO** para os fluxos exercitados; `SOLICITADA` permanece estado legado no enum, além de estados do ciclo de vida. |
| RC-06 | Administrador ou responsável autorizado decide reservas restritas; solicitante não pode autoaprovar. | BDD-APR-02/04; `ReservaApiIntegrationTest` — `patchStatusAceitaResponsavelEPreservaConflitos`, `patchStatusPermiteAdministradorMesmoSemSerResponsavelDoRecurso`, `rejeitaAprovacaoPorUsuarioQueNaoEResponsavel`, `aprovaSolicitacaoRestritaQuandoResponsavelExecuta`; commits `76559b1`, `a60b61d`, `eb2bced`. | **COBERTO** pelo endpoint PATCH de status; a rota legada `/aprovar` tem regra mais restrita (admin) e deve ser documentada no contrato. |
| RC-07 | Reserva iniciada não pode ser apagada. | `excluiReservaPorId` cobre apenas exclusão física de reserva existente. | **BLOQUEADO** — estado/início da reserva não são modelados; teste atual não comprova a proibição. |
| RC-08 | Mudanças de estado geram registro de auditoria. | Nenhuma evidência executável. | **BLOQUEADO** — estados e auditoria não implementados. |

## 5. Resumo da rastreabilidade crítica

**Atualização 2026-11-10:** os parágrafos de runs `42731780666` e `42841405941` abaixo documentam falhas históricas e não representam o estado mais recente. O run atual `42859680966` no commit `eb2bced` passou no Maven verify, SonarCloud e JaCoCo; cobertura global atual 87% linhas/71% branches. A meta de cobertura de requisitos críticos permanece pendente.

Todos os RF-01–RF-14, RNF-01–RNF-09 e RC-01–RC-08 estão enumerados nesta RTM e ligados a evidências ou à ausência explícita delas. Isso significa **100% de rastreabilidade documental**, não 100% de cobertura executável dos requisitos críticos. RC-01, RC-05 e RC-06 têm evidência adequada nos escopos declarados; RC-02–RC-04 são parciais; RC-07 e RC-08 permanecem bloqueados. Portanto, a meta de cobertura de 100% dos requisitos críticos **não está atendida**. O run `42731780666` do commit `4c27bb8` reprovou o Quality Gate por cobertura nova de 79,202% (mínimo 80%) e pela pendência `java:S4502`; a cobertura foi elevada no commit `884c38d`, mas o finding persistiu e gerou rating D. A exceção CSRF foi então documentada e limitada a requisições Bearer stateless, com supressão da regra sujeita a revisão manual; a análise SonarCloud subsequente deve confirmar o resultado.

A cobertura JaCoCo é uma medida agregada de código executado, não substitui rastreabilidade, qualidade de asserções, SonarCloud ou testes em infraestrutura representativa. No run `42731780666`, a compilação, os testes e o JaCoCo passaram, mas o passo SonarCloud reprovou; a validação local da correção passou e aguarda reexecução remota para confirmar o Quality Gate.

## 6. Atualização da RTM

Atualizar esta matriz quando um requisito mudar, um teste/evidência for acrescentado ou o status de implementação/validação se alterar. Ao concluir uma evidência, registrar o identificador do teste, ambiente (ex.: H2 ou Testcontainers), comando/run e resultado; não promover **PARCIAL** ou **BLOQUEADO** a **COBERTO** apenas com base no percentual global de cobertura.

## 7. Evidência BDD executável — Aprovação e bloqueio

**Snapshot atual:** `./mvnw -B verify` passou com 92 testes. JaCoCo reportou 87% de linhas e 71% de branches; o run `42859680966` no commit `eb2bced` passou por Maven verify, SonarCloud e publicação do relatório. Os números de runs anteriores, reproduzidos ao final da seção, são evidências históricas.

O arquivo abaixo preserva os nomes e os textos dos cenários de `docs/testes/bdd-aprovacao-bloqueio.md`. Todos os passos fazem requests à aplicação com MockMvc; os passos de aprovação usam JWT assinado com o perfil e o sujeito do ator. O runner JUnit Platform inclui explicitamente a feature, portanto `./mvnw -B verify` executa estes exemplos no CI.

| ID | Cenário/esquema literal | Feature e execução | Steps relevantes | Commit de código |
|---|---|---|---|---|
| BDD-APR-01 | `Solicitação de recurso restrito aguarda aprovação` (`PENDENTE`) | `src/test/resources/features/aprovacao-bloqueio.feature`; `CucumberTest` | Steps de criação e assert de status da feature; nomes de métodos legados são mantidos em `ReservaApiSteps` | `76559b1`, `eb2bced` |
| BDD-APR-02 | `Responsável aprova uma solicitação restrita` (`PENDENTE → APROVADA`) | Mesma feature e runner | `ReservaApiSteps.existeReservaRestritaSolicitada`, `atorResponsavel`, `atorAprovaReserva`, `reservaPassaParaAprovada` | `76559b1`, `a60b61d`, `eb2bced` |
| BDD-APR-03 | `Solicitante não pode aprovar a própria reserva` (`PENDENTE`) | Mesma feature e runner | `ReservaApiSteps.atorSolicitante`, `atorTentaAprovarReserva`, `operacaoRejeitadaReservaSolicitada` | `76559b1`, `a60b61d`, `eb2bced` |
| BDD-APR-04 | `Usuário sem papel de responsável não aprova uma reserva restrita` (`PENDENTE`) | Mesma feature e runner | `ReservaApiSteps.atorNaoResponsavel`, `atorTentaAprovarReserva`, `operacaoRejeitadaReservaSolicitada` | `76559b1`, `a60b61d`, `eb2bced` |
| BDD-MAN-01 | `Criar reserva na fronteira do período de manutenção` — três exemplos da tabela `Exemplos` | Mesma feature e runner | `ReservaApiSteps.recursoTemManutencao`, `solicitanteCriaReservaNoPeriodo`, `respostaApiEEstadoPersistencia` | `76559b1` |
| BDD-EST-01 | `Executar uma transição do fluxo principal` — três linhas de `Exemplos` | Mesma feature e runner | `ReservaApiSteps.reservaEstaNoEstado`, `atorAutorizadoSolicitaTransicao`, `operacaoAceitaEstado` | `76559b1` |
| BDD-EST-02 | `Registrar um estado alternativo permitido` — três linhas executadas | Mesma feature e runner | `ReservaApiSteps.reservaEstaNoEstado`, `atorAutorizadoSolicitaTransicao`, `operacaoAceitaEstado` | `76559b1`, `eb2bced` |
| BDD-EST-03 | `Rejeitar transição inválida` — três linhas de `Exemplos` | Mesma feature e runner | `ReservaApiSteps.reservaEstaNoEstado`, `atorSolicitaTransicao`, `operacaoRejeitadaEstado` | `76559b1` |

**Validação local após correção:** `./mvnw -B verify` passou: 84 testes, zero falhas/erros; JaCoCo: 312/353 linhas (88,4%) e 92/126 branches (73,0%). **CI/SonarCloud do merge:** run `42731780666`, commit `4c27bb8`; o passo Maven passou e o passo SonarCloud falhou: cobertura nova 79,202% (meta ≥80%) e `java:S4502` aberto em `SecurityConfig.java:28`. No run `42841405941`, commit `884c38d`, a cobertura nova passou em 90%, mas `java:S4502` permaneceu aberto e o rating de segurança nova foi D. A decisão arquitetural é manter CSRF protegido para requisições sem Bearer e ignorá-lo somente para endpoints de API que exigem JWT Bearer em cabeçalho (não cookie/sessão); a regra foi suprimida somente nessa configuração, com justificativa e testes de escopo. A confirmação de que o finding foi removido depende da próxima análise SonarCloud.
