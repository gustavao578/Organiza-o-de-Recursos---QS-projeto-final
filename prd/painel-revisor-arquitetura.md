# Painel revisor — arquitetura e ADRs

> **Parecer independente para discussão; não aprovado como fonte de verdade.**
> Elaborado em 2026-11-02 com base nos RF/RNF, personas, visão geral,
> [arquitetura.md](./arquitetura.md), que contém a análise ATAM, e
> [arquitetura-proposta.md](./arquitetura-proposta.md), que contém diagramas C4 e
> sequências. Nenhum desses documentos-fonte foi alterado por este parecer.
>
> O painel abaixo é uma síntese editorial dos pontos de vista solicitados, não
> uma consulta a especialistas humanos. ADRs têm status **Proposto — decisão
> pendente**, salvo quando explicitamente descritos como não decisão. Não há
> métricas operacionais ou de ML inventadas; qualquer critério não previsto nos
> requisitos está marcado como pendente.

## 1. Síntese executiva

O baseline tem requisitos testáveis para autorização, integridade concorrente de
reservas, auditoria, responsividade, documentação e evidência de qualidade. A
restrição do cron diário e incremental é explícita na solicitação, mas não consta
dos RF/RNF: não define finalidade, fonte, semântica de “dados novos”, algoritmo,
uso do modelo ou critério de promoção. Assim, o painel não aprova a arquitetura
de ML como pronta para implementação.

As propostas de cursor incremental, idempotência, lease, gates, registry,
promoção e rollback são plausíveis como mecanismos, mas não têm ainda contrato
operacional suficiente. Em particular:

1. A trilha de auditoria precisa ter consistência definida com a alteração de
   negócio; a sequência atual pode confirmar reserva antes de persistir auditoria.
2. O checkpoint de leitura, o estado do run, o resultado de treino e o alias do
   modelo são estados diferentes e não devem ser representados como um único
   checkpoint.
3. Dados novos e delta-only training não garantem modelo válido. Avaliação pode
   necessitar histórico/holdout; uso disso deve ser autorizado e não presumido.
4. Não há autoridade ou métrica aprovada para promoção. Não promover candidato
   antes de critério e responsável definidos é uma recomendação conservadora,
   não um requisito existente.
5. Não há aceite formal de risco no baseline. Este parecer não declara nenhum
   risco residual aceito.

## 2. Objeções fundamentadas por papel

### 2.1 Arquiteto

- **Objeção:** os diagramas são propostas lógicas, não uma arquitetura implantável.
  C4Context coloca “Aplicação” e “Rotina diária” no mesmo boundary, embora a
  segunda possa ser parte interna ou sistema separado; C4Container também não
  define fonte do batch. Isso mistura contexto, fronteira de sistema e componente
  sem fechar propriedade do dado.
- **Evidência:** `C4Context` e `C4Container` em
  [arquitetura-proposta.md](./arquitetura-proposta.md), §§ 4.1–4.2; visão geral
  ainda declara banco, autenticação, arquitetura de implantação e integrações
  em aberto.
- **Objeção:** em fluxo online, estado de reserva é confirmado antes da chamada
  separada de auditoria. Se auditoria falhar após commit, o sistema pode violar
  RF-10/RNF-07.
- **Evidência:** sequência online em § 5.1; RF-10 exige eventos e dados
  auditáveis, RNF-07 exige registro de toda mudança de estado.
- **Recomendação:** representar explicitamente boundary de persistência e
  consistência da auditoria; manter API/worker como separação lógica até prova
  de que precisam de implantação independente.

### 2.2 Dados e ML

- **Objeção:** “treinar somente dados novos” é ambíguo e pode ser incompatível
  com o algoritmo ou com a avaliação. Treinar apenas no delta não equivale
  necessariamente a retreinar o modelo e pode não manter desempenho.
- **Evidência:** ASR-06 não especifica fonte, definição de novidade, objetivo,
  algoritmo ou métrica; a própria AT-R4 aponta esse risco.
- **Objeção:** holdout, referência histórica ou dados anteriores para avaliação
  podem ser necessários, mas não se deve reinterpretar “somente dados novos”
  para permiti-los. Separar treino de avaliação não resolve a autorização sobre
  o conjunto de avaliação.
- **Recomendação:** antes de qualquer job real, definir finalidade, algoritmo,
  separação temporal e dados autorizados para avaliação. Se delta-only não servir,
  retornar ao requisitante; não relaxar a restrição por decisão técnica tácita.
- **Divergência preservada:** treino incremental exige input novo; avaliação
  robusta pode precisar de referência anterior. Requisitos atuais não resolvem
  essa tensão.

### 2.3 Segurança e LGPD

- **Objeção:** “pseudonimizar” e omitir PII dos logs são salvaguardas, não prova
  de anonimização, base legal ou conformidade LGPD. O conteúdo de snapshots,
  features, modelos e lineage ainda pode permitir identificação ou inferência.
- **Evidência:** RF-01/RNF-01 cobrem autorização e proteção, porém não há
  inventário de dados do modelo, finalidade, categorias, base legal ou retenção.
- **Objeção:** a arquitetura cita criptografia “conforme capacidade do ambiente”,
  sem determinar controles mínimos e gestão de chaves; os documentos ainda não
  escolheram ambiente.
- **Recomendação:** bloquear ingestão de dados de produção até inventário,
  finalidade, minimização, acesso, retenção e análise pelos responsáveis
  competentes. Não registrar valores pessoais brutos no lineage/logs. Nenhuma
  declaração de conformidade jurídica é feita por este painel.
- **Divergência preservada:** a auditoria de negócio pede histórico, enquanto
  privacidade pode exigir limitação/eliminação. Retenção e conteúdo precisam de
  decisão conjunta; não resolver por sobrescrever histórico ou reter tudo.

### 2.4 Operações e SRE

- **Objeção:** um cron diário descreve frequência, não garantia de início,
  conclusão, atraso aceitável ou recuperação. Não há SLO, RTO, RPO, política de
  catch-up nem tratamento de falha/execução perdida.
- **Evidência:** RNF § 5 deixa disponibilidade/recuperação por definir; ASR-06
  informa “diariamente ao final do dia”, sem fuso nem tolerância.
- **Objeção:** lease/fencing impede escritor obsoleto apenas se todas as
  operações de commit verificarem o token. Um lock isolado não torna
  atomicamente consistente fonte, snapshot, checkpoint e registry.
