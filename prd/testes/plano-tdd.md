# Plano TDD — Sistema de Organização de Recursos

> **Status:** fase green parcial. A suíte RED histórica de 40 casos foi removida
> durante a substituição do placeholder pela aplicação Spring Boot; não está
> disponível como fonte executável. Em 2026-11-10, `./mvnw -B verify` passou com
> 92 testes; JaCoCo reportou 87% de linhas e 71% de branches, acima dos gates
> globais de 80%/70%. O CI/SonarCloud também passou no run `42859680966`.
> A matriz abaixo registra o que está coberto agora e separa cobertura parcial
> das regras bloqueadas por decisões pendentes.
> **Fonte de verdade:** requisitos funcionais, requisitos não funcionais,
> personas, visão geral e arquitetura existentes em `prd/`. Os casos abaixo
> verificam o comportamento declarado; pontos sem resultado especificado ficam
> marcados como pendências, não como regras presumidas.

## 1. Objetivo e convenções

Este plano inventaria as regras de negócio e organiza casos verificáveis para
guiar um ciclo TDD. Cada `R-TDD-nn` é um identificador editorial deste plano,
não um novo identificador de requisito. A coluna **Origem** mantém os IDs
oficiais RF/RNF e aponta as regras críticas da visão geral quando aplicável.

Os rótulos de caso significam:

- **Feliz:** resultado permitido descrito na fonte.
- **Limite:** fronteira explicitamente definida ou questão a confirmar.
- **Inválida:** dado ou ação que a fonte exige rejeitar.
- **Conflito:** interação com outra reserva, estado ou operação concorrente.
- **Proibido:** perfil, transição ou resultado que a fonte não permite.
- **Pendente:** a fonte não define a resposta; não fixar expectativa até decisão.

Uma expectativa marcada **Pendente** é um teste a fechar com produto antes da
implementação, não uma autorização para a aplicação escolher um comportamento.

## 2. Inventário de regras e casos funcionais

### RF-01 — Autenticação e autorização por perfil

| ID / regra (rótulo editorial) | Origem | Feliz | Limite | Inválida | Conflito | Proibido / resultado esperado |
|---|---|---|---|---|---|---|
| R-TDD-01 — Identidade autenticada e permissões limitam ações e dados | RF-01; RNF-01 | Usuário autenticado com permissão efetua a operação permitida. | Usuário com um perfil ou conjunto de perfis autorizados é reconhecido conforme configuração documentada. | Sem autenticação: negar acesso a recurso protegido. | Chamada direta à operação protegida fora da interface mantém a mesma autorização. | Ação fora do escopo do perfil é negada com mensagem clara; tentativa não autorizada é registrada. |
| R-TDD-02 — Perfil atribuído é válido e persistido | RF-01; CA01 da US-05 | Cadastrar e retornar um perfil dentre SOLICITANTE, RESPONSAVEL e ADMINISTRADOR. | O modelo atual aceita um perfil por usuário; composição múltipla não implementada nem aprovada. | Perfil ausente/inválido deve ser validado pelo DTO/modelo. | Permissões sobrepostas não se aplicam ao modelo atual de perfil único. | Solicitante não pode aprovar solicitação restrita. |
| R-TDD-03 — Operações de gerenciamento exigem autorização específica | RF-01; personas §§ 2–3 | Administrador ou responsável autorizado executa a operação correspondente. | Pendente: a matriz detalhada de operações/perfis ainda não foi aprovada. | Perfil sem permissão para operação de gerenciamento é negado. | Permissões concorrentes ou conflitantes: Pendente, conforme definição da matriz de acesso. | Administrador não pode alterar o perfil de outro usuário sem ação autorizada; tentativa não autorizada deve ser negada e registrada. |

### RF-02 — Cadastro e consulta de salas, professores e materiais

| ID / regra (rótulo editorial) | Origem | Feliz | Limite | Inválida | Conflito | Proibido / resultado esperado |
|---|---|---|---|---|---|---|
| R-TDD-04 — Cadastro, consulta, atualização e manutenção dos registros de recursos e pessoas | RF-02 | Criar e consultar registros de cada categoria; atualizar/manter registro existente. | Os dados mínimos por categoria são aceitos quando válidos; limites numéricos de capacidade/quantidade não estão definidos. | Dados mínimos ausentes ou inconsistentes: rejeitar o registro. | Identificador ou registro duplicado: rejeitar o cadastro duplicado. | Sem autorização para a operação de gerenciamento, negar (RF-01); regra de exclusão de registro de recurso não está detalhada. |
| R-TDD-05 — Dados mínimos de sala | RF-02 | Sala com identificador, nome, tipo, capacidade, localização e status válidos pode ser cadastrada. | Capacidade mínima/máxima e valores válidos de status: Pendente. | Campo obrigatório ausente ou dado inconsistente: rejeitar. | Identificador duplicado: rejeitar. | Não aceitar sala em estado incompatível com as regras de disponibilidade; estados permitidos e efeito de cada status: Pendente. |
| R-TDD-06 — Dados mínimos de professor | RF-02 | Professor com identificador, nome, competência, disponibilidade e agenda válidos pode ser cadastrado. | Quantidade/formato de competências e semântica de disponibilidade: Pendente. | Campo obrigatório ausente ou dado inconsistente: rejeitar. | Identificador duplicado ou agenda incompatível com alocação posterior: cadastro duplicado é rejeitado; conflito de agenda deve ser tratado pelas regras RF-05/RF-06. | Não permitir ação de gerenciamento sem autorização (RF-01). |
| R-TDD-07 — Dados mínimos de material | RF-02 | Material com identificador, nome, tipo, quantidade, estado, localização e regras de empréstimo válidos pode ser cadastrado. | Quantidade mínima, unidade, estado permitido e limites: Pendente. | Campo obrigatório ausente ou dado inconsistente: rejeitar. | Identificador duplicado: rejeitar; concorrência de quantidade é coberta por RF-05/RF-06, mas a regra de alocação por unidade/quantidade precisa ser definida. | Material em manutenção/bloqueado não pode ser apresentado como disponível (RF-03/RF-08). |
| R-TDD-08 — Cadastro rejeita duplicidade/inconsistência e consulta usa filtros | RF-02 | Consulta retorna registros correspondentes; status atual de salas e materiais reflete reservas/manutenção. | Duplicidade e campos mínimos como fronteiras; critérios de equivalência para detectar duplicados: Pendente. | Registro duplicado/inconsistente: rejeitar. | Mudança de status por reserva/manutenção deve aparecer na consulta atualizada; consistência temporal/cache: Pendente. | Não apresentar sala/material com status atual indisponível como disponível. |

### RF-03 — Pesquisa e disponibilidade

| ID / regra (rótulo editorial) | Origem | Feliz | Limite | Inválida | Conflito | Proibido / resultado esperado |
|---|---|---|---|---|---|---|
| R-TDD-09 — Filtros por tipo, capacidade/quantidade, localização, competência e data/horário | RF-03 | Cada filtro retorna recursos que atendem ao critério; professor por competência; sala por tipo/localização. | Capacidade ou quantidade mínima solicitada: comparar resultado compatível ao valor solicitado. Igualdade e zero: Pendente, conforme regras dos dados mínimos. | Tipo/filtro desconhecido, intervalo inválido ou formato inválido: validar e não executar consulta inválida; formato/código da resposta: Pendente. | Combinação de filtros e disponibilidade afetada por reserva, manutenção ou bloqueio retorna somente opções compatíveis. | Não retornar recurso indisponível por manutenção, conflito de horário ou bloqueio como disponível. |

