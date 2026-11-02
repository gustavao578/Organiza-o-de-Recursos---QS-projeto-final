# Arquitetura — baseline inicial

> Esta é a seção inicial de análise arquitetural, não uma decisão de arquitetura detalhada. Requisitos e decisões são citados pelas fontes existentes; lacunas permanecem abertas. Este documento não altera os requisitos funcionais ou não funcionais.

## 1. Contexto e objetivo

O Sistema de Organização de Recursos deve apoiar a consulta, alocação e gestão de salas, professores e materiais, com reservas e prevenção de conflitos de horário. O projeto é semestral e sua avaliação inclui evidências de qualidade, segurança, rastreabilidade e desempenho. A visão geral registra Java 21 e Spring Boot 3.x como tecnologias previstas, mas informa que arquitetura, persistência, autenticação e integrações ainda dependem de definição ([visão-geral.md](visão-geral.md), “Visão do produto” e “Contexto técnico e entrega”; [requisitos-não-funcionais.md](requisitos-não-funcionais.md), §§ 2 e 4).

Esta análise organiza os fatores que condicionam a arquitetura e explicita o que ainda precisa de decisão, sem escolher tecnologias ou estratégias não aprovadas.

**Restrição explícita desta solicitação:** existe um cron job executado diariamente ao final do dia para treinar ou retreinar usando somente dados novos. Essa restrição foi fornecida pelo usuário nesta solicitação; ela não aparece nos requisitos funcionais, nos requisitos não funcionais ou na visão geral revisados. Seu domínio, dados, modelo e critérios operacionais ainda não estão definidos. Registrá-la aqui não altera o escopo funcional aprovado.

## 2. Stakeholders e personas

| Perfil / stakeholder | Responsabilidade ou interesse documentado | Origem |
|---|---|---|
| **Solicitante** | Professor ou coordenador; consulta disponibilidade, cria, altera e cancela as próprias reservas. | [personas.md](personas.md), § 1; RF-03, RF-04 |
| **Responsável** | Avalia solicitações especiais, valida alocação de docentes e acompanha retiradas e devoluções de materiais. | [personas.md](personas.md), § 2; RF-07, RF-09 |
| **Administrador** | Mantém cadastros de salas, professores, materiais e usuários, além de bloqueios e manutenções. | [personas.md](personas.md), § 3; RF-02, RF-08 |
| **Equipe do projeto** | Desenvolve o projeto em equipe e consulta documentação de fluxos e contratos. Não é um perfil funcional do sistema. | [visão-geral.md](visão-geral.md), “Contexto técnico e entrega”; RF-14 |

As permissões detalhadas, a designação do Responsável e a confirmação de que esses são todos os perfis permanecem pendentes ([personas.md](personas.md), “Pendências transversais para validação”; RF-01).

## 3. Requisitos arquiteturalmente significativos

Os identificadores ASR abaixo servem apenas para organizar esta análise; não substituem nem modificam os identificadores RF/RNF existentes.

| ID | Necessidade que condiciona a arquitetura | Origem e rastreabilidade |
|---|---|---|
| ASR-01 | Impedir sobreposição de reservas e preservar integridade sob solicitações simultâneas: no máximo uma reserva conflitante pode ser aceita. | RF-05, RF-06; regras prioritárias 1–3 de [requisitos-funcionais.md](requisitos-funcionais.md); RNF-02; “Tempo e concorrência” em [visão-geral.md](visão-geral.md). |
| ASR-02 | Autenticar usuários e aplicar autorização por perfil às operações e também às chamadas fora da interface. | RF-01; RNF-01; personas de [personas.md](personas.md). |
| ASR-03 | Não confirmar reservas de recursos restritos antes da aprovação do Responsável; bloquear recursos durante manutenção. | RF-07, RF-08; regras prioritárias 4–5 de [requisitos-funcionais.md](requisitos-funcionais.md). |
| ASR-04 | Manter histórico consultável e rastreável de operações relevantes e mudanças de estado, sem apagar reservas iniciadas. | RF-10; RNF-07; “Estados e auditoria” em [visão-geral.md](visão-geral.md). |
| ASR-05 | Produzir evidências de teste e qualidade rastreáveis: JUnit 5, testes de integração/API/end-to-end, Testcontainers, WireMock, JaCoCo, SonarCloud e CI em pull requests; medir desempenho com JMeter. | RNF-03 a RNF-06; estratégia de qualidade e metas em [visão-geral.md](visão-geral.md). |
| ASR-06 | Executar diariamente, ao final do dia, um cron job de treinamento ou retreinamento, usando somente dados novos. | Restrição explícita da solicitação atual do usuário; ausente dos artefatos de requisitos revisados. |
| ASR-07 | Oferecer notificações simuladas ou integração com API externa e documentar fluxos públicos e, quando aplicável, contratos de API. | RF-11, RF-14; RNF-09. A opção de integração ainda não foi decidida. |

## 4. Atributos de qualidade