- **Recomendação:** checkpoints e saídas duráveis por fase, retomada idempotente,
  tentativas limitadas e alertas a definir. Não prometer “exactly once” entre
  serviços; projetar efeitos idempotentes e reconciliáveis.
- **Divergência preservada:** avanço do cursor após snapshot durável favorece
  retomada, mas exige snapshot confiável; avanço só após treino simplifica
  semântica, mas pode reler/reprocessar e aumentar atraso/custo.

### 2.5 Custos

- **Objeção:** processo/worker separado, snapshots imutáveis, registry e
  retenção de modelos elevam custo operacional e de armazenamento; não há
  volume, orçamento ou comparação de opções que sustente uma escolha.
- **Evidência:** visão geral indica equipe/projeto semestral; não existe
  orçamento nem escala documentados. A proposta cita deployment gerenciado,
  compartilhado ou local como alternativas.
- **Objeção:** worker no mesmo host da API pode reduzir custo fixo, mas disputar
  CPU/memória e prejudicar RF-03/RF-04; isolamento pode custar mais.
- **Recomendação:** não escolher microserviço/plataforma/serviço gerenciado por
  antecipação. Fazer estimativa com volume real/sintético representativo,
  algoritmo candidato, duração, custo de retenção e impacto da concorrência.
  Limites de execução são proposta pendente, não valores fixados.
- **Divergência preservada:** isolamento operacional versus custo e simplicidade;
  falta evidência para fechar.

### 2.6 Facilitador ATAM

- **Objeção:** utility tree e dificuldade estão classificadas qualitativamente;
  as categorias não têm escala calibrada com stakeholders. “Alta importância”
  para rollback, indisponibilidade, qualidade do modelo e lineage é uma
  priorização analítica, não prioridade de negócio aprovada.
- **Evidência:** AT-08 a AT-12 são propostas; não há requisitos de rollback,
  serving, disponibilidade de ML ou lineage do modelo nos RF/RNF.
- **Objeção:** alguns cenários descrevem respostas desejáveis e critérios zero
  para perda/duplicidade, que devem ser lidos como critérios arquiteturais
  sugeridos, não medição operacional já validada.
- **Recomendação:** obter aceite de importância, dificuldade e limiares com
  stakeholders antes de transformar cenário em gate. Manter a distinção entre
  requisito, resposta desejada, métrica observada e limiar pendente.

## 3. Coerência entre C4, sequências, pipeline, utility tree e riscos

| Achado | Evidência confrontada | Problema / efeito | Tratamento recomendado |
|---|---|---|---|
| Fonte de dados inconsistente/indefinida | C4Container liga worker ao DB da aplicação apenas “se aprovado”; pipeline C4 apresenta `Fonte de dados` própria; C4Context desenha batch lendo da aplicação condicionalmente. | Pode ser interpretado como requisito de que reservas alimentam ML, embora isso não tenha sido confirmado. | ADR-02 mantém a fonte como porta lógica e decisão aberta; desenhar fonte externa/aplicação apenas após confirmação. |
| Checkpoint ligado à fonte, não a um store explícito | Pipeline C4 faz `run -> source: Confirma checkpoint`; sequência combina lease e checkpoint no participante `L`; componentes não mostram claramente o armazenamento do estado do run. | A origem pode ser confundida com autoridade de checkpoint; não fica clara a durabilidade nem a recuperação. | Separar conceitualmente fonte, snapshot/manifests, estado do run e watermark; definir backend posteriormente. |
| Checkpoint e promoção acoplados na sequência | A sequência confirma checkpoint após avaliação e bloco de promoção/aprovação; nota diz que avanço depende da política. | Candidato rejeitado ou aguardando aprovação pode reprocessar o mesmo delta indefinidamente; falha transitória pode depender de aprovação humana. | ADR-02 define watermark de ingestão após snapshot durável e estado de treino separado; ADR-04 separa gate de promoção. |
| Checkpoint e promoção em stores diferentes | Watermark/lease parecem em `L`; modelo e alias em `R`. | Não existe transação única abrangendo lease/checkpoint e registry; alegar atomicidade global é incorreto. | ADR-03/05 limitam atomicidade por store, usam staging, fencing e reconciliação; garantir invariantes, não “atomicidade distribuída”. |
| Auditoria online após confirmação de reserva | Sequência confirma DB→API antes de API→AUD; contrato declara auditoria associada, mas sem unidade de consistência. | Pode haver reserva válida sem trilha auditável se a escrita de auditoria falhar. | ADR-01 exige estado e auditoria no mesmo commit lógico (ou mecanismo durável com garantia equivalente antes de responder); revisar sequência futuramente. |
| Inferência é simultaneamente opcional e modelada como contêiner/cliente | C4Container e C4Component incluem runtime/cliente, enquanto texto diz que não há caso de uso online. | Risco de escopo creep e investimento sem requisito. | ADR-06 mantém serving fora do baseline; só aparece como extensão condicional. |
| C4Component e C4Container com tecnologias não decididas | Diagramas marcam Java/Spring como previstos e banco/scheduler/registry como não definidos. | Não há conflito se tratados como lógico; seria incoerente apresentá-los como decisões fechadas. | Todos ADRs ficam “propostos/pendentes”; tecnologia fica fora da decisão. |
| Utility tree e ATAM apontam o risco, mas não definem política | AT-03/04/05/06/07 e AT-R1..R6 ligam incrementalidade, falha, concorrência, dados e promoção. | A proposta C4/sequence apresenta fluxo nominal, mas não especifica retries, compensação nem estado terminal de algumas falhas. | ADR-02..04 adicionam contratos lógicos e evidências; limiares e política final seguem pendentes. |
| “Lock” versus idempotência não são equivalentes | Pipeline inclui lease/fencing; chave idempotente aparece apenas no texto de projeto. | Lock previne concorrência; idempotência evita efeitos duplicados; nenhum substitui o outro. | ADR-03 exige ambos como mecanismos distintos, com teste de execução repetida e worker obsoleto. |
| Retry de notificação versus transação online está indefinido | RF-11 aceita simulação ou API externa; proposta diz que falha de envio não desfaz reserva, sem outbox/estado de entrega definido. | Pode haver notificação perdida ou repetida; retry síncrono também pode atrasar reserva. | ADR-01 recomenda outbox/idempotência se integração assíncrona for escolhida; contrato e evidência ainda pendentes. |
| Importância de ML/rollback/indisponibilidade não validada | Utility tree classifica AT-07..AT-10 como alta; RNF não estabelece serving, disponibilidade ou rollback de ML. | Prioridade alta pode ser confundida com requisito aprovado. | Manter importância como avaliação do painel; confirmar com stakeholders e sem alterar os requisitos. |
| Sobreposição de jobs é risco, não impossibilidade demonstrada | AT-05 propõe um writer válido por janela, mas deployment/scheduler ainda não definidos. | Não há evidência empírica de ocorrência ou arquitetura para provar prevenção. | Tratar como cenário de teste obrigatório se o cron for implementado; não declarar risco eliminado até teste. |