### RF-04 — Criação, alteração e cancelamento de reservas

| ID / regra (rótulo editorial) | Origem | Feliz | Limite | Inválida | Conflito | Proibido / resultado esperado |
|---|---|---|---|---|---|---|
| R-TDD-10 — Criar reserva após validar recurso e período | RF-04; RF-05; visão geral, “Tempo e concorrência” | Solicitação para recurso disponível e período válido é registrada e confirmada conforme as regras aplicáveis. | Início e término: o término deve ser posterior ao início (visão geral); duração mínima/máxima não está definida. | Término igual/anterior ao início: não criar reserva. Dados obrigatórios de reserva: Pendente. | Recurso indisponível ou horário conflitante: não confirmar reserva conflitante (RF-05 e regras críticas). | Recurso restrito sem aprovação e recurso em manutenção não podem ser comprometidos (RF-07/RF-08). |
| R-TDD-11 — Alteração de reserva | RF-04; RF-05; RF-06; RNF-07 | Solicitante responsável ou usuário com permissão atribuída altera data, horário, professor, material ou recurso; alteração fica rastreável. | Alteração mantendo os mesmos valores: tratamento/idempotência não definido. | Alteração com dados inválidos: não persistir; campos e validações não especificados devem ser fechados. | Revalidar disponibilidade e conflitos para a nova alocação, inclusive concorrência; rejeição não deve deixar estado parcial (RNF-02). | Usuário sem permissão não altera reserva; reserva iniciada não pode ser apagada (RNF-07). |
| R-TDD-12 — Cancelamento registra motivo, responsável e horário; não permanece vigente | RF-04; RF-10; RNF-07 | Cancelamento aplicável registra motivo, responsável e horário; estado não é mais vigente. | “Justificativa quando aplicável”: quais cancelamentos exigem motivo é Pendente. | Cancelamento sem motivo quando obrigatório: resultado Pendente até decisão; não presumir obrigatoriedade universal. | Requisições repetidas/simultâneas de cancelamento: resultado final e idempotência Pendente; manter trilha correta. | Usuário sem permissão não cancela; não apagar reserva iniciada. |

### RF-05 — Conflitos de horário e disponibilidade

| ID / regra (rótulo editorial) | Origem | Feliz | Limite | Inválida | Conflito | Proibido / resultado esperado |
|---|---|---|---|---|---|---|
| R-TDD-13 — Reserva verifica sobreposição de sala | RF-05; RF-06; regra prioritária 2 | Período sem sobreposição pode prosseguir se os demais critérios forem atendidos. | Intervalos adjacentes que só se tocam no início/fim: Pendente; a semântica de extremidade não está definida. | Intervalo inválido (término não posterior ao início): não aceitar, conforme regra crítica da visão geral. | Sobreposição com reserva vigente para mesma sala: aviso claro e reserva conflitante bloqueada segundo a regra crítica da visão geral. | Não manter duas reservas conflitantes vigentes. |
| R-TDD-14 — Reserva verifica agenda individual/alocação do professor | RF-05; RF-03; regra prioritária 2 | Reserva pode prosseguir quando professor está disponível e sem conflito. | Limites da agenda e disponibilidade do professor: Pendente. | Período inválido: não aceitar. | Conflito de professor deve ser detectado mesmo quando sala/material estão disponíveis; aviso identifica professor e intervalo conflitante. | Não aceitar dupla alocação conflitante do mesmo professor. |
| R-TDD-15 — Reserva verifica sobreposição de material/equipamento e capacidade | RF-05; RF-02; RF-03 | Material/equipamento sem conflito e em quantidade compatível pode ser alocado. | Reserva simultânea de parte da quantidade, divisão por unidade e se conflito é por item ou estoque: Pendente. | Quantidade inválida/incompatível: rejeição de quantidade é esperada quando não satisfaz demanda; limites e resposta exata pendentes. | Sobreposição no mesmo material/equipamento impede conflito; a granularidade por quantidade deve ser decidida. | Não aceitar alocação acima da disponibilidade confirmada; regra detalhada de estoque/compartilhamento pendente. |
| R-TDD-16 — Reserva verifica conflito com manutenção e agenda | RF-05; RF-08 | Período fora de manutenção e sem conflito pode prosseguir. | Intervalo com borda no início/fim da manutenção: Pendente. | Período inválido: não aceitar. | Reserva que se cruza com manutenção é sinalizada ou rejeitada conforme regra a definir; na ausência de decisão, não considerar o recurso disponível para o período coincidente. | Não apresentar nem confirmar alocação conflitante com manutenção. |
| R-TDD-17 — Resultado de conflito é claro e bloqueante | RF-05; RF-13; RF-04 § 5 regra 2 | Operação sem conflito prossegue e feedback é explícito. | Mensagem deve indicar recurso e intervalo conflitante. | — | Requisição conflitante é rejeitada; RF-05 também permite encaminhamento para revisão, mas a visão geral/regra prioritária determinam bloqueio. Tratar como pendência de reconciliação, sem aceitar conflito enquanto isso. | Não confirmar reserva conflitante; não expor erro técnico na interface (RNF-01/RF-13). |

### RF-06 — Concorrência e dupla reserva

| ID / regra (rótulo editorial) | Origem | Feliz | Limite | Inválida | Conflito | Proibido / resultado esperado |
|---|---|---|---|---|---|---|
| R-TDD-18 — Criação/alteração é atômica e disponibilidade/gravação são protegidas | RF-06; RNF-02 | Uma requisição válida sem concorrência conclui com estado íntegro. | Testar o ponto de disputa com duas ou mais requisições simultâneas; volume além disso não foi fixado. | Falha/rejeição não deixa gravação parcial. | Duas requisições conflitantes ao mesmo recurso/período: exatamente uma pode ser aceita; uma é rejeitada explicitamente e a vencedora permanece válida. | Nunca persistir duas reservas conflitantes nem perder integridade por race condition. |
| R-TDD-19 — Conflito de concorrência preserva disponibilidade e integridade | RF-06; RNF-02 | Consultas posteriores refletem apenas a reserva válida. | Testar ordem invertida e sincronização próxima; mecanismo técnico permanece decisão de arquitetura. | Requisição rejeitada não altera estado ou disponibilidade. | Repetir teste de corrida para sala, professor, material e combinações aplicáveis. | Nenhum estado parcial/duplicado deve ficar visível. |

### RF-07 — Aprovação de recursos restritos

| ID / regra (rótulo editorial) | Origem | Feliz | Limite | Inválida | Conflito | Proibido / resultado esperado |
|---|---|---|---|---|---|---|
| R-TDD-20 — Recurso restrito exige aprovação do responsável/admin autorizado | RF-07; RF-01 | Reserva restrita nasce `PENDENTE`; perfil autorizado aprova e o estado passa a `APROVADA`. | Indicador `restrito` existe; critérios de negócio e quem pode configurá-lo permanecem pendentes. | Recurso restrito sem aprovação não pode nascer aprovada; motivo da decisão não está definido. | Conflito com outra reserva é verificado ao aprovar; pedidos pendentes sobrepostos podem coexistir. | Solicitante não aprova; autoaprovação é recusada. |
| R-TDD-21 — Estado reflete pendente, aprovado, rejeitado ou cancelado | RF-07; visão geral “Estados e auditoria” | API altera status via endpoint dedicado; unidade e integração testam aprovação/rejeição. | Fluxos de estado executáveis; matriz geral de perfis e motivo da decisão pendentes. | Transição inválida/não autorizada é rejeitada e preserva estado. | Política de concorrência para alterações simultâneas de estado ainda não foi formalizada. | Transição não autorizada ou aprovação sem validação de conflito é proibida. |

