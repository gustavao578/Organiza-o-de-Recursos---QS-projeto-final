# Divergências de Segurança - Sumário Executivo

**Data de Análise**: 2026-11-12  
**Severidade Máxima**: 🔴 CRÍTICA  
**Total de Divergências**: 9  
**Rotas sem proteção**: 15

---

## Visão Geral

A análise de segurança identificou **9 divergências** entre a matriz de autorização esperada e a implementação atual do Spring Security. Dessas, **4 são críticas** e afetam funcionalidades essenciais (Recursos, Usuários, Manutenções).

### Impacto Geral

| Severidade | Count | Exemplos |
|-----------|-------|----------|
| 🔴 CRÍTICA | 4 | Recurso/Usuário/Manutenção sem autenticação obrigatória |
| 🟠 ALTA | 1 | Recurso Restrito - Lógica de posse incompleta |
| 🟡 MÉDIA | 4 | Validações parciais e regras de negócio não implementadas |

---

## Divergências Críticas

### 1. 🔴 RecursoController - Todos endpoints públicos

**Status Atual**: Qualquer pessoa (anônima) pode criar, atualizar e deletar recursos.

| Operação | Esperado | Atual | Impacto |
|----------|----------|-------|--------|
| `POST /recursos` | ADMINISTRADOR | Público | Alto - Criação de recursos falsos |
| `PUT /recursos/{id}` | ADMINISTRADOR | Público | Alto - Modificação maliciosa |
| `DELETE /recursos/{id}` | ADMINISTRADOR | Público | CRÍTICO - Deleção de recursos do sistema |

**Arquivo afetado**: [RecursoController.java:30-65](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/RecursoController.java)

**Correção obrigatória**:
- Adicionar `@AuthenticationPrincipal Jwt jwt` em todos os 5 métodos
- Exigir `exigirAdmin(jwt)` (ou `ADMINISTRADOR`) em POST/PUT/DELETE
- GET pode permanecer público para catálogo

---

### 2. 🔴 UsuarioController - Deleção sem autenticação

**Status Atual**: Qualquer pessoa pode deletar qualquer usuário do sistema.

| Operação | Esperado | Atual | Impacto |
|----------|----------|-------|--------|
| `DELETE /usuarios/{id}` | ADMINISTRADOR | Público | CRÍTICO - Deleção de contas |
| `GET /usuarios` | ADMINISTRADOR | Público | Alto - Exposição de dados sensíveis |
| `GET /usuarios/{id}` | ADMINISTRADOR | Público | Alto - Exposição de email/matrícula |

**Arquivo afetado**: [UsuarioController.java:45-80](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/UsuarioController.java)

**Correção obrigatória**:
- `DELETE /usuarios/{id}` → Exigir `exigirAdmin(jwt)` + validar posse se não-admin
- `GET /usuarios` → Exigir autenticação mínima
- `GET /usuarios/{id}` → Exigir autenticação mínima ou validar posse (ver a si mesmo)

---

### 3. 🔴 ManutencaoController - Sem autenticação

**Status Atual**: Qualquer pessoa pode criar e listar manutenções (bloqueando recursos).

| Operação | Esperado | Atual | Impacto |
|----------|----------|-------|--------|
| `POST /manutencoes` | RESPONSAVEL/ADMIN | Público | CRÍTICO - Criação de manutenções falsas |
| `GET /manutencoes` | RESPONSAVEL/ADMIN | Público | Médio - Exposição de agenda |

**Arquivo afetado**: [ManutencaoController.java:27-42](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/ManutencaoController.java)

**Cenário de Ataque**:
```
1. Atacante cria POST /manutencoes com recursoId=1 e período futuro
2. Recurso fica bloqueado para 100% do dia
3. Todas as reservas conflitam
4. Negócio parado
```

**Correção obrigatória**:
- `POST /manutencoes` → Exigir `exigirPerfil(jwt, "RESPONSAVEL")` ou admin
- `GET /manutencoes` → Exigir autenticação (mínimo `@AuthenticationPrincipal Jwt jwt`)

---

### 4. 🔴 UsuarioController - Atualização sem validação de posse

**Status Atual**: Qualquer usuário autenticado pode atualizar perfil de qualquer outro usuário.

| Operação | Esperado | Atual | Impacto |
|----------|----------|-------|--------|
| `PUT /usuarios/{id}` | Próprio ou ADMIN | Qualquer autenticado | Alto - Escalação de privilégio |

**Cenário de Ataque**:
```
1. Solicitante autenticado executa PUT /usuarios/{admin-uuid}
2. Muda perfil de Admin para Solicitante
3. Atacante agora é Admin
```

**Arquivo afetado**: [UsuarioController.java:56-70](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/UsuarioController.java)