### 3.1 Revisão cruzada da cadeia

| Camada | Estado coerente após este parecer | Lacuna remanescente |
|---|---|---|
| C4 | API de recursos e pipeline batch são caixas lógicas; modelo serving condicional. | Fronteira de sistema, fonte real, deployment e stores físicos precisam de decisão. |
| Sequência online | Autorização, verificação, transação de reserva e notificação aparecem. | Auditoria deve integrar o commit lógico; sequência atual precisa correção em futura revisão. |
| Sequência batch | Scheduler, worker, lease, fonte, qualidade, treino, avaliação e registry são identificados. | Retry/recovery, checkpoint independente, aprovação pendente, candidato recusado e stores não estão fechados. |
| Pipeline | Delta, qualidade, treino, avaliação, lineage e promoção têm responsabilidades lógicas. | Critérios de dado/modelo e política de avanço watermark não são requisito definido. |
| Utility tree | Cenários AT-01..AT-13 cobrem as preocupações do painel. | Priorização qualitativa é provisória; alguns atributos de ML não são requisitos. |
| Riscos | AT-R1..R10 identificam riscos alinhados a falhas de incrementalidade, dados, acesso, operação e custo. | Nenhum está comprovadamente mitigado; não existe aceite formal de risco. |

### 3.2 Estado das áreas críticas solicitadas

| Área | Parecer do painel | O que falta para considerar decidido |
|---|---|---|
| Watermark/checkpoint | **Proposto:** cursor de ingestão distinto do estado do run e da versão ativa; janela fechada por limite superior; confirmar ingestão após snapshot/manifesto duráveis e retomar treino no mesmo run. | Fonte, semântica de novidade, late arrivals, updates/deletes e aprovação do ponto de commit. |
| Idempotência | **Proposta:** chave determinística por fonte/janela/pipeline/configuração; reentrega recupera run já existente. | Definição de identidade de evento, persistência da chave e evidência de replay. |
| Lock/concorrência | **Proposta:** apenas writer válido; lease/fencing se scheduler não oferecer singleton; token validado em todas as mutações protegidas. | Topologia, mecanismo, expiração/renovação e testes de worker obsoleto. |
| Retry/recuperação | **Proposta:** retry limitado e classificado, estado durável por fase e saída idempotente; sem promessa de conclusão diária. | Limites/backoff, timeouts, catch-up, alertas, runbook e RTO/RPO, se aplicáveis. |
| Gates | **Proposta:** gate estrutural separado do gate preditivo/de negócio; candidato não promovível sem critérios aprovados. | Regras e thresholds de qualidade, algoritmo, métrica, baseline, avaliação e aprovador. |
| Registry/versionamento | **Condicional:** artefato imutável, hash, manifesto e estados separados do alias ativo. | Confirmar ML, ferramenta, metadados obrigatórios e retenção. |
| Promoção | **Aberta:** publicar candidato não promove; promoção automática ou manual não foi decidida. Até critérios/autoridade existirem, recomendação é manter versão ativa. | Política de aprovação, critérios, dono e reconciliação entre registry e runtime. |
| Rollback | **Proposto e condicional:** repontar para versão aprovada compatível, com auditoria; não reverte dados de negócio. | Compatibilidade, retenção de artefatos, autorizador e objetivo temporal. |
| Observabilidade | **Proposta:** IDs de run/operação e métricas de fases, watermark, erros, dados, versão e decisões; logs minimizados. | Ferramentas, retenção, alarmes, thresholds e responsáveis de plantão. |
| Segurança/LGPD | **Não aprovado para dados reais:** menor privilégio e minimização como princípios; não há conclusão jurídica ou base legal avaliada. | Inventário, finalidade, base legal, dados pessoais, direitos, retenção, eliminação e revisão competente. |
| Custo | **Não decidido:** sem orçamento, escala ou topologia; worker separado versus compartilhado exige experimento/estimativa. | Perfil de carga, volumes, algoritmo, retenção, ambiente e teto de custo aprovado. |

## 4. ADRs

Os registros abaixo recomendam decisões para fechar ambiguidades, mas **não
alteram RF/RNF e não são aprovados por este painel**. Uma decisão condicionada
deve ser convertida em ADR aceito somente após as questões indicadas serem
respondidas.

### Divergências consolidadas sem consenso

| Divergência | Posições em tensão | Consolidação do painel |
|---|---|---|
| Fonte do batch | Extrair da persistência da aplicação versus consumir uma fonte/CDC independente. | Sem decisão. Não ligar o treinamento a reservas até o owner confirmar finalidade e fonte. |
| Avanço do cursor | Confirmar só após o treino versus confirmar após snapshot durável e manter run separado. | Recomendação técnica favorece separar watermark de ingestão do estado do run; sem fonte replayable/snapshot confiável, volta à decisão. |
| Promoção | Automática por métrica versus aprovação humana. | Sem consenso de produto/ML; candidato não vira ativo até critérios e autoridade aprovados. A opção manual não é imposta como requisito. |
| Topologia | API e batch compartilhando host/processo versus worker isolado/gerenciado. | Adiada por falta de carga, SLO e orçamento; manter apenas fronteira lógica. |
| Auditoria | Escrita posterior independente versus persistência na mesma transação lógica. | Requisito de rastreabilidade favorece garantia durável antes de sucesso; tecnologia e mecanismo ainda abertos. |
| Prioridade de ML | Tratar pipeline como P1 do produto versus como restrição adicional ainda sem escopo de negócio. | A frequência do job foi explicitamente solicitada; importância e implantação do modelo seguem condicionais até confirmar escopo. |

### ADR-001 — Consistência de reserva, auditoria e notificações

- **Status:** Proposto — pendente de decisão do projeto.
- **Contexto:** RF-06 exige atomicidade contra conflito; RF-10/RNF-07 exigem
  auditoria de alterações; RF-11 exige notificação, mas admite simulação ou
  integração externa.