| Atributo | Expectativa documentada | Fonte / limite conhecido |
|---|---|---|
| **Segurança e autorização** | Restringir dados e operações conforme identidade e perfil; validar entradas e não expor detalhes sensíveis em erros. | RNF-01, RF-01; permissões por perfil ainda precisam de detalhamento. |
| **Integridade e concorrência** | Evitar dupla reserva e alterações parciais em operações conflitantes. | RNF-02, RF-05, RF-06. |
| **Desempenho mensurável** | Avaliar consulta de disponibilidade e criação de reserva com JMeter; registrar duração, volume e falhas. | RNF-03. Limites de resposta, carga, dados e taxa de erro não foram aprovados. |
| **Testabilidade e evidência** | Testes automatizados nas fronteiras pertinentes, cobertura com JaCoCo, RTM para requisitos críticos e execução de CI em pull requests. | RNF-04 a RNF-06; metas registradas na visão geral: 80% de linhas, 70% de branches e 100% de requisitos críticos cobertos pela RTM. |
| **Auditabilidade** | Identificar operação, usuário responsável e momento da ação; manter histórico consultável por usuários autorizados. | RNF-07, RF-10. Retenção e permissões de consulta ainda não estão definidas. |
| **Usabilidade e responsividade** | Manter fluxos utilizáveis em desktop, tablet e mobile, com mensagens compreensíveis e feedback explícito. | RNF-08, RF-13. Navegadores suportados ainda não foram definidos. |
| **Documentação** | Manter documentação dos fluxos públicos e contratos coerente com o comportamento implementado. | RNF-09, RF-14. |

## 5. Drivers arquiteturais priorizados

Prioridade abaixo é uma organização inicial para orientar decisões; não é uma nova priorização de negócio nem altera o status dos requisitos.

| Prioridade | Driver | Origem |
|---|---|---|
| **P1 — obrigatório** | Integridade de reservas sob conflito e concorrência. | RF-05, RF-06; regras prioritárias 1–3 de [requisitos-funcionais.md](requisitos-funcionais.md); RNF-02; regras críticas da [visão-geral.md](visão-geral.md). |
| **P1 — obrigatório** | Rotina diária de treinamento/retreinamento ao final do dia, limitada a dados novos. | Solicitação explícita do usuário nesta tarefa; não consta do baseline dos RF/RNF. |
| **P1 — obrigatório** | Autenticação, autorização por perfil e proteção de dados/operações. | RF-01; RNF-01; personas de [personas.md](personas.md). |
| **P1 — obrigatório** | Auditabilidade de reservas, recursos, decisões e movimentações de materiais. | RF-09, RF-10; RNF-07; regras de estados e auditoria da [visão-geral.md](visão-geral.md). |
| **P2 — evidência exigida** | Verificabilidade por testes, cobertura, rastreabilidade e CI. | RNF-04 a RNF-06; estratégia e metas da [visão-geral.md](visão-geral.md). |
| **P2 — mensuração exigida; limites pendentes** | Medir desempenho nos fluxos de disponibilidade e reserva. | RF-03, RF-04, RF-06; RNF-03; JMeter previsto na [visão-geral.md](visão-geral.md). |
| **P3 — comportamento exigido** | Fluxos responsivos e mensagens claras para os três perfis. | RF-13; RNF-08; necessidades em [personas.md](personas.md). |
| **P3 — decisão de integração pendente** | Notificar usuários e documentar fluxos/contratos públicos. | RF-11, RF-14; RNF-09. |

## 6. Glossário

| Termo | Significado no contexto documentado |
|---|---|
| **Recurso** | Sala, professor, material ou equipamento gerenciado ou alocado pelo sistema. |
| **Reserva** | Solicitação para alocar recursos em um período; pode ter estados e aprovação conforme as regras aplicáveis. |
| **Recurso restrito** | Recurso ou combinação de uso que exige aprovação do Responsável antes da confirmação (RF-07); os critérios para classificá-lo ainda não foram definidos. |
| **Conflito / sobreposição** | Interseção de horários de reservas ou indisponibilidades de um mesmo recurso, incluindo a agenda do professor (RF-05). |
| **RTM** | Matriz de rastreabilidade de requisitos, usada para relacionar requisitos críticos às evidências de teste (RNF-05). |
| **Cron job** | Tarefa agendada referida na solicitação atual para executar diariamente ao final do dia o treinamento/retreinamento usando somente dados novos. |
| **Dados novos** | Dados que a rotina diária pode usar conforme a restrição explícita do usuário. Critério de novidade, origem, janela e tratamento de repetição ainda não definidos. |

## 7. Riscos iniciais

| ID | Risco | Evidência / origem |
|---|---|---|
| R-01 | A rotina de treinamento/retreinamento pode estar fora do escopo atual de organização de recursos; não há RF/RNF que descreva domínio de modelo ou dados de treinamento. | Comparação da restrição da solicitação atual com o escopo de [visão-geral.md](visão-geral.md), RF-01 a RF-14 e RNF-01 a RNF-09. |
| R-02 | “Final do dia” e “somente dados novos” não definem fuso horário, corte temporal, fonte, deduplicação, falhas ou recuperação; implementações diferentes podem violar a restrição. | Solicitação atual do usuário; sem especificação operacional correspondente nos artefatos consultados. |
| R-03 | Sem estratégia arquitetural já decidida para concorrência, a aplicação pode aceitar reservas conflitantes ou deixar gravações parciais. | RF-05, RF-06; RNF-02; estratégia ainda pendente em RF, § 6. |
| R-04 | Ausência de limites quantitativos de desempenho impede um aceite objetivo de latência, carga ou taxa de erro. | RNF-03, seção “Parâmetros pendentes de aprovação”. |
| R-05 | Perfis, retenção de auditoria, política para reservas existentes diante de manutenção e comportamento da notificação ainda não estão definidos, afetando autorização, dados e contratos. | RF, § 6; RNF, § 5; pendências em [personas.md](personas.md). |

## 8. Fatos, lacunas, conflitos e suposições

### Fatos documentados

