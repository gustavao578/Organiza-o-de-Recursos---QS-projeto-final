# DEF-001: POST /recursos sem autenticação obrigatória

**Data**: 2026-11-12  
**Status**: ✅ FECHADO  
**Severidade**: CRÍTICA  
**Categoria**: Falta de autenticação  
**Fechado por**: PR #8 - fix(epic-02): secure reservation SPA and API  
**Merged em**: 2026-11-12T12:14:52-03:00  
**Commit**: 58411ac0904a248e1ccae74094f775e8611c3cf8  

## Descrição

O endpoint `POST /recursos` permite que **usuários anônimos** criem recursos sem autenticação.

**Matriz de autorização esperada:**
- Anônimo: `401 Unauthorized`
- Solicitante: `401 Forbidden` (sem permission)
- Responsável: `401 Forbidden` (sem permission)
- Administrador: `201 Created`

**Código atual:**
- Anônimo: `201 Created` ⚠️ **DIVERGÊNCIA**

## Cenário de ataque

```bash
curl -X POST http://localhost:8080/recursos \
  -H "Content-Type: application/json" \
  -d '{"nome":"Computador Hackeado","tipo":"EQUIPAMENTO"}'

# Retorna 201 Created (deveria ser 401)
```

Um atacante pode:
1. Criar recursos falssos sem se autenticar
2. Poluir o sistema com dados inválidos
3. Criar recurso "trap" para capturar reservas

## Código de correção

### Passo 1: Adicionar anotação no RecursoController.POST

```java
// RecursoController.java - método criarRecurso()

@PostMapping
@PreAuthorize("hasRole('ADMINISTRADOR')")  // ← ADICIONAR
public ResponseEntity<RecursoResponseDTO> criarRecurso(
    @Valid @RequestBody RecursoRequestDTO dto,
    @AuthenticationPrincipal Jwt jwt) {  // ← ADICIONAR
    
    if (jwt == null) {
        throw new RegraNegocioException("Autenticação obrigatória");
    }
    
    // ... resto do código
}
```

### Passo 2: Testes de validação

```java
@Test
void anonimoCriaRecursoExpecta401() throws Exception {
    RecursoRequestDTO request = new RecursoRequestDTO();
    request.setNome("Sala Hack");
    request.setTipo("SALA");
    
    mockMvc.perform(post("/recursos")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());  // 401
}

@Test
void administradorCriaRecursoComSucesso() throws Exception {
    RecursoRequestDTO request = new RecursoRequestDTO();
    request.setNome("Sala Legítima");
    request.setTipo("SALA");
    
    mockMvc.perform(post("/recursos")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .with(jwtContexto(administrador.getId(), "ADMINISTRADOR")))
        .andExpect(status().isCreated());  // 201
}
```

## Checklist de implementação

- [ ] Adicionar `@PreAuthorize("hasRole('ADMINISTRADOR')")`
- [ ] Adicionar `@AuthenticationPrincipal Jwt jwt` no método
- [ ] Validar que jwt != null e extrair user claims
- [ ] Testes passando (testarCriarRecursoDesprotegido[0])
- [ ] Rodar suite completa (mvn test)
- [ ] Commit com referência a este defeito

## Referências

- Matriz de autorização: [docs/seguranca/matriz-autorizacao.md](../seguranca/matriz-autorizacao.md#recursos---gerenciamento)
- Análise de divergências: [docs/seguranca/DIVERGENCIAS.md](../seguranca/DIVERGENCIAS.md)
- Teste parametrizado: [src/test/java/br/edu/organizacaorecursos/security/MatrizAutorizacaoTest.java:165](../../src/test/java/br/edu/organizacaorecursos/security/MatrizAutorizacaoTest.java)
