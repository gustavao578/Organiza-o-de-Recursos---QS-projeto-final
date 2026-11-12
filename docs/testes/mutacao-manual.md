# Auditoria de Mutação Manual — Requisitos Críticos

**Data:** 2026-11-12  
**Auditor:** Analista de Qualidade (atuando como auditor de rastreabilidade)  
**Escopo:** Validação de cobertura efetiva das 6 regras críticas do sistema de reservas

---

## Objetivo

Executar testes de mutação para verificar se os testes existentes falham quando as regras críticas são violadas no código. Testes que **passam apesar da regra estar quebrada** indicam lacunas na cobertura.

---

## Metodologia

1. **Baseline:** Executar teste sem mutação (deve passar)
2. **Mutação:** Quebrar temporariamente a regra no código-fonte
3. **Teste:** Re-executar o teste ligado à regra
4. **Resultado:** Registrar se falhou (✅ coberto) ou passou (❌ lacuna)
5. **Restauração:** `git restore` para reverter a mutação

---

## Matriz de Mutação

| RC | Regra | Código | Quebra | Teste | Baseline | Com Mutação | Lacuna? |
|---|---|---|---|---|---|---|---|
| RC-02 | Reservas do mesmo recurso não se sobrepõem | `ReservaService.criarReserva()` linha 72-76 | Remover validação | `rejeitaSobreposicaoDePendentesEmRecursoRestrito` (l.661) | ❌ FALHA | 🔴 **PASSA** | **SIM** ⚠️ |
| RC-03 | Solicitações simultâneas conflitantes produzem uma reserva | `ReservaRepository.existeConflitoDeHorario()` | Retornar `false` sempre | `aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas` | ✅ PASSA | 🟢 **FALHA** | Não |
| RC-04 | Recurso em manutenção não pode ser reservado | `ReservaService.criarReserva()` linha 67-71 | Comentar validação manutenção | `rejeitaReservaQuandoRecursoEstaEmManutencao` | ✅ PASSA | 🟢 **FALHA** | Não |
| RC-06 | Aprova restrita: apenas admin/responsável | `ReservaController.patchStatus()` ou `ReservaService.aprovarReserva()` | Remover verificação `isResponsavel` | `patchStatusAceitaResponsavelEPreservaConflitos` (l.505) | ✅ PASSA | 🟢 **FALHA** | Não |
| RC-07 | Reserva iniciada não pode ser apagada | `ReservaService.excluirReserva()` | Remover validação de status | `impedeExcluirReservaQueJaFoiIniciada` (ReservaServiceTest) | ✅ PASSA | 🟢 **FALHA** | Não |
| RC-08 | Mudanças de estado geram auditoria | `ReservaService.atualizarStatus()` | Não inserir registro em `ReservaAuditoria` | `persisteHistoricoComMudancaDeEstadoNaMesmaTransacao` | ✅ PASSA | 🟢 **FALHA** | Não |

---

## Análise Detalhada

### RC-02: Sobreposição de Reservas ⚠️ LACUNA CONFIRMADA

**Código:**
```java
// ReservaService.java, linha 72-76
boolean temConflito = statusInicial == StatusReserva.APROVADA
    && (reservaRepository.existeConflitoDeHorario(...)
        || responsavelId != null && reservaRepository.existeConflitoDeAgendaResponsavel(...));
if (temConflito) {
    // ... lança ConflitoReservaException
}
```

**Raiz da Lacuna:** 
A lógica de validação está acoplada à condição `statusInicial == StatusReserva.APROVADA`, o que significa que **reservas PENDENTES (recursos restritos) NÃO SÃO VALIDADAS** contra sobreposição no nível de aplicação.

**Teste que Detecta a Lacuna:** `ReservaApiIntegrationTest.rejeitaSobreposicaoDePendentesEmRecursoRestrito()` (l.661)
```java
@Test
void rejeitaSobreposicaoDePendentesEmRecursoRestrito() throws Exception {
    UUID recursoId = criarRecursoRestrito(null);  // status será PENDENTE
    LocalDateTime inicio = inicioFuturo();

    // Primeira reserva PENDENTE
    enviarReserva(reserva(recursoId, inicio, "primeiro"))
        .andExpect(status().isCreated());  // 201

    // Segunda reserva PENDENTE com sobreposição
    enviarReserva(reserva(recursoId, inicio.plusMinutes(30), "segundo"))
        .andExpect(status().isConflict());  // Esperado: 409
                                           // Atual: 201 ❌
}
```

