# Defeito DEF-002: Sobreposição de Reservas PENDENTES Não Validada

> **Atualização vigente — 2026-11-14:** **RESOLVIDO E VERIFICADO** no código atual. `ReservaService.criarReserva` consulta conflitos antes de atribuir `PENDENTE` ou `APROVADA`; `ReservaApiIntegrationTest.rejeitaSobreposicaoDePendentesEmRecursoRestrito` passou em `./mvnw -B verify`. A descrição abaixo registra a condição histórica que motivou o defeito, não o comportamento atual. O teste comprova o cenário em escopo local; não substitui CI/Quality Gate ou revisão por pares da branch.

**ID:** DEF-002  
**Título:** Reservas PENDENTES podem se sobrepor, violando RC-02  
**Severidade:** 🔴 **CRÍTICA**  
**Status:** ✅ **FECHADO**  
**Detectado em:** 2026-11-12 02:45:00Z  
**Detectado por:** Auditor de Rastreabilidade  
**Fechado em:** 2026-11-12  
**Fechado por:** PR #8 - fix(epic-02): secure reservation SPA and API  
**Merged em:** 2026-11-12T12:14:52-03:00  
**Commit:** 58411ac0904a248e1ccae74094f775e8611c3cf8  
**Relacionado:** RC-02 (Reservas do mesmo recurso não se sobrepõem)

---

## Resumo Executivo

Duas reservas PENDENTES (criadas sobre recurso restrito) podem ser criadas para o mesmo período do mesmo recurso, **violando a regra crítica RC-02**. A raiz é que a validação de sobreposição está condicionada a `statusInicial == StatusReserva.APROVADA`, o que significa que reservas PENDENTES **não são validadas contra conflitos no nível de aplicação**.

---

## Descrição do Problema

### Comportamento Atual
```java
// ReservaService.java, linha 72-76
boolean temConflito = statusInicial == StatusReserva.APROVADA
    && (reservaRepository.existeConflitoDeHorario(...));
if (temConflito) {
    throw new ConflitoReservaException(...);
}
```

Se `statusInicial == StatusReserva.PENDENTE` (recurso restrito):
- A verificação de conflito é **completamente pulada**
- Duas reservas PENDENTES sobrepostas podem coexistir
- Problema explode quando responsável tenta aprovar (ambas tentarão se aprovar)

### Cenário de Falha
```
1. Criar Recurso (restrito=true)
2. POST /reservas {recursoId, dataInicio, dataFim="09:00-10:00"}
   → 201 CREATED, Status: PENDENTE
3. POST /reservas {recursoId, dataInicio, dataFim="09:30-10:30"}  (sobreposto)
   → 201 CREATED, Status: PENDENTE  ❌ (deveria ser 409 CONFLICT)
4. Banco tem 2 PENDENTES sobrepostas
```

### Impacto Funcional
- **Aprovação ambígua:** Quando responsável aprova, qual das duas deve passar?
- **Uso duplo:** Duas reservas PENDENTES podem virar APROVADAS para o mesmo período
- **Calendário inconsistente:** `GET /reservas/recurso/{id}?dataInicio=...` pode retornar conflitos

---

## Evidência

### Teste que Comprova
```java
@Test
void rejeitaSobreposicaoDePendentesEmRecursoRestrito() throws Exception {
    UUID recursoId = criarRecursoRestrito(null);
    LocalDateTime inicio = inicioFuturo();

    enviarReserva(reserva(recursoId, inicio, "primeiro"))
        .andExpect(status().isCreated());  // 201

    enviarReserva(reserva(recursoId, inicio.plusMinutes(30), "segundo"))
        .andExpect(status().isConflict());  // Esperado: 409
}
```

### Execução e Resultado
```
Teste: ReservaApiIntegrationTest#rejeitaSobreposicaoDePendentesEmRecursoRestrito
Status: FALHA
Erro: java.lang.AssertionError: Status expected:<409> but was:<201>

Banco após execução:
- Reserva 1: PENDENTE, 09:00-10:00 ✅
- Reserva 2: PENDENTE, 09:30-10:30 ✅ (não deveria existir)
```

---

## Raiz Identificada

1. **Condição acoplada a status:** Validação só ocorre quando `statusInicial == APROVADA`
2. **Sem defesa em profundidade:** Não há constraint de BD invalidando sobreposição de PENDENTES
3. **Lógica de negócio incompleta:** RC-02 deveria aplicar a todos os status, não só APROVADA

### Código Problemático
```java
StatusReserva statusInicial = recurso.isRestrito()
    ? StatusReserva.PENDENTE        // Recurso restrito → PENDENTE
    : StatusReserva.APROVADA;

boolean temConflito = statusInicial == StatusReserva.APROVADA  // ← AQUI: pula se PENDENTE
    && (reservaRepository.existeConflitoDeHorario(...));
```

---

## Solução Proposta

### Opção A: Validação Universal (Recomendada)
Mover validação de sobreposição **antes** de definir o status:

```java
// 1. Validar sobreposição INDEPENDENTE de status
boolean temConflito = reservaRepository.existeConflitoDeHorario(
    recurso.getId(), dto.getDataInicio(), dto.getDataFim());

if (temConflito) {
    throw new ConflitoReservaException(...);  // Falha aqui para PENDENTES e APROVADAS
}

// 2. Depois, atribuir status
StatusReserva statusInicial = recurso.isRestrito()
    ? StatusReserva.PENDENTE
    : StatusReserva.APROVADA;
```

