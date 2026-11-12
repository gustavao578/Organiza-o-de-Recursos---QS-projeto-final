# Prova Oral — Banca Examinadora de Qualidade de Software
**Sistema: Organização de Recursos — QS Projeto Final**  
**Data**: 2026-11-12  
**Distribuição**: 15 perguntas específicas do projeto, nunca genéricas

---

## Instruções para a Banca

Cada pergunta:
- Refere-se a **código, testes, decisões arquiteturais ou defeitos específicos deste projeto**
- Indica o arquivo-fonte onde a resposta está
- Descreve o que seria uma **resposta fraca** (superficial, sem evidência)
- Deve ser respondida por um integrante que **não escreveu** essa parte do código

**Distribuição sugerida**: Distribua as 15 perguntas entre 4-5 integrantes, ~3 perguntas cada, sempre designando quem NÃO escreveu aquele módulo.

---

## Pergunta 1: Mecanismo de Prevenção de Dupla Reserva

**Arquivo-fonte**: [`src/main/resources/db/migration/V1__reservas_sem_sobreposicao_ativa.sql`](</src/main/resources/db/migration/V1__reservas_sem_sobreposicao_ativa.sql>)

**Pergunta**:
> Qual é o mecanismo específico que impede que duas reservas se sobreponham no mesmo horário para o mesmo recurso? Desenhe a constraint no papel ou explique sua sintaxe SQL. O que significa `tsrange()` e `WITH &&`? Por que incluir `WHERE (status IN ('PENDENTE', 'APROVADA', 'EM_USO'))`?

**Onde buscar a resposta**:
- SQL constraint EXCLUDE com gist
- Extensão `btree_gist`
- Lógica de range temporal com `tsrange` e operador `&&`

**Resposta fraca seria**:
- "Tem um constraint no banco de dados"
- Não mencionar `EXCLUDE`, `gist`, ou `tsrange`
- Não explicar por que status não-processados (REJEITADA, CANCELADA) não bloqueiam
- Não entender que é uma constraint **anterior ao aplicativo**, na camada de dados

---

## Pergunta 2: Validação de Concorrência — Teste vs. Constraint

**Arquivo-fonte**: [`src/test/java/br/edu/organizacaorecursos/ReservaConcorrenciaPostgresIntegrationTest.java`](</src/test/java/br/edu/organizacaorecursos/ReservaConcorrenciaPostgresIntegrationTest.java>) (linhas 1-120)

**Pergunta**:
> Por que este teste usa `Testcontainers` com PostgreSQL real e não mocks/Mockito? Qual cenário de erro esse teste cobre que um teste unitário com mock não cobriria? O que aconteceria se removermos a constraint EXCLUDE do banco—este teste detectaria o problema?

**Onde buscar a resposta**:
- Classe `ReservaConcorrenciaPostgresIntegrationTest`
- Annotations `@Testcontainers`, `@Container`, `PostgreSQLContainer`
- Método `dispararJuntos()` com `CountDownLatch` e `ExecutorService`
- Execução simultânea de múltiplas solicitações HTTP contra o mesmo timeslot

**Resposta fraca seria**:
- "Porque testa no banco de dados"
- Não diferenciar entre testar **race condition** (múltiplas threads) vs. testar **validação de entrada**
- Não notar que o teste executa 20+ requisições **concorrentemente** (não sequencial)
- Não compreender que, sem a constraint, o teste falharia mesmo com validação na API
- Dizer que Mockito seria equivalente

---

## Pergunta 3: Escolha de Nível de Teste — Mock vs. Integração

**Arquivo-fonte**: [`src/test/java/br/edu/organizacaorecursos/security/MatrizAutorizacaoTest.java`](</src/test/java/br/edu/organizacaorecursos/security/MatrizAutorizacaoTest.java>)

**Pergunta**:
> A `MatrizAutorizacaoTest` usa `@AutoConfigureMockMvc` (mocks do Spring) e não Testcontainers. Por que essa escolha foi apropriada aqui, mas não para o teste de concorrência? A qual nível de teste (unitário, integração, sistema) você classificaria este teste? Qual é o escopo específico que ele valida?

**Onde buscar a resposta**:
- `@SpringBootTest` + `@AutoConfigureMockMvc`
- Teste parametrizado com `@ParameterizedTest` + `@CsvSource`
- Cada célula da matriz autorização (perfil × operação × status esperado)
- Não há concorrência real, não há banco de dados real (em-memória H2)

**Resposta fraca seria**:
- "É um teste de integração porque usa `@SpringBootTest`"
- Não distinguir entre "integração com banco em-memória" vs. "integração com PostgreSQL real"
- Não reconhecer que é um teste de **políticas de segurança** (não de integridade de dados)
- Dizer que ambos os testes poderiam fazer a mesma coisa