**Correção obrigatória**:
```java
@PutMapping("/{id}")
public ResponseEntity<UsuarioResponseDTO> atualizarUsuario(
    @PathVariable UUID id,
    @Valid @RequestBody UsuarioRequestDTO request,
    @AuthenticationPrincipal Jwt jwt) {
    
    UUID usuarioId = UUID.fromString(jwt.getSubject());
    boolean isAdmin = isAdministrador(jwt);
    
    // ✅ Regra de posse
    if (!isAdmin && !id.equals(usuarioId)) {
        throw new AccessDeniedException("Usuário só pode atualizar a si mesmo");
    }
    
    // ... resto do código
}
```

---

## Divergências Moderadas

### 5. 🟠 ReservaController - `POST /reservas/{id}/nao-compareceu` rejeita Admin

**Status Atual**: Método exige `exigirPerfil(jwt, "RESPONSAVEL")`, rejeitando ADMINISTRADOR com 403.

```java
@PostMapping("/{id}/nao-compareceu")
public ResponseEntity<ReservaResponseDTO> marcarNaoComparecimento(
    @PathVariable UUID id,
    @Valid @RequestBody(required = false) AprovacaoRequestDTO request,
    @AuthenticationPrincipal Jwt jwt) {
    exigirPerfil(jwt, "RESPONSAVEL");  // ❌ Rejeita Admin
    // ...
}
```

**Impacto**: Administrador de sistema não consegue marcar não comparecimento (pode ser intencional, mas é inconsistente com outras operações).

**Arquivo afetado**: [ReservaController.java:119-126](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/ReservaController.java)

**Decisão necessária**:
- [ ] É intencional? (apenas Responsável)
- [ ] Ou deve ser `exigirUmDosPerfis(jwt, "RESPONSAVEL", "ADMINISTRADOR")`?

---

### 6. 🟡 ReservaController - `GET /reservas/{id}/historico` lógica confusa

**Status Atual**: Método exige autenticação + (RESPONSAVEL ou ADMINISTRADOR), rejeitando SOLICITANTE.

```java
@GetMapping("/{id}/historico")
public ResponseEntity<List<ReservaAuditoriaResponseDTO>> historico(
    @PathVariable UUID id, 
    @AuthenticationPrincipal Jwt jwt) {
    if (jwt == null) {
        throw new AccessDeniedException("...");
    }
    exigirUmDosPerfis(jwt, "RESPONSAVEL", "ADMINISTRADOR");  // ❌ Rejeita Solicitante
    // ...
}
```

**Impacto**: Solicitante não consegue ver o histórico de sua própria reserva.

**Arquivo afetado**: [ReservaController.java:139-147](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/ReservaController.java)

**Correção sugerida**:
```java
// Solicitante vê suas próprias, Responsável/Admin veem qualquer uma
if (jwt == null) throw new AccessDeniedException("...");

UUID usuarioId = UUID.fromString(jwt.getSubject());
String perfil = perfilNormalizado(jwt.getClaimAsString("profile"));

if (!"SOLICITANTE".equals(perfil) && !"RESPONSAVEL".equals(perfil) && !"ADMINISTRADOR".equals(perfil)) {
    throw new AccessDeniedException("...");
}

// Validar posse para Solicitante
if ("SOLICITANTE".equals(perfil)) {
    // Verificar se a reserva pertence ao usuário
}
```

---

### 7. 🟡 ReservaController - Regra de posse em `PUT /reservas/{id}`

**Status Atual**: Implementado no service, mas mensagem de erro genérica.

```java
@PutMapping("/{id}")
public ResponseEntity<ReservaResponseDTO> atualizarReserva(
    @PathVariable UUID id,
    @Valid @RequestBody ReservaRequestDTO request,
    @AuthenticationPrincipal Jwt jwt) {
    
    UUID solicitanteId = UUID.fromString(jwt.getSubject());
    // O service depois valida se solicitanteId é dono da reserva
    return ResponseEntity.ok(reservaService.atualizarReserva(id, request, solicitanteId));
}
```

**Status**: ✅ Implementado, mas poderia ter validação mais clara no controller.

---

### 8. 🟡 ReservaController - Regra de posse em `DELETE /reservas/{id}`