### RF-08 — Manutenção e bloqueios

| ID / regra (rótulo editorial) | Origem | Feliz | Limite | Inválida | Conflito | Proibido / resultado esperado |
|---|---|---|---|---|---|---|
| R-TDD-22 — Registrar manutenção com início, fim e descrição; status acompanha ciclo | RF-08; personas, Administrador | Administrador autorizado registra período e descrição; status pode refletir manutenção ativa, cancelada ou concluída. | Início/fim iguais, passados ou ordenação: Pendente; apenas manutenção passada deve ser registrada como encerrada. | Dados mínimos ausentes/inconsistentes devem ser rejeitados; validação temporal precisa de decisão. | Sobreposição de manutenções para o mesmo recurso: Pendente. | Perfil sem permissão não cria/edita bloqueio. |
| R-TDD-23 — Qualquer intervalo em manutenção torna recurso indisponível | RF-08; regra prioritária 5; visão geral | Fora da manutenção, disponibilidade segue demais regras. | No instante exato do início/fim, inclusive/exclusivo: Pendente; recurso é indisponível durante o período definido. | Período inválido não deve ser tratado como manutenção válida. | Reserva que cruza manutenção é sinalizada ou rejeitada; regra existente de disponibilidade impede tratar como livre. | Recurso não aparece disponível no calendário nem nos filtros e não pode ser reservado/usar durante indisponibilidade. |
| R-TDD-24 — Manutenção passada encerrada e histórico rastreável | RF-08; RF-10 | Consulta mostra status atual e histórico de bloqueios ao administrador autorizado. | Fim já ocorrido: status encerrado/concluído; prazo de atualização após fim: Pendente. | Usuário não autorizado não consulta/edita dados protegidos. | Manutenção que afeta reserva existente deve sinalizar ou rejeitar segundo RF-08; ação sobre reserva existente e notificações: Pendente. | Não apagar histórico de bloqueio sem justificativa e auditoria (RF-10). |

### RF-09 — Retirada e devolução de materiais/equipamentos

| ID / regra (rótulo editorial) | Origem | Feliz | Limite | Inválida | Conflito | Proibido / resultado esperado |
|---|---|---|---|---|---|---|
| R-TDD-25 — Registrar ciclo com material, responsável, horários, condição, observações e pendências | RF-09; regra prioritária 6 | Retirada e devolução autorizadas criam movimento consultável e atualizam status/histórico do item. | Data/hora de retirada e devolução: horários exatos, prazo esperado e fuso não definidos; devolução pode ainda não ter ocorrido. | Campos obrigatórios ausentes: tratamento deve ser validado por etapa (retirada vs. devolução); esquema temporal pendente. | Duas retiradas do mesmo item/estoque ou devolução repetida: regra de disponibilidade/idempotência Pendente. | Sem autenticação/autorização do responsável, retirada é negada; material não devolvido deve aparecer pendente. |
| R-TDD-26 — Atraso/dano no retorno é identificado e material mantém estado/movimentação | RF-09 | Devolução sem atraso/dano registra condição e histórico. | Prazo que caracteriza atraso e vocabulário de condição: Pendente. | Devolução sem condição/data/hora obrigatória: regra de rejeição/validação a fechar antes do teste final. | Registro de dano/atraso simultâneo: ambos devem ser identificados no histórico; tratamento posterior do item é Pendente. | Não apagar movimento nem ocultar material não devolvido da lista de pendências. |

### RF-10 — Auditoria

| ID / regra (rótulo editorial) | Origem | Feliz | Limite | Inválida | Conflito | Proibido / resultado esperado |
|---|---|---|---|---|---|---|
| R-TDD-27 — Registrar cada ação relevante com ator, data/hora, operação e antes/depois quando aplicável | RF-10; RNF-07; regra prioritária 7 | Para cadastro/alteração/exclusão, reserva, aprovação/rejeição, manutenção, retirada/devolução e mudança de perfil/permissão/dados sensíveis, verificar evento com campos obrigatórios. | “Antes/depois quando aplicável”: eventos/campos aplicáveis precisam de matriz; relógio/fuso e precisão não definidos. | Operação crítica sem ator, timestamp ou tipo de evento não satisfaz auditoria; gravação de negócio sem trilha obrigatória deve falhar ou ser recuperável conforme decisão ADR-001. | Falha entre operação e auditoria: verificar não haver sucesso sem trilha durável segundo recomendação ADR-001; mecanismo final não aprovado. | Ação não autorizada também é registrada (RF-01). Histórico deve ser consultável apenas por usuário autorizado. |
| R-TDD-28 — Alterações críticas não são apagadas sem justificativa e auditoria; reserva iniciada não pode ser apagada | RF-10; RNF-07; visão geral | Correção/retificação autorizada preserva histórico da mudança conforme política a definir. | Definição de “crítica”, “iniciada” e formato de justificativa: parcialmente pendente; estados principais constam da visão geral. | Tentativa de exclusão de reserva iniciada é negada. | Tentativa simultânea de alteração/exclusão e auditoria deve preservar histórico consistente. | Não remover eventos/histórico nem apagar reserva iniciada; exclusão crítica sem justificativa e auditoria é proibida. |

### RF-11 — Notificações

| ID / regra (rótulo editorial) | Origem | Feliz | Limite | Inválida | Conflito | Proibido / resultado esperado |
|---|---|---|---|---|---|---|
| R-TDD-29 — Informar criação, alteração, cancelamento, aprovação, conflito, rejeição e pendências | RF-11; RF-13 | Usuário recebe confirmação/aviso correspondente ao evento; pendência é encaminhada ao responsável/solicitante pertinente. | Destinatário e momento exatos para cada evento além do declarado: Pendente. | Destinatário/canal indisponível ou endereço inválido: tratamento, retry e falha visível Pendente. | Falha de envio não deve desfazer reserva já confirmada (arquitetura, proposta ADR-001; recomendação, não regra aprovada de tecnologia). | Não enviar informação sensível a usuário não autorizado; conteúdo mínimo de mensagem não está definido. |
| R-TDD-30 — Notificação pode ser simulada ou via API externa; pode ser registrada no histórico | RF-11; RF-10; RNF-04 | Verificar comportamento com notificação simulada ou integração selecionada; registrar no histórico quando essa opção for adotada. | Escolha de canal e obrigatoriedade do registro: Pendente. | Resposta inválida do provedor externo: formato de erro/retry Pendente. | Eventos repetidos/retries: deduplicação não especificada. | Provedor externo não deve ser assumido como obrigatório. |

### RF-12 — Relatórios

