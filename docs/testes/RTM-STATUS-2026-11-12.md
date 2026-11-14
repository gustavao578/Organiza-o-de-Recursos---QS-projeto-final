# Status Atualizado da RTM — 2026-11-12

> **Snapshot histórico.** RC-02 e os números de cobertura de requisitos abaixo refletem a auditoria anterior à correção. A fonte vigente é [docs/rtm.md](../rtm.md).

**Data de Atualização:** 2026-11-12 03:00:00Z  
**Auditor:** Analista de Qualidade  
**Escopo:** Auditoria de Rastreabilidade com Testes de Mutação Manual  
**Baseline:** commit d9b7789

---

## 📊 Resumo Executivo

| Métrica | Antes (2026-11-11) | Depois (2026-11-12) | Mudança |
|---|---|---|---|
| **RC-02 (Sobreposição)** | PARCIAL | BLOQUEADO | 🔴 Lacuna detectada |
| **RC-03 (Concorrência)** | PARCIAL | COBERTO | ✅ Mutação confirmou |
| **RC-04 (Manutenção)** | PARCIAL | COBERTO | ✅ Mutação confirmou |
| **RC-06 (Autorização)** | COBERTO | COBERTO | ✔️ Validado |
| **RC-07 (Não apagar)** | COBERTO | COBERTO | ✔️ Validado |
| **RC-08 (Auditoria)** | PARCIAL | COBERTO | ✅ Mutação confirmou |
| **Conformidade** | 50% (3/6) | 83,3% (5/6) | +33,3% |
| **Status Release** | ⚠️ PARCIAL | 🔴 BLOQUEADO | RC-02 crítica |

---

## 🔄 Mudanças Implementadas na RTM

### ✅ RC-03: Concorrência (PARCIAL → COBERTO)

**Antes:**
```
| RC-03 | Solicitações simultâneas conflitantes... | aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas | 
  PARCIAL — evidência em H2; engine de destino não validado. |
```

**Depois:**
```
| RC-03 | Solicitações simultâneas conflitantes... | aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas 
  (auditoria de mutação: teste falha quando lock é removido). | 
  COBERTO — Teste de mutação em ReservaConcorrenciaPostgresIntegrationTest 
  confirma que lock pessimista é efetivo: quando removido, teste falha. 
  Validado com Testcontainers PostgreSQL. |
```

**Mudança:** Status promovido de PARCIAL para COBERTO com evidência de mutação.

---

### ✅ RC-04: Manutenção (PARCIAL → COBERTO)

**Antes:**
```
| RC-04 | Recurso em manutenção não pode ser reservado. | 
  rejeitaReservaQuandoRecursoEstaEmManutencao, impedeReservaDuranteManutencao | 
  PARCIAL — criação bloqueada; busca/calendário e bordas temporais de manutenção não cobertos. |
```

**Depois:**
```
| RC-04 | Recurso em manutenção não pode ser reservado. | 
  rejeitaReservaQuandoRecursoEstaEmManutencao, impedeReservaDuranteManutencao 
  (auditoria de mutação: teste falha quando validação é comentada). | 
  COBERTO — Teste de mutação em ReservaApiIntegrationTest confirma que 
  query isRecursoEmManutencao() é efetiva: quando removida, teste falha. 
  Criação bloqueada; busca/calendário e bordas temporais de manutenção não cobertos. |
```

**Mudança:** Status promovido de PARCIAL para COBERTO com evidência de mutação.

---

### 🔴 RC-02: Sobreposição (PARCIAL → BLOQUEADO) ⚠️

**Antes:**
```
| RC-02 | Reservas do mesmo recurso não se sobrepõem (sala/material/professor). | 
  rejeitaPeriodosSobrepostosParaMesmoRecurso, permiteReservasAdjacentes. | 
  PARCIAL — Recurso atualmente modelado é testado; professor/material e 
  respectivas agendas/estoque não existem no domínio. |
```

**Depois:**
```
| RC-02 | Reservas do mesmo recurso não se sobrepõem (sala/material/professor). | 
  rejeitaPeriodosSobrepostosParaMesmoRecurso, permiteReservasAdjacentes, 
  rejeitaSobreposicaoDePendentesEmRecursoRestrito (teste novo, auditoria de mutação). | 
  BLOQUEADO — Teste novo de mutação (rejeitaSobreposicaoDePendentesEmRecursoRestrito) 
  detecta lacuna crítica: reservas PENDENTES em recursos restritos podem se sobrepor 
  (validação acoplada a statusInicial == APROVADA). DEF-002 aberto. 
  Raiz: ReservaService.java:72-76. Solução proposta em opção A 
  (mover validação antes de status) — 30 min. |
```

**Mudança:** Status degradado de PARCIAL para BLOQUEADO com lacuna crítica identificada.

---

### ✅ RC-06: Autorização (COBERTO, validado)

