# Auditoria de requisitos — Organização de Recursos

**Data da verificação:** 2026-11-11
**Fontes:** [requisitos funcionais](requisitos-funcionais.md), [RTM](rtm.md),
[visão geral](visão-geral.md), código e testes atuais.
**Execução local:** `./mvnw -B verify` — BUILD SUCCESS, 112 testes, 0 falhas,
0 erros e 0 ignorados. A suíte inclui Spring Boot, Cucumber e PostgreSQL via
Testcontainers. A execução verifica o build e os limites globais de JaCoCo;
não equivale a uma validação de produção.

## Critério de classificação

- **feito:** teste executável que falharia se o comportamento declarado
  quebrasse; significa apenas o escopo explicitamente coberto.
- **parcial:** há comportamento e teste para uma parte, mas um ou mais
  critérios, perfis, recursos ou caminhos relevantes continuam sem evidência.
- **ausente:** não foi encontrada implementação/teste executável do
  comportamento.

Uma rota, classe, dependência ou linha na RTM, isoladamente, não prova que o
requisito está satisfeito.

## Limitadores de nota — verificação e prioridade

1. **Aplicação que não executa — não observado nesta verificação.** `./mvnw -B
   verify` passou e os testes `@SpringBootTest` iniciaram o contexto. A
   inicialização isolada de uma implantação/ambiente de entrega não foi
   verificada.
2. **Pipeline ausente — não observado.** [.github/workflows/ci.yml](../.github/workflows/ci.yml)
   está configurado para `push` e `pull_request` e executa `./mvnw -B verify`.
   A [RTM](rtm.md) registra o run `42859680966` como verde; a avaliação
   arquitetural anterior registra falha de configuração SonarCloud. Não foi
   feita consulta remota nesta auditoria, portanto o estado remoto atual e a
   configuração dos secrets/vars devem ser confirmados. O pipeline não deve
   ser classificado como inexistente.
3. **Falha de autorização — pendência prioritária (RF-01 parcial).** JWT é
   exigido para a API e decisões de reserva restrita têm checagem de papel e
   responsável; falta matriz aplicada e testada em todos os endpoints, escopo
   de leitura e prova de identidade/autorização ponta a ponta.
4. **Possibilidade de dupla reserva — pendência prioritária (RF-06 parcial,
   RC-03 feito no recorte testado).** Existe teste real de concorrência em
   PostgreSQL/Testcontainers: 20 criações simultâneas, repetidas cinco vezes,
   devem resultar em um `201`, dezenove `409` e uma linha persistida por
   disputa. Não há evidência equivalente para atualizações concorrentes nem
   para todos os tipos/caminhos de alocação.

Depois desses limitadores, as pendências com maior impacto funcional são
pesquisa de disponibilidade, modelos de professor/material, auditoria e
proteção contra exclusão de reserva iniciada. Os requisitos ausentes estão
ordenados por risco de negócio antes de itens de apresentação/documentação.

## Requisitos funcionais

