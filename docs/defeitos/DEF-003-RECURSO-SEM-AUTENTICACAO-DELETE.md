# DEF-003: DELETE /recursos sem autenticação obrigatória

**Data**: 2026-11-12  
**Status**: 🔴 ABERTO  
**Severidade**: CRÍTICA  
**Categoria**: Falta de autenticação  

## Resumo

Endpoint `DELETE /recursos/{id}` permite qualquer usuário anônimo deletar recursos.

| Perfil | Esperado | Atual | Status |
|--------|----------|-------|--------|
| Anônimo | 401 | 204 | ⚠️ FALHA |
| Solicitante | 401 | 204 | ⚠️ FALHA |
| Responsável | 401 | 204 | ⚠️ FALHA |
| Administrador | 204 | 204 | ✅ OK |

## Impacto

Qualquer pessoa pode deletar qualquer recurso do sistema sem se autenticar. Destruição completa de dados.

## Código de correção

```java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public ResponseEntity<Void> deletarRecurso(
    @PathVariable UUID id,
    @AuthenticationPrincipal Jwt jwt) {
    
    if (jwt == null) {
        throw new RegraNegocioException("Autenticação obrigatória");
    }
    recursoRepository.deleteById(id);
    return ResponseEntity.noContent().build();
}
```

## Teste

`testarDeletarRecursoDesprotegido[0]` - Anônimo deveria receber 401
