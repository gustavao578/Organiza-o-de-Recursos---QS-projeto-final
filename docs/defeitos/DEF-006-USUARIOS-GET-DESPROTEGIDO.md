# DEF-006: GET /usuarios/{id} sem autenticação obrigatória

**Data**: 2026-11-12  
**Status**: ✅ FECHADO  
**Severidade**: ALTA  
**Categoria**: Exposição de dados sensíveis  
**Fechado por**: PR #8 - fix(epic-02): secure reservation SPA and API  
**Merged em**: 2026-11-12T12:14:52-03:00  
**Commit**: 58411ac0904a248e1ccae74094f775e8611c3cf8  

## Resumo

Endpoint `GET /usuarios/{id}` expõe dados de usuário específico sem autenticação.

| Perfil | Esperado | Atual | Status |
|--------|----------|-------|--------|
| Anônimo | 401 | 200 | ⚠️ FALHA |
| Solicitante | 200 | 200 | ✅ OK |
| Responsável | 200 | 200 | ✅ OK |
| Administrador | 200 | 200 | ✅ OK |

## Impacto

Qualquer pessoa pode buscar informações de qualquer usuário:
- Email
- Matrícula
- Perfil/Role
- Dados pessoais

## Código de correção

```java
@GetMapping("/{id}")
@PreAuthorize("isAuthenticated()")  // Exigir autenticação
public ResponseEntity<UsuarioResponseDTO> buscarUsuario(@PathVariable UUID id) {
    return ResponseEntity.ok(usuarioService.buscar(id));
}
```

## Teste

`testarBuscarUsuarioDesprotegido[0]` - Anônimo deveria receber 401
