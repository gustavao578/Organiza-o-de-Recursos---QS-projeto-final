# Avaliação arquitetural — Ciclo 01 (ATAM)

**Data:** 2026-11-08
**Escopo:** avaliação da arquitetura documentada e da implementação observável
da API de reservas. Esta é uma análise baseada em evidências, não uma aprovação
de arquitetura nem uma validação de produção. Requisitos e decisões propostos
foram mantidos separados.

## 1. Síntese e limites da avaliação

O caminho de criação de reserva possui uma tática concreta de consistência:
transação de serviço, bloqueio pessimista por recurso, verificação de
manutenção/conflito e teste de concorrência em H2. Isso endereça o cenário
prioritário de reserva concorrente no escopo atual de recursos, mas ainda não
comprova a garantia em banco de produção ou em toda a modelagem exigida.

Os riscos mais relevantes para os direcionadores do produto continuam sendo:

1. Autenticação e autorização por perfil não aparecem implementadas nas rotas
   observadas.
2. O domínio não representa estados de reserva ou eventos de auditoria; a
   exclusão atual não verifica se uma reserva já começou.
3. A API cobre somente parte do modelo de recurso e dos fluxos previstos.
4. Há evidência local de JaCoCo e suíte Maven; Testcontainers e JMeter seguem
  ausentes. O workflow foi configurado para SonarCloud, mas ainda não existe
  relatório/issues nem Quality Gate remoto observável.

**Limite de evidência remota:** o workflow local em
[ci.yml](../../.github/workflows/ci.yml) ainda não foi publicado nem executado
em um pull request. A configuração usa checkout completo, Maven SonarScanner,
XML JaCoCo, `SONAR_TOKEN` somente como variável de ambiente e espera pelo
Quality Gate. A API GitHub negou a listagem de secrets com HTTP 403, e não há
`SONAR_ORGANIZATION`/`SONAR_PROJECT_KEY` locais; portanto não foi possível
confirmar o secret, executar a análise ou ler o gate. Nenhum token foi acessado
ou exposto. A verificação local atual é `mvn -B clean verify`: 39 testes
passaram, com JaCoCo em 101/105 linhas (96,2%) e 22/26 branches (84,6%);
`ReservaService` está em 51/51 linhas e 16/16 branches. Relatórios locais:
`target/site/jacoco/index.html` e `target/site/jacoco/jacoco.xml`. Esses dados
não substituem o relatório do SonarCloud nem uma execução remota.

### Base documental

- [Visão geral](../visão-geral.md): propósito, personas, regras críticas,
  metas de qualidade e estado atual.
- [Requisitos funcionais](../requisitos-funcionais.md) e
  [não funcionais](../requisitos-não-funcionais.md): requisitos RF-01–RF-14 e
  RNF-01–RNF-09.