- **Forças:** integridade de reserva; trilha auditável; resposta clara; falha de
  provedor de notificação não deve desfazer uma reserva confirmada.
- **Alternativas:** (a) persistir reserva e auditoria em commit transacional
  comum, e emitir notificação após commit via outbox; (b) gravar auditoria em
  chamada independente após o commit como sequência atual; (c) transação
  distribuída com provedor externo.
- **Decisão proposta:** a reserva e sua auditoria obrigatória devem formar uma
  única unidade lógica durável. Preferir persistência atômica no mesmo limite
  transacional; se isso não for possível, adotar mecanismo durável/reconciliável
  que não responda sucesso antes de garantir a trilha. Notificação externa deve
  ser assíncrona/idempotente, opcional e não participante da transação de
  negócio. Não selecionar tecnologia.
- **Consequências:** estado de auditoria não pode ser apenas “best effort”;
  eventual outbox agrega estado e manutenção. A sequência atual deve ser
  atualizada se essa recomendação for aceita.
- **Riscos:** falha de outbox/retry pode duplicar notificação; armazenar antes/depois
  pode reter dados pessoais além do necessário. Definir idempotência, acesso e
  retenção.
- **Requisitos:** RF-04, RF-06, RF-10, RF-11; RNF-01, RNF-02, RNF-07.
- **Evidência:** teste de concorrência em persistência realista (Testcontainers);
  injeção de falha entre commit e entrega; verificar ausência de reserva sem
  auditoria e deduplicação de notificações. Sem execução nesta revisão.

### ADR-002 — Watermark, snapshot e checkpoints independentes

- **Status:** Proposto — bloqueado pela definição de fonte e “dados novos”.
- **Contexto:** o cron deve ser diário e usar somente dados novos; fonte,
  semântica de novidade, atualizações tardias e cursor ainda não foram definidos.
- **Forças:** não perder dados; não treinar repetidamente o mesmo delta;
  recuperar falhas; reprodutibilidade e minimização.
- **Alternativas:** (a) avançar watermark somente depois de treino/promoção;
  (b) avançar ao materializar snapshot imutável e manter estado de treino/run
  separado; (c) janela temporal sobreposta com deduplicação; (d) CDC/event log.
- **Decisão proposta:** distinguir (1) cursor/watermark de ingestão, (2) estado e
  checkpoint de processamento por `run_id`, (3) versão de modelo ativa. Fixar
  limite superior `H` por run; materializar um snapshot ou referência imutável.
  Avançar cursor de ingestão apenas quando snapshot e manifesto estiverem
  duráveis e reprocessáveis; não considerar run completo até treino/avaliação
  atingir estado terminal. Falha de treino retoma o mesmo input/run. Rejeição
  de candidato não retrocede a ingestão, pois a decisão e evidência ficam
  registradas; nova tentativa exige política explícita, não reprocessamento
  implícito.
- **Consequências:** requer storage durável ou fonte replayable; um registro
  rejeitado não será automaticamente reenviado em nova janela. A semântica de
  “novo” precisa definir late arrivals, updates e deletes para que `H` seja
  correto.
- **Riscos:** timestamp não monotônico pode omitir eventos; snapshot pode aumentar
  armazenamento; avançar cursor sem validar persistência quebra a garantia.
- **Requisitos:** restrição ASR-06 da solicitação; não é RF/RNF validado.
- **Evidência:** testar timestamp empatado, late arrival, alteração/remoção,
  retomada após snapshot e reprovação de candidato; conferir IDs e manifesto
  contra a definição aprovada de novidade.

### ADR-003 — Idempotência, exclusão mútua e fencing do batch

- **Status:** Proposto — mecanismo de infraestrutura pendente.
- **Contexto:** job pode ser reentregue, atrasado, duplicado ou executar em mais
  de uma instância.
- **Forças:** impedir efeitos duplicados, writer obsoleto, corrupção de cursor ou
  promoção fora de ordem.
- **Alternativas:** (a) scheduler singleton confiável; (b) lease compartilhada
  persistente e fencing; (c) lock local; (d) execução concorrente sem exclusão.
- **Decisão proposta:** separar chave de idempotência (mesma fonte/janela,
  pipeline e configuração identifica o mesmo efeito) de exclusão mútua. Exigir
  um único proprietário válido por janela, usando lease compartilhada com
  fencing se o scheduler não der garantia singleton; cada atualização de estado,
  checkpoint ou promoção deve rejeitar token vencido. Lock local não é
  considerado suficiente para múltiplas instâncias.
- **Consequências:** lease precisa de expiração/renovação e monitoramento;
  repetição deve recuperar o run existente; comportamento de execução perdida
  ou coalescida continua por decidir.
- **Riscos:** fencing ausente em uma das saídas permite efeitos tardios; lease
  excessivamente curta causa contenção, longa atrasa recuperação. Nenhum tempo
  é definido aqui.
- **Requisitos:** ASR-06; mecanismo não especificado em RF/RNF.
- **Evidência:** executar disparos duplicados/concomitantes; expirar lease durante
  worker; confirmar que token obsoleto não avança checkpoint nem atualiza alias.

### ADR-004 — Gates de qualidade, avaliação e elegibilidade de candidato

- **Status:** Proposto — critérios de negócio/ML não aprovados.
- **Contexto:** o pipeline proposto valida dados, treina delta, avalia candidato
  e registra decisão; não há métrica, threshold, baseline, holdout nem
  comportamento de dados inválidos definido.
- **Forças:** evitar treino contaminado e promoção de modelo regressivo; preservar
  reprodutibilidade; não inventar objetivo preditivo.
- **Alternativas:** (a) bloquear qualquer anomalia; (b) classificar regra como
  bloqueante/aviso com limites acordados; (c) treinar e promover sem avaliação;
  (d) decisão automática com métricas; (e) aprovação humana.
- **Decisão proposta:** separar gate estrutural (esquema, integridade,
  elegibilidade e manifesto) de gate preditivo/de negócio. Sem critérios de
  avaliação aprovados, candidato permanece não promovível. Erro estrutural
  bloqueante impede treino/publicação; para outras verificações, severidade e
  limites devem ser aprovados. Aprovação manual versus automática permanece
  divergência em aberto; nenhuma persona atual recebe autoridade de modelo por
  inferência.
- **Consequências:** candidatos podem acumular em estado não promovível; o
  owner precisa definir validação compatível com treino somente no delta.
  “Dados novos” não autoriza incorporar histórico ao treino sem aceite.