| ID / regra (rótulo editorial) | Origem | Feliz | Limite | Inválida | Conflito | Proibido / resultado esperado |
|---|---|---|---|---|---|---|
| R-TDD-31 — Relatórios de utilização, carga horária, aprovadas/canceladas, conflitos evitados e solicitações de aprovação | RF-12 | Cada indicador previsto pode ser consultado no relatório. | Período selecionado deve ser refletido; limites inclusivos/exclusivos de datas e fuso: Pendente. | Período inválido: validar e não calcular sobre intervalo sem sentido; formato de erro a definir. | Comparar reservas simultâneas, canceladas, rejeitadas e aprovadas: regras de inclusão e fórmula das métricas Pendente. | Métricas não podem ser expostas a perfil não autorizado. |
| R-TDD-32 — Relatórios autorizados, compreensíveis, comparáveis e visualizáveis/exportáveis | RF-12; RF-01 | Administrador ou responsável autorizado consulta e visualiza/exporta resultado. | Formato de exportação e critérios de comparabilidade: Pendente. | Perfil não autorizado é negado. | Dados alterados durante geração: consistência temporal/snapshot Pendente. | Solicitante ou outro perfil não autorizado não consulta dados restritos do relatório. |

### RF-13 — Interface, responsividade e mensagens

| ID / regra (rótulo editorial) | Origem | Feliz | Limite | Inválida | Conflito | Proibido / resultado esperado |
|---|---|---|---|---|---|---|
| R-TDD-33 — Interface utilizável em desktop, tablet e mobile; campos críticos sinalizados | RF-13; RNF-08 | Fluxos de consulta, reserva e acompanhamento permanecem utilizáveis nas classes de dispositivo. | Dimensões/resoluções, navegadores e critérios objetivos de usabilidade: Pendente. | Campo crítico ausente/inconsistente apresenta orientação para correção; validações concretas conforme RF aplicável. | Erro/conflito/bloqueio/rejeição exibe status e mensagem clara, sem jargão; texto e acessibilidade não possuem critérios detalhados. | Não expor detalhe técnico/sensível ao usuário (RNF-01). |
| R-TDD-34 — Feedback explícito após sucesso/rejeição e indicação dos passos de correção | RF-13; RF-05; RNF-08 | Confirmação visual após sucesso; rejeição e próximos passos em linguagem compreensível quando conhecidos. | Conteúdo por erro e passos para cada inconsistência: Pendente. | Entrada rejeitada deve ter mensagem clara, sem revelar detalhes técnicos. | Conflito identifica recurso e intervalo segundo RF-05. | Não apresentar rejeição como sucesso nem esconder estado resultante. |

### RF-14 — Documentação de contratos e fluxos

| ID / regra (rótulo editorial) | Origem | Feliz | Limite | Inválida | Conflito | Proibido / resultado esperado |
|---|---|---|---|---|---|---|
| R-TDD-35 — Documentar fluxos públicos e, quando aplicável, API, autenticação, exemplos, respostas e erros | RF-14; RNF-09 | Integrante do projeto/usuário final consegue consultar os fluxos públicos documentados. | Se API não existir, documentação de fluxos continua requerida; detalhes de formato/endpoint dependem da solução. | Documentação que omite operações, erros ou regras aplicáveis não satisfaz os critérios. | Alteração de contrato sem atualização da documentação falha na verificação de consistência. | Não documentar comportamento diferente do implementado nem apresentar contrato não aprovado como fato. |

## 3. Regras críticas transversais da visão geral

Estas regras estão explicitadas em `visão-geral.md` e complementam os RFs. Os
casos já vinculados acima devem ser rastreados também a estas fontes.

| ID editorial | Regra e origem | Casos e resultado esperado |
|---|---|---|
| R-TDD-36 | Término da reserva posterior ao início — visão geral, “Tempo e concorrência”; RF-04. | Caminho feliz: início antes do término é aceito se demais regras forem satisfeitas. Limite/inválido: término igual ou anterior é rejeitado; não persiste reserva. Duração mínima/máxima não especificada. |
| R-TDD-37 | Reservas do mesmo recurso não se sobrepõem, incluindo sala, material e professor — visão geral, “Tempo e concorrência”; RF-05. | Caminho feliz: intervalo sem conflito. Conflito: sobreposição é bloqueada. Limite de intervalos apenas adjacentes precisa de decisão. |
| R-TDD-38 | Duas solicitações simultâneas conflitantes produzem uma única reserva aceita — visão geral, “Tempo e concorrência”; RF-06/RNF-02. | Executar concorrência sincronizada: exatamente uma aceita, a outra rejeitada explicitamente; estado final sem gravação parcial. |
| R-TDD-39 | Recursos em manutenção não podem ser reservados — visão geral, “Tempo e concorrência”; RF-08. | Reserva parcialmente ou totalmente coincidente deve ser sinalizada/rejeitada e recurso não aparece disponível; bordas do período de manutenção pendentes. |
| R-TDD-40 | Fluxo `PENDENTE → APROVADA → EM_USO → CONCLUÍDA`; estados alternativos `REJEITADA`, `CANCELADA`, `NÃO_COMPARECEU` — visão geral, “Estados e auditoria”. `SOLICITADA` permanece como estado legado. | Fluxos principais, alternativos e transições inválidas estão executados em BDD/API; matriz completa de perfis e o momento de marcar falta ainda precisam de decisão. |
| R-TDD-41 | Administrador ou responsável autorizado decide reservas restritas; reservas iniciadas não podem ser apagadas; mudança de estado gera auditoria — RF-07/RF-10; RNF-07. | Aprovação/rejeição, autoaprovação e ator não autorizado têm testes. Exclusão de reserva iniciada e persistência de auditoria continuam sem implementação. |
| R-TDD-42 | Notificações e erros devem ser claros e compreensíveis — visão geral, regras prioritárias; RF-11/RF-13; RNF-08. | Para sucesso, conflito, bloqueio, rejeição e pendência, confirmar feedback correspondente e compreensível; conteúdo literal e critérios de leitura permanecem pendentes. |

## 4. Casos de verificação dos requisitos não funcionais

RNFs não são regras de negócio, mas têm critérios testáveis que precisam entrar
na estratégia e na rastreabilidade.

| Origem | Casos planejados e resultado esperado |
|---|---|
| RNF-01 — Segurança e autorização | Para operações protegidas, tentar acesso sem autenticação, com perfil sem permissão e por chamada direta à API: negar. Enviar entradas inválidas: validar antes do processamento. Verificar que resposta não expõe detalhe técnico/dado sensível e que tentativa não autorizada é registrada. Verificar ausência de vulnerabilidade crítica conhecida nos gates previstos. |
| RNF-02 — Integridade/concorrência | Reutilizar R-TDD-18/19/38; comprovar atomicidade com falha/conflito, no máximo uma reserva conflitante aceita e nenhuma gravação parcial. |
| RNF-03 — Desempenho mensurável | Executar JMeter em consulta de disponibilidade e criação de reserva, incluindo concorrência relevante; relatório contém duração, volume, falhas, cenários e carga. Não emitir veredito de conformidade quantitativa enquanto limites de carga, latência e taxa de erro não forem aprovados. |
| RNF-04 — Estratégia/evidência de testes | Planejar JUnit 5 e testes unitários parametrizados; integração; API caixa-preta; end-to-end; persistência via Testcontainers; integração externa via WireMock; mocks somente em testes unitários isolados. Incluir evidência de ao menos uma funcionalidade desenvolvida com TDD/BDD. Cobrir caminhos válidos, inválidos e conflitos aplicáveis. |
| RNF-05 — Cobertura e rastreabilidade | JaCoCo gera HTML/XML e aplica gates globais de 80% de linhas e 70% de branches em `verify`; estado de 2026-11-10: 87% linhas/71% branches. O SonarCloud passou no run `42859680966`. Manter RTM ligando 100% dos requisitos críticos a evidências; essa meta funcional ainda não está atendida. |
| RNF-06 — CI | Em pull request, GitHub Actions executa testes e verificações de qualidade configuradas; resultados disponíveis para consulta. |
| RNF-07 — Auditoria | Reusar R-TDD-27/28; toda mudança de estado gera auditoria identificável; reserva iniciada não é apagada; consulta de histórico restrita a usuários autorizados. Retenção e acesso detalhados estão pendentes. |
| RNF-08 — Responsividade | Executar fluxos cobertos em desktop, tablet e mobile; confirmar usabilidade, erros/conflitos/bloqueios/rejeições/confirmações compreensíveis e feedback explícito. Viewports/navegadores de aceite pendentes. |
| RNF-09 — Documentação | Reusar R-TDD-35; conferir cobertura e consistência dos fluxos/contratos públicos e atualizar documentação após alteração contratual. |