- O sistema organiza salas, professores e materiais e deve evitar conflitos de horário (visão geral; RF-05 e RF-06).
- Os perfis documentados são Solicitante, Responsável e Administrador ([personas.md](personas.md), §§ 1–3).
- Java 21 e Spring Boot 3.x são a stack **prevista**, não uma decisão detalhada de componentes ([visão-geral.md](visão-geral.md), “Contexto técnico e entrega”; RNF, § 4).
- Testes, ferramentas e metas de qualidade estão especificados nos RNF e na visão geral; metas de desempenho quantitativo não estão fixadas (RNF-03 a RNF-06).
- A rotina diária de treinamento usando somente dados novos é uma restrição expressa pelo usuário nesta solicitação, não um requisito existente nos arquivos de requisitos.

### Lacunas

- Banco de dados, persistência, autenticação concreta, arquitetura de implantação e contratos de integração não foram decididos (visão geral; RNF, § 4).
- Os limites de desempenho e as políticas de disponibilidade, recuperação, retenção e compatibilidade de navegadores permanecem pendentes (RNF, § 5).
- Os critérios de negócio e as permissões ainda não estão completos (RF, § 6; [personas.md](personas.md), “Pendências transversais para validação”).
- Para a rotina agendada, não estão especificados dados, modelo, critério de novidade, fuso/horário exato ou comportamento em falhas (solicitação atual).

### Conflitos e ambiguidades identificados

- **Não foi encontrado conflito textual inequívoco** entre os RF e RNF consultados. Há, porém, pontos que exigem conciliação antes de decisões arquiteturais:
  - A visão geral apresenta `SOLICITADA → APROVADA → EM_USO → CONCLUÍDA`, enquanto RF-07 condiciona aprovação a recursos restritos. Não está explícito como reservas sem restrição percorrem os estados.
  - RF-05 deixa a rejeição ou revisão de conflitos conforme regra a definir; RF-06 exige que, em concorrência conflitante, apenas uma operação seja aceita. A política para encaminhamento à revisão não está especificada.
  - O treinamento/retreinamento diário foi solicitado agora, mas o escopo documental existente é a organização de recursos. A relação da rotina com o produto precisa ser confirmada; a restrição é registrada sem presumir integração ou funcionalidade adicional.

### Suposições

- Nenhuma suposição de tecnologia, volume, disponibilidade, SLA, fonte de dados ou comportamento de negócio foi adotada.
- Java 21 e Spring Boot 3.x são tratados somente como tecnologias previstas nos documentos, não como decisões adicionais desta análise.
- A restrição do cron job é tratada como obrigatória porque foi explicitamente imposta pelo usuário; detalhes não fornecidos permanecem em aberto.

## 9. Perguntas abertas

1. A rotina de treinamento/retreinamento pertence ao escopo deste sistema? Qual dado/modelo será treinado e qual origem define “dados novos”?
2. Qual horário e fuso representam “ao final do dia”? O que fazer se a rotina falhar ou encontrar dados já processados?
3. Como o fluxo de estados se aplica a reservas que não exigem aprovação?
4. Em que casos um conflito deve ser rejeitado imediatamente ou encaminhado para revisão, sem permitir duas reservas válidas?
5. Quais perfis e permissões finais se aplicam, inclusive à consulta de auditoria?
6. Quais limites quantitativos de desempenho, carga e taxa de erro serão aprovados?
7. São necessários requisitos de disponibilidade, backup e recuperação? Se sim, quais metas?
8. Qual política de retenção e acesso será aplicada ao histórico auditável?
9. Qual mecanismo de notificação será usado: simulação no sistema ou integração externa?
10. Qual banco de dados, mecanismo de autenticação e arquitetura de implantação serão escolhidos?

## Referências consultadas

- [personas.md](personas.md)
- [requisitos-funcionais.md](requisitos-funcionais.md)
- [requisitos-não-funcionais.md](requisitos-não-funcionais.md)
- [visão-geral.md](visão-geral.md)

## 10. Avaliação ATAM independente

> Análise acrescentada em 2026-11-02. Preserva as evidências anteriores e não
> altera os RF/RNF. A avaliação é independente e baseada nos documentos
> consultados; não equivale a validação por execução ou prova da arquitetura.
> Não existem arquitetura detalhada, dados de produção ou medições para validar
> comportamentos. Todo limiar quantitativo que não conste dos requisitos é
> **hipótese pendente de aprovação**, não meta aceita.

### 10.1 Drivers, atributos prioritários e abordagens consideradas

| Driver | Atributos prioritários | Abordagem arquitetural a avaliar | Evidência / limite |
|---|---|---|---|
| Evitar conflito e dupla reserva em operação simultânea | Integridade, consistência, feedback | Validar e persistir em unidade atômica; arbitrar conflito no limite de persistência e retornar rejeição explícita. Estratégia física (constraint, lock ou serialização) depende do banco ainda não escolhido. | RF-05, RF-06, RNF-02; não há banco escolhido. |
| Executar o treinamento diário somente com dados novos | Correção do processamento, recuperabilidade, rastreabilidade | Watermark/cursor persistido, janela fechada por run, manifesto e checkpoint; definir formalmente “novo” antes de implementar. | ASR-06 é solicitação explícita, ausente de RF/RNF. |
| Operar o job sem duplicidade ou sobreposição prejudicial | Consistência, disponibilidade do pipeline, recuperabilidade | Idempotência por run/janela e exclusão mútua com fencing; escolha de scheduler/lease não definida. | Lacuna operacional de ASR-06; sem requisito para mecanismo específico. |
| Impedir promoção de candidato inadequado | Qualidade do modelo, segurança operacional, auditabilidade | Treinar candidato separado do ativo, avaliar conforme critérios aprovados, promover como passo controlado e permitir retornar à versão anterior. | Não há métricas preditivas, processo de aprovação ou RF/RNF de ML. |
| Proteger dados e operações por perfil | Segurança, privacidade, auditabilidade | Autorizar no backend e nos acessos do batch/registry; minimizar dados e auditar operações privilegiadas. | RF-01, RF-10; RNF-01, RNF-07; política LGPD e retenção pendentes. |
| Medir desempenho, qualidade e conformidade | Testabilidade, observabilidade, desempenho | Usar evidências automatizadas e medições (incluindo JMeter para os fluxos previstos), sem declarar conformidade antes de aprovar cargas e limites. | RNF-03 a RNF-06; metas de cobertura existentes não são metas de latência. |