---

## Pergunta 4: Divergências na Matriz de Autorização

**Arquivo-fonte**: [`docs/seguranca/matriz-autorizacao.md`](</docs/seguranca/matriz-autorizacao.md>) — seção **[DIVERGÊNCIA]**

**Pergunta**:
> O documento marca três operações com `[DIVERGÊNCIA]`. Cite uma delas e explique: qual é a discrepância entre o que a especificação diz e o que o código faz? Qual é o risco de negócio? Como essa divergência foi testada e qual deveria ser a ação: aceitar, corrigir ou documentar?

**Onde buscar a resposta**:
- "RECURSOS Desprotegidos [DIVERGÊNCIA]" — linha de `POST /recursos`
- "Histórico de auditorias [DIVERGÊNCIA]" — linha de `GET /reservas/{id}/historico`
- "Marcar não comparecimento [DIVERGÊNCIA]" — linha de `POST /reservas/{id}/nao-compareceu`

**Resposta fraca seria**:
- "Tem umas inconsistências no documento"
- Não citar qual operação específica
- Não identificar o risco (ex: qualquer usuário anônimo pode criar recursos?)
- Não mencionar teste ou cobertura dessa divergência
- Não propor resolução (aceitar, corrigir, documentar)

---

## Pergunta 5: Aprovação Requerida — Teste de Conformidade

**Arquivo-fonte**: [`src/test/java/br/edu/organizacaorecursos/security/MatrizAutorizacaoTest.java`](</src/test/java/br/edu/organizacaorecursos/security/MatrizAutorizacaoTest.java>) — método `testarCriarReserva`

**Pergunta**:
> No teste, ao criar uma reserva como `Responsável` ou `Administrador`, o status esperado é `403 Forbidden`. Qual é a regra de negócio por trás dessa decisão? O que mudaria se essa validação fosse removida do código? Como o teste provaria essa regressão?

**Onde buscar a resposta**:
- Matriz linha "Criar reserva"
- Implementação do controller `POST /reservas`
- Lógica que restringe criação apenas a `SOLICITANTE`

**Resposta fraca seria**:
- "Porque o teste espera 403"
- Não conectar a lógica técnica (HTTP 403) à regra de negócio (apenas solicitantes criam)
- Não imaginar o cenário se fosse removido (qualquer usuário criaria reservas para si mesmo)
- Não descrever o teste que detectaria a regressão

---

## Pergunta 6: Posse (Ownership) e Regra de Cancelamento

**Arquivo-fonte**: [`src/test/java/br/edu/organizacaorecursos/security/MatrizAutorizacaoTest.java`](</src/test/java/br/edu/organizacaorecursos/security/MatrizAutorizacaoTest.java>) — método `testarOwnershipReservaAlteracao`

**Pergunta**:
> Explique a regra de posse testada em `testarOwnershipReservaAlteracao`. Um `Solicitante` pode alterar sua própria reserva mas retorna `403` se tentar alterar de outro. Qual é a evidência técnica dessa validação no código (não no teste)? Como ela evita que um usuário roube a reserva de outro?

**Onde buscar a resposta**:
- Teste: linhas que distinguem `proprio` vs `outro`
- Controller: validação `usuarioId` do JWT contra `usuarioId` da reserva
- Exceção `403 Forbidden`

**Resposta fraca seria**:
- "O teste verifica se alguém consegue alterar reserva de outro"
- Não descrever o mecanismo técnico (JWT + comparação de IDs)
- Não mencionar a entidade `Usuario` ou o campo que conecta `Reserva.usuario`
- Não explicar por que `Responsável` retorna `403` (ele não é proprietário, mesmo que seja responsável do recurso)

---

## Pergunta 7: Constraint de Exclusão — Semântica da Range

**Arquivo-fonte**: [`src/main/resources/db/migration/V1__reservas_sem_sobreposicao_ativa.sql`](</src/main/resources/db/migration/V1__reservas_sem_sobreposicao_ativa.sql>)

**Pergunta**:
> A constraint usa `tsrange(data_inicio, data_fim, '[)')`. O que significa `'[)'` na notação PostgreSQL? Se uma reserva vai das 10:00 às 11:00 e outra das 11:00 às 12:00, elas se sobrepõem? Desenhe uma timeline ou descreva por que uma começa exatamente quando a outra termina.

**Onde buscar a resposta**:
- PostgreSQL `tsrange` documentation
- Operador `&&` (overlap)
- Notação de intervalo `[inicio, fim)`