## 5. Rastreabilidade priorizada e ordem sugerida para TDD

1. **Reserva e integridade:** RF-04, RF-05, RF-06; RNF-02; regras críticas
   R-TDD-10–19 e R-TDD-36–39. Começar com validação temporal e conflito,
   prosseguir para concorrência com persistência realista.
2. **Autorização:** RF-01; RNF-01; regras R-TDD-01–03. Fechar matriz de acesso
   antes de fixar os casos por perfil.
3. **Aprovação, estados e manutenção:** RF-07/RF-08; regras R-TDD-20–24 e
   R-TDD-40–41. Fechar matriz de autorização, decisões pendentes e limites
   temporais.
4. **Auditoria e movimentação:** RF-09/RF-10; RNF-07; regras R-TDD-25–28.
   Incluir cenários de falha entre operação e persistência de auditoria após
   decisão de consistência.
5. **Notificação, relatório e interface:** RF-11–RF-13; RNF-08; regras
   R-TDD-29–34/R-TDD-42.
6. **Documentação e gates de qualidade:** RF-14; RNF-03–RNF-06/RNF-09;
   R-TDD-35 e seção 4.

Esta ordem é uma proposta de organização de TDD baseada nas regras críticas
documentadas; não altera prioridade de negócio nem aprova tecnologia.

### 5.1 Matriz de cobertura JaCoCo das regras de negócio

O gate cobre o bundle inteiro da aplicação. A revisão por classe separou as
regras de negócio/API dos fallbacks estruturais. O primeiro relatório apontou
lacunas de risco em `ReservaService` e `ManutencaoController`; os testes abaixo
fecharam os caminhos acessíveis, mantendo sem testes somente defesas cosméticas.

| Classe e ponto inicial | Regra relacionada | Lacuna e classificação | Teste/evidência após correção |
|---|---|---|---|
| `ReservaService.java:34` — 2 de 4 branches não cobertos: `dataInicio == null` e, com início presente, `dataFim == null`. | RF-04; R-TDD-10 | **Risco:** caminhos de entrada com uma das datas ausente não tinham evidência de rejeição. | `ReservaServiceTest.rejeitaReservaQuandoDataObrigatoriaEstaAusente` testa os dois casos, verifica a mensagem e prova que nenhum repositório é acessado. 4/4 branches cobertos. |
| `ReservaService.java:35` — exceção para datas obrigatórias sem execução. | RF-04; R-TDD-10 | **Risco:** não havia teste da resposta funcional para datas incompletas. | Mesmo teste parametrizado; a asserção confirma `RegraNegocioException` e a mensagem definida. Linha coberta. |
| `ManutencaoController.java:29-30` — faltavam os caminhos com recurso/ID ausente. | RF-08; R-TDD-22 | **Risco:** cadastro sem ID de recurso poderia não devolver rejeição explícita. | `rejeitaManutencaoSemIdentificadorDeRecurso` confirma HTTP 400, mensagem de erro e ausência de persistência; resta somente a ramificação de objeto `recurso` nulo, bloqueada por `@NotNull` antes da execução do controller. |
| `ManutencaoController.java:33` — lookup sem recurso persistido não exercitado. | RF-08; R-TDD-22 | **Risco:** referência inexistente precisava rejeitar cadastro sem gravar manutenção. | `retornaNotFoundAoCriarManutencaoParaRecursoInexistente` confirma HTTP 404, mensagem e ausência de persistência. Linha coberta. |

O relatório final ainda marca como **cosméticos** (sem teste dedicado):
`ManutencaoController.java:29`, ramo `recurso == null`, pois a validação
`@NotNull` rejeita o corpo antes do controller; `ApiExceptionHandler.java:50`
e `:52`, fallback para erros que não são `FieldError`; `:53-54`, mensagem
ausente em uma violação; `:59-60`, lista de erros vazia; e
`OrganizationResourcesApplication.java:10-11`, chamada do `main` não usada pelo
bootstrap dos testes Spring. Nenhum desses caminhos corresponde a uma regra de
negócio exercitada por entradas atuais. Após os testes, `ReservaService` tem
51/51 linhas e 16/16 branches cobertos. `ReservaRequestDTOTest` complementa a
fronteira temporal dos dois campos com `@ParameterizedTest` em `-1`, `0` e `+1`
segundos usando `Clock.fixed`, sem depender de espera ou relógio de parede.

No bundle histórico anterior à ampliação da suíte, o relatório era 101/105
linhas (96,2%) e 22/26 branches (84,6%). Esse valor não é o snapshot atual:
em 2026-11-10, o JaCoCo reportou 87% de linhas e 71% de branches.

| Regra / caso | Evidência automatizada atual | Resultado |
|---|---|---|
| RF-04; R-TDD-10 — datas obrigatórias | Datas de início e fim ausentes, com assert de erro e ausência de efeitos em repositórios. | Coberto |
| RF-04; R-TDD-10/R-TDD-36 — datas no passado, presente e futuro imediato | `ReservaRequestDTOTest`, ambos os campos, `-1/0/+1` segundos com relógio injetado. | Coberto |
| RF-05/RF-08; R-TDD-13/R-TDD-23 — conflitos de reserva e manutenção | Testes de serviço, integração e persistência existentes; branches do serviço exercitados. | Coberto |
| RF-08; R-TDD-22 — manutenção com recurso obrigatório e existente | HTTP 400 para ID ausente e HTTP 404 para recurso não cadastrado, ambos sem persistência. | Coberto |

## 6. Ambiguidades, lacunas e perguntas para decisão

Os itens abaixo aparecem como pendentes nos artefatos consultados. Os testes
dependentes deles não devem inventar um resultado esperado.

1. **Permissões (RF-01; personas):** quais ações cada perfil pode executar,
   sobre quais recursos/reservas, como usuários recebem perfis, se existe um
   perfil ou vários e quem pode alterar perfis?
2. **Dados e validações (RF-02/RF-03):** quais campos são obrigatórios por
   operação; intervalos permitidos para capacidade/quantidade; regra de
   duplicidade; domínio dos status, competências, tipos, estados e localização?
3. **Intervalos de tempo (RF-03–RF-08; visão geral):** intervalos são
   inclusivos ou semiabertos? Um evento pode começar exatamente no término de
   outro? Como tratar fuso, horário de verão, duração mínima/máxima e reservas
   que cruzam o início/fim de uma manutenção?
