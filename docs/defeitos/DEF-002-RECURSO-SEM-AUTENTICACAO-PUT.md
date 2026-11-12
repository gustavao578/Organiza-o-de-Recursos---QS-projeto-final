# DEF-002: PUT /recursos sem autenticação obrigatória

**Data**: 2026-11-12  
**Status**: ✅ FECHADO  
**Severidade**: CRÍTICA  
**Categoria**: Falta de autenticação  
**Fechado por**: PR #8 - fix(epic-02): secure reservation SPA and API  
**Merged em**: 2026-11-12T12:14:52-03:00  
**Commit**: 58411ac0904a248e1ccae74094f775e8611c3cf8  

## Resumo

Endpoint `PUT /recursos/{id}` permite qualquer usuário anônimo atualizar recursos.

| Perfil | Esperado | Atual | Status |
|--------|----------|-------|--------|
| Anônimo | 401 | 200 | ⚠️ FALHA |
| Solicitante | 401 | 200 | ⚠️ FALHA |
| Responsável | 401 | 200 | ⚠️ FALHA |
| Administrador | 200 | 200 | ✅ OK |

## Teste falhando

`testarAtualizarRecursoDesprotegido[1]` - Anônimo deveria receber 401

## Código de correção

```java
@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public ResponseEntity<RecursoResponseDTO> atualizarRecurso(
    @PathVariable UUID id,
    @Valid @RequestBody RecursoRequestDTO dto,
    @AuthenticationPrincipal Jwt jwt) {
    
    if (jwt == null) {
        throw new RegraNegocioException("Autenticação obrigatória");
    }
    // ... resto do código
}
```

## Referência

- Teste: [MatrizAutorizacaoTest:220](../../src/test/java/br/edu/organizacaorecursos/security/MatrizAutorizacaoTest.java)
- Matriz: [matriz-autorizacao.md#recursos](../seguranca/matriz-autorizacao.md)