- [Arquitetura](../arquitetura.md): baseline inicial, avaliação ATAM anterior,
  arquitetura complementar e ADRs propostos. O documento declara que propostas
  e pareceres não alteram requisitos nem equivalem a decisões aprovadas
  ([arquitetura.md](../arquitetura.md#L137),
  [arquitetura.md](../arquitetura.md#L1092)).
- [Personas](../personas.md): Solicitante, Responsável e Administrador; as
  próprias personas mantêm permissões e detalhes de fluxo como questões abertas
  ([personas.md](../personas.md#L1)).

**Notação:** em cada cenário, “medida” é o resultado observável. Limites são
classificados como **requisito** quando a fonte os define; onde a fonte não
aprova números, a medida é coleta/decisão pendente, não SLA presumido.

## 2. Direcionadores de negócio

| ID | Direcionador documentado | Origem | Consequência para a arquitetura |
|---|---|---|---|
| D1 | Organizar salas, professores e materiais para atividades, evitando conflitos e permitindo consultar disponibilidade e acompanhar reservas. | [Visão geral](../visão-geral.md#L7), RF-02–RF-05 | Modelo de disponibilidade precisa representar cada categoria e combinar reserva, agenda, bloqueio e manutenção. |
| D2 | Servir Solicitantes, Responsáveis e Administradores com operações compatíveis com seus papéis. | [Personas](../personas.md#L7), [RF-01](../requisitos-funcionais.md#L59) | Identidade e autorização devem ser aplicadas no backend, inclusive para chamadas diretas à API. |
| D3 | Preservar integridade de reserva: fim posterior ao início, sem sobreposição, uma única reserva aceita em conflito simultâneo, e indisponibilidade em manutenção. | [Visão geral](../visão-geral.md#L55), [RF-05/RF-06](../requisitos-funcionais.md#L140) | Verificação e persistência devem ser coordenadas em uma fronteira transacional e testadas sob concorrência. |
| D4 | Rastrear operações relevantes, mudanças de estado e movimentações de materiais. | [RF-09/RF-10](../requisitos-funcionais.md#L227), [RNF-07](../requisitos-não-funcionais.md#L101) | Persistência do evento de negócio e auditoria precisa ter garantia de consistência definida. |
| D5 | Demonstrar qualidade de software com testes, segurança, rastreabilidade e desempenho mensuráveis; metas registradas: 80% de linhas, 70% de branches e 100% de requisitos críticos na RTM. | [Visão geral](../visão-geral.md#L75), [RNF-03–RNF-06](../requisitos-não-funcionais.md#L46) | Requer evidência automatizada por fronteira, integração contínua, cobertura, testes de carga e rastreabilidade. |
| D6 | Oferecer interface responsiva, mensagens compreensíveis e documentação coerente com os fluxos públicos. | [RF-13/RF-14](../requisitos-funcionais.md#L303), [RNF-08/RNF-09](../requisitos-não-funcionais.md#L114) | Requer superfícies de UI/API e validação compatíveis com os três perfis. |

O cron de treinamento diário e incremental aparece em uma solicitação anterior
e em propostas da arquitetura, mas não nos RF/RNF validados. O próprio documento
de arquitetura o identifica como restrição de escopo ainda não confirmada
([arquitetura.md](../arquitetura.md#L8),
[arquitetura.md](../arquitetura.md#L515)). Portanto, não é tratado aqui como
direcionador de negócio aprovado nem como comportamento que a API deva ter.

## 3. Utility tree

**Importância:** impacto para os direcionadores e força da exigência existente.
**Dificuldade:** incerteza e interação técnica aparentes; não é estimativa de
esforço aprovada pela equipe. **A/M/B** significam alta, média e baixa.

```text
Utilidade do sistema
├── Integridade operacional (D1, D3)
│   ├── AT-01: disputa simultânea pela mesma alocação [A, A]
│   └── AT-02: manutenção sobreposta impede nova reserva [A, M]
├── Segurança e responsabilidade (D2)
│   └── AT-03: chamada sem identidade ou sem permissão é negada [A, A]
├── Rastreabilidade (D4)
│   └── AT-04: alteração de negócio tem histórico consultável [A, A]
├── Evidência de qualidade (D5)
│   ├── AT-05: alteração em pull request executa testes e disponibiliza resultado [A, M]
│   └── AT-06: desempenho online medido sob carga aprovada [M, A]
└── Experiência e contrato (D6)
    └── AT-07: erros/documentação e fluxos permanecem compreensíveis [M, M]
```

| ID | Folha/cenário | Importância | Dificuldade | Justificativa e rastreabilidade |
|---|---|---:|---:|---|
| AT-01 | Uma única reserva conflitante aceita sob concorrência | A | A | RF-06/RNF-02 definem limite observável; estratégia precisa funcionar no banco usado. |
| AT-02 | Reserva que cruza manutenção é rejeitada e não aparece disponível | A | M | Regra explícita em RF-08; pesquisa de disponibilidade também deve refletir bloqueios. |
| AT-03 | Operação protegida sem autenticação/autorização é negada | A | A | RF-01/RNF-01; perfis e limites de ação ainda precisam de definição detalhada. |
| AT-04 | Mudança relevante gera registro com ator, instante, ação e antes/depois aplicável | A | A | RF-10/RNF-07; retenção, acesso e atomicidade da auditoria estão pendentes. |
| AT-05 | Pull request recebe resultado verificável de testes automatizados | A | M | RNF-06; workflows e runs remotos não foram encontrados. |
| AT-06 | Consulta/criação sob carga tem duração, volume e falhas registrados | M | A | RNF-03 exige JMeter; carga e limites de latência/taxa de erro ainda não foram aprovados. |
| AT-07 | Erros e contratos públicos refletem os fluxos implementados | M | M | RF-13/RF-14 e RNF-08/RNF-09; ainda não há UI e a documentação é parcial. |

## 4. Cenários de atributo de qualidade (seis partes)

### AT-01 — Concorrência de reserva

- **Fonte:** dois ou mais Solicitantes autenticados.
- **Estímulo:** enviam reservas incompatíveis para o mesmo recurso e período,
  em paralelo.
- **Artefato:** API de reservas, serviço de domínio, repositórios e persistência.
- **Ambiente:** fluxo online com persistência e concorrência; requisito não
  especifica um banco, e o RNF exige teste automatizado de concorrência.
- **Resposta:** no máximo uma reserva conflitante é aceita; respostas rejeitadas
  são explícitas e não deixam gravações parciais.
- **Medida de resposta:** aceita por grupo de requisições simultâneas ≤ 1;
  gravações parciais de operações rejeitadas = 0. A duração e carga devem ser
  coletadas; seus limites seguem pendentes.
- **Rastreabilidade:** RF-05, RF-06, RNF-02; D1 e D3.

### AT-02 — Reserva durante manutenção

- **Fonte:** Solicitante e Administrador.
- **Estímulo:** um Solicitante pede um recurso cujo período solicitado se
  sobrepõe a uma manutenção cadastrada.
- **Artefato:** fluxo de criação, consulta de manutenção e pesquisa de
  disponibilidade.
- **Ambiente:** manutenção persistida; operação normal da API.
- **Resposta:** a criação é rejeitada com mensagem clara e o recurso não é
  apresentado como disponível nesse intervalo.
- **Medida de resposta:** reservas aceitas que se sobrepõem à manutenção = 0;
  resultados de disponibilidade incorretos = 0.
- **Rastreabilidade:** RF-03, RF-08, RNF-02; D1 e D3.

### AT-03 — Autorização no backend

- **Fonte:** usuário anônimo ou autenticado sem permissão.
- **Estímulo:** chama diretamente endpoint protegido ou tenta operação fora do
  próprio perfil.
- **Artefato:** autenticação, autorização, controllers e auditoria de segurança.
- **Ambiente:** qualquer rota pública/protegida, sem depender de interface.
- **Resposta:** negar acesso, não revelar dados/ações protegidos e registrar a
  tentativa não autorizada conforme RNF-01.
- **Medida de resposta:** operações protegidas concluídas sem autorização = 0;
  tentativas registradas conforme a política aprovada. Mapeamento de perfis e
  matriz detalhada ainda precisam ser acordados.
- **Rastreabilidade:** RF-01, RNF-01; D2.

### AT-04 — Consistência da auditoria de negócio

- **Fonte:** Solicitante, Responsável ou Administrador executa uma operação
  relevante.
- **Estímulo:** cria/alterar/cancela reserva, decide aprovação, mantém recurso
  ou registra movimentação de material.
- **Artefato:** serviço de domínio, transação de persistência e histórico
  auditável.
- **Ambiente:** operação normal; a garantia entre commit e gravação do histórico
  deve ser especificada e testável.
- **Resposta:** persistir evento identificando usuário, data/hora, operação e
  valores antes/depois quando aplicável; permitir consulta apenas a usuários
  autorizados.
- **Medida de resposta:** operações relevantes sem evento auditável = 0; campos
  requeridos ausentes = 0. Retenção e política de consulta permanecem pendentes.
- **Rastreabilidade:** RF-10, RNF-07; D4.

### AT-05 — Feedback de CI em pull request

- **Fonte:** membro da equipe abre ou atualiza um pull request.
- **Estímulo:** commit altera a aplicação ou seus testes.
- **Artefato:** workflow GitHub Actions e job de verificação.
- **Ambiente:** GitHub Actions para um pull request contra o repositório.
- **Resposta:** executar verificações previstas, expor estado no PR e preservar
  relatórios de testes e análise estática para consulta.
- **Medida de resposta:** o workflow local executa Maven verify, publica o
  artifact JaCoCo e invoca SonarCloud com `fetch-depth: 0`, XML JaCoCo,
  `SONAR_TOKEN` e espera pelo Quality Gate. A configuração está no worktree; sem
  secret/variáveis confirmados e execução no PR, status do gate e lista de issues
  continuam sem evidência.
- **Rastreabilidade:** RNF-04–RNF-06; D5.

### AT-06 — Desempenho de consulta e reserva

- **Fonte:** Solicitantes executam consulta de disponibilidade e criação de
  reserva.
- **Estímulo:** uso concorrente sob perfil de carga representativo.
- **Artefato:** API, consultas de persistência, mecanismo de bloqueio e runtime.
- **Ambiente:** avaliação JMeter com conjunto de dados e carga definidos.
- **Resposta:** coletar duração, volume e falhas por cenário, identificar
  gargalos e comparar somente contra limites aprovados.
- **Medida de resposta:** relatório deve registrar duração, volume, falhas,
  cenários e carga aplicada. Latência, volume simultâneo, dados e taxa de erro
  aceitável continuam sem valor aprovado.
- **Rastreabilidade:** RNF-03; D5.

### AT-07 — Clareza e fidelidade dos fluxos públicos

- **Fonte:** Solicitante, Responsável e Administrador.
- **Estímulo:** consulta documentação ou recebe validação, conflito, bloqueio,
  rejeição ou confirmação.
- **Artefato:** API, representação de erros, documentação de contrato e UI
  responsiva.
- **Ambiente:** fluxos cobertos em desktop, tablet e mobile quando houver UI.
- **Resposta:** comunicação compreensível e consistente com a implementação,
  com erros e operações documentados.
- **Medida de resposta:** fluxos públicos documentados correspondem aos
  endpoints, erros e regras implementados; critérios de usabilidade,
  navegadores e métricas objetivas ainda precisam de validação.
- **Rastreabilidade:** RF-13, RF-14, RNF-08, RNF-09; D6.

## 5. Abordagens e táticas observadas no código

Esta seção cobre as folhas de maior importância do utility tree. “Observado”
descreve implementação, não garante comportamento em produção.

| Cenário | Abordagem/tática encontrada e evidência | Avaliação da evidência |
|---|---|---|
| **AT-01 — concorrência** | O método de criação está em transação e adquire bloqueio pessimista da linha do recurso antes das consultas e da gravação ([ReservaService.java](../../src/main/java/br/edu/organizacaorecursos/service/ReservaService.java#L32), [RecursoRepository.java](../../src/main/java/br/edu/organizacaorecursos/repository/RecursoRepository.java#L14)). A consulta identifica sobreposição com `início existente < fim solicitado` e `fim existente > início solicitado` ([ReservaRepository.java](../../src/main/java/br/edu/organizacaorecursos/repository/ReservaRepository.java#L13)); o serviço então verifica manutenção, conflito e persiste ([ReservaService.java](../../src/main/java/br/edu/organizacaorecursos/service/ReservaService.java#L49)). | É uma defesa no backend e não apenas na UI. Testes de integração verificam uma única reserva aceita em corrida e ausência de persistência em conflito ([ReservaApiIntegrationTest.java](../../src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java#L391)). A prova atual usa H2; não há Testcontainers/engine de produção nem cobertura de professor/material. |
| **AT-02 — manutenção** | Uma consulta JPA detecta a interseção de intervalos de manutenção e reserva ([ManutencaoRepository.java](../../src/main/java/br/edu/organizacaorecursos/repository/ManutencaoRepository.java#L12)); o serviço rejeita antes de checar conflito de reservas ou salvar ([ReservaService.java](../../src/main/java/br/edu/organizacaorecursos/service/ReservaService.java#L49)). | O caso de criação está coberto por teste unitário e integração ([ReservaServiceTest.java](../../src/test/java/br/edu/organizacaorecursos/service/ReservaServiceTest.java#L84), [ReservaApiIntegrationTest.java](../../src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java#L306)). Não existe busca de disponibilidade para comprovar que manutenção seja excluída dos resultados de pesquisa. |
| **AT-03 — autorização** | **Nenhuma tática de autenticação/autorização observada.** Os controllers expõem endpoints REST de criação, leitura e exclusão sem anotação ou componente de segurança nesses controllers ([ReservaController.java](../../src/main/java/br/edu/organizacaorecursos/controller/ReservaController.java#L20), [RecursoController.java](../../src/main/java/br/edu/organizacaorecursos/controller/RecursoController.java#L17), [UsuarioController.java](../../src/main/java/br/edu/organizacaorecursos/controller/UsuarioController.java#L16)). | Validação de DTO/entrada e tratamento de erros existem, mas não substituem identidade, autorização e registro de acesso. Não foi localizada implementação de segurança nos arquivos de aplicação inspecionados. |
| **AT-04 — auditoria** | **Não há entidade, repositório ou serviço de auditoria na aplicação atual.** `Reserva` contém recurso, intervalo e usuário, sem estado ou metadados de mudança ([Reserva.java](../../src/main/java/br/edu/organizacaorecursos/domain/Reserva.java#L17)). A exclusão verifica existência e apaga sem condição de estado ([ReservaService.java](../../src/main/java/br/edu/organizacaorecursos/service/ReservaService.java#L94)). | A resposta HTTP não demonstra histórico persistente, antes/depois, imutabilidade nem consistência transacional da auditoria. |
| **AT-05 — CI e análise estática** | O workflow local usa checkout `fetch-depth: 0`, Java Temurin 21, cache Maven, `mvn --batch-mode verify`, Sonar Maven Scanner apontado para `https://sonarcloud.io`, importação de `target/site/jacoco/jacoco.xml`, espera pelo Quality Gate e upload do artifact JaCoCo ([ci.yml](../../.github/workflows/ci.yml)). | Execução local: 39 testes verdes; JaCoCo global 96,2% linhas/84,6% branches. O plugin Sonar está configurado para ler `SONAR_TOKEN` de env e os identificadores de variáveis Actions, mas secret e vars não puderam ser confirmados (403 na API). Nenhum relatório SonarCloud, issue ou gate foi produzido/consultado; PR e execução remota continuam pendentes. |
| **AT-06 — desempenho** | **Nenhuma tática de medição/performance observada.** O serviço executa consultas de conflito e, quando há conflito, busca detalhes em uma consulta adicional ([ReservaService.java](../../src/main/java/br/edu/organizacaorecursos/service/ReservaService.java#L56), [ReservaRepository.java](../../src/main/java/br/edu/organizacaorecursos/repository/ReservaRepository.java#L25)). | O banco configurado é H2 em memória ([application.properties](../../src/main/resources/application.properties#L1)); não há evidência JMeter ou perfil de carga. Não se pode concluir desempenho ou falha contra limite inexistente. |
| **AT-07 — erros/contratos** | `@RestControllerAdvice` mapeia erro de negócio, conflito, tipo inválido e validação em respostas 4xx ([ApiExceptionHandler.java](../../src/main/java/br/edu/organizacaorecursos/exception/ApiExceptionHandler.java#L14)). OpenAPI/Swagger existe conforme cobertura registrada em [plano-tdd](../testes/plano-tdd.md#L356). | Há feedback de erro básico, mas não existe UI responsiva. A documentação não demonstra autenticação, fluxos completos ou sincronização contínua com todos os requisitos. |

## 6. Pontos de sensibilidade

| Ponto sensível | Cenários/direcionadores | Efeito e decisão/evidência pendente |
|---|---|---|
| Semântica e garantia de `PESSIMISTIC_WRITE` no banco escolhido | AT-01, AT-06; D3/D5 | O código depende do lock por linha de `Recurso`; isolamento, timeout e comportamento devem ser comprovados no banco de destino. H2 em memória é apenas a configuração observada, não uma decisão de produção. |
| Granularidade do lock | AT-01, AT-06; D3/D5 | O lock é por recurso. Isso serializa requisições concorrentes daquele recurso, inclusive intervalos diferentes; preserva simplicidade de correção, mas pode limitar throughput. Medir antes de trocar mecanismo. |
| Regra de intervalo | AT-01/AT-02; D3 | A fórmula estrita permite intervalos adjacentes e bloqueia sobreposição parcial. Testes confirmam adjacência e colisão de reserva; cobrir as bordas equivalentes da manutenção e demais tipos de recurso. |
| Limite transacional de negócio | AT-01/AT-04; D3/D4 | O serviço transaciona reserva, mas não há auditoria nem notificação persistente para incluí-las na mesma unidade lógica. A proposta ADR-001 discute consistência, ainda com status proposto ([arquitetura.md](../arquitetura.md#L1300)). |
| Modelo do domínio | AT-01–AT-04; D1/D2/D4 | O tipo de recurso é string; a reserva não contém estado; usuários não possuem perfil no modelo atual. Essas lacunas limitam conflitos por professor/material, aprovação e autorização. |
| Banco H2 em memória e DDL mutável | AT-01, AT-04, AT-06; D3–D5 | `jdbc:h2:mem` e `ddl-auto=update` favorecem protótipo, mas não provam persistência durável, recuperação nem equivalência com engine de produção. |
| Perfil/carga e limites | AT-06; D5 | RNF-03 exige medir mas deixa carga, volumes, latência e taxa de erro para aprovação; não é possível fechar conformidade sem esses parâmetros. |
| Configuração SonarCloud e credenciais | AT-05; D5 | Workflow exige `SONAR_TOKEN`, `SONAR_ORGANIZATION` e `SONAR_PROJECT_KEY`. A API GitHub negou leitura da lista de secrets e as variáveis não estão definidas localmente; o scanner não foi executado, então o Quality Gate e os issues permanecem desconhecidos. |
| Publicação e proteção de branch | AT-05; D5 | Workflow configurado no worktree, mas ainda sem PR/run remoto. Check obrigatório `verify`/Quality Gate e branch protection não foram confirmados. |

## 7. Pontos de trade-off

| Trade-off | Benefício observado | Custo/risco | Cenários |
|---|---|---|---|
| Bloqueio pessimista por recurso vs. maior paralelismo | Simplifica a arbitragem da corrida entre validar conflito e salvar; teste atual verifica uma única reserva aceita. | Serializa escritas do mesmo recurso, inclusive horários não sobrepostos; depende da semântica do banco e pode causar espera. | AT-01, AT-06 |
| H2 em memória vs. banco representativo de operação | Inicialização simples, rápida e reproduzível para testes atuais. | Perde estado no ciclo da aplicação e não prova comportamento de concorrência/durabilidade da produção. | AT-01, AT-04, AT-06 |
| Regra no serviço vs. constraint transacional de exclusão | Facilita mensagens de negócio e consulta do intervalo conflitante. | Não há constraint temporal de banco observada; a garantia depende do lock/correção da consulta e não foi verificada em outro engine. | AT-01 |
| Resposta de conflito com detalhe contextual vs. minimização da resposta | Ajuda a identificar recurso e intervalo conflitante. | O contrato, autorização e nível de detalhe para diferentes perfis não estão definidos. | AT-03, AT-07 |
| API em camadas simples vs. fronteiras operacionais separadas | Estrutura controller/service/repository é direta e suficiente ao fluxo presente. | Não existe evidência para decidir se worker/batch precisa ser separado; pipeline de ML permanece não aprovado e não implementado. | AT-05, evolução |
| Auditoria síncrona vs. notificação assíncrona | Auditoria consistente com operação favorece rastreabilidade; notificação fora da transação evita depender de provedor no commit. | Exige decidir outbox/reconciliação/idempotência; nenhuma dessas táticas está implementada. | AT-04 |

## 8. Riscos, não-riscos e temas de risco

### Riscos identificados

| ID | Risco e consequência | Direcionador / cenário | Evidência que reduz ou confirma o risco |
|---|---|---|---|
| R1 | Sem autenticação/autorização, qualquer cliente capaz de chamar endpoints pode tentar operações que RF-01 reserva a certos perfis. | D2; AT-03 | Implementar matriz de permissões e testar chamadas diretas anônimas/sem perfil; código observado não mostra controles de segurança nos controllers. |
| R2 | Exclusão não condicional pode apagar reserva iniciada, contrariando RNF-07; não há estado para decidir o que é “iniciada”. | D3/D4; AT-04 | Definir estados/transições e teste que prove bloqueio de exclusão; domínio atual não contém status. |
| R3 | Falta de auditoria impede rastrear ator, momento, operação e antes/depois, e pode deixar alterações sem trilha. | D4; AT-04 | Adicionar persistência de auditoria com garantia de consistência definida e teste de falha entre operação e evento. |
| R4 | O domínio de `Recurso` não representa sala, professor e material conforme dados mínimos; consulta pode não atender filtros/agenda e conflito de todas as categorias. | D1/D3; AT-01/02 | Fechar modelo de domínio e regras por categoria; cruzar com RF-02/03/05. |
| R5 | Lock pessimista validado em H2 pode não demonstrar RNF-02 para o banco operacional; falta Testcontainers. | D3/D5; AT-01 | Teste concorrente repetível no engine escolhido, medindo uma aceitação e zero estado parcial. |
| R6 | Sem workflow publicado e run remoto, alterações em PR ainda não demonstram os checks de Maven, JaCoCo e SonarCloud nem a retenção de relatórios. | D5; AT-05 | YAML local inclui verify, artifact JaCoCo e espera pelo Sonar Quality Gate; falta publicar e confirmar run/check no PR e branch protection. |
| R7 | Ausência de JMeter, perfil de carga aprovado e metas impede identificar regressão ou declarar desempenho conforme. | D5; AT-06 | Definir perfil/limites e produzir relatório de medição; não inferir SLA. |
| R8 | Rotas de cadastro retornam entidades diretamente e os contratos de erro/documentação são parciais; sem auth, exposição e evolução de contrato não estão controladas por perfil. | D2/D6; AT-03/07 | Decidir DTOs públicos, campos por perfil e política de contrato/documentação; validar RF-14. |
| R9 | Sem `SONAR_TOKEN` e identificadores SonarCloud confirmados, a análise não pode produzir Quality Gate nem lista de issues; aceitar ausência como aprovação esconderia risco estático. | D5; AT-05 | Configuração está no workflow, mas a API retornou 403 ao listar secrets e não há execução. Confirmar secret/vars e ler o gate antes de classificar qualquer issue. |

### Não-riscos nesta avaliação

| Item | Por que não é risco confirmado agora | Reclassificação |
|---|---|---|
| Não escolheram fornecedor de banco ou cloud | A arquitetura registra essas escolhas como pendentes e não há requisito de fornecedor específico. O risco está na ausência de prova do requisito de concorrência/durabilidade, não em não usar um fornecedor determinado. | Quando deployment e engine forem escolhidos, verificar lock, recuperação, segurança e custo. |
| Não atingir um valor específico de latência | RNF-03 não aprova limite quantitativo. Não há base para declarar violação de SLA inexistente. | Produto/equipe aprova carga e limites; então executar JMeter. |
| Inferência ML no fluxo de reservas | Não há caso de uso ou requisito funcional que conecte modelo à reserva; arquitetura marca inferência como condicional. | Só reavaliar se requisito aprovado introduzir previsão no caminho transacional. |
| Pipeline de treinamento delta-only como obrigação do produto | A solicitação aparece no documento de arquitetura como item ausente dos RF/RNF e condicionado à definição de finalidade/fonte. Não é uma falha da API por não implementá-lo. | Confirmar escopo e requisitos antes de um ciclo de arquitetura para ML. |

### Temas de risco ligados aos direcionadores

- **D1/D3 — alocação correta:** categoria e identidade do recurso, conflito
  temporal, manutenção e corrida entre leitura e escrita.
- **D2 — confiança e responsabilidade:** identidade, matriz de permissões,
  exposição de endpoints e auditoria de tentativas.
- **D4 — contestabilidade e rastreabilidade:** consistência de histórico,
  retenção, autorização de consulta e proteção contra exclusão de reservas em
  andamento.
- **D5 — evidência verificável:** banco representativo, suíte nos limites
  relevantes, cobertura rastreável (JaCoCo local acima das metas), CI publicado,
  Quality Gate SonarCloud consultado e critérios quantitativos aprovados. A
  cobertura está evidenciada; CI/Sonar remotos ainda não.
- **D6 — uso e compreensão:** contratos claros, UI inexistente neste escopo
  corrente e documentação que reflita comportamento real.

## 9. Comparação: promessas da arquitetura e comportamento observado

As promessas abaixo distinguem requisitos obrigatórios de propostas ainda não
aprovadas. “Ausente” significa não observado no código/ferramentas consultados,
não uma afirmação sobre qualquer branch ou ambiente não acessível.

| Promessa/requisito documentado | O que o código/ferramentas fazem | Resultado |
|---|---|---|
| Stack prevista Java 21/Spring Boot 3.x ([visão geral](../visão-geral.md#L37)) | `pom.xml` configura Java 21 e Spring Boot 3.2.4 ([pom.xml](../../pom.xml#L6)). | **Alinhado** no build; runtime de produção não avaliado. |
| API/domain/persistência em camadas com reserva atômica proposta ([arquitetura](../arquitetura.md#L539), [RF-06](../requisitos-funcionais.md#L161)) | Controllers delegam ao serviço; serviço `@Transactional` obtém lock pessimista e checa conflito antes de salvar ([ReservaService](../../src/main/java/br/edu/organizacaorecursos/service/ReservaService.java#L32), [RecursoRepository](../../src/main/java/br/edu/organizacaorecursos/repository/RecursoRepository.java#L14)). | **Parcialmente alinhado**; teste atual usa H2; atomicidade não comprovada em banco containerizado. |
| Recurso compreende sala, professor, material, capacidade, localização, agenda e estado (RF-02/RF-03) | `Recurso` tem somente UUID, nome e tipo; não há modelo observado de professor/material/agendas. ([Recurso.java](../../src/main/java/br/edu/organizacaorecursos/domain/Recurso.java#L15)) | **Divergência funcional significativa.** |
| Pesquisa por filtros e disponibilidade (RF-03) | Endpoint de recurso somente cadastra/lista tudo; endpoint não recebe filtros nem período ([RecursoController.java](../../src/main/java/br/edu/organizacaorecursos/controller/RecursoController.java#L17)). | **Não implementado.** |
| Criar, alterar e cancelar reservas com estado e justificativa (RF-04) | Reserva permite POST, GET por ID e DELETE; não há PUT/PATCH, cancelamento com motivo ou estado no domínio ([ReservaController.java](../../src/main/java/br/edu/organizacaorecursos/controller/ReservaController.java#L20), [Reserva.java](../../src/main/java/br/edu/organizacaorecursos/domain/Reserva.java#L22)). | **Parcial; exclusão física não equivale a cancelamento.** |
| Detectar conflito de sala/material/professor e agenda (RF-05) | Consulta de conflito filtra pelo ID de `Recurso` e intervalo ([ReservaRepository.java](../../src/main/java/br/edu/organizacaorecursos/repository/ReservaRepository.java#L13)); modelo não representa agenda de professor/estoque de material. | **Parcial** para o recurso atual; cobertura multi-recurso ausente. |
| Uma reserva aceita sob corrida (RF-06/RNF-02) | Lock pessimista por recurso, transação e teste simultâneo H2; plano atual informa 39 testes verdes e registra Testcontainers como pendente ([plano-tdd](../testes/plano-tdd.md#L331), [plano-tdd](../testes/plano-tdd.md#L380)). | **Evidência parcial, não prova independente do engine.** |
| Aprovação de recurso restrito e estados (RF-07) | Não há estado de reserva, aprovador ou endpoint de decisão no domínio/API inspecionados. | **Não implementado.** |
| Manutenção bloqueia reserva (RF-08) | Cadastro/listagem de manutenção e teste de bloqueio no fluxo de criação; status de disponibilidade não existe na pesquisa. | **Parcial.** |
| Retirada/devolução de material (RF-09) | Não há modelo/fluxos de movimentação observados. | **Não implementado.** |
| Auditoria consultável e imutabilidade (RF-10/RNF-07) | Não há componente de auditoria observado; deleção apenas verifica existência e remove. | **Divergência.** |
| Notificação simulada ou externa (RF-11) | Não há componente de notificação nem integração observada. | **Não implementado.** |
| Relatórios e métricas (RF-12) | Não há endpoints/serviços de relatório observados. | **Não implementado.** |
| UI responsiva e acessível (RF-13/RNF-08) | O inventário do workspace mostra aplicação Java/API e nenhum cliente de UI para os fluxos do produto. | **Não implementado.** |
| Documentação de contratos completos (RF-14/RNF-09) | Swagger/OpenAPI está habilitado e um teste verifica publicação; não cobre evidência de autorização, exemplos/erros completos nem todos os fluxos de negócio. | **Parcial.** |
| Segurança e negação de acesso (RF-01/RNF-01) | Nenhum filtro/configuração de autenticação ou autorização observado; controllers publicados sem anotação de autorização. | **Não implementado/sem evidência.** |
| Testes exigidos (RNF-04) | `mvn -B clean verify` executou 39 testes (12 unitários/validação, 26 API e 1 persistência); Testcontainers, WireMock e E2E continuam sem evidência. | **Parcial.** |
| Cobertura JaCoCo (RNF-05) | Relatório local: 101/105 linhas (96,2%) e 22/26 branches (84,6%), com gates globais 80/70 aprovados; `ReservaService` 51/51 linhas e 16/16 branches. Arquivos `target/site/jacoco/index.html` e `target/site/jacoco/jacoco.xml`. | **Atende metas locais de cobertura; não prova qualidade estática nem RTM completa.** |
| Análise SonarCloud (RNF-05) | Workflow local configura Maven SonarScanner, `fetch-depth: 0`, XML JaCoCo, `SONAR_TOKEN` e espera pelo Quality Gate; secret/vars e relatório remoto indisponíveis nesta execução. | **Configurado, gate e issues sem evidência; não marcar como aprovado.** |
| CI em PR (RNF-06) | YAML local contém Maven verify, artifact JaCoCo e análise SonarCloud; ainda não publicado/executado em PR. | **Preparado localmente, não demonstrado no remoto.** |
| Desempenho mensurável (RNF-03) | Não foi encontrado perfil nem relatório JMeter. | **Sem evidência; critérios quantitativos pendentes.** |
| Batch/ML diário incremental | Arquitetura tem propostas de componentes/ADRs, marcadas condicionais/pendentes ([arquitetura](../arquitetura.md#L515), [ADR-002](../arquitetura.md#L1329)); código não contém pipeline batch. | **Não é divergência com RF/RNF validados; escopo não aprovado.** |

## 10. Ações priorizadas para o próximo ciclo

| Prioridade | Ação | Direcionadores/cenários | Evidência de saída para reavaliação | Status deste ciclo |
|---|---|---|---|---|
| **P0** | Definir e implementar identidade, perfis e matriz de autorização para cada operação; testar também acesso direto à API. | D2; AT-03; RF-01/RNF-01 | Matriz aprovada, testes anônimo/sem perfil por endpoint, registro de tentativas não autorizadas. | **Pendente**; sem mudança neste ciclo. |
| **P0** | Definir estados e transições da reserva; implementar auditoria e impedir exclusão física de reserva iniciada, com política de retenção/acesso. | D3/D4; AT-04; RF-04/RF-10/RNF-07 | Modelo de estados aprovado; teste de consistência operação-auditoria, acesso autorizado e bloqueio de exclusão. | **Pendente**; sem mudança neste ciclo. |
| **P0** | Publicar o workflow de CI no remoto e configurar `verify`/Quality Gate como checks requeridos na `main`. | D5; AT-05; RNF-06 | Run remoto verde verificável no PR, artifacts JaCoCo/Sonar e regra de proteção aplicada. | **Parcial**; YAML com verify, JaCoCo, SonarCloud e espera do gate preparado; PR, execução remota, secret/vars e branch protection pendentes. |
| **P1** | Selecionar/confirmar banco de destino e testar concorrência e persistência com Testcontainers nesse engine. | D3/D5; AT-01 | Repetições concorrentes: no máximo uma aceita, zero estado parcial; evidência do engine de destino. | **Pendente**; suíte continua em H2. |
| **P1** | Fechar o domínio e critérios de conflito para salas, professores e materiais; decidir como manutenção interage com reservas existentes e com busca. | D1/D3; AT-01/02; RF-02/RF-03/RF-05/RF-08 | Modelo validado, testes por categoria, filtro de disponibilidade e casos de manutenção. | **Parcial**; testes da API rejeitam manutenção sem ID/recurso existente; domínio multi-recurso e busca seguem pendentes. |
| **P1** | Aprovar perfil de carga e limites quantitativos de desempenho; medir consulta e criação com JMeter. | D5; AT-06; RNF-03 | Cenários, dataset, duração, volume e falhas reportados; limites aprovados antes de declarar conformidade. | **Pendente**; JaCoCo não é evidência de desempenho. |
| **P2** | Completar testes requeridos (Testcontainers, E2E, WireMock onde houver integração), JaCoCo/RTM/SonarCloud, sem reduzir escopo das asserções. | D5; RNF-04/RNF-05 | Evidências e métricas publicadas e ligadas a requisitos críticos. | **Parcial**; JaCoCo configurado e metas atingidas localmente; SonarCloud configurado mas sem análise/gate/issues; RTM completa e demais suites pendentes. |
| **P2** | Definir contratos DTO/API, códigos e conteúdo de erro; completar documentação de fluxos e decidir a UI responsiva. | D6; AT-07; RF-13/RF-14 | Contratos e exemplos sincronizados; critérios de viewport/navegador e testes de UI aprovados. | **Pendente**; sem mudança neste ciclo. |
| **P3 — condicional** | Confirmar se o cron/treinamento incremental pertence ao produto; somente então estabelecer finalidade, fonte, semântica de dados novos, avaliação e governança em ciclo ATAM separado. | Escopo não aprovado | Decisão de produto rastreável; não implementar inferência/batch por inferência a partir da arquitetura proposta. | **Pendente**; escopo segue não aprovado. |

## 11. Referências de código e testes

- [ReservaService.java](../../src/main/java/br/edu/organizacaorecursos/service/ReservaService.java)
- [ReservaRepository.java](../../src/main/java/br/edu/organizacaorecursos/repository/ReservaRepository.java)
- [RecursoRepository.java](../../src/main/java/br/edu/organizacaorecursos/repository/RecursoRepository.java)
- [ManutencaoRepository.java](../../src/main/java/br/edu/organizacaorecursos/repository/ManutencaoRepository.java)
- [ReservaController.java](../../src/main/java/br/edu/organizacaorecursos/controller/ReservaController.java)
- [ApiExceptionHandler.java](../../src/main/java/br/edu/organizacaorecursos/exception/ApiExceptionHandler.java)
- [Reserva.java](../../src/main/java/br/edu/organizacaorecursos/domain/Reserva.java)
- [Recurso.java](../../src/main/java/br/edu/organizacaorecursos/domain/Recurso.java)
- [application.properties](../../src/main/resources/application.properties)
- [ReservaApiIntegrationTest.java](../../src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java)
- [ReservaServiceTest.java](../../src/test/java/br/edu/organizacaorecursos/service/ReservaServiceTest.java)
- [ReservaRequestDTOTest.java](../../src/test/java/br/edu/organizacaorecursos/ReservaRequestDTOTest.java)
- Relatório JaCoCo HTML: `target/site/jacoco/index.html` (96,2% linhas; 84,6% branches).
- Relatório JaCoCo XML: `target/site/jacoco/jacoco.xml` (consumido pelo passo Sonar Maven).
- Relatório SonarCloud/Quality Gate: **não gerado nesta execução**; requer secret `SONAR_TOKEN` e variáveis `SONAR_ORGANIZATION`/`SONAR_PROJECT_KEY`, além de run do workflow no PR.
- [ci.yml](../../.github/workflows/ci.yml)