### 10.2 Utility tree

Prioridade usa **Importância** e **Dificuldade** qualitativas: Alta / Média / Baixa.
Importância deriva de requisito explícito ou do impacto operacional do cron
solicitado. Dificuldade estima incerteza e interação de decisões ainda abertas,
não esforço de implementação. Folhas e métricas sem critério aprovado indicam
medidas a coletar e pontos que precisam de decisão, não SLAs.

| Ramo de qualidade | Folha / cenário | Importância | Dificuldade | Justificativa |
|---|---|---:|---:|---|
| Integridade | AT-01: duas reservas conflitantes simultâneas | Alta | Alta | RF-06/RNF-02 exigem garantia de exclusão; estratégia de persistência está aberta. |
| Correção / dados | AT-02: execução diária no horário e período aprovados | Alta | Média | ASR-06 torna a execução diária obrigatória, mas horário, fuso e recuperação estão abertos. |
| Correção / dados | AT-03: processar exclusivamente dados novos | Alta | Alta | Restrição explícita; definição de novidade e cursor não existem. |
| Recuperabilidade | AT-04: falha parcial e retomada sem perda ou duplicidade | Alta | Alta | Necessária para cumprir execução incremental; sem política aprovada de checkpoint/retry. |
| Concorrência / disponibilidade | AT-05: execuções sobrepostas ou worker com lease vencida | Alta | Alta | Risco de checkpoint e promoção concorrentes; mecanismo de exclusão não decidido. |
| Qualidade dos dados | AT-06: dados degradados ou mudança de esquema | Alta | Alta | Sem limites de qualidade nem comportamento aprovado de quarentena/bloqueio. |
| Qualidade do modelo | AT-07: candidato pior que versão ativa | Alta | Alta | Sem métricas, baseline, avaliação ou regra de promoção definidos. |
| Recuperabilidade / auditabilidade | AT-08: rollback de modelo promovido | Alta | Média | Proposta de segurança operacional; rollback não aparece como RF/RNF. |
| Disponibilidade | AT-09: fonte, storage, registry ou runtime indisponível | Alta | Alta | Deve-se evitar corrupção e promoção parcial; disponibilidade/SLA e fallback não definidos. |
| Segurança / privacidade | AT-10: vazamento ou acesso indevido a dados/artefatos | Alta | Alta | RNF-01 e RF-01 exigem proteção; escopo de dados e controles LGPD não definidos. |
| Desempenho | AT-11: consulta e criação de reserva sob carga | Média | Alta | Medição JMeter é requerida (RNF-03), mas carga e limites permanecem pendentes. |
| Auditabilidade | AT-12: reconstruir origem e decisão de um run/modelo | Alta | Média | Rastreabilidade de negócio é exigida; lineage de ML é proposta, sem contrato aprovado. |
| Evolução / usabilidade | AT-13: mensagens claras e uso responsivo | Média | Média | RF-13/RNF-08; critérios de compatibilidade e medida de usabilidade não existem. |

### 10.3 Cenários de qualidade

Formato: **fonte, estímulo, ambiente, artefato, resposta e métrica**. Métricas
descrevem observáveis. Quando um limiar não consta dos requisitos, a coluna o
marca como pendente; nenhum valor é presumido.

#### AT-01 — Reserva concorrente (Importância alta; Dificuldade alta)

- **Fonte:** dois ou mais solicitantes autenticados.
- **Estímulo:** submetem reservas incompatíveis para o mesmo recurso e período
  com sobreposição temporal.
- **Ambiente:** fluxo online normal, com requisições concorrentes.
- **Artefato:** API de reservas, validação de disponibilidade e persistência.
- **Resposta:** no máximo uma reserva conflitante é aceita; as demais recebem
  rejeição explícita e não deixam gravações parciais.
- **Métrica:** número de reservas conflitantes aceitas por conjunto de
  requisições simultâneas (critério requerido: no máximo uma); número de
  operações rejeitadas com estado parcial (critério requerido: zero); latência
  e carga são coletadas, mas limites dependem de aprovação.
- **Rastreabilidade:** RF-05, RF-06, RNF-02; ASR-01.

#### AT-02 — Disparo diário (Importância alta; Dificuldade média)

- **Fonte:** scheduler configurado.
- **Estímulo:** chega o horário diário de treinamento/retreinamento.
- **Ambiente:** operação normal no fuso e janela lógica que ainda serão
  aprovados.
- **Artefato:** scheduler, orquestrador e registro de execução.
- **Resposta:** iniciar ou registrar a execução esperada com run identificável;
  não iniciar execução duplicada para a mesma janela; registrar resultado e
  permitir detectar execução ausente ou atrasada.
- **Métrica:** execuções iniciadas/concluídas por janela lógica; diferença entre
  horário aprovado e início/conclusão; duplicatas por janela. Horário, tolerância
  de atraso e taxa aceitável são **pendentes**, não valores definidos.