| Requisito | Situação | Evidência (arquivo e nome do teste) | Risco se ficar de fora |
|---|---|---|---|
| **RF-01 — Autenticação e autorização por perfil** | **parcial** | `src/test/java/br/edu/organizacaorecursos/SecurityConfigTest.java` — `apiRequerBearerJwtSemCriarSessaoHttp`, `apiComBearerIgnoraCsrfMasAindaExigeJwtValido`; `ReservaApiIntegrationTest.java` — `rejeitaAutoAprovacaoEPreservaSolicitacao`, `rejeitaAprovacaoPorUsuarioQueNaoEResponsavel`. `SecurityConfig.java` exige JWT, mas o serviço/controller não demonstra matriz completa por endpoint; os testes de regra de negócio usam `@AutoConfigureMockMvc(addFilters = false)`, então não comprovam a integração ponta a ponta entre JWT e autorização de perfil. | Operações administrativas ou dados de outros usuários podem ficar acessíveis a perfil indevido; requisições diretas não estarão protegidas de forma consistente. |
| **RF-02 — Cadastro e consulta de salas, professores e materiais** | **parcial** | `ReservaApiIntegrationTest.java` — `criaEListaRecursosPelosEndpointsRest`, `atualizaConsultaERemoveRecursoPelosEndpointsRest`, `criaEListaUsuariosPelaApi`; domínio em `src/main/java/br/edu/organizacaorecursos/domain/Recurso.java`. Há CRUD genérico, mas não modelos/campos completos específicos de professor e material. | Cadastros não representam competências, agenda, estoque, estado e localização necessários às alocações reais. |
| **RF-03 — Pesquisa por tipo, capacidade, localização, competência e disponibilidade** | **ausente** | Nenhum teste de filtros/pesquisa seletiva encontrado. `ReservaApiIntegrationTest.java` testa listagem, não disponibilidade por critérios e intervalo; `Recurso.java` não possui os atributos obrigatórios. | Usuários não conseguem encontrar recursos adequados; manutenção e conflitos não podem ser refletidos numa consulta de disponibilidade. |
| **RF-04 — Criação, alteração e cancelamento de reservas** | **parcial** | `ReservaApiIntegrationTest.java` — `criaReservaEDevolveDtoSemExporEntidades`, `atualizaReservaPendenteSomentePeloSolicitante`, `excluiReservaPorId`; `ReservaServiceTest.java` — `impedeExcluirReservaQueJaFoiIniciada`; `ReservaAuditoriaPostgresIntegrationTest.java` — `persisteHistoricoComMudancaDeEstadoNaMesmaTransacao`. Criação, atualização limitada, exclusão protegida após início e uma mudança auditada são testadas; justificativas e o restante do fluxo especificado não. | Alterações/cancelamentos fora dos caminhos testados podem perder rastreabilidade ou apagar reservas ainda em uso. |
| **RF-05 — Detecção de sobreposição, incluindo agenda do professor** | **parcial** | `ReservaApiIntegrationTest.java` — `rejeitaPeriodosSobrepostosParaMesmoRecurso`, `permiteReservasAdjacentes`; `ReservaConcorrenciaPostgresIntegrationTest.java` — `aceitaUmaReservaPorSalaEPorAgendaDoProfessorSobConcorrencia`. O segundo teste modela a agenda via `responsavel_id`; não substitui entidade/agenda de professor e material previstas no RF. | Alocações conflitantes de docentes, equipamentos ou materiais podem ser aceitas ou reportadas incorretamente. |
| **RF-06 — Proteção contra dupla reserva simultânea** | **parcial** | `src/test/java/br/edu/organizacaorecursos/ReservaConcorrenciaPostgresIntegrationTest.java` — `aceitaUmaReservaPorSalaEPorAgendaDoProfessorSobConcorrencia` (`@RepeatedTest(5)`, PostgreSQL 16 em Testcontainers; asserta `1 × 201`, `19 × 409` e uma linha); `ReservaApiIntegrationTest.java` — `aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas`. Criação concorrente está provada; atualização concorrente e demais tipos de recurso não. | Uma corrida por caminho não coberto pode gravar conflitos e comprometer a disponibilidade. |
| **RF-07 — Aprovação obrigatória de recursos restritos** | **parcial** | `ReservaApiIntegrationTest.java` — `mantemSolicitacaoRestritaPendenteAteAprovacao`, `patchStatusAceitaResponsavelEPreservaConflitos`, `patchStatusPermiteAdministradorMesmoSemSerResponsavelDoRecurso`, `rejeitaAprovacaoPorUsuarioQueNaoEResponsavel`; `aprovacao-bloqueio.feature` cobre BDD. Falta evidência para informar o motivo da restrição e fechar política de pedidos pendentes sobrepostos. | Recursos restritos podem ser comprometidos sem comunicação/política consistente, ou pedidos conflitantes podem permanecer pendentes. |
| **RF-08 — Bloqueio de recursos em manutenção** | **parcial** | `ReservaApiIntegrationTest.java` — `impedeReservaDuranteManutencao`, `aplicaFronteirasDoIntervaloDeManutencao`; `src/test/resources/features/aprovacao-bloqueio.feature` — “Criar reserva na fronteira do período de manutenção”. Criação é bloqueada nos intervalos testados; calendário, consulta, histórico e reservas já afetadas não. | A consulta pode indicar disponibilidade falsa; manutenção pode não ser rastreável nem reconciliada com reservas existentes. |
| **RF-09 — Registro de retirada e devolução de materiais/equipamentos** | **ausente** | Nenhum teste de retirada/devolução; não há entidades, endpoints ou fluxo de empréstimo correspondentes em `src/main/java`. | Perda de controle de estoque, itens não devolvidos, danos e responsabilidade pela retirada. |
| **RF-10 — Histórico auditável de mudanças** | **parcial** | `ReservaAuditoriaPostgresIntegrationTest.java` — `persisteHistoricoComMudancaDeEstadoNaMesmaTransacao`, `rollbackDaMudancaDeEstadoTambemRemoveAuditoria`; `ReservaApiIntegrationTest.java` — `disponibilizaHistoricoDeEstadoComDadosDeAuditoria`, `solicitanteRecebe403AoConsultarHistoricoDaReserva`, `apiNaoPermiteAlterarOuExcluirRegistroDeAuditoria`. Prova transação/rollback de uma transição, campos do evento, leitura restrita e ausência de mutação pela API; não prova cada caminho de mudança nem audita recursos, usuários ou manutenção. | Mudanças por caminhos não cobertos ou operações fora de reservas podem ficar sem trilha para investigação e prestação de contas. |
| **RF-11 — Notificação simulada ou integração externa** | **ausente** | Nenhum teste, serviço ou adaptador de notificação encontrado em `src/main/java` e `src/test/java`. | Usuários podem não saber sobre criação, rejeição, decisão ou pendência de reserva. |
| **RF-12 — Relatório de utilização, carga horária e conflitos evitados** | **parcial** | `RelatorioUtilizacaoApiTest.java` — `agregaHorasPorRecursoEUsuarioComInterseccaoDoPeriodo`, `periodoSemReservasRetornaListasVaziasEZeroConflitos`, `rejeitaPeriodoVazioOuInvertido`, `exigeTokenEPermiteSomenteResponsavelOuAdministrador`, `registraTentativa409EContaPeloInstanteDaRecusa`, `publicaEndpointNoOpenApi`; `RelatorioController.java`; README, seção Relatório de utilização. Prova via HTTP real as horas por recurso/usuário (`Reserva.usuario`), recorte temporal, exclusão de canceladas, registro de conflito por instante, validação e autorização. | Ainda faltam volume de reservas aprovadas/canceladas, quantidade de solicitações que exigiram aprovação, exportação e visualização simples do relatório. |
| **RF-13 — Interface responsiva e mensagens compreensíveis** | **parcial** | `ReservaApiIntegrationTest.java` — `informaRecursoEIntervaloQuandoExisteConflitoDeReserva`, `validaCamposObrigatoriosDoRequest`, `retornaBadRequestQuandoRecursoNaoExiste`; `ApiExceptionHandlerTest.java` cobre respostas de erro. Não foi encontrada interface web responsiva nem teste por viewport. | Erros podem ser claros na API, mas não há experiência verificável para usuário final ou dispositivos móveis. |
| **RF-14 — Documentação da API e fluxos públicos** | **parcial** | `ReservaApiIntegrationTest.java` — `publicaDocumentacaoOpenApiEEndpointsDeReserva`; `src/main/java/br/edu/organizacaorecursos/config/OpenApiConfig.java`. OpenAPI está publicado, mas autenticação, autorização, exemplos/erros completos e sincronismo com todos os fluxos não são demonstrados. | Consumidores podem integrar incorretamente ou depender de comportamentos não documentados. |