- **Riscos:** gate rígido reduz disponibilidade do pipeline; gate permissivo
  degrada o modelo; holdout contaminado invalida avaliação.
- **Requisitos:** ASR-06; não existem critérios ML em RF/RNF.
- **Evidência:** dataset com falha estrutural, delta vazio/insuficiente, candidato
  pior e avaliação sem baseline; comprovar que nenhum alias ativo muda sem gate
  aprovado.

### ADR-005 — Registry, promoção e rollback como operações distintas

- **Status:** Proposto — plataforma, autoridade e retenção pendentes.
- **Contexto:** a proposta inclui registry, artefatos versionados, candidato,
  alias ativo, promoção e rollback; estes elementos não aparecem nos RF/RNF.
- **Forças:** rastrear a versão, manter artefato imutável, prevenir ativação
  acidental e restaurar modelo anterior compatível.
- **Alternativas:** (a) sobregravar único modelo; (b) armazenar versões imutáveis e
  mudar referência ativa; (c) registry comercial/gerenciado ou catálogo próprio.
- **Decisão proposta:** se o caso de uso de ML for confirmado, separar publicação
  de candidato de promoção; versionar artefato e metadados imutavelmente; trocar
  uma referência ativa controlada somente após gate/aprovação aprovados. Rollback
  reponta para versão aprovada compatível e gera auditoria. Não escolher
  produto. Não afirmar atomicidade junto ao checkpoint em outro store.
- **Consequências:** retenção de artefatos e compatibilidade de feature/runtime
  são pré-requisitos para rollback; opção de canary/shadow fica fora da primeira
  decisão até necessidade demonstrada.
- **Riscos:** versão anterior pode não carregar com contrato atual; alias e
  artefato podem divergir se promoção não tiver reconciliação.
- **Requisitos:** ASR-06; registry/promoção/rollback são decisão sem RF/RNF.
- **Evidência:** integridade de hash, transição auditada, promoção concorrente,
  rollback para versão anterior e compatibilidade; critérios de sucesso
   operacionais pendentes.

### ADR-006 — Serving de inferência condicional, não acoplado à reserva

- **Status:** Proposto — nenhum serving online no baseline.
- **Contexto:** RFs cobrem reserva e relatório, não previsão ML; C4 inclui runtime
  e cliente de modelo em caráter opcional.
- **Forças:** não deixar ML comprometer a regra determinística de conflito,
  segurança ou disponibilidade da reserva; evitar serviço sem usuário/objetivo.
- **Alternativas:** (a) inferência inline na API; (b) runtime síncrono externo;
  (c) batch-only sem serving; (d) recomendar sem automatizar decisão.
- **Decisão proposta:** não incluir inferência no caminho online de reservas até
  novo requisito identificar finalidade, consumidor, contrato, latência/falha,
  fallback e responsabilidade decisória. O pipeline batch pode ser analisado
  separadamente, condicionado a finalidade aprovada.
- **Consequências:** C4Runtime e componente client são projeções futuras, não
  contêineres confirmados. Sem serving, rollback de modelo pode não ser
  prioridade operacional, embora versionamento ainda ajude a reprodutibilidade.
- **Riscos:** caso de uso posterior pode demandar mudança de interfaces e dados;
  isso não justifica antecipar uma API sem requisitos.
- **Requisitos:** nenhum RF/RNF de inferência online; ASR-06 apenas pede treino.
- **Evidência:** rastrear todos os caminhos de criação/alteração de reserva e
  comprovar que decisão de disponibilidade independe de inferência.

### ADR-007 — Acesso, minimização, retenção e observabilidade

- **Status:** Proposto — depende de inventário e avaliação competente.
- **Contexto:** RF-01/RNF-01 protegem acesso; RF-10/RNF-07 pedem auditoria; os
  dados de ML e sua natureza não foram informados.
- **Forças:** menor privilégio, investigação de falhas, privacidade e custo de
  retenção.
- **Alternativas:** (a) copiar payload integral para logs/lineage; (b) registrar
  IDs/checksums e referência protegida; (c) pseudonimizar e limitar campos; (d)
  não coletar lineage.
- **Decisão proposta:** conceder identidades separadas e privilégio mínimo à
  API, worker, armazenamento e registry; instrumentar run/versão sem PII bruta;
  registrar métricas e eventos operacionais necessários; preferir referências
  controladas/hash e snapshot protegido a duplicar payload em logs. Definir
  finalidade, base legal, retenção, eliminação, atendimento a direitos e acesso
  com responsáveis antes de usar dados reais. Isso não declara conformidade
  LGPD.
- **Consequências:** maior segregação e controle de acesso; lineage pode ser
  limitado se uma referência expirar e snapshot não puder ser mantido legalmente.
  A tensão auditoria/eliminação permanece aberta.
- **Riscos:** metadados também podem revelar informações; pseudonimização não é
  anonimização; retenção inadequada afeta privacidade ou investigação.
- **Requisitos:** RF-01, RF-10; RNF-01, RNF-07; privacidade específica do ML é
  decisão sem RF/RNF detalhado.
- **Evidência:** teste de acesso negado/autorizado por perfil; revisão de logs e
  exports; evidência de retenção/expurgo após política definida; teste de
  rotação/segredo depende de plataforma escolhida.

### ADR-008 — Retry, recuperação e política de disponibilidade do job

- **Status:** Proposto — parâmetros e SLOs pendentes.
- **Contexto:** a frequência diária não descreve recuperação; indisponibilidade
  de fonte, worker, artefatos ou registry pode interromper etapas.
- **Forças:** retomada segura, não corromper watermark/modelo ativo, evitar
  retry infinito e custo não limitado.
- **Alternativas:** (a) retry ilimitado; (b) retry limitado com backoff e jitter;
  (c) parar e exigir operação manual; (d) execução catch-up automática.
- **Decisão proposta:** retries devem ser limitados e distinguidos por falha
  transitória/determinística; estado durável por fase e saída idempotente permitem
  retomar o mesmo run. Falha mantém ativo anterior e não atualiza checkpoint
  além do dado recuperável. Parâmetros, alertas, catch-up, RTO/RPO e fallback
  permanecem pendentes. Não prometer conclusão diária garantida sem objetivo
  operacional aprovado.
- **Consequências:** requer classificação de erro e reconciliação; execução
  pode não terminar no dia em que foi agendada.