- **Rastreabilidade:** solicitação explícita do cron diário; ASR-06.

#### AT-03 — Somente dados novos (Importância alta; Dificuldade alta)

- **Fonte:** fonte de dados que vier a ser aprovada.
- **Estímulo:** job diário consulta dados elegíveis para treino.
- **Ambiente:** execução normal com watermark/checkpoint persistido e registros
  dentro e fora da janela.
- **Artefato:** extrator, filtro de elegibilidade, manifesto e checkpoint.
- **Resposta:** processar somente a janela aprovada, excluir registros já
  consumidos segundo a definição aprovada e preservar evidência reproduzível da
  seleção; não avançar cursor de modo que registros elegíveis sejam perdidos.
- **Métrica:** IDs/registros processados fora da janela ou repetidos; registros
  elegíveis omitidos; correspondência entre cursor e manifesto. Critério esperado
  para fora da janela/duplicidade: zero conforme definição de novidade; a
  semântica da janela e tratamento de late-arriving data estão pendentes.
- **Rastreabilidade:** ASR-06; definição de “dados novos” em aberto.

#### AT-04 — Falha parcial e retomada (Importância alta; Dificuldade alta)

- **Fonte:** falha de processo, rede ou armazenamento em uma fase do job.
- **Estímulo:** a execução falha após leitura, transformação, treino ou publicação
  de candidato, mas antes de confirmar todas as fases.
- **Ambiente:** execução batch com checkpoint anterior válido e possibilidade de
  retry ou retomada.
- **Artefato:** worker, estado do run, manifesto, artefatos temporários e
  checkpoint.
- **Resposta:** não promover candidato incompleto nem confirmar checkpoint sem
  saída recuperável; retomar com mesma entrada lógica ou refazer a fase de modo
  idempotente; conservar o modelo ativo.
- **Métrica:** registros perdidos/duplicados após recuperação; runs sem estado
  terminal; checkpoint avançado além da saída durável; versão ativa alterada
  durante falha. Critério desejado para perda, duplicidade e promoção inválida:
  zero; tempo máximo de recuperação e número de retries estão pendentes.
- **Rastreabilidade:** ASR-06; estratégia de checkpoint/retry é proposta, não RF.

#### AT-05 — Jobs sobrepostos (Importância alta; Dificuldade alta)

- **Fonte:** scheduler duplicado, retry, atraso de execução ou worker antigo.
- **Estímulo:** uma segunda execução reivindica a mesma janela enquanto a primeira
  ainda está ativa, ou um worker continua após expiração de seu lock.
- **Ambiente:** implantação com uma ou mais instâncias e possibilidade de
  atraso/reinício.
- **Artefato:** scheduler, lease/lock, fencing token, checkpoint e registry.
- **Resposta:** somente o proprietário válido da lease pode confirmar checkpoint
  ou promover versão; a execução concorrente é recusada, anexada ao run existente
  ou coalescida conforme política aprovada.
- **Métrica:** escritores válidos por janela, atualizações de checkpoint por
  token expirado e promoções concorrentes; critérios de integridade: um escritor
  válido e zero commit/promoção de token obsoleto. Política de coalescência e
  duração de lease estão pendentes.
- **Rastreabilidade:** ASR-06; não há mecanismo de lock escolhido.

#### AT-06 — Dados degradados (Importância alta; Dificuldade alta)

- **Fonte:** sistema produtor, alteração de esquema ou processo de ingestão.
- **Estímulo:** chegam dados ausentes, duplicados, fora de domínio, com drift ou
  incompatíveis com o esquema esperado.
- **Ambiente:** execução batch antes do treino.
- **Artefato:** validação de esquema/qualidade, quarentena, treinador e
  checkpoint.
- **Resposta:** classificar e reportar a falha; bloquear treino quando condição
  aprovada for impeditiva; não descartar silenciosamente; preservar dados
  inválidos e checkpoint conforme política de recuperação.
- **Métrica:** contagens e proporções por regra de qualidade, esquema divergente,
  registros em quarentena, runs bloqueados e dados inválidos usados em treino.
  Limites para bloqueio/aviso e tolerância são pendentes de aprovação.
- **Rastreabilidade:** ASR-06; não há requisito de qualidade de dados de ML.

#### AT-07 — Candidato pior (Importância alta; Dificuldade alta)

- **Fonte:** pipeline de treinamento/avaliação.
- **Estímulo:** candidato treinado no delta é inferior ao modelo ativo ou não
  satisfaz uma métrica de aceitação.
- **Ambiente:** avaliação offline com protocolo e baseline que precisam ser
  aprovados.
- **Artefato:** avaliador, registry, alias/versão ativa e processo de promoção.
- **Resposta:** marcar candidato como não elegível/rejeitado; não alterar modelo
  ativo; guardar evidências e motivo para auditoria.
- **Métrica:** resultado de cada critério aprovado, decisão de elegibilidade,
  alterações do alias e diferenças em relação à versão ativa. Métricas,
  baseline, tolerâncias e critérios de aceite **não estão definidos**.
- **Rastreabilidade:** ASR-06; validação/promoção de ML são lacunas.

#### AT-08 — Rollback (Importância alta; Dificuldade média)

- **Fonte:** operador autorizado ou procedimento aprovado.
- **Estímulo:** modelo promovido apresenta regressão, incompatibilidade ou falha
  operacional.
- **Ambiente:** runtime de inferência em uso, se confirmado; versão anterior
  aprovada ainda retida e compatível.