## Regras críticas da visão geral

| Regra | Situação | Evidência (arquivo e nome do teste) | Risco se ficar de fora |
|---|---|---|---|
| **RC-01 — Término posterior ao início** | **feito** | `ReservaApiIntegrationTest.java` — `rejeitaDataFimIgualAoInicio`, `rejeitaDataFimAnteriorAoInicio`; `ReservaRequestDTOTest.java` — `validaDatasNaFronteiraDoRelogioInjetado`. Asserções cobrem intervalo inválido e fronteiras do relógio. | — |
| **RC-02 — Sem sobreposição para recurso/sala/material/professor** | **parcial** | `ReservaApiIntegrationTest.java` — `rejeitaPeriodosSobrepostosParaMesmoRecurso`, `permiteReservasAdjacentes`; `ReservaConcorrenciaPostgresIntegrationTest.java` cobre sala e agenda modelada por responsável. Faltam modelos/agenda reais de professor e material. | Conflitos podem ocorrer fora do recorte de recurso genérico testado. |
| **RC-03 — Uma única reserva aceita em solicitações concorrentes** | **feito** | `ReservaConcorrenciaPostgresIntegrationTest.java` — `aceitaUmaReservaPorSalaEPorAgendaDoProfessorSobConcorrencia`: cinco repetições; cada cenário afirma uma resposta aceita, dezenove recusadas e uma linha no PostgreSQL. | — |
| **RC-04 — Recurso em manutenção não pode ser reservado** | **parcial** | `ReservaApiIntegrationTest.java` — `impedeReservaDuranteManutencao`, `aplicaFronteirasDoIntervaloDeManutencao`; cenário BDD “Criar reserva na fronteira do período de manutenção”. Não testa pesquisa/calendário nem manutenção histórica. | Recurso indisponível pode aparecer como disponível ou reservas existentes podem não ser tratadas. |
| **RC-05 — Transições válidas de estado e estados alternativos** | **feito** | `src/test/resources/features/aprovacao-bloqueio.feature` — “Executar uma transição do fluxo principal”, “Registrar um estado alternativo permitido” e “Rejeitar transição inválida”; executados por `src/test/java/br/edu/organizacaorecursos/bdd/CucumberTest.java` (16 invocações Cucumber na verificação). | — |
| **RC-06 — Admin/responsável autorizado decide reserva restrita; sem autoaprovação** | **feito** | `ReservaApiIntegrationTest.java` — `patchStatusAceitaResponsavelEPreservaConflitos`, `patchStatusPermiteAdministradorMesmoSemSerResponsavelDoRecurso`, `rejeitaAprovacaoPorUsuarioQueNaoEResponsavel`, `rejeitaAutoAprovacaoEPreservaSolicitacao`. Comprova a regra de negócio no endpoint de status; não significa que a matriz de autorização de todos os endpoints esteja pronta. | — |
| **RC-07 — Reserva iniciada não pode ser apagada** | **feito** | `ReservaServiceTest.java` — `impedeExcluirReservaQueJaFoiIniciada`, parametrizado para `EM_USO`, `CONCLUÍDA` e `NÃO_COMPARECEU`; também se impede excluir reservas com histórico para preservar a trilha. | — |
| **RC-08 — Mudanças de estado geram auditoria** | **parcial** | `ReservaAuditoriaPostgresIntegrationTest.java` — `persisteHistoricoComMudancaDeEstadoNaMesmaTransacao`, `rollbackDaMudancaDeEstadoTambemRemoveAuditoria`; `ReservaApiIntegrationTest.java` — `disponibilizaHistoricoDeEstadoComDadosDeAuditoria`, `solicitanteRecebe403AoConsultarHistoricoDaReserva`. O caso exercitado persiste reserva, ator, instante, estados e motivo atomicamente; falta asserção de auditoria para cada caminho de transição. | Uma mudança por caminho não verificado pode deixar de gerar evento sem falha na suíte. |

