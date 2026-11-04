# Plano TDD — Sistema de Organização de Recursos

> **Status:** fase red iniciada. Foram criados testes JUnit 5 e uma API
> placeholder sem lógica de negócio. Resultados e cobertura por identificador
> estão registrados na matriz ao final; pendências e verificações fora do
> escopo unitário permanecem explicitamente sem teste.
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
| R-TDD-02 — Perfil atribuído é único ou conjunto autorizado | RF-01 | Usar cada forma de perfil permitida pelo cadastro/configuração. | Pendente: não há limite ou modelo escolhido para quantidade/composição dos perfis. | Perfil não autorizado ou configuração incompatível: comportamento a definir; não inferir se rejeita cadastro ou autenticação. | Usuário com múltiplos perfis e permissões sobrepostas: Pendente; validar matriz de autorização. | Solicitante não pode aprovar solicitação restrita. |
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
| R-TDD-20 — Recurso/combinação restrita exige aprovação do responsável | RF-07; RF-01 | Responsável autorizado aprova e a reserva passa ao estado aprovado/confirmado conforme fluxo definido. | Tipos de restrição e quem é responsável por cada recurso: Pendente; os exemplos do RF não fecham a lista. | Recurso restrito sem aprovação não pode ser confirmado; motivo da exigência deve ser informado. | Aprovação concorrente com cancelamento/rejeição ou nova reserva: prioridade/ordem de transição Pendente. | Solicitante não aprova; recurso restrito não é comprometido antes da aprovação final. |
| R-TDD-21 — Estado reflete pendente, aprovado, recusado ou cancelado | RF-07; visão geral “Estados e auditoria” | Cada ação autorizada reflete o estado correspondente e gera histórico conforme RF-10/RNF-07. | Transições completas e estado inicial exato: Pendente; RF-07 lista os estados, visão geral também lista estados alternativos. | Ação sem dados/motivo necessários: requisitos de motivo para recusa/cancelamento Pendente. | Ações simultâneas sobre a mesma solicitação não podem produzir estado contraditório; regra de concorrência de estados Pendente. | Transição não autorizada ou confirmação sem aprovação é proibida. |

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
| R-TDD-40 | Fluxo principal `SOLICITADA → APROVADA → EM_USO → CONCLUÍDA`; estados alternativos `REJEITADA`, `CANCELADA`, `NÃO_COMPARECEU` — visão geral, “Estados e auditoria”. | Testar transições previstas quando ações e pré-condições forem especificadas. Caminho e nomes de estados são registrados; quais perfis/condições permitem iniciar, concluir ou marcar falta, e transições entre estados alternativos, são pendentes. Não presumir transições adicionais. |
| R-TDD-41 | Apenas responsáveis podem aprovar reservas restritas; reservas iniciadas não podem ser apagadas; mudança de estado gera auditoria — visão geral, “Estados e auditoria”; RF-07/RF-10; RNF-07. | Perfil não responsável tenta aprovar: negar. Tentativa de apagar reserva iniciada: negar. Cada mudança de estado: criar registro que identifique operação, usuário e momento. |
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
| RNF-05 — Cobertura e rastreabilidade | Coletar JaCoCo; metas 80% linhas e 70% branches. Manter RTM ligando 100% dos requisitos críticos a evidências. Verificar qualidade comportamental dos testes, não só percentuais. Integração SonarCloud deve estar presente. |
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
   R-TDD-40–41. Formalizar transições e limites temporais.
4. **Auditoria e movimentação:** RF-09/RF-10; RNF-07; regras R-TDD-25–28.
   Incluir cenários de falha entre operação e persistência de auditoria após
   decisão de consistência.
5. **Notificação, relatório e interface:** RF-11–RF-13; RNF-08; regras
   R-TDD-29–34/R-TDD-42.
6. **Documentação e gates de qualidade:** RF-14; RNF-03–RNF-06/RNF-09;
   R-TDD-35 e seção 4.

Esta ordem é uma proposta de organização de TDD baseada nas regras críticas
documentadas; não altera prioridade de negócio nem aprova tecnologia.

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
- **Criados nesta fase red:** `pom.xml` configura JUnit 5; a API mínima
  `src/main/java/br/edu/organizacaorecursos/OrganizationResourceService.java`
  contém apenas tipos e assinaturas com retorno placeholder, sem regras de
  negócio; os testes ficam em
  `src/test/java/br/edu/organizacaorecursos/OrganizationResourceServiceRedTest.java`.
  O package-lock preexistente não foi usado.
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