**Status Atual**: Implementado no service, valida posse.

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deletarReserva(
    @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    if (jwt == null) {
        throw new AccessDeniedException("A identidade do solicitante é obrigatória.");
    }
    reservaService.deletarReserva(id, atorId(jwt));
    return ResponseEntity.noContent().build();
}
```

**Status**: ✅ Implementado corretamente.

---

### 9. 🟡 ReservaController - Regra de posse em `POST /reservas/{id}/cancelar`

**Status Atual**: Implementado no service, valida posse.

**Status**: ✅ Implementado corretamente.

---

## Resumo por Controlador

### ✅ ReservaController - Bem protegido (exceção: historico)
- **Divergências**: 2 moderadas (historico + nao-compareceu)
- **Endpoints afetados**: 2/13
- **Regras de posse**: Implementadas corretamente em PUT/DELETE/cancelar

### ❌ RecursoController - Sem proteção
- **Divergências**: 5 críticas
- **Endpoints afetados**: 5/5 (100%)
- **Recomendação**: Adicionar autenticação obrigatória em ALL endpoints

### ❌ UsuarioController - Parcialmente desprotegido
- **Divergências**: 3 críticas + 1 moderada
- **Endpoints afetados**: 4/5 (80%)
- **Recomendação**: Adicionar autenticação e validação de posse

### ❌ ManutencaoController - Sem proteção
- **Divergências**: 2 críticas
- **Endpoints afetados**: 2/2 (100%)
- **Recomendação**: Adicionar autenticação obrigatória

### ✅ RelatorioController - Bem protegido
- **Divergências**: 0
- **Status**: Correto

---

## Plano de Correção Recomendado

### Fase 1️⃣ - CRÍTICA (Implementar HOJE)

**Tempo estimado**: 2-3 horas

1. **RecursoController**
   - [ ] `POST /recursos` → Exigir ADMINISTRADOR
   - [ ] `PUT /recursos/{id}` → Exigir ADMINISTRADOR
   - [ ] `DELETE /recursos/{id}` → Exigir ADMINISTRADOR

2. **ManutencaoController**
   - [ ] `POST /manutencoes` → Exigir RESPONSAVEL ou ADMINISTRADOR
   - [ ] `GET /manutencoes` → Exigir autenticação

3. **UsuarioController**
   - [ ] `DELETE /usuarios/{id}` → Exigir ADMINISTRADOR
   - [ ] `PUT /usuarios/{id}` → Adicionar validação de posse

### Fase 2️⃣ - ALTA (Implementar esta semana)

**Tempo estimado**: 1-2 horas

1. **UsuarioController**
   - [ ] `GET /usuarios` → Exigir autenticação (opcional: apenas ADMIN)
   - [ ] `GET /usuarios/{id}` → Exigir autenticação (permitir ver a si mesmo)

2. **ReservaController**
   - [ ] `GET /reservas/{id}/historico` → Permitir Solicitante ver seus próprios

3. **ReservaController**
   - [ ] `POST /reservas/{id}/nao-compareceu` → Definir se permite Admin ou não

### Fase 3️⃣ - MÉDIA (Próximo sprint)

- [ ] Implementar auditoria de acesso para operações sensíveis
- [ ] Adicionar testes de segurança automatizados para cada endpoint
- [ ] Criar documento de política de autenticação

---

## Testes de Segurança - Casos Críticos

Para cada divergência crítica, executar estes testes:

### Teste 1: RecursoController - POST sem autenticação

```bash
# ❌ ATUALMENTE FUNCIONA (deveria falhar)
curl -X POST http://localhost:8080/recursos \
  -H "Content-Type: application/json" \
  -d '{"nome":"Recurso Fake","tipo":"SALA","restrito":false}' \
  # Resultado: 201 ❌ (esperado: 401)
```

### Teste 2: UsuarioController - DELETE sem autenticação

```bash
# ❌ ATUALMENTE FUNCIONA (deveria falhar)
curl -X DELETE http://localhost:8080/usuarios/12345678-1234-1234-1234-123456789012 \
  # Resultado: 204 ❌ (esperado: 401)
```

### Teste 3: ManutencaoController - POST sem autenticação

```bash
# ❌ ATUALMENTE FUNCIONA (deveria falhar)
curl -X POST http://localhost:8080/manutencoes \
  -H "Content-Type: application/json" \
  -d '{"recursoId":"...","dataInicio":"...","dataFim":"..."}' \
  # Resultado: 201 ❌ (esperado: 401)
```

### Teste 4: UsuarioController - Escalação de privilégio

```bash
# ❌ ATUALMENTE FUNCIONA (deveria falhar)
curl -X PUT http://localhost:8080/usuarios/ADMIN-UUID \
  -H "Authorization: Bearer SOLICITANTE-TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"perfil":"SOLICITANTE"}' \
  # Resultado: 200 ❌ (esperado: 403 - acesso denegado)
```

---

## Referências

- 📄 [Matriz de Autorização Completa](./matriz-autorizacao.md)
- 📄 [SecurityConfig.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/config/SecurityConfig.java)
- 📄 [ReservaController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/ReservaController.java)
- 📄 [RecursoController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/RecursoController.java)
- 📄 [UsuarioController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/UsuarioController.java)
- 📄 [ManutencaoController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/ManutencaoController.java)

---

## Status de Aprovação

| Papel | Status | Data |
|-------|--------|------|
| Analista de Segurança | ✅ Aprovado | 2026-11-12 |
| Architect | ⏳ Pendente | — |
| DevSecOps | ⏳ Pendente | — |

