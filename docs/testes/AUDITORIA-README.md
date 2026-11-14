# Índice de Auditoria de Rastreabilidade

> **Snapshot histórico de 2026-11-12.** A afirmação abaixo de que RC-02 está aberta está superada; consulte a [RTM vigente](../rtm.md) e o [relatório de testes atual](relatorio-testes.md).

**Data de Conclusão:** 2026-11-12  
**Escopo:** Auditoria de rastreabilidade com testes de mutação manual para 6 regras críticas  
**Status:** 🔴 **BLOQUEADOR** — 1 lacuna crítica (RC-02)  
**Conformidade:** 83,3% (5/6 regras cobertas)

---

## 📚 Documentação Gerada

### 1. **Relatório Executivo** (este arquivo)
- **Arquivo:** [AUDITORIA-README.md](AUDITORIA-README.md)
- **Tamanho:** ~3 KB
- **Público:** Executivos, Product Owner, QA Lead
- **Conteúdo:** Sumário, métricas, recomendações

### 2. **Análise Completa de Mutação** ⭐ PRINCIPAL
- **Arquivo:** [mutacao-manual.md](mutacao-manual.md)
- **Tamanho:** 14 KB
- **Público:** QA, Devs, Arquitetos
- **Conteúdo:**
  - Matriz de mutação (6 regras críticas)
  - Análise profunda de cada regra
  - Teste que detecta lacuna RC-02
  - Comandos para reproduzir

**Seções:**
- Objetivo e metodologia (como executar testes de mutação)
- Matriz de mutação (RC-02 a RC-08)
- Análise detalhada: RC-02 ⚠️ lacuna confirmada
- Análise detalhada: RC-03 a RC-08 ✅ cobertas
- Sumário de conformidade: 83,3%
- Lacuna RC-02: Hipóteses investigadas
- Recomendações e cronograma
- Conclusão e próximas ações

### 3. **Relatório Final Resumido**
- **Arquivo:** [AUDITORIA-FINAL.md](AUDITORIA-FINAL.md)
- **Tamanho:** 7,4 KB
- **Público:** PMO, QA Lead, Dev Lead
- **Conteúdo:**
  - Tabela de cobertura (5/6 = 83,3%)
  - Descobertas principais
  - Evidência executável
  - Status final e recomendações

**Seções:**
- Métricas de conformidade (tabela RC-02 a RC-08)
- Descobertas principais (lacuna RC-02)
- Análise de cada regra (RC-03 a RC-08)
- Documentação gerada (lista de arquivos)
- Conclusões e ações críticas
- Evidência de execução (commits e testes)

### 4. **Defeito Crítico Criado**
- **Arquivo:** [../defeitos/DEF-002-sobreposicao-pendentes.md](../defeitos/DEF-002-sobreposicao-pendentes.md)
- **Tamanho:** 7,9 KB
- **Público:** Dev Team (implementação)
- **Conteúdo:**
  - Descrição técnica do defeito
  - Raiz identificada (linha de código)
  - Cenário de falha
  - Duas soluções propostas
  - Testes de aceitação
  - Cronograma de correção

**Seções:**
- Resumo executivo
- Descrição e impacto
- Evidência (teste falhando)
- Raiz (linha 72-76 de ReservaService)
- Solução Opção A: Mover validação em código
- Solução Opção B: Constraint de BD
- Testes de aceitação
- Passos de correção

---

## 🎯 Fluxo de Leitura Recomendado

### Para Executivos / PMO
1. Este arquivo (sumário)
2. [AUDITORIA-FINAL.md](AUDITORIA-FINAL.md) (métricas e recomendações)
3. [../defeitos/DEF-002-sobreposicao-pendentes.md](../defeitos/DEF-002-sobreposicao-pendentes.md) (impacto e solução)

### Para QA / Testers
1. [AUDITORIA-FINAL.md](AUDITORIA-FINAL.md) (overview)
2. [mutacao-manual.md](mutacao-manual.md) **⭐ PRINCIPAL** (análise completa)
3. Revisar teste novo: `ReservaApiIntegrationTest.rejeitaSobreposicaoDePendentesEmRecursoRestrito()`

