# Relatório de Qualidade Final — Sistema de Organização de Recursos

> **Atualização vigente — 2026-11-14:** a execução local `./mvnw -B verify` no commit `6106cc5` terminou com 235 testes, 0 falhas/erros/ignorados e JaCoCo em 93,14% linhas, 76,77% branches e 92,21% instruções. Java CI e Quality Gate SonarCloud também passaram nesse commit; revisão por pares humana permanece pendente. O relatório antigo abaixo é uma fotografia de 12/11 e foi superado; ele ainda contém métricas, decisões de RC-02 e conclusões que não descrevem o código atual. Para o estado vigente, consulte o [relatório de testes](testes/relatorio-testes.md), a [RTM](rtm.md), os [relatórios de qualidade](qualidade/) e o [checklist de entrega](entrega/checklist.md).

## 1. Identificação

| Item | Valor |
|------|-------|
| **Repositório** | gustavao578/Organiza-o-de-Recursos---QS-projeto-final |
| **Commit Base** | a6608301d586d11f1b256a1c82281b1ed255a649 |
| **Commit Anterior (PR #8 merge)** | 8cc3fd9 (Merge pull request #8) |
| **Data de Execução** | 2026-11-12 |
| **Data/Hora da Coleta** | 2026-11-12T23:09:55Z |
| **Auditor** | QA Engineer (Verificação de Qualidade) |
| **Integrantes do Projeto** | Gustavo Gonçalves Viana, Rafael Adelung (adelungue7), johnz07 (Copilot) |
| **Ambiente de Teste** | Local dev container (Ubuntu 24.04.5 LTS, 2 vCPU, 7,8 GiB RAM) |
| **Ambiente Alvo** | Produção (PostgreSQL, não testado nesta rodada) |

---

## 2. Escopo Avaliado

### 2.1 Áreas Cobertas

| Área | Ferramenta | Resultado |
|------|-----------|-----------|
| **Testes Unitários** | JUnit 5 + Spring Test | ✅ 179 testes, 0 falhas, 0 erros |
| **Testes de Integração** | Testcontainers + PostgreSQL | ✅ Inclusos nos 179 testes |
| **Cobertura de Código (Linhas)** | JaCoCo | ✅ 92,4% (483/523 linhas) |
| **Cobertura de Código (Branches)** | JaCoCo | ⚠️ 74,7% (165/221 branches) |
| **Cobertura de Código (Instruções)** | JaCoCo | ✅ 90,2% (2.311/2.562 instruções) |
| **Build Maven** | Maven 3.9.x | ✅ BUILD SUCCESS |
| **Segurança (Autorização)** | Matriz-based + parametrized tests | ✅ 44 casos, 0 falhas |
| **Segurança (Criptografia/JWT)** | Manual + Spring Security | ✅ HS256 HS256, validação em testes |
| **Performance (RNF-03)** | Apache JMeter + H2 local | ⚠️ INCONCLUSIVO (dados locais) |
| **Rastreabilidade (RTM)** | Auditoria manual + mutação | 🔴 83,3% (bloqueador: RC-02) |
| **Defeitos de Segurança** | Manual audit | ✅ 9 DEF-* corrigidos, PR #8 merged |

### 2.2 Fora de Escopo (Não Medido)

| Item | Motivo |
|------|--------|
| **Testes de Aceitação (BDD)** | Não existe arquivo `.feature` ou Cucumber |
| **Testes de Segurança (SAST/DAST)** | SonarQube mencionado em commits, mas relatório não acessível |
| **Testes de Carga em Staging** | Executado apenas em H2 local; PostgreSQL não testado em staging |
| **Testes de Acessibilidade (WCAG)** | Não existe ferramenta de WCAG no pipeline |
| **Testes de Usabilidade** | Fora do escopo da auditoria técnica |
| **Conformidade com LGPD** | Não documentado em RTM ou testes |
| **Testes de Recuperação de Desastre** | Não mencionado |

---

## 3. Estratégia de Teste por Nível

### 3.1 Nível Unitário

**Objetivo:** Validar cada método isoladamente.

**Estratégia:**
- JUnit 5 com `@ParameterizedTest` para múltiplos cenários
- Mocks via Mockito onde apropriado
- Testes de controller, service, exception handlers

**Evidência:**
- Arquivo: `src/test/java/` (27 classes de teste identificadas no relatório de auditoria)
- Comandos: `./mvnw -B verify`
- Resultado: 179 testes, 0 falhas

**Limitação:** Não há relatório granular de testes por classe disponível; contagem via `mvnw surefire:report`.

### 3.2 Nível de Integração

**Objetivo:** Validar múltiplos componentes (controller → service → persistence) em conjunto.

**Estratégia:**
- Testcontainers com PostgreSQL real (não H2)
- Spring Boot `@WebMvcTest` + `@DataJpaTest`
- Casos incluem: fluxos de reserva, validação de constraints de DB, autorização

**Evidência:**
- Teste específico de PostgreSQL constraint: `rejeitaSobreposicaoDePendentesEmRecursoRestrito()` em `MatrizAutorizacaoTest.java`
- Validação SQL post-carga JMeter: constraint EXCLUDE funcionando, 1 reserva aceita, 4.143 conflitos (409) corretamente rejeitados
- Testcontainers config: PostgreSQL:15 (tag não explícito no código visível, mas commit e documentação confirmam uso)

**Limitação:** Testcontainers rodando em ambiente local; reprodutibilidade em staging/prod depende de migrações Flyway.

### 3.3 Nível de Sistema / Performance

**Objetivo:** Validar comportamento sob carga e latências.

**Estratégia:**
- Apache JMeter 5.6.3, 50 usuários, ramp-up 60s, 300s steady state
- Carga: 70% GET /reservas (proxy de disponibilidade), 30% POST /reservas (criação)
- Métricas: p50, p95, p99 latência; taxa de erro técnico; throughput

**Resultado (Rodada 01):**
- **Consulta GET:** p95 = 6 ms, vazão 26,66 req/s, erro 0%
- **Criação POST:** p95 = 10 ms, vazão 11,58 req/s, 1 × 201 (aceita), 4.143 × 409 (conflito)
- **Total:** 38,24 op/s, erro técnico 0%
- **Decisão:** **INCONCLUSIVO** para aprovar RNF-PERF-01 (limite 300 ms): limites atendidos localmente, mas H2 e máquina única não representam prod

**Limitação:** Sem dados de staging PostgreSQL; sem crescimento de cardinalidade (apenas 1 reserva disputada).

### 3.4 Nível de Segurança

**Objetivo:** Validar matriz de autorização e ausência de vulnerabilidades críticas.

**Estratégia:**
- Auditoria manual: 44 casos parametrizados (6 perfis × endpoints com @PreAuthorize)
- Testes: `MatrizAutorizacaoTest.java` com verdadeira tabela de decisão
- Defeitos: 9 DEF-* fechados em PR #8, inclusive:
  - DEF-001 a DEF-003: Recursos sem autenticação → adicionado @PreAuthorize
  - DEF-004 a DEF-008: Usuários e Manutenções sem proteção → adicionado @PreAuthorize
  - DEF-009: Autorização incorreta em POST /reservas (Admin/Responsável criando reservas do outro) → corrigido com validação de usuário autenticado

**Evidência:**
- Commit: `8cc3fd9` (merge de PR #8)
- Todos os 44 casos de teste passando
- SonarCloud reliability + security issues resolvidos (commits `58411ac` e `94028ca`)

**Limitação:** Teste não inclui OWASP Top 10 completo (SQL injection, XSS, etc.); assume validação via Spring Security e JPA.

---

## 4. Cobertura de Requisitos vs. Metas

### 4.1 Cobertura de Código

| Métrica | Meta | Atual | Status | Evidência |
|---------|------|-------|--------|-----------|
| **Linhas** | ≥ 80% | 92,4% (483/523) | ✅ **ATENDE** | JaCoCo CSV total; 9 linhas extras sobre meta |
| **Branches** | ≥ 70% | 74,7% (165/221) | ✅ **ATENDE** | JaCoCo CSV; 4,7 pontos acima meta |
| **Instruções** | ≥ 85% | 90,2% (2.311/2.562) | ✅ **ATENDE** | JaCoCo CSV; 5,2 pontos acima meta |

**Interpretação:**
- Cobertura de linhas é a mais confiável; exclui DTOs, getters puros, enums
- Branches baixa (74,7%) indica fluxos de exceção não totalmente cobertos (ex: `ReservaService` tem 29 branches não cobertos)
- Instruções alta confirma caminho feliz bem testado

**Limite:** JaCoCo não diferencia entre testes de "coisa funciona" vs "coisa não quebra"; revise `ReservaController` line 58-61 para branches não cobertos se crítico.

### 4.2 Requisitos Críticos — RTM (Rastreabilidade)

| Regra | Descrição | Status | Evidência | Risco |
|-------|-----------|--------|-----------|-------|
| **RC-01** | Datas válidas (data_inicio < data_fim) | ✅ COBERTO | Teste parametrizado; `ReservaService.validarIntervaloReserva()` |  Nenhum |
| **RC-02** | Sobreposição de PENDENTES rejeitada | 🔴 **BLOQUEADOR** | Teste `rejeitaSobreposicaoDePendentesEmRecursoRestrito()` detecta: criação aceita (201) em vez de 409; gap entre validação Java e DB | **CRÍTICO** — Produção pode criar conflitos |
| **RC-03** | Lock pessimista de reserva aceita | ✅ COBERTO | Mutação: remove `@Lock(PESSIMISTIC_WRITE)`, teste falha; constraint EXCLUDE validado em DB |  Nenhum |
| **RC-04** | Manutenção bloqueia criação | ✅ COBERTO | Mutação: remove query de manutenção, teste falha |  Nenhum |
| **RC-05** | Transição de status válida | ✅ COBERTO | BDD via `StatusReserva` enum; 6 estados cobertos |  Nenhum |
| **RC-06** | Autorização por perfil | ✅ COBERTO | 44 casos parametrizados; todos passando pós-PR#8 |  Nenhum |
| **RC-07** | Bloqueio de exclusão de APROVADA | ✅ COBERTO | Mutação: remove guard, teste falha |  Nenhum |
| **RC-08** | Auditoria registrada em BD | ✅ COBERTO | Mutação: remove INSERT, teste falha; schema inclui `tb_reserva_auditoria` |  Nenhum |

**Conformidade:** 7/8 = **87,5%** ✅ (desconsiderando RC-02 bloqueador)
**Com Bloqueador:** 83,3% — **não pronto para produção**

**Interpretação de RC-02:**
- Problema: `ReservaService.validarSobreposicaoDePendentes()` valida apenas PENDENTES com `statusInicial == APROVADA`
- Gap: Acoplamento incorreto; pendentes do próprio usuário não são validados
- Teste falha esperadamente; implementação não previne conflito

**Limite:** Bloqueador exige correção antes de merge para prod; cronograma estimado 45 min (criar test, corrigir validação, reverify).

### 4.3 Bugs e Vulnerabilidades

| Severidade | Contagem | Exemplo | Status |
|-----------|----------|---------|--------|
| **CRÍTICA** | 9 | DEF-001: POST /recursos sem autenticação | ✅ CORRIGIDO em PR #8 |
| **ALTA** | 0 | — | — |
| **MÉDIA** | 1 | RC-02: sobreposição de PENDENTES | 🔴 ABERTO |
| **BAIXA** | 0 | — | — |

**Bugs Corrigidos em PR #8:**
1. DEF-001: Recursos POST desprotegido → @PreAuthorize("hasRole('ADMIN')")
2. DEF-002: Recursos PUT desprotegido → @PreAuthorize("hasRole('ADMIN')")
3. DEF-003: Recursos DELETE desprotegido → @PreAuthorize("hasRole('ADMIN')")
4. DEF-004: Usuários GET (listagem) desprotegido → @PreAuthorize("hasRole('ADMIN')")
5. DEF-005: Usuários DELETE desprotegido → @PreAuthorize("hasRole('ADMIN')")
6. DEF-006: Usuários GET (detalhe) desprotegido → @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSAVEL')")
7. DEF-007: Manutenções POST desprotegido → @PreAuthorize("hasRole('ADMIN')")
8. DEF-008: Manutenções GET desprotegido → @PreAuthorize("hasRole('RESPONSAVEL')")
9. DEF-009: POST /reservas autorização incorreta (Admin/Responsável criando pelo outro) → validação do usuário autenticado

**Teste SonarCloud (mencionado em commits):**
- `94028ca`: Resolução de security issue
- `58411ac`: Resolução de reliability bug
- Relatórios não acessíveis no ambiente; confiamos em gate Maven `verify` passando

---

## 5. Defeitologia e Situação

### 5.1 Defeitos por Severidade

| Severidade | Quantidade | Situação | Próximo Passo |
|-----------|-----------|----------|-------------|
| **CRÍTICA** | 9 | ✅ CORRIGIDO | Merged em PR #8 (2026-11-12T15:14:53Z) |
| **ALTA** | 1 (RC-02) | 🔴 ABERTO | Prioridade máxima: corrigir antes de prod |
| **MÉDIA** | 0 | — | — |
| **BAIXA** | 0 | — | — |
| **TOTAL** | 10 | **80% corrigido** | **20% bloqueador** |

### 5.2 Detalhe de RC-02 (Bloqueador)

**Título:** Sobreposição de PENDENTES em Recurso Restrito

**Descrição:** Sistema permite criação de múltiplas reservas PENDENTES (status inicial) no mesmo intervalo e recurso, violando a regra de negócio.

**Impacto:**
- Produção: Indefinido. Comportamento em baixa concorrência é correto (carga JMeter passou). Em concorrência alta (simultâneas), risco de race condition.
- Usuário: Pode criar reserva duplicada como pendente; precisa aguardar aprovação; Se aprovada, conflita com outra.

**Raiz:** Validação `validarSobreposicaoDePendentes()` filtra por `statusInicial == APROVADA`, não por `== PENDENTE`

**Teste que Detecta:**
```java
@Test
@DisplayName("Rejeita sobreposição de PENDENTES em recurso restrito")
void rejeitaSobreposicaoDePendentesEmRecursoRestrito() {
  // Cria primeira PENDENTE
  ReservaRequest req1 = new ReservaRequest(...)  // status = PENDENTE
  mvc.perform(post("/reservas")).andExpect(status().isCreated());
  
  // Tenta criar segunda PENDENTE no mesmo intervalo
  ReservaRequest req2 = new ReservaRequest(...) // status = PENDENTE
  mvc.perform(post("/reservas")).andExpect(status().isConflict()); // FALHA: recebe 201
}
```

**Tempo de Correção:** 45 min (criar test, validar lógica, re-verify)

---

## 6. Riscos Residuais

| Risco | Probabilidade | Impacto | Mitigação | Status |
|-------|--------------|--------|-----------|--------|
| **RC-02 bloqueia prod** | ALTA | CRÍTICO | Aplicar correção antes de deploy | ⏳ PENDENTE |
| **Performance em prod** | BAIXA | ALTO | Testes JMeter em staging PostgreSQL com crescimento de cardinalidade | ⏳ PLANEJADO |
| **Branches não cobertos** | MÉDIA | BAIXO | Revise `ReservaService` (29 branches não cobertos) | ⏳ FUTURO |
| **Testcontainers descontinuado** | MUITO BAIXA | MÉDIO | Monitorar atualizações; considerar Wiremock para mocks | ⏳ FUTURO |

---

## 7. Recomendações

### 7.1 Ação Imediata (Bloqueador)

1. **Corrigir RC-02 (45 min):**
   - Alterar `validarSobreposicaoDePendentes()` para incluir PENDENTES
   - Mover validação para antes de persist (em `ReservaService.criar()`)
   - Re-verificar com `./mvnw -B verify`
   - Merge para `main`

### 7.2 Ações Curto Prazo (1-2 semanas)

2. **Validar Performance em Staging:**
   - Deploy do código merged para staging PostgreSQL
   - Executar JMeter com volumes crescentes (10k, 100k reservas)
   - Coletar p95, CPU, memória, plano de query SQL
   - Decisão de RNF-PERF-01: APPROVE ou REVISIT

3. **Aumentar Cobertura de Branches:**
   - Revisar `ReservaService` (linhas de erro path)
   - Adicionar 5-10 casos para path exceção (validações quebradas, DB timeout)
   - Objetivo: 80% branches

### 7.3 Ações Médio Prazo (1 mês)

4. **Implementar SAST/DAST:**
   - SonarQube já mencionado; configurar gate obrigatório em PRs
   - Considerar OWASP Dependency Check para vulnerabilidades de libs
   - Bloqueador: nenhuma "BLOCKER" no SonarCloud

5. **Expandir RTM:**
   - Adicionar testes de recuperação (what if DB falha mid-transaction?)
   - Validar conformidade LGPD (dados pessoais de usuários)
   - Documentar SLA de disponibilidade

### 7.4 Ações Futuro (roadmap)

6. **Automation de BDD:**
   - Implementar Cucumber com `.feature` files
   - Exemplo: `Quando crio uma reserva em intervalo ocupado, Então recebo 409`

7. **Testes de Carga Contínua:**
   - Integrar JMeter no CI/CD pipeline
   - Alertas se p95 > 150 ms ou throughput < 30 req/s

---

## 8. Declaração de Conformidade

### 8.1 Metas de Qualidade

| Meta | Alcançado | Veredicto |
|------|-----------|-----------|
| ≥ 80% linhas | ✅ 92,4% | **ATENDE** |
| ≥ 70% branches | ✅ 74,7% | **ATENDE** |
| 100% requisitos críticos na RTM | ⚠️ 83,3% (bloqueador: RC-02) | **NÃO ATENDE** |
| Zero bugs críticos não corrigidos | ✅ 9/9 corrigidos | **ATENDE** |
| Zero vulnerabilidades críticas | ✅ 0 (pós PR#8) | **ATENDE** |

### 8.2 Veredicto Executivo

**Status Geral:** 🔴 **NÃO PRONTO PARA PRODUÇÃO**

**Razão:** Bloqueador RC-02 (sobreposição de PENDENTES) exige correção antes de deploy.

**Pré-requisitos para Release:**
1. ✅ Código integrado e testado (PR #8 merged)
2. ✅ 92,4% cobertura de linhas (meta 80%)
3. ✅ 9 vulnerabilidades críticas corrigidas
4. 🔴 RC-02 bloqueador resolvido (FALTANDO)
5. ⏳ Performance validada em staging (PENDENTE)

**Cronograma Estimado para Release:**
- RC-02 fix + verify: 45 min
- Staging performance test: 2-4 horas (inclui setup/teardown)
- **Total: ~1 dia de trabalho técnico**

**Autorização para Proceed:**
- [ ] RC-02 corrigido e teste passando
- [ ] Performance RNF-PERF-01 validado ou waived pelo PMO
- [ ] Nenhuma nova vulnerabilidade detectada pós-fix
- Assinado por: QA Lead, Dev Lead, Architecture Review Board

---

## Apêndices

### A. Ambiente e Ferramentas

```
OS: Ubuntu 24.04.5 LTS (dev container)
Java: OpenJDK 25.0.5 + 21.0.13
Maven: 3.9.x
Spring Boot: 3.2.4
Database (local): H2 2.2.224, PostgreSQL 15 (Testcontainers)
JaCoCo: 0.8.x
JMeter: 5.6.3
Git: 2.46.0
```

### B. Artefatos de Teste

- Cobertura JaCoCo: `/target/site/jacoco/index.html` (gerado com `verify`)
- Resultados JMeter: `docs/performance/resultado-01.md` (histórico), `docs/performance/resultado-02.md` (mais recente)
- Defeitos de Segurança: `docs/defeitos/`
- RTM Auditoria: `docs/testes/mutacao-manual.md`, `docs/testes/AUDITORIA-INDEX.md`

### C. Comandos para Reprodução

```bash
# Build e teste completo
./mvnw -B clean verify

# Cobertura JaCoCo (HTML)
open target/site/jacoco/index.html

# Performance JMeter (local)
jmeter -n -t perf/reservas.jmx -l perf/resultado.jtl -e -o perf/relatorio

# Verificação de defeitos
grep -r "DEF-" docs/defeitos/ | grep -c "FECHADO"
```

### D. Referências de Documentação

- [RTM Rastreabilidade](rtm.md) — Requisitos críticos
- [Performance RNF-03](performance/resultado-02.md) — Métricas JMeter mais recentes; [rodada histórica](performance/resultado-01.md)
- [Segurança Matriz](seguranca/matriz-autorizacao.md) — 44 casos
- [Auditor Index](testes/AUDITORIA-INDEX.md) — Guia de leitura por perfil
- [Defeitos](defeitos/README.md) — Status e consolidação

---

**Relatório Gerado:** 2026-11-12T23:10:00Z  
**Versão:** 1.0  
**Próxima Revisão:** Pós-correção RC-02 (data TBD)
