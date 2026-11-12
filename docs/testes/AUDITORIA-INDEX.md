# 📋 Índice Consolidado de Auditoria de Rastreabilidade

**Data:** 2026-11-12  
**Status:** 🔴 **BLOQUEADOR** — RC-02 lacuna confirmada (DEF-002)  
**Conformidade:** 83,3% (5/6 regras críticas cobertas)  
**Responsável:** Auditor de Rastreabilidade / Analista de Qualidade

---

## 🎯 Comece Aqui

Escolha seu perfil para um roteiro de leitura personalizado:

### 👔 Executivos / PMO
**Perguntas:** Qual é o status de cobertura? Há bloqueadores? Quanto tempo para fix?

**Leitura recomendada:**
1. [Este arquivo - Seção "Dashboard Executivo"](#-dashboard-executivo)
2. [AUDITORIA-FINAL.md](AUDITORIA-FINAL.md) - Métricas e recomendações (7 min)
3. [DEF-002](../defeitos/DEF-002-sobreposicao-pendentes.md) - Impacto e solução (5 min)

**Resposta rápida:** 83,3% cobertura, 1 lacuna crítica (RC-02), bloqueador de release, fix estimado em 45 minutos (30 min código + 15 min validação).

---

### 🧪 QA / Testers
**Perguntas:** Como foi validada cada regra? Como reproduzir as descobertas? Qual teste precisa ser adicionado?

**Leitura recomendada:**
1. [Este arquivo - Seção "Matriz de Cobertura"](#-matriz-de-cobertura-das-regras-críticas)
2. [mutacao-manual.md](mutacao-manual.md) ⭐ **PRINCIPAL** (30 min) - Análise completa de cada regra
3. Teste novo: [`ReservaApiIntegrationTest.rejeitaSobreposicaoDePendentesEmRecursoRestrito()`](../../src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java#L661) (5 min)
4. [DEF-002](../defeitos/DEF-002-sobreposicao-pendentes.md) - Casos de teste para fix (5 min)

**Resposta rápida:** Análise de mutação confirmou que 5 regras têm validação efetiva (testes falham quando regra é quebrada). RC-02 lacuna detectada por novo teste que falha com 201 vs 409 esperado.

---

### 👨‍💻 Developers
**Perguntas:** Qual é o bug? Onde está no código? Como faço para corrigir?

**Leitura recomendada:**
1. [DEF-002 - Seção "Descrição"](../defeitos/DEF-002-sobreposicao-pendentes.md#descrição-e-impacto) (5 min) - Entender o problema
2. [DEF-002 - Seção "Raiz"](../defeitos/DEF-002-sobreposicao-pendentes.md#raiz) (2 min) - Localização no código
3. [DEF-002 - Seção "Solução Proposta"](../defeitos/DEF-002-sobreposicao-pendentes.md#solução-proposta) (10 min) - Opção A (rápida) vs Opção B (defesa em profundidade)
4. [mutacao-manual.md - Seção RC-02](mutacao-manual.md#rc-02-sobreposição) (5 min) - Contexto técnico

**Resposta rápida:** Validação de sobreposição está acoplada a `statusInicial == APROVADA` em `ReservaService.java:72-76`. Mover validação **antes** de atribuir status (5 linhas, 30 min).

---

### 🏛️ Arquitetos
**Perguntas:** É uma falha de design? Há padrão subjacente? Qual é a estratégia de longo prazo?

**Leitura recomendada:**
1. [mutacao-manual.md - Seção "Conclusão"](mutacao-manual.md#conclusão) (5 min) - Análise geral de padrões
2. [mutacao-manual.md - Seção "Recomendações"](mutacao-manual.md#recomendações) (10 min) - Médio/longo prazo
3. [DEF-002 - Ambas as soluções](../defeitos/DEF-002-sobreposicao-pendentes.md#solução-proposta) (10 min) - Opção A vs B
4. [prd/rtm.md - Seção 5](../../prd/rtm.md#5-resumo-da-rastreabilidade-crítica) (10 min) - Status consolidado da RTM

**Resposta rápida:** Lacuna em RC-02 é resultado de acoplamento entre status de aprovação e validação de regra de negócio. Opção A: fix tático (reordenar código). Opção B: defesa arquitetural (constraint de BD). Roadmap: revisar padrão de validação em outras operações.

---

## 📊 Dashboard Executivo

### Status Consolidado

| Métrica | Valor | Status |
|---|---|---|
| **Conformidade** | 83,3% (5/6 regras) | 🟡 ACEITÁVEL |
| **Lacunas críticas** | 1 (RC-02) | 🔴 BLOQUEADOR |
| **Testes novos** | 1 (mutação) | ✅ |
| **Defeitos criados** | 1 (DEF-002) | 🔴 CRÍTICA |
| **Esforço fix** | 45 min (30 código + 15 validação) | ✅ VIÁVEL |
| **Documentação** | 29 KB em 5 arquivos | ✅ COMPLETA |
| **Status release** | BLOQUEADO até fix RC-02 | 🔴 DECISÃO |

### Regras Críticas

| ID | Regra | Status | Teste |
|---|---|---|---|
| RC-01 | Datas válidas (fim > início) | ✅ COBERTO | `rejeitaDataFimAnteriorAoInicio()` |
| RC-02 | Sobreposição | 🔴 **LACUNA** | `rejeitaSobreposicaoDePendentesEmRecursoRestrito()` ❌ |
| RC-03 | Concorrência | ✅ COBERTO | `aceitaApenasUmaReservaEmSolicitacoesConflitantes()` |
| RC-04 | Manutenção | ✅ COBERTO | `rejeitaReservaQuandoRecursoEstaEmManutencao()` |
| RC-05 | Fluxo de estados | ✅ COBERTO | BDD-EST-01/02/03 |
| RC-06 | Autorização | ✅ COBERTO | `patchStatusAceitaResponsavel()` |
| RC-07 | Não apagar iniciada | ✅ COBERTO | `impedeExcluirReservaQueJaFoiIniciada()` |
| RC-08 | Auditoria | ✅ COBERTO | `persisteHistoricoComMudancaDeEstado()` |

### Recomendação

🔴 **NÃO MERGEAR** sem corrigir DEF-002. Esforço mínimo: 45 minutos (Opção A simples, 5 linhas).

---

## 🔍 Matriz de Cobertura das Regras Críticas

### Legenda
- ✅ **COBERTO** — Teste falha quando regra é quebrada (mutação confirmou efetividade)
- 🔴 **LACUNA** — Teste falta ou falha mesmo com regra quebrada (cobertura inefetiva)
- ⚠️ **PARCIAL** — Teste cobre escopo limitado (histórico, descontinuado)

### Matriz Detalhada

#### RC-01: Datas Válidas ✅
- **Teste:** `ReservaRequestDTOTest.rejeitaDataFimAnteriorAoInicio()`
- **Validação:** Prazo mínimo (fim > início) com relógio fixo
- **Mutação:** ✅ Teste falha quando validação é removida
- **Status:** COBERTO

#### RC-02: Sobreposição 🔴
- **Teste existente:** `rejeitaPeriodosSobrepostosParaMesmoRecurso()` (APROVADA)
- **Teste novo:** `rejeitaSobreposicaoDePendentesEmRecursoRestrito()` (PENDENTE)
- **Problema:** Validação acoplada a `statusInicial == APROVADA`
- **Efeito:** PENDENTES sobrepostas coexistem
- **Mutação:** ❌ Teste novo falha (201 vs 409)
- **Raiz:** `ReservaService.java:72-76`
- **Status:** LACUNA CRÍTICA → DEF-002
- **Fix estimado:** 30 min (Opção A)

#### RC-03: Concorrência ✅
- **Teste:** `aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas()`
- **Infraestrutura:** Lock pessimista + Testcontainers PostgreSQL
- **Mutação:** ✅ Teste falha quando lock é removido
- **Status:** COBERTO

#### RC-04: Manutenção ✅
- **Teste:** `rejeitaReservaQuandoRecursoEstaEmManutencao()`
- **Validação:** Query `isRecursoEmManutencao()` bloqueia criação
- **Mutação:** ✅ Teste falha quando query é comentada
- **Status:** COBERTO

#### RC-05: Fluxo de Estados ✅
- **Testes BDD:** `aprovacao-bloqueio.feature` (BDD-EST-01/02/03)
- **Estados:** PENDENTE → APROVADA → EM_USO → CONCLUÍDA
- **Validação:** Transições válidas e rejeição de transições inválidas
- **Mutação:** ✅ Testes BDD falham quando transições são removidas
- **Status:** COBERTO

#### RC-06: Autorização ✅
- **Teste:** `patchStatusAceitaResponsavelEPreservaConflitos()`, `rejeitaAprovacaoPorUsuarioQueNaoEResponsavel()`
- **Validação:** Verificação de `Perfil.RESPONSAVEL` ou ADMIN
- **Mutação:** ✅ Teste falha quando verificação é removida
- **Status:** COBERTO

#### RC-07: Não Apagar Iniciada ✅
- **Teste:** `impedeExcluirReservaQueJaFoiIniciada()`
- **Validação:** Bloqueio por status EM_USO, CONCLUÍDA, NÃO_COMPARECEU
- **Mutação:** ✅ Teste falha quando bloqueio é removido
- **Status:** COBERTO

#### RC-08: Auditoria ✅
- **Teste:** `persisteHistoricoComMudancaDeEstadoNaMesmaTransacao()`
- **Validação:** Insert em `ReservaAuditoria` na mesma transação
- **Mutação:** ✅ Teste falha quando insert é removido
- **Atomicidade:** ✅ Testcontainers PostgreSQL valida rollback
- **Status:** COBERTO

---

## 📁 Estrutura de Documentação

```
docs/testes/
├── AUDITORIA-INDEX.md (este arquivo)
│   └── Índice consolidado com roteiros por perfil
├── AUDITORIA-README.md
│   └── Índice original com matriz resumida
├── AUDITORIA-FINAL.md
│   └── Sumário executivo com métricas (7,4 KB)
└── mutacao-manual.md ⭐ PRINCIPAL
    └── Análise completa de cada regra crítica (14 KB)
    ├── Objetivo e metodologia
    ├── Matriz de mutação (RC-02 a RC-08)
    ├── Análise detalhada: RC-02 ⚠️ lacuna confirmada
    ├── Análise detalhada: RC-03 a RC-08 ✅ cobertas
    ├── Sumário de conformidade
    ├── Recomendações e cronograma
    └── Conclusão

docs/defeitos/
├── DEF-002-sobreposicao-pendentes.md ⭐ AÇÃO REQUERIDA
│   └── Defeito crítico com raiz e soluções (7,9 KB)
│   ├── Descrição e impacto
│   ├── Raiz: ReservaService.java:72-76
│   ├── Teste falhando: rejeitaSobreposicaoDePendentesEmRecursoRestrito()
│   ├── Solução Opção A: Mover validação (5 linhas, 30 min)
│   └── Solução Opção B: Constraint de BD

prd/rtm.md
└── Matriz de Rastreabilidade de Requisitos (atualizada 2026-11-12)
    ├── RC-01: COBERTO
    ├── RC-02: BLOQUEADO (DEF-002)
    ├── RC-03: COBERTO (promovida de PARCIAL via mutação)
    ├── RC-04: COBERTO (promovida de PARCIAL via mutação)
    ├── RC-05: COBERTO
    ├── RC-06: COBERTO
    ├── RC-07: COBERTO
    └── RC-08: COBERTO
```

---

## 🚀 Plano de Ação Imediato

### Sprint Atual (Crítico) — BLOQUEIA DEPLOY

| Tarefa | Esforço | Status | Responsável |
|---|---|---|---|
| 1️⃣ Implementar Opção A em ReservaService.java:72-76 | 30 min | ⏳ TODO | Dev Team |
| 2️⃣ Re-executar `rejeitaSobreposicaoDePendentesEmRecursoRestrito()` | 5 min | ⏳ TODO | Dev/QA |
| 3️⃣ Re-validar com mutação (quebrar e verificar falha) | 10 min | ⏳ TODO | QA |
| 4️⃣ Atualizar prd/rtm.md (RC-02 BLOQUEADO → COBERTO) | 5 min | ⏳ TODO | Arquiteto |
| **TOTAL** | **50 min** | | |

### Sprint +1 (Importante)

| Tarefa | Esforço | Status |
|---|---|---|
| Adicionar constraint de BD (Opção B como defesa em profundidade) | 2h | 📋 BACKLOG |
| Expandir cobertura de RC-08 (todos os caminhos de estado) | 4h | 📋 BACKLOG |
| Validar casos limites de RC-04 (bordas de manutenção) | 3h | 📋 BACKLOG |

---

## 🔗 Referências Rápidas

### Arquivos Críticos
- **Serviço problemático:** [`ReservaService.java:72-76`](../../src/main/java/br/edu/organizacaorecursos/service/ReservaService.java#L72)
- **Teste que detecta:** [`ReservaApiIntegrationTest.rejeitaSobreposicaoDePendentesEmRecursoRestrito()`](../../src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java#L661)
- **Defeito:** [`DEF-002`](../defeitos/DEF-002-sobreposicao-pendentes.md)
- **RTM oficial:** [`prd/rtm.md`](../../prd/rtm.md)

### Comandos Úteis

**Executar teste que detecta lacuna:**
```bash
./mvnw -Dtest=ReservaApiIntegrationTest#rejeitaSobreposicaoDePendentesEmRecursoRestrito test
```

**Executar matriz completa de mutação:**
```bash
./mvnw -B verify
```

**Revisar código problemático:**
```bash
git show HEAD:src/main/java/br/edu/organizacaorecursos/service/ReservaService.java | sed -n '60,85p'
```

---

## 📞 Contato & Suporte

- **Dúvidas sobre cobertura?** → Leia [mutacao-manual.md](mutacao-manual.md)
- **Dúvidas sobre implementação?** → Leia [DEF-002](../defeitos/DEF-002-sobreposicao-pendentes.md)
- **Dúvidas sobre RTM?** → Consulte [`prd/rtm.md`](../../prd/rtm.md)
- **Reproduzir descoberta?** → Execute teste novo + observe falha (veja seção "Comandos Úteis")

---

## ✅ Checklist de Conclusão

**Auditoria de Rastreabilidade com Mutação Manual:**

- [x] Analisar RTM e identificar 6 regras críticas
- [x] Criar testes de mutação para cada regra (quebrar código, verificar falha)
- [x] Executar testes com regra quebrada
- [x] Documentar análise em `mutacao-manual.md` (14 KB)
- [x] Identificar lacuna RC-02 via teste novo
- [x] Criar DEF-002 com raiz e soluções
- [x] Gerar relatório final `AUDITORIA-FINAL.md` (7,4 KB)
- [x] Consolidar em `AUDITORIA-INDEX.md` com roteiros por perfil
- [x] Atualizar RTM com novos status
- [x] Revisar 3 commits de auditoria
- [x] Marcar conformidade final: 83,3% (5/6 regras cobertas)

**Status:** ✅ COMPLETO | 🔴 BLOQUEADOR: RC-02 (DEF-002)

---

**Auditado por:** Analista de Qualidade  
**Data:** 2026-11-12  
**Versão:** 1.0  
**Próxima revisão:** Após correção de DEF-002