### Para Developers
1. [../defeitos/DEF-002-sobreposicao-pendentes.md](../defeitos/DEF-002-sobreposicao-pendentes.md) (entender o problema)
2. Seção "Solução Proposta" → Opção A (5 linhas de reordenação)
3. [mutacao-manual.md](mutacao-manual.md#rc-02-sobreposição) → Seção RC-02 (contexto)

### Para Arquitetos
1. [mutacao-manual.md](mutacao-manual.md#conclusão) (conclusão)
2. [mutacao-manual.md](mutacao-manual.md#recomendações) (recomendações de médio prazo)
3. [../defeitos/DEF-002-sobreposicao-pendentes.md](../defeitos/DEF-002-sobreposicao-pendentes.md#solução-proposta) (ambas as opções)

---

## 📊 Matriz de Conformidade Rápida

| RC | Regra | Status | Teste |
|---|---|---|---|
| **RC-02** | Sobreposição | 🔴 **LACUNA** | `rejeitaSobreposicaoDePendentesEmRecursoRestrito()` ❌ |
| **RC-03** | Concorrência | ✅ Coberto | `aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas()` ✅ |
| **RC-04** | Manutenção | ✅ Coberto | `rejeitaReservaQuandoRecursoEstaEmManutencao()` ✅ |
| **RC-06** | Autorização | ✅ Coberto | `patchStatusAceitaResponsavelEPreservaConflitos()` ✅ |
| **RC-07** | Não apagar | ✅ Coberto | `impedeExcluirReservaQueJaFoiIniciada()` ✅ |
| **RC-08** | Auditoria | ✅ Coberto | `persisteHistoricoComMudancaDeEstadoNaMesmaTransacao()` ✅ |

**Cobertura:** 5/6 = **83,3%** | **Bloqueador:** RC-02

---

## 🔴 Lacuna Crítica — Ação Requerida

### Problema (RC-02)
**Reservas PENDENTES podem se sobrepor**, violando requisito crítico.

```java
// Código problemático em ReservaService.java:72-76
boolean temConflito = statusInicial == StatusReserva.APROVADA  // ← AQUI: pula se PENDENTE
    && reservaRepository.existeConflitoDeHorario(...);
```

### Teste que Detecta
```java
@Test
void rejeitaSobreposicaoDePendentesEmRecursoRestrito() throws Exception {
    UUID recursoId = criarRecursoRestrito(null);
    LocalDateTime inicio = inicioFuturo();
    
    enviarReserva(reserva(recursoId, inicio, "primeiro"))
        .andExpect(status().isCreated());  // 201 ✅
    
    enviarReserva(reserva(recursoId, inicio.plusMinutes(30), "segundo"))
        .andExpect(status().isConflict());  // Esperado: 409
                                           // Real: 201 ❌ LACUNA CONFIRMADA
}
```

### Solução Rápida (Opção A)
```java
// Mover validação ANTES de atribuir status
boolean temConflito = reservaRepository.existeConflitoDeHorario(
    recurso.getId(), dto.getDataInicio(), dto.getDataFim());  // Executa SEMPRE

if (temConflito) {
    throw new ConflitoReservaException(...);
}

// Depois, atribuir status com segurança
StatusReserva statusInicial = recurso.isRestrito()
    ? StatusReserva.PENDENTE
    : StatusReserva.APROVADA;
```

**Esforço:** ~30 minutos | **Impacto:** Desbloqueia deploy

---

## ✅ Conformidade das Regras Cobertas

Testes de mutação confirmam que **5 regras têm validação efetiva**:

- **RC-03 (Concorrência):** Lock pessimista + Testcontainers PostgreSQL ✅
- **RC-04 (Manutenção):** Query `isRecursoEmManutencao()` valida ✅
- **RC-06 (Autorização):** Verificação de `Perfil.RESPONSAVEL` ou ADMIN ✅
- **RC-07 (Não apagar):** Bloqueio por status EM_USO/CONCLUÍDA/NÃO_COMPARECEU ✅
- **RC-08 (Auditoria):** Insert em `ReservaAuditoria` na mesma transação ✅

---

## 📈 Métricas Finais

| Métrica | Valor |
|---|---|
| Conformidade | 83,3% (5/6 cobertas) |
| Lacunas críticas | 1 (RC-02) |
| Testes novos | 1 (`rejeitaSobreposicaoDePendentesEmRecursoRestrito`) |
| Documentação | 29 KB em 4 arquivos |
| Commits | 2 (auditoria + relatório) |
| Status Release | 🔴 BLOQUEADO (DEF-002) |
| Esforço fix estimado | 30 min (código) + 15 min (validação) |

---

## 🚀 Próximas Ações

### Críticas (Sprint Atual) — BLOQUEIA DEPLOY
- [ ] Implementar correção Opção A (mover validação)
- [ ] Re-executar `rejeitaSobreposicaoDePendentesEmRecursoRestrito` → deve passar
- [ ] Re-validar com mutação (quebrar validação, verificar que teste falha)
- [ ] Marcar RC-02 como COBERTO em `prd/rtm.md`

### Importantes (Sprint +1)
- [ ] Adicionar constraint de BD (Opção B)
- [ ] Expandir cobertura de RC-08 (todos os caminhos de estado)
- [ ] Validar casos limites de RC-04 (bordas de manutenção)

### Melhorias (Roadmap)
- [ ] Testes de validação de entrada (períodos nulos, inversão)
- [ ] Revisar rota legada `/aprovar` vs PATCH `/status`

---

## 📎 Referências Rápidas

### Documentação
- **RTM Oficial:** `prd/rtm.md` (matriz de rastreabilidade com 8 regras críticas)
- **Matriz de Autorização:** `docs/seguranca/matriz-autorizacao.md`
- **Testes Existentes:**
  - `src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java`
  - `src/test/java/br/edu/organizacaorecursos/service/ReservaServiceTest.java`
  - `src/test/java/br/edu/organizacaorecursos/ReservaAuditoriaPostgresIntegrationTest.java`
  - `src/test/java/br/edu/organizacaorecursos/ReservaConcorrenciaPostgresIntegrationTest.java`

### Código
- **Serviço:** `src/main/java/br/edu/organizacaorecursos/service/ReservaService.java` (linha 72-76)
- **Repositório:** `src/main/java/br/edu/organizacaorecursos/repository/ReservaRepository.java`

### Defeito
- **ID:** DEF-002
- **Arquivo:** `docs/defeitos/DEF-002-sobreposicao-pendentes.md`
- **Severidade:** 🔴 CRÍTICA
- **Status:** ABERTO

---

## 🏁 Conclusão

**Auditoria completada com sucesso.** 83,3% dos requisitos críticos estão cobertos por testes efetivos. Uma lacuna crítica foi identificada em RC-02 (sobreposição de PENDENTES), documentada em defeto DEF-002, e tem solução proposta. Implementar Opção A desbloqueia deploy.

**Recomendação:** Priorizar correção de RC-02 antes da próxima release.

---

**Auditado por:** Analista de Qualidade (Auditor de Rastreabilidade)  
**Data:** 2026-11-12 02:45:00Z  
**Próxima Revisão:** Após correção de DEF-002