4. **Conflitos (RF-05):** RF-05 permite rejeitar ou encaminhar para revisão,
   enquanto a regra prioritária da visão geral exige bloquear sobreposição.
   Confirmar se encaminhamento significa somente revisão sem confirmação ou se
   há outro fluxo compatível.
5. **Material e quantidade (RF-02/RF-05):** conflito é por unidade identificada
   ou por estoque total? Como tratar reserva de quantidade parcial, estoque
   alterado, consumo e empréstimos concorrentes?
6. **Alteração/cancelamento (RF-04):** quais mudanças exigem justificativa,
   em que estados são permitidas, como repetir cancelamento, e qual o resultado
   de alterar uma reserva já aprovada/em uso?
7. **Estados (RF-07; visão geral):** quais são as transições completas e
   pré-condições para `EM_USO`, `CONCLUÍDA`, `NÃO_COMPARECEU`, rejeição e
   cancelamento? Qual estado uma reserva comum sem aprovação exige antes de ser
   confirmada?
8. **Aprovação (RF-07):** quais recursos/combinações são restritos, qual
   responsável aprova, quais motivos/prazos se aplicam e como tratar recusa,
   cancelamento ou corrida de decisões?
9. **Manutenção (RF-08):** como tratar reservas existentes afetadas, conflito
   entre períodos de manutenção, manutenção já passada e estados de
   cancelamento/conclusão?
10. **Retirada/devolução (RF-09):** quem pode devolver/acompanhar, como
    determinar atraso, condição/dano, campos obrigatórios por fase e regras para
    item já emprestado ou devolvido mais de uma vez?
11. **Auditoria (RF-10/RNF-07):** quais dados antes/depois registrar, o que é
    alteração crítica, quem pode consultar, por quanto tempo reter, como
    conciliar exclusão/minimização e como garantir atomicidade com a operação?
12. **Notificações (RF-11):** simulação ou integração escolhida, destinatário
    por evento, conteúdo, momento, registro obrigatório, retry/deduplicação e
    tratamento de indisponibilidade?
13. **Relatórios (RF-12):** fórmulas de utilização/carga/conflitos evitados,
    categorias de reservas incluídas, limites de período, formatos e regras de
    acesso?
14. **Interface/documentação (RF-13/RF-14):** viewports/navegadores de aceite,
    critérios observáveis de usabilidade, textos/idiomas, endpoints/formato
    público e códigos de erro esperados?
15. **Qualidade quantitativa (RNF-03/RNF-05):** qual perfil de carga, limite de
    latência e taxa de erro serão aprovados? A cobertura exige 80% linhas, 70%
    branches e 100% da lista de requisitos críticos, mas confirmar se a lista
    crítica é exatamente a seção “Regras críticas” da visão geral.
16. **Cron de treinamento (ASR-06, arquitetura):** a arquitetura registra um
    cron diário ao final do dia usando somente dados novos, mas informa que isso
    não está nos RF/RNF revisados. Confirmar se integra o escopo; definir
    finalidade, fonte, semântica de “novo”, fuso/horário, recuperação, avaliação
    e critérios antes de especificar testes de aceitação. Nenhum algoritmo,
    dado ou resultado do modelo é presumido neste plano.
17. **Parâmetros operacionais (RNF-03/RNF-06; arquitetura):** dependências,
    ambiente e limites operacionais não estão definidos; metas de
    disponibilidade/recuperação, navegadores, retenção e detalhes de
    implantação continuam decisões pendentes.

## 7. Artefatos inspecionados e inventário do repositório

Fontes do produto inspecionadas:

- `prd/requisitos-funcionais.md` — RF-01 a RF-14, regras de negócio prioritárias
  e pontos que precisam de validação.
- `prd/requisitos-não-funcionais.md` — RNF-01 a RNF-09, critérios de qualidade
  e parâmetros pendentes.
- `prd/personas.md` — Solicitante, Responsável, Administrador e pendências de
  permissões/fluxos.
- `prd/visão-geral.md` — regras críticas de concorrência, tempo, estados e
  auditoria, além da estratégia e metas de qualidade.
- `prd/arquitetura.md` — baseline, ASRs, propostas/ADRs e lacunas; propostas
  técnicas pendentes não foram tratadas como regras aprovadas.
- `AGENTS.md` e `README.md` — instruções e contexto do repositório.

Busca por implementação e testes do produto:

- Inspecionados os diretórios e arquivos `src/`, `tests/`, `test/` e `app/`,
  arquivos com nomes contendo `test`/`spec`, arquivos rastreados pelo Git e
  manifestos Java (`pom.xml`, Gradle), fontes Java e scripts de `package.json`.
- **Na inspeção inicial:** não havia código Java do produto, testes da aplicação
  nem configuração JUnit/Maven/Gradle. Não foram encontrados stories em
  `docs/stories/`.
- **Estado atual:** a API placeholder e a suíte RED original foram removidas.
   A aplicação Java/Spring Boot está em `src/main/java/br/edu/organizacaorecursos/`;
   os testes executáveis são `ReservaApiIntegrationTest`,
   `ReservaRequestDTOTest`, `ReservaServiceTest`, `SecurityConfigTest`,
   `ApiExceptionHandlerTest`, `RecursosPersistenceTest` e a suíte Cucumber.
   O repositório contém Maven Wrapper; a CI em `.github/workflows/ci.yml`
   executa `./mvnw -B verify`, SonarCloud e publica o relatório JaCoCo como
   artifact. Não há ADRs aprovados em arquivos separados.
   Os ADRs propostos em `prd/arquitetura.md` permanecem pendentes e não foram
   tratados como decisões.
- **Encontrados, mas fora do escopo funcional do produto:** testes internos do
  framework em `.aiox-core/core/permissions/__tests__/`,
  `.aiox-core/infrastructure/tests/` e
  `.aiox-core/workflow-intelligence/__tests__/`; não foram usados como evidência
  dos comportamentos de Organização de Recursos.
- **Pasta de documentação:** `prd/testes/` foi criada na etapa de planejamento.

## 8. Critério de planejamento

- Cada RF-01–RF-14 está rastreado a cenários verificáveis neste documento.
- Regras críticas transversais da visão geral estão ligadas às fontes e aos
  cenários correspondentes.
- RNF-01–RNF-09 estão incluídos na estratégia de verificação.
- Resultados não especificados estão explicitamente marcados como pendentes.
- O plano não define comportamento para lacunas ainda sem decisão.

## 9. Matriz regra → caso → teste → status (estado green parcial)

A matriz descreve a cobertura executável atual, não os 40 testes RED históricos,
que não existem mais no repositório. **PASS** significa que o caso indicado
passou na suíte atual; **PARCIAL** significa que somente parte da regra está
coberta; **BLOQUEADO** significa que dados, política ou critérios necessários
permanecem sem decisão ou que a funcionalidade não existe. Regras pendentes não
foram transformadas em comportamento presumido.