## 9. Matriz regra → caso → teste → status (fase red)

**Status RED** significa que o teste compilou e falhou em asserção porque o
placeholder ainda não fornece o resultado esperado. Não significa que a regra
foi implementada ou que a falha foi reproduzida contra uma implementação real.
Nenhum teste passou; portanto, não há caso aprovado por execução nesta fase.

| Regra | Caso determinado exercitado | Teste JUnit | Status / cobertura restante |
|---|---|---|---|
| R-TDD-01 | Acesso permitido, usuário não autenticado, chamada direta e tentativa negada auditável | `R_TDD_01_authorizesPermittedUserAndDeniesUnauthenticatedOrDirectApiAccess` | **RED**; validar também mensagem/campos completos da tentativa após contrato de auditoria. |
| R-TDD-02 | Perfil único e conjunto de perfis autorizados | `R_TDD_02_acceptsSingleAndAuthorizedMultipleRoles` | **RED**; composição e precedência de múltiplos perfis pendentes. |
| R-TDD-03 | Permissão administrativa versus solicitante em operação de gestão e mudança de perfil | `R_TDD_03_requiresSpecificManagementAuthorizationAndProtectsRoleChanges` | **RED**; matriz de permissões e mudança autorizada de perfil pendentes. |
| R-TDD-04 | Cadastro válido, duplicado e inconsistente | `R_TDD_04_registersValidResourcesAndRejectsDuplicateOrInconsistentRecords` | **RED**; regras de duplicidade e CRUD completo ainda parciais. |
| R-TDD-05 | Dados mínimos válidos/ausentes de sala | `R_TDD_05_registersRoomWithRequiredFieldsAndRejectsMissingFields` | **RED**; limites de capacidade/status pendentes. |
| R-TDD-06 | Dados mínimos válidos/ausentes de professor | `R_TDD_06_registersTeacherWithRequiredFieldsAndRejectsMissingFields` | **RED**; semântica de agenda/competência pendente. |
| R-TDD-07 | Dados mínimos válidos/ausentes de material | `R_TDD_07_registersMaterialWithRequiredFieldsAndRejectsMissingFields` | **RED**; unidade, quantidade e regras de empréstimo pendentes. |
| R-TDD-08 | Filtro de sala compatível e resultado atual | `R_TDD_08_searchReflectsCurrentResourceStatusAndFilters` | **RED**; consistência temporal/cache e filtros restantes parcialmente cobertos em R-TDD-09. |
| R-TDD-09 | Filtro de capacidade/local e competência, exclui recurso bloqueado | `R_TDD_09_searchAppliesCapacityLocationAndTeacherCompetenceFilters` | **RED**; tipo, quantidade, localização, competência e data/horário; combinações e entradas inválidas ainda parciais. |
| R-TDD-10 | Criar reserva válida para recurso disponível | `R_TDD_10_createsBookingForAvailableResourceAndValidPeriod` | **RED**; limite temporal coberto em R-TDD-36; campos mínimos pendentes. |
| R-TDD-11 | Alteração autorizada e novo período | `R_TDD_11_amendsOnlyAuthorizedBookingAndRevalidatesAvailability` | **RED**; cenário de ator não autorizado e ausência de gravação parcial não exercitados isoladamente. |
| R-TDD-12 | Cancelamento com justificativa e estado cancelado | `R_TDD_12_cancelsBookingWithReasonAndReturnsCanceledState` | **RED**; quando justificativa é obrigatória e repetição concorrente pendentes. |
| R-TDD-13 | Conflito de sala e intervalo não conflitante | `R_TDD_13_detectsRoomOverlapAndAllowsNonConflictingPeriod` | **RED**; semântica de intervalos adjacentes pendente. |
| R-TDD-14 | Conflito de agenda do professor | `R_TDD_14_detectsTeacherAgendaConflict` | **RED**; limites de agenda pendentes. |
| R-TDD-15 | Conflito de material/equipamento no mesmo período | `R_TDD_15_detectsMaterialOverlapConflict` | **RED**; quantidade por unidade/estoque não testada, regra ainda pendente. |
| R-TDD-16 | Manutenção sobreposta torna recurso indisponível | `R_TDD_16_detectsMaintenanceOverlapAndDoesNotTreatResourceAsAvailable` | **RED**; bordas do intervalo de manutenção pendentes. |
| R-TDD-17 | Rejeição explícita e mensagem identifica recurso/intervalo | `R_TDD_17_conflictResponseIdentifiesResourceAndConflictingInterval` | **RED**; decisão rejeitar versus encaminhar para revisão precisa ser consolidada. |
| R-TDD-18 | Requisição conflitante rejeitada sem resposta de sucesso | `R_TDD_18_rejectedBookingConflictDoesNotProducePartialSuccess` | **RED**; persistência/ausência de estado parcial exige teste de integração, ainda não disponível. |
| R-TDD-19 | Duas requisições simultâneas produzem uma aceita | `R_TDD_19_simultaneousConflictingRequestsAcceptExactlyOne` | **RED**; concorrência real com persistência/Testcontainers não implementada nem validada. |
| R-TDD-20 | Recurso restrito não confirma antes da aprovação; apenas Responsável aprova | `R_TDD_20_restrictedResourceRequiresResponsibleApprovalBeforeConfirmation` | **RED**; catálogo de restrições, motivo e corrida de decisões pendentes. |
| R-TDD-21 | Aprovação e rejeição refletem estados correspondentes | `R_TDD_21_approvalAndRejectionUpdateBookingState` | **RED**; transições completas e pré-condições pendentes. |
| R-TDD-22 | Administrador registra manutenção com campos requeridos | `R_TDD_22_authorizedAdministratorRegistersMaintenanceWithRequiredFields` | **RED**; validação temporal completa pendente. |
| R-TDD-23 | Recurso em manutenção indisponível | `R_TDD_23_resourceIsUnavailableDuringMaintenance` | **RED**; bordas de data/horário pendentes. |
| R-TDD-24 | Manutenção passada termina como concluída e aparece em histórico | `R_TDD_24_pastMaintenanceIsCompletedAndRemainsAuditable` | **RED**; prazo de atualização e acesso/retenção pendentes. |
| R-TDD-25 | Retirada autorizada, não autorizada e devolução pendente | `R_TDD_25_checksOutMaterialsOnlyWithResponsibleAuthorizationAndTracksPendingReturn` | **RED**; disponibilidade por unidade e prazos pendentes. |
| R-TDD-26 | Devolução com dano registra condição/observações | `R_TDD_26_returnRecordsDamagedConditionAndObservations` | **RED**; atraso não testado: não há prazo esperado definido. |
| R-TDD-27 | Histórico identifica ator, momento e operação após mudança | `R_TDD_27_auditsRelevantChangesWithActorTimeOperationAndBeforeAfter` | **RED**; atomicidade, antes/depois aplicável e retenção precisam de integração/decisão. |
| R-TDD-28 | Não apagar reserva iniciada nem evento crítico sem justificativa | `R_TDD_28_doesNotDeleteStartedBookingOrCriticalAuditWithoutReason` | **RED**; critério de alteração crítica e trilha de exclusão pendentes. |
| R-TDD-29 | Notificações para criação, conflito e pendência/aprovação | `R_TDD_29_notifiesUsersAboutBookingAndApprovalEvents` | **RED**; destinatário/momento por evento e falha do canal pendentes. |
| R-TDD-30 | Canais simulado e API externa como opções | `R_TDD_30_supportsSimulatedAndExternalNotificationChannels` | **RED**; seleção real do canal, retry e idempotência pendentes. |
| R-TDD-31 | Consulta de relatório no período selecionado | `R_TDD_31_reportsExpectedMetricsForSelectedPeriod` | **RED**; fórmulas, valores e limites de período não testados por falta de definição. |
| R-TDD-32 | Acesso autorizado e negado aos relatórios | `R_TDD_32_allowsAuthorizedReportAccessAndDeniesUnauthorizedAccess` | **RED**; matriz de acesso pendente. |
| R-TDD-33 | Responsividade desktop/tablet/mobile | **Sem teste** | **SEM TESTE — bloqueado:** não existe UI nem framework/browser de E2E; não é verificável por JUnit unitário. |
| R-TDD-34 | Feedback não nulo para entrada inválida e conflito | `R_TDD_34_returnsFeedbackForValidationAndConflictOutcomes` | **RED / parcial**; presença testada, compreensão e passos de correção dependem de critérios de UX. |
| R-TDD-35 | Consistência dos documentos com fluxos/contratos públicos | **Sem teste** | **SEM TESTE — bloqueado:** não há contrato/API implementado ou ferramenta de validação documental configurada. |
| R-TDD-36 | Término igual/anterior rejeitado; término posterior permitido | `R_TDD_36_requiresBookingEndAfterStart` | **RED**; duração mínima/máxima não definida. |
| R-TDD-37 | Sobreposição do mesmo recurso é bloqueada | `R_TDD_37_blocksOverlappingReservationForSameResource` | **RED**; intervalos adjacentes pendentes. |
| R-TDD-38 | Concorrência com exatamente uma reserva aceita | `R_TDD_38_concurrentConflictingRequestsYieldExactlyOneAcceptedReservation` | **RED**; teste unitário do contrato; persistência concorrente ainda sem teste de integração. |
| R-TDD-39 | Recurso em manutenção não aparece disponível nem é confirmado | `R_TDD_39_neverOffersOrConfirmsResourceDuringMaintenance` | **RED**; conflito com reserva existente e semântica das bordas pendentes. |
| R-TDD-40 | Fluxo documentado de estados principais | `R_TDD_40_allowsDocumentedMainBookingStateFlow` | **RED**; estados alternativos e demais transições pendentes. |
| R-TDD-41 | Só Responsável aprova; mudança auditada; reserva iniciada não apagada | `R_TDD_41_onlyResponsibleApprovesAndStateChangesAreAudited` | **RED**; transações de auditoria e regras de transição dependem de integração/decisão. |
| R-TDD-42 | Mensagem presente em sucesso, conflito e rejeição | `R_TDD_42_notifiesWithUnderstandableOutcomeForSuccessConflictAndRejection` | **RED / parcial**; clareza semântica não pode ser verificada sem critérios/conteúdo aprovado. |