**Resposta fraca seria**:
- "É um range de tempo"
- Não explicar a notação `[)`
- Não resolver o exemplo das 11:00 (resposta correta: NÃO se sobrepõem, porque é `[)` — direita aberta)
- Não desenhar ou ilustrar

---

## Pergunta 8: Estratégia de Backup Pré-Migração

**Arquivo-fonte**: Descrição do plano de execução FIX-EPIC-02 (captura da instrução)

**Pergunta**:
> Antes de executar `flyway:migrate` no banco de produção, há um passo de backup: `pg_dump ... > backup.sql`. Por que esse backup é crítico? O que poderia dar errado na migração? Se a migração falhar parcialmente, como esse backup permite rollback?

**Onde buscar a resposta**:
- Plano de execução do FIX-EPIC-02
- Documentação de migrations Flyway
- Estratégia de disaster recovery

**Resposta fraca seria**:
- "Para ter um backup em caso de erro"
- Não descrever cenários específicos (constraint invalida, dados não conformes)
- Não explicar a estratégia de restore (restaurar SQL, reapplicar migrations)
- Não mencionar a ordem de execução (backup → migrate → validate)

---

## Pergunta 9: Timezone S8700 — Regressão e Prova

**Arquivo-fonte**: Procure por "S8700" ou "timezone" em [`docs/`](</docs>) e [`src/test/`](</src/test>)

**Pergunta**:
> No PR #8, um dos itens é "Timezone S8700 Regression Test". O que é o defeito S8700? Qual foi a causa raiz (timezone handling)? Qual teste garante que essa regressão não volte? Como esse teste teria falhado antes da correção?

**Onde buscar a resposta**:
- `docs/defeitos/` — procure por "S8700" ou "timezone"
- Commit do PR #8
- Teste de regressão correspondente
- Documentação de ADR ou análise de raiz

**Resposta fraca seria**:
- "É um bug de timezone"
- Não descrever o cenário (ex: user em fuso X, servidor em Y, resultado incorreto)
- Não citar o teste específico
- Não explicar por que a correção resolve e não é um workaround

---

## Pergunta 10: RTM — Rastreabilidade de Requisitos Críticos

**Arquivo-fonte**: [`docs/rtm.md`](</docs/rtm.md>)

**Pergunta**:
> Consulte a `docs/rtm.md`. Qual é a relação entre requisitos críticos (ex: "Impedir dupla reserva") e testes? Cite um requisito crítico e o(s) teste(s) que o cobrem. Se um desses testes falhasse, qual seria o impacto no sistema?

**Onde buscar a resposta**:
- `docs/rtm.md` — matriz Requisito → Teste
- Identificadores de requisitos (ex: FR-001, NFR-002)
- Severidade (Crítico, Alto, Médio, Baixo)

**Resposta fraca seria**:
- "A RTM mapeia requisitos e testes"
- Não citar um requisito específico com seu ID
- Não nomear o teste correspondente
- Não descrever o impacto de falha
- Dizer que não consegue achar a info (procure melhor!)

---

## Pergunta 11: Performance — P95 e o Que Permite Afirmar

**Arquivo-fonte**: [`docs/performance/resultado-01.md`](</docs/performance>) (se existir) ou relatório de qualidade

**Pergunta**:
> Qual é o P95 de latência para a operação crítica (ex: criar reserva, listar reservas)? O que um P95 = 250ms permite afirmar? O que **não** permite afirmar (ex: peak loads, pior caso, SLA de 99.9%)? Se o P95 subir para 500ms, qual seria sua recomendação?

**Onde buscar a resposta**:
- `docs/performance/resultado-01.md` ou relatório de qualidade
- Metodologia de teste de performance
- Definição estatística de P95 (95º percentil)

**Resposta fraca seria**:
- "P95 é a média de latência"
- Não descrever 95º percentil
- Não distinguir P95 de máximo, mínimo ou média
- Não mencionar o que fazer se degradar

---

## Pergunta 12: Cobertura de Testes — Meta 80% Linhas

**Arquivo-fonte**: [`target/site/jacoco/index.html`](</target/site/jacoco/index.html>) (gerar com `mvn verify`)

**Pergunta**:
> Execute `mvn verify` e abra o relatório de cobertura Jacoco. Qual é a cobertura atual de linhas do projeto? Qual é a meta? Qual módulo tem a cobertura mais baixa? Por que algumas linhas **não deveriam** ser cobertas (identificar padrões: ex: exceções improváveis, getters/setters, código legado)?

**Onde buscar a resposta**:
- `mvn verify` → `target/site/jacoco/index.html`
- Relatório de cobertura Jacoco (por classe, por pacote)
- `docs/relatorio-qualidade.md` — seção "Estratégia de Teste"