**Impacto:** ✅ Simples, cobre PENDENTES e APROVADAS, alinha com RTM

### Opção B: Constraint de BD
Adicionar constraint de overlapping em PostgreSQL:

```sql
ALTER TABLE reserva ADD CONSTRAINT chk_sem_sobreposicao_qualquer_status
EXCLUDE USING gist (
    recurso_id WITH =,
    daterange(data_inicio, data_fim) WITH &&
)
WHERE (status IN ('PENDENTE', 'APROVADA', 'EM_USO'));
```

**Impacto:** ✅ Defesa em profundidade, mas deixa aplicação sem validação explícita

### Recomendação
**Implementar Opção A + Opção B:**
- A = lógica clara na aplicação, feedback rápido
- B = garantia de BD, impossível contornar via UPDATE direto

---

## Impacto Esperado

### Linha de Base (ANTES)
```
Teste: rejeitaSobreposicaoDePendentesEmRecursoRestrito
Status: ❌ FALHA
Motivo: 2 PENDENTES sobrepostas criadas (201, 201)
Cobertura RC-02: PARCIAL (apenas APROVADAS)
```

### Depois da Correção (DEPOIS)
```
Teste: rejeitaSobreposicaoDePendentesEmRecursoRestrito
Status: ✅ PASSA
Resultado: Primeira 201, Segunda 409 (rejeitada)
Cobertura RC-02: COBERTO (todos os status)
```

---

## Passos de Correção

### 1. Implementar Solução Código (Opção A)
- Arquivo: `src/main/java/br/edu/organizacaorecursos/service/ReservaService.java`
- Linhas: 72-76 (mover `temConflito` antes de `statusInicial`)

### 2. Adicionar Constraint de BD (Opção B)
- Arquivo: `src/main/resources/db/migration/V002_AddOverlapConstraint.sql` (nova)
- SQL: EXCLUDE constraint em `reserva` table

### 3. Validar com Testes
```bash
mvn test -Dtest=ReservaApiIntegrationTest#rejeitaSobreposicaoDePendentesEmRecursoRestrito -q
```

### 4. Revalidar Mutação
```bash
# Quebrar a validação (comentar) e verificar que teste falha
git checkout src/main/java/.../ReservaService.java
# sed -i '72,75s/^/\/\/ /' src/main/java/.../ReservaService.java
mvn test -Dtest=ReservaApiIntegrationTest#rejeitaSobreposicaoDePendentesEmRecursoRestrito
# Esperado: FALHA
git restore src/main/java/.../ReservaService.java
```

### 5. Atualizar RTM
- Mover RC-02 de `PARCIAL` para `COBERTO` em `prd/rtm.md`

---

## Teste de Aceitação

### Critérios de Aceitação (CA)
```
CA-1: Primeira reserva PENDENTE criada com sucesso (201)
CA-2: Segunda reserva PENDENTE sobreposta rejeitada (409)
CA-3: Mensagem de erro menciona recurso e período conflitante
CA-4: Teste rejeitaSobreposicaoDePendentesEmRecursoRestrito PASSA
CA-5: Teste rejeitaPeriodosSobrepostosParaMesmoRecurso continua PASSANDO
CA-6: Mutação de validação quebra o teste (indica cobertura real)
```

### Teste Manual
```bash
# Recurso restrito + sobreposição deve rejeitar segunda
POST /recursos {nome: "Sala A", tipo: "SALA", restrito: true}
→ {"id": "abc123"}

POST /reservas {
  recursoId: "abc123",
  dataInicio: "2026-11-20T09:00:00",
  dataFim: "2026-11-20T10:00:00",
  usuarioId: "xxx"
}
→ 201, Status: PENDENTE

POST /reservas {
  recursoId: "abc123",
  dataInicio: "2026-11-20T09:30:00",  # sobreposta
  dataFim: "2026-11-20T10:30:00",
  usuarioId: "xxx"
}
→ 409 CONFLICT, erro: "O recurso já está reservado neste período."
```

---

## Bloqueadores Relacionados
- Nenhum (defeito é isolado, não depende de outras correções)

---

## Referências
- **RTM:** `prd/rtm.md` — RC-02 (Sobreposição de reservas)
- **Testes:** `src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java` — `rejeitaSobreposicaoDePendentesEmRecursoRestrito` (l.661)
- **Mutação:** `docs/testes/mutacao-manual.md` — Análise e confirmação de lacuna

---

## Cronograma Proposto
- **Sprint atual:** 📋 Análise e planejamento (✅ COMPLETO)
- **Sprint +1:** 🔧 Implementação Opção A (validação em código)
- **Sprint +1:** 🗄️ Implementação Opção B (constraint de BD)
- **Sprint +1:** ✅ Validação final e release

---

## Assinante / Triagem
- **Auditor:** Analista de Qualidade / Auditor de Rastreabilidade
- **Data de criação:** 2026-11-12T02:45:00Z
- **Status:** 🔴 ABERTO → [Aguardando triage de dev]