- **Riscos:** retry excessivo sobrecarrega dependência; retry insuficiente deixa
  janela atrasada; fallback implícito pode criar sucesso falso.
- **Requisitos:** ASR-06; RNF não especifica disponibilidade/recuperação.
- **Evidência:** fault injection por fase, verificar estado terminal, checkpoint
  e modelo ativo; reportar duração/tentativas, sem comparar com threshold ainda
  inexistente.

### ADR-009 — Topologia e orçamento: decisão adiada

- **Status:** Não decisão deliberada; reavaliar com evidência.
- **Contexto:** API online e batch têm perfis diferentes, mas volume, orçamento,
  ambiente e SLO são desconhecidos.
- **Forças:** simplicidade e custo inicial versus isolamento, contenção e
  escalabilidade.
- **Alternativas:** worker no mesmo processo, worker isolado na mesma plataforma,
  serviço gerenciado, ou execução local/acadêmica.
- **Decisão proposta:** não escolher implantação, cloud, microsserviços,
  capacidade ou quotas neste estágio. Manter boundary lógico entre API e batch;
  comparar opções depois de medir workload e estimar custo total (execução,
  armazenamento de snapshots/modelos, logs e operação).
- **Consequências:** desenho lógico não constitui deployment view; pode haver
  revisão dos contêineres após estudo.
- **Riscos:** decisão adiada pode atrasar implementação; escolha prematura pode
  gerar custo/complexidade ou contenção.
- **Requisitos:** RNF-03 requer medição JMeter online; nenhum orçamento/custo ou
  isolamento de batch está especificado.
- **Evidência:** experimento comparativo com mesma carga representativa e
  estimativa de retenção; valores-alvo e orçamento precisam ser aprovados antes
  de decisão.

## 5. Matriz requisito → decisão → componente → evidência

“Evidência” nesta matriz significa artefato de validação a produzir, não teste
executado nem comportamento já comprovado. ADRs propostos não alteram a condição
dos requisitos.

| Requisito | Decisão relacionada | Componente / fronteira | Evidência necessária |
|---|---|---|---|
| RF-01 — autenticação/autorização | ADR-007; tecnologia e matriz final de permissões pendentes | API/autorização; gestão de identidade | Testes de API autenticados e não autenticados, matriz papel×operação, negativa inclusive fora da UI; auditoria dos acessos negados. |
| RF-02 — cadastro e consulta de recursos | ADR-001 para auditoria; esquema ainda pendente | Domínio de recursos e persistência | Testes de validação, duplicidade, atualização e consistência entre status, reserva e manutenção. |
| RF-03 — pesquisa/filtros/disponibilidade | ADR-001; ADR-009 para capacidade não decidida | Disponibilidade, consulta, persistência | Testes de filtros e manutenção/conflito; JMeter com carga aprovada (valores pendentes). |
| RF-04 — criar/alterar/cancelar reservas | ADR-001 e ADR-002 para consistência/auditoria; fluxos de estado ainda pendentes | API/reservas, persistência, auditoria | Testes de transição, autorização, cancelamento e ausência de gravação parcial; critérios de aprovação sem restrição precisam de definição. |
| RF-05 — detectar sobreposição | ADR-001 | Disponibilidade e limite de integridade da persistência | Testes de intervalos, professor, sala, material, manutenção e conflitos simultâneos; regra de rejeitar vs. revisão ainda pendente. |
| RF-06 — dupla reserva concorrente | ADR-001 | Reserva e persistência transacional | Teste concorrente realista: no máximo uma aceita, rejeição explícita, nenhuma alteração parcial (Testcontainers conforme visão geral). |
| RF-07 — aprovação de recurso restrito | ADR-001; não confundir com aprovação de modelo | Reserva/aprovação e autorização do Responsável | Testes de estado e perfil; critérios de recurso restrito, aprovação e fluxo final ainda pendentes. |
| RF-08 — manutenção | ADR-001 | Gestão de recurso/manutenção e disponibilidade | Testes de bloqueio e filtros; política para reserva existente que cruza manutenção precisa ser decidida. |
| RF-09 — retirada/devolução | ADR-001 e ADR-007 | Movimentação de materiais, autorização, auditoria | Testes de retirada/devolução, atraso/dano e histórico consultável; regras detalhadas de quantidade/estados pendentes. |
| RF-10 — histórico auditável | ADR-001 e ADR-007 | Auditoria transacional, persistência e consulta autorizada | Falha induzida no registro, verificação de usuário/instante/ação/antes-depois, imutabilidade conforme regra e teste de acesso. Retenção pendente. |
| RF-11 — notificações | ADR-001 | Adaptador/outbox/notificação simulada ou externa | Testes WireMock se integração externa; confirmação, erro, retry/idempotência e registro conforme opção aprovada. Mecanismo permanece aberto. |
| RF-12 — relatórios de utilização | ADR-007 para acesso/minimização | Relatórios e consulta agregada | Testes de autorização, período, consistência de métricas e exportação/visualização; regras de agregação/reidentificação pendentes. |
| RF-13 — UI responsiva/mensagens | Sem ADR de tecnologia de UI; contrato de erro da ADR-001 | Interface e resposta da API | Testes por desktop/tablet/mobile, erros de conflito/bloqueio/rejeição compreensíveis; navegadores e critérios de usabilidade pendentes. |
| RF-14 — documentação pública/API | ADR-001 e ADR-006 condicionam contratos | Contratos/API e documentação de fluxos | Revisão de correspondência entre endpoints, auth, exemplos, erros e implementação; publicação/localização da documentação ainda não especificada. |
| RNF-01 — segurança/autorização | ADR-007; mecanismo concreto pendente | API, identidade, worker, storage, registry | Testes de controle de acesso, validação, erro sem dado sensível e revisão de vulnerabilidades; zero vulnerabilidade crítica é meta da visão geral. |
| RNF-02 — integridade/concorrência | ADR-001 | Reserva e persistência | Teste de concorrência e atomicidade em banco containerizado; demonstrar sem estado parcial. |
| RNF-03 — desempenho mensurável | ADR-009 | Consulta e criação de reserva | Relatório JMeter com duração, carga, falhas e cenários. Carga, tempo e taxa aceitável dependem de aprovação. |
| RNF-04 — estratégia de testes | ADR-001..009 como fronteiras aplicáveis | Testes unitários, integração, API, E2E, Testcontainers, WireMock | Evidência JUnit 5, testes parametrizados, integração/API/E2E e TDD/BDD; mocks limitados conforme RNF. |
| RNF-05 — cobertura/RTM/qualidade | ADRs e matriz desta seção como insumos | CI, JaCoCo, SonarCloud, RTM | Relatório JaCoCo; metas existentes de cobertura 80% linhas/70% branches; RTM 100% requisitos críticos; evidência não gerada por esta revisão. |
| RNF-06 — CI em PR | ADR-009 não escolhe plataforma além do GitHub Actions já requerido | GitHub Actions e análise | Execução visível em pull requests com testes e verificações previstas. |
| RNF-07 — auditoria | ADR-001 e ADR-007 | Auditoria/persistência/logs autorizados | Toda mudança de estado com usuário, instante e operação; reserva iniciada não removida; teste de acesso e retenção quando definida. |
| RNF-08 — responsividade | Sem decisão de framework; ADR-001 para mensagens de negócio | Interface web | Fluxos em desktop/tablet/mobile com mensagens e feedback observáveis; navegadores pendentes. |
| RNF-09 — documentação | ADR-001/006 apenas delimitam contrato | Documentação de API/fluxos | Verificação de conformidade dos documentos com comportamento/contrato publicado e atualização junto a mudanças. |
| ASR-06 — job diário incremental (solicitação; não RF/RNF) | ADR-002, ADR-003, ADR-004, ADR-005, ADR-007, ADR-008, ADR-009 | Scheduler, worker, fonte, checkpoint, qualidade, registry, observabilidade | Testes de watermark, idempotência, lock/fencing, retries, dados degradados, avaliação, promoção, rollback, indisponibilidade, privacidade e custo depois de decisões aprovadas. |