| Regra | Caso determinado exercitado | Teste JUnit atual | Status / cobertura restante |
|---|---|---|---|
| R-TDD-01 | Autenticação, autorização e chamadas diretas protegidas | `SecurityConfigTest.apiRequerBearerJwtSemCriarSessaoHttp`; testes de autorização de status em `ReservaApiIntegrationTest` | **PARCIAL**; JWT e decisões de reserva protegidas; matriz dos demais endpoints, escopo de leitura e auditoria de negativas pendentes. |
| R-TDD-02 | Perfil do usuário atribuído e persistido | Testes de cadastro/DTO de usuário e autorização da API | **PARCIAL**; enum e campo Perfil implementados; gerenciamento completo de identidade/perfis e matriz de acesso pendentes. |
| R-TDD-03 | Autorização administrativa/responsável por operação | `patchStatusAceitaResponsavelEPreservaConflitos`, `patchStatusPermiteAdministradorMesmoSemSerResponsavelDoRecurso`, `rejeitaAprovacaoPorUsuarioQueNaoEResponsavel` | **PARCIAL**; papéis cobertos no fluxo de decisão, não nos demais endpoints. |
| R-TDD-04 | Cadastro/listagem dos modelos simples de recurso e usuário; campos obrigatórios de Recurso | `criaEListaRecursosPelosEndpointsRest`, `criaEListaUsuariosPelaApi`, `rejeitaRecursoSemNomeOuTipoObrigatorios` | **PARCIAL**; sem atualização completa, duplicidade com erro de domínio e categorias sala/professor/material do RF-02. |
| R-TDD-05 | Nome/tipo de Recurso obrigatórios | `rejeitaRecursoSemNomeOuTipoObrigatorios` | **PARCIAL**; capacidade, localização e status de sala não fazem parte do modelo atual. |
| R-TDD-06 | Cadastro/consulta de professor com competência e agenda | — | **BLOQUEADO**; entidade e dados de professor não modelados. |
| R-TDD-07 | Cadastro/consulta de material, quantidade e empréstimo | — | **BLOQUEADO**; entidade, estoque e regra por unidade não modelados. |
| R-TDD-08 | Lista básica de recursos cadastrados | `criaEListaRecursosPelosEndpointsRest` | **PARCIAL**; filtros/status atualizados não implementados. |
| R-TDD-09 | Busca por tipo, capacidade, local, competência e disponibilidade | — | **BLOQUEADO**; atributos e critérios de filtro ausentes. |
| R-TDD-10 | Reserva válida exige datas, recurso e usuário existentes; regra condicional do status | `criaReservaRestritaComStatusPendente`, `criaReservaEDevolveDtoSemExporEntidades`, `criaReservaQuandoDadosSaoValidosERecursoEstaDisponivel`, `rejeitaReservaQuandoDataObrigatoriaEstaAusente`, `retornaBadRequestQuandoRecursoNaoExiste`, `rejeitaUsuarioInexistente` | **PASS** para criação, lookup e status inicial restrito/não restrito; categorias de recursos adicionais permanecem fora do modelo. |
| R-TDD-11 | Alterar reserva e revalidar autorização/disponibilidade | `atualizaReservaPendenteSomentePeloSolicitante` | **PARCIAL**; atualização de pendente é coberta; contrato completo, matriz de acesso e revalidação após mudança ainda pendentes. |
| R-TDD-12 | Cancelar com justificativa e refletir estado | Testes de integração de cancelamento e transição de estado | **PARCIAL**; cancelamento implementado; justificativa e regra de cancelamento para todos os estados não definidas. |
| R-TDD-13 | Rejeitar sobreposição no mesmo recurso e permitir período adjacente | `rejeitaPeriodosSobrepostosParaMesmoRecurso`, `permiteReservasAdjacentes` | **PASS** para o modelo atual de recurso e fórmula de intervalo adotada; agenda de professor/material ainda ausente. |
| R-TDD-14 | Conflito na agenda do professor | — | **BLOQUEADO**; modelo de agenda de professor ausente. |
| R-TDD-15 | Conflito de material/equipamento e quantidade | — | **BLOQUEADO**; regra de unidade/estoque ainda depende de decisão. |
| R-TDD-16 | Manutenção sobreposta impede nova reserva | `rejeitaReservaQuandoRecursoEstaEmManutencao`, `impedeReservaDuranteManutencao` | **PASS** para criação de reserva; limites adjacentes e busca de disponibilidade ainda não estão cobertos. |
| R-TDD-17 | Resposta de conflito identifica Recurso e intervalo reservado conflitante | `informaRecursoEIntervaloQuandoExisteConflitoDeReserva` | **PASS**; os detalhes do conflito acompanham a mensagem mantendo o campo `erro` existente. |
| R-TDD-18 | Reserva conflitante rejeitada sem gravação parcial | `rejeitaPeriodosSobrepostosParaMesmoRecurso` | **PASS** em integração H2; evidência com banco containerizado pendente. |
| R-TDD-19 | Duas criações simultâneas conflitantes aceitam exatamente uma | `aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas` | **PARCIAL**; testado localmente com H2, falta repetição/robustez com Testcontainers conforme RNF-04. |
| R-TDD-20 | Recurso restrito exige aprovação por perfil autorizado | `criaReservaRestritaComStatusPendente`, `patchStatusAceitaResponsavelEPreservaConflitos`, `patchStatusPermiteAdministradorMesmoSemSerResponsavelDoRecurso` | **PASS** para a regra restrita e perfis Admin/Responsável; conflitos entre pedidos pendentes ainda não têm política. |
| R-TDD-21 | Aprovar/rejeitar muda o estado da reserva | `ReservaServiceTest` e `ReservaApiIntegrationTest`; BDD-APR-02–04 | **PASS** para aprovação/rejeição e negativas de autorização; auditoria dos eventos segue pendente. |
| R-TDD-22 | Cadastro/listagem de manutenção, referência válida e campos requeridos | `listaManutencoesPelaApi`, `rejeitaManutencaoSemDatasOuDescricaoObrigatorias`, `rejeitaManutencaoSemIdentificadorDeRecurso`, `retornaNotFoundAoCriarManutencaoParaRecursoInexistente` | **PARCIAL**; caminhos de referência inválida cobertos; autorização administrativa e regras de estado/validade temporal seguem pendentes. |
| R-TDD-23 | Não confirmar reserva em período de manutenção | `impedeReservaDuranteManutencao` | **PASS** para criação de reserva; disponibilidade pesquisável ainda não implementada. |
| R-TDD-24 | Manutenção passada concluída e consultável no histórico | — | **BLOQUEADO**; estados, atualização e retenção pendentes. |
| R-TDD-25 | Retirada/devolução autorizada e pendente | — | **BLOQUEADO**; fluxo de empréstimo e estoque não modelados. |
| R-TDD-26 | Devolução registra dano e observações | — | **BLOQUEADO**; modelo e campos de devolução ausentes. |
| R-TDD-27 | Auditoria identifica ator, momento, operação e antes/depois | — | **BLOQUEADO**; JWT identifica/autentica requests, mas auditoria de negócio, retenção e consulta autorizada não estão implementadas. |
| R-TDD-28 | Impedir exclusão de reserva iniciada | `excluiReservaPorId` cobre apenas exclusão de uma reserva existente | **PARCIAL/INCOMPLETO**; estados existem, mas exclusão não verifica se a reserva já começou; não declarar esta proteção atendida. |
| R-TDD-29 | Notificações de reserva, conflito e aprovação | — | **BLOQUEADO**; eventos, canal e destinatários pendentes. |
| R-TDD-30 | Canais simulado e externo | — | **BLOQUEADO**; opção de integração não decidida. |
| R-TDD-31 | Relatório e métricas por período | — | **BLOQUEADO**; fórmulas e categorias não especificadas. |
| R-TDD-32 | Autorização e exportação de relatórios | — | **BLOQUEADO**; perfis, acesso e formato pendentes. |
| R-TDD-33 | Interface responsiva em desktop/tablet/mobile | — | **BLOQUEADO**; não existe UI; critérios de viewport/navegador pendentes. |
| R-TDD-34 | Respostas explícitas para campos inválidos, recurso/usuário ausente e conflito | `validaCamposObrigatoriosDoRequest`, `rejeitaManutencaoSemDatasOuDescricaoObrigatorias`, `retornaBadRequestQuandoRecursoNaoExiste`, `rejeitaUsuarioInexistente`, `informaRecursoEIntervaloQuandoExisteConflitoDeReserva` | **PARCIAL**; status/campos de erro exercitados, UX e passos de correção não definidos. |
| R-TDD-35 | OpenAPI e Swagger expõem endpoints atuais | `publicaDocumentacaoOpenApiEEndpointsDeReserva` | **PARCIAL**; documentação básica passa; autenticação, exemplos/erros completos e verificação de sincronismo permanecem pendentes. |
| R-TDD-36 | Início no passado e fim igual/anterior rejeitados; presente/futuro imediato válido | `ReservaRequestDTOTest.validaDatasNaFronteiraDoRelogioInjetado`, `rejeitaDataDeInicioNoPassado`, `rejeitaDataFimIgualAoInicio`, `rejeitaDataFimAnteriorAoInicio`, `criaReservaEDevolveDtoSemExporEntidades` | **PASS**; os campos de início/fim são testados em -1, 0 e +1 segundo com `Clock.fixed`; duração mínima/máxima não definida. |
| R-TDD-37 | Impedir sobreposição no mesmo recurso | `rejeitaPeriodosSobrepostosParaMesmoRecurso`, `permiteReservasAdjacentes` | **PASS** para as reservas do recurso modelado; professor/material seguem sem implementação. |
| R-TDD-38 | Concorrência resulta em uma única reserva aceita | `aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas` | **PARCIAL**; H2 local passa; falta Testcontainers e medição de concorrência em ambiente realista. |
| R-TDD-39 | Recurso em manutenção não é reservado | `impedeReservaDuranteManutencao` | **PASS** no fluxo de criação; o catálogo de disponibilidade/busca não existe. |
| R-TDD-40 | Fluxo `PENDENTE → APROVADA → EM_USO → CONCLUÍDA` e estados alternativos | BDD-EST-01–03; testes de integração de transição | **PARCIAL**; transições atuais testadas; `SOLICITADA` permanece como legado e matriz completa de perfis por transição pendente. |
| R-TDD-41 | Admin/Responsável decide; auditoria; reserva iniciada não pode ser apagada | Testes de API cobrem papéis e decisão; exclusão/auditoria sem evidência | **PARCIAL**; decisão está coberta; auditoria persistida e proteção contra exclusão de reserva iniciada não implementadas. |
| R-TDD-42 | Mensagens de sucesso, conflito e rejeição compreensíveis | `criaReservaEDevolveDtoSemExporEntidades`, `informaRecursoEIntervaloQuandoExisteConflitoDeReserva`, `validaCamposObrigatoriosDoRequest` | **PARCIAL**; resposta explícita e contexto de conflito verificados; conteúdo/UX de outros fluxos pendentes. |