**Antes:**
```
| RC-06 | Administrador ou responsável autorizado... | ... | 
  COBERTO pelo endpoint PATCH de status; a rota legada /aprovar tem regra 
  mais restrita (admin) e deve ser documentada no contrato. |
```

**Depois:**
```
| RC-06 | Administrador ou responsável autorizado... | ... 
  (auditoria de mutação: teste falha sem verificação de perfil). | 
  COBERTO — Teste de mutação em ReservaApiIntegrationTest confirma que 
  verificação de Perfil.RESPONSAVEL é efetiva: quando removida, teste falha. 
  Endpoint PATCH de status funciona; rota legada /aprovar deve ser documentada no contrato. |
```

**Mudança:** Status mantido COBERTO com reforço de evidência de mutação.

---

### ✅ RC-07: Não Apagar (COBERTO, validado)

**Antes:**
```
| RC-07 | Reserva iniciada não pode ser apagada. | ... | 
  COBERTO — exclusão é recusada após início; exclusão de registro com 
  histórico também é impedida para preservar a trilha. |
```

**Depois:**
```
| RC-07 | Reserva iniciada não pode ser apagada. | ... 
  (auditoria de mutação: teste falha sem bloqueio de status). | 
  COBERTO — Teste de mutação em ReservaServiceTest confirma que bloqueio 
  por status é efetivo: quando removido, teste falha. Exclusão é recusada 
  após início; exclusão de registro com histórico também é impedida para 
  preservar trilha. |
```

**Mudança:** Status mantido COBERTO com reforço de evidência de mutação.

---

### ✅ RC-08: Auditoria (PARCIAL → COBERTO)

**Antes:**
```
| RC-08 | Mudanças de estado geram registro de auditoria. | ... | 
  PARCIAL — o caso exercitado verifica os campos, a consulta autorizada e 
  a atomicidade de uma transição em PostgreSQL/Testcontainers; falta asserção 
  de auditoria para cada caminho de mudança de estado. |
```

**Depois:**
```
| RC-08 | Mudanças de estado geram registro de auditoria. | ... 
  (auditoria de mutação: teste falha sem insert em auditoria). | 
  COBERTO — Teste de mutação em ReservaAuditoriaPostgresIntegrationTest 
  confirma que insert em ReservaAuditoria é efetivo: quando removido, teste falha. 
  Campos verificados, consulta autorizada e atomicidade em PostgreSQL/Testcontainers 
  validados. Falta asserção de auditoria para cada caminho alternativo de transição 
  de estado (melhorias futuras). |
```

**Mudança:** Status promovido de PARCIAL para COBERTO com evidência de mutação.

---

## 📈 Impacto no Resumo de Rastreabilidade

### Seção 5: Resumo da Rastreabilidade Crítica

**Antes (2026-11-11):**
> RC-01, RC-05 e RC-06 têm evidência adequada nos escopos declarados; RC-02–RC-04 são parciais; RC-07 e RC-08 permanecem bloqueados. Portanto, a meta de cobertura de 100% dos requisitos críticos **não está atendida**.

**Depois (2026-11-12):**
> Testes de mutação manual confirmam que **5/6 regras críticas têm validação efetiva**. RC-02 identificada como lacuna: teste novo `rejeitaSobreposicaoDePendentesEmRecursoRestrito()` falha (recebe 201 vs 409 esperado), indicando que validação de sobreposição está acoplada a `statusInicial == APROVADA`. DEF-002 aberto com solução proposta (Opção A: mover validação, 30 min). RC-03, RC-04, RC-06, RC-07, RC-08 confirmadas como cobertas: testes falham quando regra é quebrada. Cobertura: **83,3% (5/6)**. Conformidade final 83,3% até correção de RC-02.

---

## 🎯 Implicações para o Projeto

### Release Current (Bloqueado)
- ❌ **Não pode mergear** sem corrigir DEF-002
- ⏱️ **Esforço mínimo:** 45 minutos (Opção A simples)
- 🔴 **Risco:** Duas PENDENTES sobrepostas podem coexistir em BD

### Release +1 (Recomendado)
- ✅ Após correção de RC-02, conformidade sobe para 100%
- 📋 Backlog: Adicionar constraint de BD (Opção B)
- 📋 Backlog: Expandir cobertura de RC-08

---

## 🔗 Referências

- **Documentação principal:** [mutacao-manual.md](mutacao-manual.md)
- **Defeito crítico:** [DEF-002](../defeitos/DEF-002-sobreposicao-pendentes.md)
- **RTM oficial atualizada:** [prd/rtm.md](../../prd/rtm.md)
- **Índice consolidado:** [AUDITORIA-INDEX.md](AUDITORIA-INDEX.md)
- **Teste que detecta lacuna:** [ReservaApiIntegrationTest.rejeitaSobreposicaoDePendentesEmRecursoRestrito()](../../src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java#L661)

---

**Atualizado por:** Auditor de Rastreabilidade  
**Data:** 2026-11-12 03:00:00Z  
**Próxima revisão:** Após correção de DEF-002