- **Artefato:** registry, alias ativo, runtime e trilha de auditoria.
- **Resposta:** restaurar referência a versão anterior aprovada, registrar ator,
  motivo e versões antes/depois; não alterar reservas nem apagar lineage.
- **Métrica:** versão ativa antes/depois, sucesso/falha da operação, tempo entre
  autorização e restauração, erros de compatibilidade e presença do registro
  auditável. Tempo-alvo e retenção mínima estão pendentes.
- **Rastreabilidade:** proposta de governança decorrente de ASR-06; rollback não
  é requisito atual.

#### AT-09 — Indisponibilidade de dependência (Importância alta; Dificuldade alta)

- **Fonte:** falha de fonte de dados, armazenamento de artefato, registry ou
  runtime de inferência.
- **Estímulo:** componente deixa de responder durante extração, publicação,
  promoção ou inferência.
- **Ambiente:** execução diária ou serviço online, conforme o componente afetado.
- **Artefato:** worker/API, checkpoint, registry, referência de modelo ativo e
  tratamento de erro.
- **Resposta:** não avançar checkpoint sem dados duráveis; não apontar produção a
  artefato incompleto; manter última versão aprovada; retornar erro explícito ou
  aplicar fallback somente se previamente aprovado. Não presumir fallback que
  possa contornar regras obrigatórias de reserva.
- **Métrica:** fase, erros, duração da indisponibilidade, checkpoint, versão
  ativa, disponibilidade observada e estado do run. SLA, RTO/RPO e política de
  fallback são pendentes.
- **Rastreabilidade:** ASR-01 e ASR-06; RNF-02; requisitos de disponibilidade/
  recuperação ainda não definidos.

#### AT-10 — Vazamento ou acesso indevido (Importância alta; Dificuldade alta)

- **Fonte:** usuário não autorizado, credencial comprometida, log/manifesto
  excessivamente detalhado ou exportação indevida.
- **Estímulo:** tentativa de consultar, alterar, copiar ou inferir dados/artefatos
  fora da permissão concedida.
- **Ambiente:** API online, worker, storage, registry, logs e backups.
- **Artefato:** autenticação/autorização, segredos, controles de acesso, logs,
  dataset/artefato e auditoria.
- **Resposta:** negar e registrar tentativa; limitar exposição; não incluir
  valores pessoais brutos desnecessários em logs/lineage; acionar procedimento
  de incidente que vier a ser aprovado.
- **Métrica:** acessos negados/permitidos por perfil, operações privilegiadas
  sem autorização, campos pessoais expostos em saída/log e cobertura de revisão
  de acesso. Critério de acesso não autorizado bem-sucedido: zero; retenção,
  classificação de dados, base legal e tempos de resposta a incidente estão
  pendentes.
- **Rastreabilidade:** RF-01, RF-10; RNF-01, RNF-07; aspectos legais e operacionais
  ainda não avaliados.

#### AT-11 — Desempenho online (Importância média; Dificuldade alta)

- **Fonte:** Solicitante consulta disponibilidade ou envia reserva.
- **Estímulo:** execução do fluxo sob carga concorrente representativa.
- **Ambiente:** perfil de carga e volume de dados a serem definidos para JMeter.
- **Artefato:** UI/API, disponibilidade, persistência e mecanismo de concorrência.
- **Resposta:** fornecer resultados corretos, sem aceitar conflito, e registrar
  latência, volume e falhas observadas.
- **Métrica:** duração por requisição, taxa de falha e volume/carga do teste,
  conforme RNF-03. Valores de carga, latência e taxa de erro são parâmetros
  pendentes; não há limite inferido nesta análise.
- **Rastreabilidade:** RF-03, RF-04, RF-06; RNF-03.

#### AT-12 — Linhagem de execução (Importância alta; Dificuldade média)

- **Fonte:** Responsável operacional ou auditor autorizado.
- **Estímulo:** precisa explicar ou reproduzir um candidato, uma promoção ou uma
  falha de execução.
- **Ambiente:** após execução concluída, rejeitada ou interrompida.
- **Artefato:** `run_id`, checkpoint, manifesto, versão de código/configuração,
  avaliação, hash e histórico do registry.
- **Resposta:** localizar janela/dados de entrada, pipeline e versões,
  resultado/decisão e ator sem expor PII desnecessária.
- **Métrica:** campos de lineage ausentes, artefatos cujo hash não corresponde,
  runs não correlacionáveis e capacidade de reconstruir a entrada lógica. Campos
  obrigatórios e retenção são decisões pendentes.
- **Rastreabilidade:** RNF-07/RF-10 para auditoria de negócio; lineage de ML é
  proposta em ASR-06.

#### AT-13 — Responsividade e comunicação (Importância média; Dificuldade média)

- **Fonte:** Solicitante, Responsável ou Administrador usando desktop, tablet ou
  dispositivo móvel.
- **Estímulo:** consulta, reserva, conflito, bloqueio ou rejeição.
- **Ambiente:** formatos de tela previstos; navegadores ainda não definidos.
- **Artefato:** interface e contrato de erro da API.
- **Resposta:** apresentar estado e ação seguinte em linguagem compreensível e
  manter o fluxo utilizável.
- **Métrica:** cenários de fluxo aprovados por dispositivo, mensagens sem
  explicação útil, erros de usabilidade observados e navegadores testados.
  Critérios de aceite e navegadores suportados devem ser aprovados; não se
  inventam percentuais de sucesso.
- **Rastreabilidade:** RF-13; RNF-08; personas.

### 10.4 Pontos de sensibilidade

Um ponto de sensibilidade é uma decisão/parâmetro cujo valor pode afetar
significativamente um atributo ou cenário.