### 9.1 Matriz dos requisitos não funcionais

| Origem | Caso / teste associado | Status |
|---|---|---|
| RNF-01 | R-TDD-01/R-TDD-03: autorização, acesso direto e negativa | **PARCIAL**; JWT e autorização de reserva restrita cobertos; matriz completa, auditoria de negativas e escopo de leitura pendentes. |
| RNF-02 | R-TDD-18/R-TDD-19/R-TDD-38: conflito, concorrência e rejeição | **PARCIAL**; atomicidade/conflito e concorrência verificadas com H2; Testcontainers pendente. |
| RNF-03 | JMeter: consulta/reserva, métricas e relatório | **SEM TESTE — bloqueado**; aplicação, JMeter e limites aprovados ausentes. |
| RNF-04 | JUnit, integração/API, E2E, Testcontainers, WireMock e evidência TDD/BDD | **PARCIAL**; 12 invocações unitárias (Mockito/validação), 26 de API e 1 de persistência; E2E, Testcontainers, WireMock e evidência formal de TDD/BDD não completos. |
| RNF-05 | JaCoCo, cobertura mínima, RTM crítica e SonarCloud | **PARCIAL**; JaCoCo 87% linhas/71% branches e SonarCloud verde no run `42859680966`; requisitos críticos não estão todos implementados/evidenciados. |
| RNF-06 | GitHub Actions em pull requests | **COBERTO** quanto à execução CI verificada no run `42859680966`; esta evidência não substitui os requisitos funcionais faltantes. |
| RNF-07 | R-TDD-27/R-TDD-28/R-TDD-41: auditoria e reserva iniciada | **BLOQUEADO**; auditoria persistida, política de retenção e proibição de excluir reserva iniciada não implementadas; autenticação JWT existe. |
| RNF-08 | R-TDD-33/R-TDD-34: responsividade e mensagens | **SEM TESTE para responsividade; parcial para mensagem**; UI ausente e critérios objetivos pendentes. |
| RNF-09 | R-TDD-35: contratos/documentação em sincronia | **PARCIAL**; OpenAPI/Swagger existe, mas faltam verificação de sincronismo e documentação completa de autenticação, exemplos e erros. |

**ASR-06 — cron diário usando somente dados novos:** **SEM TESTE**. Consta
apenas como restrição explicitada na solicitação e está marcada na arquitetura
como ausente dos RF/RNF; finalidade, fonte e semântica de “dados novos”
continuam sem decisão. Não foi convertida em teste de negócio.

## 10. Histórico RED e verificação green atual

### Histórico RED

Uma versão anterior deste plano registrou `mvn -q test` com 40 falhas de
asserção contra um placeholder sem lógica. Os testes daquela execução foram
removidos quando a aplicação Spring Boot substituiu o placeholder; portanto,
o resultado é histórico e não pode ser reproduzido a partir do repositório
atual. Ele não comprova os comportamentos da aplicação presente.

### Verificação atual

- Comando executado: `./mvnw -B verify`.
- Resultado: **BUILD SUCCESS — 92 testes, 0 falhas, 0 erros, 0 ignorados**.
- Distribuição reportada na suíte atual: integração 49, serviço 10, DTO 6,
   segurança 7, exceção 3, persistência 1 e Cucumber 16.
- JaCoCo: 87% de linhas e 71% de branches; os gates configurados são 80%/70%.
   Relatórios em `target/site/jacoco/index.html` e `target/site/jacoco/jacoco.xml`.
- Prova de falha do gate: `mvn -B clean -Dtest=ReservaServiceTest#rejeitaDataFimAnteriorAoInicio verify` falhou com 4% de linhas e 11% de branches; `mvn -B clean verify` restaurou o build verde e a cobertura acima das metas.
- O run CI `42859680966` no commit `eb2bced` passou por Maven verify,
  SonarCloud e publicação do relatório JaCoCo. As regras marcadas **PARCIAL**
  ou **BLOQUEADO** não são declaradas concluídas; dependem de cobertura
  adicional, funcionalidade ou decisões ainda pendentes nos requisitos.