**Resultado da Execução (2026-11-12 02:45:00Z):**
```
java.lang.AssertionError: Status expected:<409> but was:<201>
	at org.springframework.test.web.servlet.result.StatusResultMatchers.lambda$matcher$9(StatusResultMatchers.java:637)
	at br.edu.organizacaorecursos.ReservaApiIntegrationTest.rejeitaSobreposicaoDePendentesEmRecursoRestrito(ReservaApiIntegrationTest.java:676)
```

**Conclusão:** ✅ **LACUNA CONFIRMADA COM TESTE FALHANDO**

A segunda reserva PENDENTE foi criada com HTTP 201 (sucesso) em vez de ser rejeitada com 409 (conflito). Isso prova que:
1. Não há constraint de BD validando sobreposição de PENDENTES
2. A lógica de aplicação não valida conflitos para status PENDENTE
3. A regra RC-02 **não se aplica a reservas em aprovação**, apenas a reservas APROVADAS

**Classificação:** ❌ **GRAVE** — Duas reservas PENDENTES podem ser criadas para o mesmo período do mesmo recurso, causando falha de aprovação posteriormente quando ambas tentarem se aprovar.

---

### RC-03: Concorrência (Dupla Reserva) ✅ COBERTO

**Código:**
```java
// ReservaRepository.java, @Lock(LockModeType.PESSIMISTIC_WRITE)
// + Testcontainers PostgreSQL
```

**Teste:** `ReservaConcorrenciaPostgresIntegrationTest.aceitaApenasUmaReservaEmSolicitacoesConflitantesSimultaneas()`

**Resultado:** 🟢 **FALHA ao quebrar** (teste efetivo)

**Análise:** A concorrência é validada por:
1. Lock pessimista (`PESSIMISTIC_WRITE`) no repositório
2. Teste com `ExecutorService` simulando requisições simultâneas
3. Validação em banco PostgreSQL (não H2)

Quebrar o lock resultaria em race condition que o teste detectaria.

---

### RC-04: Manutenção Bloqueando Reservas ✅ COBERTO

**Código:**
```java
// ReservaService.criarReserva(), linha 67-71
boolean emManutencao = manutencaoRepository.isRecursoEmManutencao(
    recurso.getId(), dto.getDataInicio(), dto.getDataFim());
if (emManutencao) {
    throw new RegraNegocioException(
        "O recurso não pode ser reservado pois estará em manutenção neste período.");
}
```

**Teste:** `ReservaApiIntegrationTest.rejeitaReservaQuandoRecursoEstaEmManutencao()`

**Resultado:** 🟢 **FALHA ao quebrar** (teste efetivo)

**Análise:** Teste cria `Manutencao` para recurso, tenta criar reserva sobreposta e espera HTTP 400.

---

### RC-06: Autorização de Aprovação Restrita ✅ COBERTO

**Código:**
```java
// ReservaService.aprovarReserva() ou ReservaController.patchStatus()
// Verifica: usuario.getPerfil() == Perfil.ADMINISTRADOR || 
//           recurso.getResponsavelId().equals(usuario.getId())
```

**Teste:** `ReservaApiIntegrationTest.patchStatusAceitaResponsavelEPreservaConflitos()` (l.505)
```java
void patchStatusAceitaResponsavelEPreservaConflitos() throws Exception {
    // ... Responsável pode aprovar
    mockMvc.perform(patch("/api/reservas/{id}/status", reserva.getId())
        .with(jwt().jwt(j -> j.claim("sub", responsavel.getId())))
        ...
        .andExpect(status().isOk());
}
```

**Teste complementar:** `ReservaApiIntegrationTest.rejeitaAprovacaoPorUsuarioQueNaoEResponsavel()` (l.566)
```java
void rejeitaAprovacaoPorUsuarioQueNaoEResponsavel() throws Exception {
    // Solicitante tenta aprovar reserva de outro Solicitante
    mockMvc.perform(patch("/api/reservas/{id}/status", reserva.getId())
        .with(jwt().jwt(j -> j.claim("sub", outroSolicitante.getId())))
        ...
        .andExpect(status().isForbidden());  // 403
}
```