| Ponto | Efeito e cenários | Decisão/valor ainda pendente |
|---|---|---|
| Semântica de “dados novos” e cursor | Define inclusão/exclusão, perda por eventos tardios e repetição (AT-03, AT-04). | Criados vs. alterados vs. eventos inéditos; cursor estável; janela e lateness. |
| Momento de confirmação do checkpoint | Define custo de repetição e possibilidade de perda ao falhar entre fases (AT-04, AT-09). | Após snapshot durável, treino, avaliação ou publicação; precisa de semântica explícita de recuperação. |
| Exclusão mútua/lease/fencing | Define possibilidade de dois escritores ou promoção obsoleta (AT-05). | Tecnologia, validade, heartbeat, retomada e política de coalescência. |
| Regra de conflito transacional | Determina se concorrência pode produzir dupla reserva (AT-01). | Banco e mecanismo de integridade; deve satisfazer RF-06/RNF-02 independentemente da tecnologia. |
| Regras de qualidade de dados | Afetam bloqueio, quantidade de dados de treino e disponibilidade de execução (AT-06). | Campos, limites, severidade e política de quarentena. |
| Algoritmo, avaliação e gate | Determinam se treino apenas no delta é válido e quando não promover (AT-03, AT-07). | Métricas, baseline, holdout/protocolo e autoridade. |
| Alias/versão ativa e compatibilidade | Determina risco de promoção e tempo/viabilidade de rollback (AT-07, AT-08, AT-09). | Atomicidade de troca, retenção e compatibilidade de feature/runtime. |
| Escopo de dados e autorização | Determina impacto de vazamento e possibilidade de cumprir finalidade/retensão (AT-10, AT-12). | Inventário, base legal, minimização, acesso e prazos com responsáveis competentes. |
| Carga e limites de recursos | Afeta latência online, duração do batch e competição entre API e worker (AT-02, AT-11). | Perfil JMeter, volumes, recursos de execução e limites de custo. |
| Fuso, horário e catch-up | Define atraso operacional e duplicidade lógica do cron (AT-02, AT-05). | Timezone, horário, atraso tolerável e tratamento de execução perdida. |

### 10.5 Trade-offs

| Trade-off | Benefício de uma opção | Custo/risco da opção | Cenários relacionados |
|---|---|---|---|
| Treinar apenas no delta vs. retreino cumulativo | Delta respeita literalmente a restrição e pode reduzir trabalho por run. | Algoritmo pode não aprender de forma equivalente; avaliação pode ficar enviesada ou insuficiente. Retreino cumulativo pode violar a restrição como interpretada. | AT-03, AT-06, AT-07. |
| Checkpoint somente após treino vs. snapshot durável | Após treino é simples de entender e evita marcar entrada como consumida sem modelo. | Pode obrigar releitura/reprocessamento caro após falha tardia. Snapshot durável permite retomar, mas exige storage e retenção. | AT-03, AT-04, AT-12. |
| Lock/lease distribuído vs. scheduler singleton | Lease/fencing tolera múltiplas instâncias e reduz split-brain se bem implementado. | Acrescenta estado, expiração e complexidade operacional. Singleton simplifica, mas depende da garantia do scheduler/plataforma. | AT-04, AT-05, AT-09. |
| Gate estrito de qualidade vs. continuidade do batch | Bloqueio protege contra treinar com dados degradados. | A execução pode ficar parada até correção; tolerância permissiva pode degradar o modelo. | AT-02, AT-06, AT-07. |
| Aprovação manual vs. promoção automática | Aprovação manual reduz mudança não revisada enquanto não há métricas e ownership. | Aumenta latência e dependência operacional; automação reduz intervenção, mas amplia impacto de critérios incorretos. | AT-07, AT-08, AT-12. |
| Worker isolado vs. compartilhamento de recursos com API | Isolamento limita interferência na experiência online. | Processo/ambiente dedicado pode aumentar custo/complexidade; compartilhar pode causar contenção. | AT-02, AT-09, AT-11. |
| Lineage detalhado vs. minimização/retensão de dados | Mais detalhe facilita reprodução e auditoria. | Pode replicar PII e ampliar superfície/custo de armazenamento. | AT-10, AT-12. |
| Rollback rápido via alias vs. ativação controlada | Alias pode acelerar restauração. | Sem compatibilidade validada, modelo antigo pode não funcionar com features/runtime atuais. | AT-08, AT-09. |
| Retry agressivo vs. limites/backoff | Retry aumenta chance de recuperação transitória. | Pode sobrecarregar dependências, prolongar concorrência ou criar custos; limites ainda não definidos. | AT-04, AT-05, AT-09. |

### 10.6 Riscos, não-riscos e temas de risco

**Riscos identificados**