## 6. Requisitos sem cobertura ou parcialmente cobertos

“Sem cobertura” aqui significa que o modelo arquitetural/decisório atual não
define componente, contrato ou evidência suficiente; não significa que o RF/RNF
foi removido.

| Lacuna | Requisitos afetados | Próxima decisão necessária |
|---|---|---|
| Fluxo de estados para reservas sem recurso restrito e critérios de rejeição/revisão | RF-04, RF-05, RF-07 | Clarificar quando uma reserva é confirmada/aprovada e como revisão não permite dupla alocação. |
| Manutenção que conflita com reserva já existente | RF-08 | Definir sinalizar, impedir manutenção, cancelar ou encaminhar para decisão; nenhuma opção assumida. |
| Critério de “dados novos”, fonte e atualizações tardias | ASR-06 | Definir evento de novidade, cursor, updates/deletes, lateness e ownership. |
| Finalidade, modelo, consumidor e avaliação de ML | ASR-06; sem requisito ML nos RF/RNF | Criar requisitos de produto/dados antes de operar treinamento. |
| Resultado, retry e consistência de notificação | RF-11 | Escolher simulação ou integração e definir persistência/entrega duplicada/falha. |
| Política de retenção e acesso à auditoria | RF-10, RNF-07 | Definir conteúdo, prazo, papéis autorizados e reconciliação com privacidade. |
| Autenticação concreta, gestão de credenciais e identidade | RF-01, RNF-01 | Escolher mecanismo após requisitos de deployment/segurança e documentar permissões. |
| Contrato de erros, endpoints e versão de API | RF-14, RNF-09 | Especificar operações e códigos sem contradizer regras funcionais. |
| Metas de desempenho, disponibilidade, backup e recuperação | RNF-03; parâmetros do RNF § 5 | Aprovar carga, latência/taxa de erro e, se aplicável, SLO/RTO/RPO. |
| Comportamento de rollback e aprovação de modelo | Não há RF/RNF específico; ADR-004/005 propostos | Definir autoridade, critérios, retenção e compatibilidade se ML confirmado. |

## 7. Decisões sem requisito específico

Estas escolhas são recomendações condicionais e não devem ser apresentadas como
compromissos do projeto sem aprovação:

| Decisão / mecanismo | Razão arquitetural | Status |
|---|---|---|
| Fonte batch específica, CDC ou coluna temporal composta | Viabilizar incrementalidade; semântica de dados não definida. | Aberto; ADR-002. |
| Snapshot imutável e manifesto | Recuperabilidade e reprodução de run. | Proposto; ADR-002. |
| Lease distribuída, fencing token e chave idempotente | Evitar jobs sobrepostos e efeitos duplicados. | Proposto condicional à topologia; ADR-003. |
| Schema/quality gates, quarentena e drift checks | Prevenir treino com dados defeituosos. | Classes propostas, limites pendentes; ADR-004. |
| Métricas do modelo, baseline, holdout e promoção manual/automática | Determinar candidato elegível. | Sem requisito nem valores; ADR-004. |
| Registry, estados de modelo, alias ativo e rollback | Governança/reprodutibilidade de ML. | Proposto apenas se ML confirmado; ADR-005. |
| Runtime/cliente de inferência online | Servir previsão a consumidor. | Fora do baseline até requisito; ADR-006. |
| Outbox e semântica de entrega da notificação | Não perder aviso ao separar envio do commit de negócio. | Recomendação, depende da integração; ADR-001. |
| Logs/métricas/alertas específicos de ML e retenção | Operar e investigar pipeline. | Proposto sem ferramenta, SLO ou duração escolhidos; ADR-007/008. |
| Processo/worker separado e quotas de recursos | Isolar interferência API/batch. | Sem implantação/volume aprovados; ADR-009. |
| Limites de retry, timeout, lease, retenção, recursos e custo | Tornar operação controlada. | Todos pendentes; não há números aprovados. |

## 8. Suposições explícitas

Nenhuma suposição deve ser promovida a fato sem validação:

1. A restrição do job diário permanece exigida pelo solicitante, mesmo não
   constando dos requisitos versionados.
2. Não se assume que reservas sejam a fonte de treinamento nem que qualquer dado
   pessoal possa ser reutilizado para ML.
3. Não se assume que treinamento delta-only seja tecnicamente adequado ao modelo
   futuro.
4. Não se assume uso online do modelo, fallback, recomendação ou automação de
   decisões de reserva.
5. Java 21 e Spring Boot 3.x são previstos pela visão geral/RNF, mas arquitetura
   de deployment e bibliotecas permanecem não escolhidas.
6. As prioridades ATAM são avaliação deste painel, sem workshop/aceite dos
   stakeholders.
7. Uma garantia de auditoria deve ser durável, mas a tecnologia e o mecanismo
   concreto ainda não foram escolhidos.