## Divergências em relação à RTM

- A RTM de 2026-11-10 registra o teste de concorrência somente com H2 e
  Testcontainers pendente. O código atual contém
  `ReservaConcorrenciaPostgresIntegrationTest`, executado sem falhas no
  `verify` atual: atualizar a evidência; ainda falta testar atualização
  concorrente.
- A RTM informa 92 testes e SonarCloud verde no run `42859680966`; a execução
  local anterior contou 105 testes e a execução atual, 112. A avaliação
  arquitetural anterior registra falha de secrets/vars no SonarCloud. Não
  inferir o estado remoto atual sem conferir o run e a configuração do
  repositório.
- A cobertura global ou aprovação do Sonar não substitui evidência
  comportamento-a-comportamento: RF-03, RF-09 e RF-11 seguem sem teste
  funcional; RF-10 e RF-12 têm cobertura parcial dos caminhos auditados.

## Proposta de divisão até a Semana 46

Os artefatos consultados não identificam nominalmente os integrantes. Os
rótulos abaixo são frentes substituíveis pelos nomes reais; cada entrega deve
incluir teste que falhe quando o comportamento for removido.

| Integrante (proposta) | Frente e prioridade | Entrega verificável até a Semana 46 |
|---|---|---|
| **Integrante A — segurança** | Prioridade 1 ativa: fechar RF-01/RNF-01. Definir matriz com Produto/PO e aplicá-la em cada endpoint; ligar ator a identidade JWT, escopo de leitura e auditoria de negativas. | Testes de API com token válido/inválido e perfis por rota; provar negação de acesso a operações fora do papel, sem filtros desativados nos testes de segurança. |
| **Integrante B — integridade de reservas** | Prioridade 2 ativa: completar RF-05/RF-06 e RC-02/RC-03; cobrir `PUT`/aprovação simultâneos e validar quais identidades representam professor versus responsável. Manter o teste PostgreSQL/Testcontainers no CI. | Testes concorrentes de criação e alteração, sem gravações duplicadas; decisão documentada sobre os recursos efetivamente modelados. |
| **Integrante C — recursos e disponibilidade** | RF-02/RF-03/RF-08: completar dados de sala/professor/material, busca por filtros e intervalo, e disponibilidade considerando manutenção. | Testes de filtros e bordas de disponibilidade; retorno não lista recursos conflitantes ou em manutenção. |
| **Integrante D — ciclo operacional e auditoria** | Priorizar RF-09/RF-10, RC-08 e parte de RF-11: movimentação de material, completar auditoria de todos os caminhos de transição e notificar alterações. RC-07 está coberto para estados iniciados. | Testes de retirada/devolução e cada caminho auditável; evento validado com ator/instante/estado anterior/novo/motivo; teste do evento de notificação. |
| **Integrante E — interface, relatórios e contrato** | RF-12/RF-13/RF-14: relatório, interface responsiva, mensagens e documentação. Apoiar a confirmação do run remoto do workflow sem tratar configuração como evidência de sucesso. | Testes de métricas/autorização do relatório, validação responsiva e conferência automatizada ou teste dos contratos documentados; status remoto de CI/Sonar registrado. |

**Ordem de integração:** primeiro Integrante A; em paralelo, B e C; D depende
da definição dos estados e do modelo de recursos; E documenta os contratos
estabilizados por A/C e integra o resultado. Reservar a última revisão da
Semana 46 para executar `./mvnw -B verify`, conferir o workflow/relatório
remoto e atualizar a RTM com nomes reais de testes. Se a capacidade da equipe
não permitir concluir todos os RFs, não reclassificar lacunas como feitas:
registrar como parciais/ausentes e priorizar autorização, integridade e
rastreabilidade.