**Resposta fraca seria**:
- "Cobertura está em 80%"
- Não citar o módulo com menor cobertura
- Não reconhecer código que é aceitável não cobrir
- Não descrever plano para melhorar cobertura (se abaixo de 80%)

---

## Pergunta 13: Bifurcação de Testes — Por Que Exclusion Separado?

**Arquivo-fonte**: 
- [`src/test/java/br/edu/organizacaorecursos/ReservaExclusionPostgresIntegrationTest.java`](</src/test/java/br/edu/organizacaorecursos/ReservaExclusionPostgresIntegrationTest.java>)
- [`src/test/java/br/edu/organizacaorecursos/ReservaConcorrenciaPostgresIntegrationTest.java`](</src/test/java/br/edu/organizacaorecursos/ReservaConcorrenciaPostgresIntegrationTest.java>)

**Pergunta**:
> Existem duas classes de teste separadas: uma para "Exclusion" (constraint EXCLUDE em si) e outra para "Concorrência" (múltiplas threads). Por que não juntar em uma? Qual é o escopo específico de cada uma? Qual você executaria primeiro durante desenvolvimento e por quê?

**Onde buscar a resposta**:
- Nomes das classes: `ReservaExclusionPostgresIntegrationTest` vs. `ReservaConcorrenciaPostgresIntegrationTest`
- Anotação `@RepeatedTest` no teste de concorrência
- Estrutura de cada teste (setup, execução, assertions)

**Resposta fraca seria**:
- "Uma testa exclusion, outra testa concorrência"
- Não descrever o objetivo específico de cada
- Não explicar a vantagem de separar (isolamento, legibilidade, debug)
- Não mencionar a ordem de execução lógica

---

## Pergunta 14: Defeitos Críticos Resolvidos — Evidência

**Arquivo-fonte**: [`docs/defeitos/`](</docs/defeitos>) — arquivos de defeitos

**Pergunta**:
> Procure no diretório `docs/defeitos/` por defeitos marcados como "CRÍTICO" ou "Severidade 1". Cite um. Qual foi a causa raiz? Qual teste foi adicionado/modificado para provar que foi resolvido? Se esse teste fosse removido, qual seria o risco?

**Onde buscar a resposta**:
- `docs/defeitos/*.md` — procure por "Crítico" ou "Sev.*1"
- Descrição, causa raiz, teste de regressão
- Commit que fechou o defeito (referência no PR #8)

**Resposta fraca seria**:
- "Tinha um defeito"
- Não citar ID ou nome específico
- Não descrever a causa raiz ou cenário de reprodução
- Não nomear o teste de regressão
- Dizer que não encontrou defeitos críticos (procure melhor!)

---

## Pergunta 15: Arquitetura de Segurança — Hardening do PR #8

**Arquivo-fonte**: 
- [`docs/seguranca/matriz-autorizacao.md`](</docs/seguranca/matriz-autorizacao.md>)
- `docs/stories/` — procure por epic do PR #8
- Commits do PR #8

**Pergunta**:
> O PR #8 menciona "Authorization Hardening". O que foi endurecido e por quê? Qual vulnerabilidade específica isso evita (ex: IDOR, privilege escalation, CSRF)? Como você testaria que a vulnerabilidade foi realmente fechada (teste de segurança específico)?

**Onde buscar a resposta**:
- Descrição do PR #8
- Documentação de segurança (`docs/seguranca/`)
- Testes de autorização em `MatrizAutorizacaoTest`
- Changelog ou ADR mencionando hardening

**Resposta fraca seria**:
- "Ficou mais seguro"
- Não descrever que tipo de ataque foi evitado
- Não citar o código específico (ex: validação de `usuarioId` na JWT)
- Não mencionar teste de segurança
- Dizer que é apenas "boas práticas" sem detalhe

---

## Instruções para Condução

1. **Selecione 3-5 perguntas** por integrante (distribua para cobrir todo o projeto)
2. **Designar respondedor**: Sempre alguém que **NÃO escreveu** o código em questão
3. **Tempo**: 5-10 minutos por pergunta
4. **Critério**: 
   - Resposta Fraca = 0-4 pontos
   - Resposta Superficial = 5-6 pontos
   - Resposta Esperada = 7-8 pontos
   - Resposta Excepcional (com detalhes extras, conexões cruzadas) = 9-10 pontos
5. **Sonda**: Se resposta fraca, pergunte: "Qual é o arquivo?" ou "Mostre o código"

---

**Fim do documento de perguntas — Não há respostas.**