| ID | Risco | Cenários / pontos associados | Evidência ou decisão necessária |
|---|---|---|---|
| AT-R1 | “Somente dados novos” não tem semântica operacional; implementação pode repetir ou omitir dados. | AT-03, AT-04; cursor e checkpoint. | Definir fonte, identificador, cursor, atualizações, deleções e lateness antes do aceite. |
| AT-R2 | Jobs concorrentes ou worker obsoleto podem avançar checkpoint ou promover candidato fora de ordem. | AT-05; lease/fencing. | Escolher mecanismo de exclusão e testar corrida/expiração/falha. |
| AT-R3 | Falha entre persistência de entrada, treino, publicação e checkpoint pode levar a perda, duplicidade ou estado ambíguo. | AT-04, AT-09. | Definir fronteiras duráveis, idempotência e recuperação por fase. |
| AT-R4 | Treino exclusivamente no delta pode não produzir candidato válido para algoritmo/caso de uso ainda desconhecido. | AT-03, AT-07; trade-off delta/cumulativo. | Confirmar caso de uso, algoritmo, avaliação e compatibilidade com restrição. |
| AT-R5 | Sem baseline, métrica e limiar aprovados, candidato inferior pode ser promovido ou candidato aceitável bloqueado. | AT-07, AT-08. | Acordar critérios, autoridade, política de promoção e rollback. |
| AT-R6 | Dados degradados ou mudança de esquema podem contaminar treino ou interromper indefinidamente o cron. | AT-06, AT-02. | Definir validações, níveis de severidade, quarentena e recuperação. |
| AT-R7 | Acesso excessivo a dados, logs, snapshots ou registry pode expor dados pessoais/sensíveis. | AT-10, AT-12; lineage vs. minimização. | Inventariar dados, avaliar finalidade/base legal, acesso, retenção e descarte. |
| AT-R8 | Indisponibilidade sem política de fallback pode tornar o serviço ou o treino indisponível ou produzir sucesso falso. | AT-09, AT-02, AT-11. | Definir SLAs/RTO/RPO, comportamento de degradação e comunicação de falhas. |
| AT-R9 | Competição de CPU/memória entre treino e API pode degradar consulta/reserva. | AT-11, AT-02; worker compartilhado/isolado. | Medir carga representativa e decidir isolamento/limites. |
| AT-R10 | Requisitos de auditoria de negócio e retenção podem conflitar com exclusão/minimização de dados de ML. | AT-10, AT-12. | Definir quais dados de auditoria precisam persistir e separar payload pessoal de metadados. |

**Não-riscos (com evidência disponível)**

| ID | Item analisado | Por que não é risco arquitetural confirmado neste momento | Condição para reclassificar |
|---|---|---|---|
| AT-NR1 | Escolha de banco/serviço de cloud como falha atual | Nenhuma tecnologia foi escolhida; não há decisão ou implementação a falhar. | Após selecionar tecnologia, avaliar semântica de concorrência, operação, segurança e custo. |
| AT-NR2 | Inferência ML online no caminho de reserva | Não há requisito nem caso de uso confirmado que conecte modelo à reserva; não se presume essa integração. | Reclassificar se um requisito vier a colocar previsões no fluxo transacional. |
| AT-NR3 | Limite de latência específico não atingido | RNF-03 exige medir, mas declara que tempos/carga/taxa de erro aguardam aprovação; não há valor normativo a violar. | Aprovar perfil de carga e limiares antes de julgar conformidade. |
| AT-NR4 | Regra de negócio de aprovação de ML atribuída ao perfil Responsável | A persona trata aprovações de reserva, não aprovação de modelo. Não é correto inferir essa autoridade. | Nomear aprovador de ML/dados ou estabelecer governança equivalente. |

“Não-risco” significa ausência de evidência para afirmar um risco arquitetural
real no baseline; não significa garantia de segurança ou qualidade futura.

**Temas de risco a acompanhar**

- **Integridade do estado e consistência temporal:** AT-R1 a AT-R3; afeta
  checkpoints, reprocessamento e concorrência de jobs.
- **Adequação estatística/semântica do treinamento:** AT-R4 a AT-R6; depende do
  domínio que ainda não foi especificado.
- **Segurança, privacidade e conformidade:** AT-R7 e AT-R10; base legal,
  categorias de dados, retenção e responsáveis precisam de avaliação competente.
- **Resiliência operacional:** AT-R2, AT-R3 e AT-R8; política de indisponibilidade
  e metas de recuperação não existem no baseline.
- **Contenção de recursos e custo:** AT-R9; volume, orçamento e topologia estão
  ausentes.
- **Governança e responsabilização:** AT-R5, AT-R7 e AT-R10; autoridade para
  aprovar, promover, auditar e responder a incidente não está definida.

### 10.7 Síntese da avaliação e questões para decisão

O baseline tem drivers fortes e testáveis para reservas concorrentes, autorização,
auditoria e medição de qualidade. Para o cron de ML, existe a restrição de
frequência e incrementalidade, mas ainda não existe informação suficiente para
afirmar que um pipeline concreto produzirá um modelo válido, seguro ou útil. A
arquitetura deve permanecer condicional até que escopo, fonte, semântica de dados
novos, algoritmo, avaliação e operação sejam aprovados.

Decisões que bloqueiam uma avaliação conclusiva:

1. Definir se o modelo integra o produto e qual decisão/usuário ele atende.
2. Especificar a fonte e o contrato de incrementalidade, incluindo atualizações,
   eventos tardios e exclusões.
3. Aprovar horário/fuso, recuperação de execução perdida e política de
   checkpoint.
4. Confirmar que treino apenas no delta é adequado ou decidir alternativa
   compatível com a restrição.
5. Aprovar métricas, baseline, critérios de bloqueio/promoção e responsável.
6. Definir dados pessoais tratados, finalidade/base legal, minimização, acesso,
   retenção e eliminação com os responsáveis competentes.
7. Fixar perfil de carga e limites quantitativos de desempenho, disponibilidade,
   recuperação e custo, se forem necessários para aceite.

## Histórico da análise ATAM

- **2026-11-02:** acrescentada avaliação independente com utility tree, cenários,
  sensibilidades, trade-offs, riscos, não-riscos e temas de risco. Valores
  quantitativos não definidos pelos requisitos foram mantidos como hipóteses
  pendentes; o conteúdo arquitetural anterior foi preservado.
