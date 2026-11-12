# DEF-005: DELETE /usuarios sem autenticação obrigatória

**Data**: 2026-11-12  
**Status**: ✅ FECHADO  
**Severidade**: CRÍTICA  
**Categoria**: Falta de autenticação  
**Fechado por**: PR #8 - fix(epic-02): secure reservation SPA and API  
**Merged em**: 2026-11-12T12:14:52-03:00  
**Commit**: 58411ac0904a248e1ccae74094f775e8611c3cf8  

## Resumo

Endpoint `DELETE /usuarios/{id}` permite qualquer usuário anônimo deletar usuários do sistema.

| Perfil | Esperado | Atual | Status |
|--------|----------|-------|--------|
| Anônimo | 401 | 204 | ⚠️ FALHA |
| Solicitante | 403 | 204 | ⚠️ FALHA |
| Responsável | 403 | 204 | ⚠️ FALHA |
| Administrador | 204 | 204 | ✅ OK |

## Impacto

- Qualquer pessoa deleta contas de usuários
- Destruição de dados de usuários
- Impossibilidade de rastrear quem deletou o quê

## Código de correção

```java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public ResponseEntity<Void> deletarUsuario(
    @PathVariable UUID id,
    @AuthenticationPrincipal Jwt jwt) {
    
    if (jwt == null) {
        throw new RegraNegocioException("Autenticação obrigatória");
    }
    usuarioRepository.deleteById(id);
    return ResponseEntity.noContent().build();
}
```

## Teste

`testarDeletarUsuarioDesprotegido` - Testa todas as permutações