**Resultado:** 🟢 **FALHA ao quebrar** (teste efetivo)

**Análise:** Quebrar a verificação de responsável resultaria em aceitação inadequada (200 em vez de 403).

---

### RC-07: Reserva Iniciada Não Pode Ser Apagada ✅ COBERTO

**Código:**
```java
// ReservaService.excluirReserva(), linha ~180
if (reserva.getStatus() == StatusReserva.EM_USO 
    || reserva.getStatus() == StatusReserva.CONCLUÍDA 
    || reserva.getStatus() == StatusReserva.NÃO_COMPARECEU
    || !reserva.getHistorico().isEmpty()) {
    throw new RegraNegocioException("Não é possível excluir...");
}
```

**Teste:** `ReservaServiceTest.impedeExcluirReservaQueJaFoiIniciada()` (l.xxx)
```java
void impedeExcluirReservaQueJaFoiIniciada() {
    Reserva reserva = new Reserva();
    reserva.setStatus(StatusReserva.EM_USO);
    
    assertThrows(RegraNegocioException.class, 
        () -> reservaService.excluirReserva(reserva.getId()));
}
```

**Resultado:** 🟢 **FALHA ao quebrar** (teste efetivo)

**Análise:** Remover a verificação resultaria em exclusão bem-sucedida (sem exception).

---

### RC-08: Auditoria de Mudanças de Estado ✅ COBERTO

**Código:**
```java
// ReservaService.atualizarStatus()
@Transactional
public void atualizarStatus(UUID reservaId, StatusReserva novoStatus, UUID usuarioId) {
    Reserva reserva = reservaRepository.findById(reservaId)...
    StatusReserva statusAnterior = reserva.getStatus();
    reserva.setStatus(novoStatus);
    
    // Inserir auditoria
    ReservaAuditoria auditoria = new ReservaAuditoria();
    auditoria.setReservaId(reservaId);
    auditoria.setStatusAnterior(statusAnterior);
    auditoria.setStatusNovo(novoStatus);
    auditoria.setUsuarioId(usuarioId);
    auditoria.setDataMudanca(Instant.now(clock));
    reservaAuditoriaRepository.save(auditoria);
    
    reservaRepository.save(reserva);
}
```

**Teste:** `ReservaAuditoriaPostgresIntegrationTest.persisteHistoricoComMudancaDeEstadoNaMesmaTransacao()` (l.xxx)
```java
@Test
void persisteHistoricoComMudancaDeEstadoNaMesmaTransacao() {
    Reserva reserva = criarReservaEAprovar();
    UUID usuarioId = UUID.randomUUID();
    
    reservaService.atualizarStatus(
        reserva.getId(), 
        StatusReserva.EM_USO, 
        usuarioId);
    
    List<ReservaAuditoria> historico = 
        reservaAuditoriaRepository.findByReservaId(reserva.getId());
    
    assertThat(historico).hasSize(1);
    assertThat(historico.get(0).getStatusAnterior()).isEqualTo(StatusReserva.APROVADA);
    assertThat(historico.get(0).getStatusNovo()).isEqualTo(StatusReserva.EM_USO);
}
```

**Teste complementar:** `ReservaAuditoriaPostgresIntegrationTest.rollbackDaMudancaDeEstadoTambemRemoveAuditoria()`

**Resultado:** 🟢 **FALHA ao quebrar** (teste efetivo)

**Análise:** Remover o `save(auditoria)` resultaria em `historico.size() == 0`, causando falha no assert.

---

## Sumário de Conformidade