### 9.1 Matriz dos requisitos não funcionais

| Origem | Caso / teste associado | Status |
|---|---|---|
| RNF-01 | R-TDD-01/R-TDD-03: autorização, acesso direto e negativa | **Parcial/RED**; sanitização de entradas, erros sem dados sensíveis e scan de vulnerabilidade não implementados. |
| RNF-02 | R-TDD-18/R-TDD-19/R-TDD-38: conflito, concorrência e rejeição | **Parcial/RED**; atomicidade real de persistência não pode ser verificada sem banco e integração. |
| RNF-03 | JMeter: consulta/reserva, métricas e relatório | **SEM TESTE — bloqueado**; aplicação, JMeter e limites aprovados ausentes. |
| RNF-04 | JUnit: testes unitários/parametrizados criados; arquitetura exige também integração, API, E2E, Testcontainers, WireMock e evidência TDD/BDD | **Parcial**; apenas os testes unitários de contrato existem; fronteiras restantes sem aplicação/integradores. |
| RNF-05 | JaCoCo, cobertura mínima, RTM crítica e SonarCloud | **SEM TESTE — bloqueado**; configuração, implementação e lista aprovada de requisitos críticos não existem. |
| RNF-06 | GitHub Actions em pull requests | **SEM TESTE — bloqueado**; workflow de CI do produto não configurado. |
| RNF-07 | R-TDD-27/R-TDD-28/R-TDD-41: auditoria e reserva iniciada | **Parcial/RED**; histórico/imutabilidade ainda sem armazenamento real e política de retenção. |
| RNF-08 | R-TDD-33/R-TDD-34: responsividade e mensagens | **SEM TESTE para responsividade; parcial para mensagem**; UI ausente e critérios objetivos pendentes. |
| RNF-09 | R-TDD-35: contratos/documentação em sincronia | **SEM TESTE — bloqueado**; API/contrato público implementado não existe. |

**ASR-06 — cron diário usando somente dados novos:** **SEM TESTE**. Consta
apenas como restrição explicitada na solicitação e está marcada na arquitetura
como ausente dos RF/RNF; finalidade, fonte e semântica de “dados novos”
continuam sem decisão. Não foi convertida em teste de negócio.

## 10. Resultado da execução red

- Comando: `mvn -q test`.
- Compilação: concluída; JUnit executou os 40 métodos identificados `R-TDD`.
- Resultado: **40 testes, 40 falhas de asserção, 0 erros, 0 ignorados**.
- Todas as falhas são asserções de comportamento contra resultados ausentes no
  placeholder (`null`); o cenário concorrente agrega zero reservas aceitas em
  vez de uma. Nenhuma falha foi causada por erro de compilação ou exceção
  inesperada.
- **Testes que passaram:** nenhum. Logo, não houve teste previamente verde a
  investigar como caso mal escrito ou regra implementada.
- As falhas confirmam que o placeholder está sem comportamento; não comprovam
  a semântica de integração, persistência, UI ou infraestrutura.
