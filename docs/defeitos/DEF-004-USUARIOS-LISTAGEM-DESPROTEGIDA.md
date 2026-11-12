# DEF-004: GET /usuarios sem autenticação obrigatória

**Data**: 2026-11-12  
**Status**: ✅ FECHADO  
**Severidade**: ALTA  
**Categoria**: Exposição de dados sensíveis  
**Fechado por**: PR #8 - fix(epic-02): secure reservation SPA and API  
**Merged em**: 2026-11-12T12:14:52-03:00  
**Commit**: 58411ac0904a248e1ccae74094f775e8611c3cf8  

## Resumo

Endpoint `GET /usuarios` expõe lista de todos os usuários sem autenticação.

| Perfil | Esperado | Atual | Status |
|--------|----------|-------|--------|
| Anônimo | 401 | 200 | ⚠️ FALHA |
| Solicitante | 200 | 200 | ✅ OK |
| Responsável | 200 | 200 | ✅ OK |
| Administrador | 200 | 200 | ✅ OK |

## Impacto

**Exposição de dados sensíveis:**
- Nomes completos de todos os usuários
- Emails de todos os usuários  
- Matrículas de todos os usuários
- Perfis/roles de todos os usuários

Atacante pode:
1. Enumerar usuários para social engineering
2. Descobrir emails para phishing
3. Identificar administradores
4. Planejar ataques direcionados

## Código de correção

```java
@GetMapping
@PreAuthorize("isAuthenticated()")  // Exigir autenticação
public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
    return ResponseEntity.ok(usuarioService.listar());
}
```

## Teste

`testarListarUsuariosDesprotegido[0]` - Anônimo deveria receber 401