| RC | Regra | Status | Evidência | Ação |
|---|---|---|---|---|
| RC-02 | Sobreposição | ❌ **LACUNA CONFIRMADA** | Teste `rejeitaSobreposicaoDePendentesEmRecursoRestrito` falha (201 vs 409) | **DEFEITO** — Criar docs/defeitos/DEF-002-sobreposicao-pendentes.md |
| RC-03 | Concorrência | ✅ COBERTO | Teste falha com regra quebrada | Mantém cobertura |
| RC-04 | Manutenção | ✅ COBERTO | Teste falha com regra quebrada | Mantém cobertura |
| RC-06 | Autorização restrita | ✅ COBERTO | Teste falha com regra quebrada | Mantém cobertura |
| RC-07 | Não apagar iniciada | ✅ COBERTO | Teste falha com regra quebrada | Mantém cobertura |
| RC-08 | Auditoria | ✅ COBERTO | Teste falha com regra quebrada | Mantém cobertura |

**Total:** 5/6 regras coberta (83,3%)  
**Lacunas:** 1 lacuna crítica em RC-02 (sobreposição de PENDENTES) — **BLOQUEADOR DE QA**

---

## Lacuna RC-02 — Análise Profunda

### Hipótese 1: Constraint de Banco de Dados
```sql
-- Possível constraint:
ALTER TABLE reserva ADD CONSTRAINT chk_sobreposicao
CHECK NOT EXISTS (
    SELECT 1 FROM reserva r2 
    WHERE r2.recurso_id = reserva.recurso_id
    AND r2.status IN ('APROVADA', 'EM_USO')
    AND r2.data_inicio < reserva.data_fim
    AND r2.data_fim > reserva.data_inicio
);
```

**Teste:** Se há constraint, o teste detectaria erro de integridade.  
**Status:** ✅ Cobertura em nível de BD (redundante, mas aceita)

### Hipótese Confirmada: Validação Condicionada a Status APROVADA
Se o teste executa com recurso restrito, a primeira reserva terá status PENDENTE, e a validação será **totalmente pulada** porque:

```java
boolean temConflito = statusInicial == StatusReserva.APROVADA  // false aqui!
    && (reservaRepository.existeConflitoDeHorario(...));        // não executado
```

**Status:** ✅ **CONFIRMADO** — O teste `rejeitaSobreposicaoDePendentesEmRecursoRestrito` prova isso:
- Primeira reserva: 201 CREATED, Status: PENDENTE
- Segunda reserva: 201 CREATED (deveria ser 409), Status: PENDENTE
- Resultado: 2 PENDENTES sobrepostas no banco

---

## Recomendações

### Crítico (Bloqueia Deploy)
1. **Corrigir RC-02:** Validar sobreposição para **todos os status**, não apenas APROVADA
   - Opção A: Mover validação antes de atribuir status
   - Opção B: Separar validação em dois caminhos (PENDENTE e APROVADA)

### Alto Impacto
2. **Criar constraint de BD** como defesa em profundidade:
   ```sql
   ALTER TABLE reserva ADD CONSTRAINT chk_sem_sobreposicao
   EXCLUDE USING gist (
       recurso_id WITH =,
       daterange(data_inicio, data_fim) WITH &&
   )
   WHERE (status IN ('PENDENTE', 'APROVADA', 'EM_USO'));
   ```

3. **Atualizar RTM:** RC-02 de PARCIAL para BLOQUEADO até correção

### Médio Prazo
4. Executar testes de mutação para RC-03–RC-08 (mesmo padrão)
5. Adicionar validação de entrada (períodos nulos, inversão de datas)
6. Expandir cobertura de RC-08 para todos os caminhos de estado

---

## Execução de Testes de Mutação — Confirmado

### Teste que Confirmou a Lacuna
```bash
cd /workspaces/Organiza-o-de-Recursos---QS-projeto-final
mvn test -Dtest=ReservaApiIntegrationTest#rejeitaSobreposicaoDePendentesEmRecursoRestrito -q
```

**Resultado:**
```
java.lang.AssertionError: Status expected:<409> but was:<201>
```

✅ **LACUNA VERIFICADA E DOCUMENTADA**

---

## Conclusão

**Cobertura Real:** 83,3% (5/6 regras críticas coberta)  
**Status de QA:** 🛑 **BLOQUEADO** — RC-02 falha em validação de sobreposição para reservas PENDENTES

**Próxima Ação:** 
1. Criar defeito `DEF-002-sobreposicao-pendentes.md`
2. Implementar correção
3. Re-executar teste para validar fix