8. Requisitos quantitativos de SLO, custo, atraso do cron e qualidade preditiva
   não existem; seus limites não são supostos.

## 9. Perguntas abertas ao produto e responsáveis

1. Qual objetivo de negócio o modelo atende e quem consome seu resultado?
2. Qual é a fonte de dados aprovada, seu owner e quais atributos podem ser
   usados? Há dados pessoais ou sensíveis?
3. “Novos” significa inseridos desde o último run, atualizados, eventos nunca
   vistos ou outra definição? Como tratar eventos tardios e exclusões?
4. Qual fuso, horário de fim de dia, atraso aceitável, catch-up e comportamento
   quando um dia é perdido?
5. Treino só no delta é obrigatório em sentido estrito? Como validar modelo sem
   usar histórico não autorizado?
6. Quais gates de schema/qualidade são bloqueantes e quem os aprova?
7. Qual métrica, baseline, avaliação e regra de promoção determinam que um
   candidato é elegível? Promoção manual ou automática? Quem aprova?
8. O modelo será servido online? Qual comportamento se runtime estiver
   indisponível e como garantir que ele não contorne RF-05/RF-06?
9. Quais dados e estados de auditoria devem ser retidos e por quanto tempo?
   Como conciliar retenção com direitos dos titulares e exclusão?
10. Qual banco/mecanismo de persistência garante a regra de não conflito? Qual
    mecanismo torna reserva e auditoria coerentes?
11. Qual mecanismo de autenticação, autorização de operação batch, gestão de
    segredos e resposta a incidente será usado?
12. Quais perfis de carga, limites de desempenho e objetivos de disponibilidade
    / recuperação serão aceitos?
13. Qual orçamento/ambiente e volume esperado permitem comparar execução local,
    worker separado e serviço gerenciado?
14. Qual é a política para reservas existentes afetadas por manutenção e para
    conflitos encaminhados a revisão?

## 10. Riscos aceitos e riscos pendentes

**Riscos formalmente aceitos: nenhum identificado.** Os documentos consultados
não registram autoridade, justificativa, prazo ou assinatura de aceite para
nenhum risco. “Não-risco” da avaliação ATAM não é aceite de risco.

| ID / tema | Status neste parecer | Condição para aceite ou encerramento |
|---|---|---|
| AT-R1/R2 — perda/duplicidade e jobs sobrepostos | Aberto; não mitigado por diagramas. | Contrato de cursor, idempotência, lock/fencing e evidência de teste aprovados. |
| AT-R3 — falha parcial | Aberto; retry e checkpoint não têm política fechada. | Plano de recuperação por fase e teste de falhas. |
| AT-R4/R5 — candidato inadequado | Aberto; critérios/autoridade ausentes. | Validação de ML aprovada; promoção bloqueada até então. |
| AT-R6 — dados degradados | Aberto; gates sem limites. | Regras e tratamento de qualidade aprovados e testados. |
| AT-R7/R10 — exposição/retenção incompatível | Aberto; nenhum inventário ou avaliação legal disponível. | Aprovação dos responsáveis por segurança/privacidade e política documentada. |
| AT-R8 — indisponibilidade | Aberto; sem SLO/RTO/RPO/fallback. | Necessidade e metas de disponibilidade/recuperação aprovadas. |
| AT-R9 — contenção/custo | Aberto; sem teste comparativo nem orçamento. | Experimento de carga/custo e aceite do responsável. |

## 11. Próximos experimentos e evidências

Os experimentos abaixo são propostas de validação; não definem thresholds sem
aceite nem constituem instruções de execução.

| Experimento | Hipótese a testar | Evidência a coletar | Dependência/decisão anterior |
|---|---|---|---|
| Concorrência de reserva sobre banco de teste realista | A estratégia escolhida impede dupla reserva e gravação parcial. | Resultado concorrente, estado persistido, auditoria e latência observada. | Escolher tecnologia de persistência; critério de integridade já existe, limites de latência não. |
| Falha no commit de reserva/auditoria e no envio de notificação | Auditoria segue a reserva e falha de notificação não perde nem duplica operação. | Estado final, trilha de auditoria e entrega idempotente. | Aceitar ADR-001 e decidir se integração externa será usada. |
| Replay de cursor/watermark com empate e atraso | Estratégia captura elegíveis sem reprocessar fora da semântica aprovada. | IDs de entrada, cursor C/H, manifestos e diferença contra fonte. | Definir fonte e “novo”; sem isso experimento não tem oráculo. |
| Worker interrompido em cada fronteira de fase | Run retoma do mesmo snapshot sem avançar estado indevido. | Estado por fase, registros duplicados/perdidos, checkpoint e versão ativa. | Definir durabilidade, ADR-002/008. |
| Dois workers e expiração de lease | Fencing impede writer obsoleto de alterar checkpoint/alias. | Tokens, commits aceitos/rejeitados e estados finais. | Escolher lease/scheduler, ADR-003. |
| Dataset com schema inválido, nulos, duplicatas e drift | Quality gate detecta e classifica dados antes do treino. | Regras acionadas, contagens, quarentena e decisão. | Aprovar regras e limiares de dados, ADR-004. |
| Comparação candidato versus baseline | Avaliação detecta não regressão sob protocolo aceito. | Métricas, intervalo de incerteza, versão e decisão de gate. | Caso de uso, métrica, baseline e dados de avaliação autorizados. |
| Promoção interrompida e rollback | Referência ativa nunca aponta a artefato incompleto e versão anterior é recuperável. | Estado do registry, compatibilidade e trilha de mudança. | Aceitar ADR-005; política de retenção e aprovador. |
| Acesso/privacidade por perfil e por serviço | Usuários e workers só acessam dados necessários; logs/lineage não expõem conteúdo excessivo. | Matriz de permissões, tentativa negada, amostras revisadas de logs/artefatos. | Inventário, finalidade, políticas e decisão de segurança/LGPD. |
| Comparativo de topologia e custo | Worker compartilhado não degrada fluxos críticos ou alternativa isolada cabe no orçamento. | Carga API, duração/custo estimado do batch, storage/retention. | Perfil de carga, volumes, ambiente e orçamento aprovados. |

## 12. Histórico

- **2026-11-02:** criado parecer independente com objeções por papel, divergências,
  análise de coerência dos diagramas/ATAM, ADRs propostos, matriz de cobertura,
  lacunas, suposições, perguntas, riscos e experimentos. Fonte de verdade não
  alterada.
