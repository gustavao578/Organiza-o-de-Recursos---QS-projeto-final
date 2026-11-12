# DEF-006: GET /usuarios/{id} sem autenticação obrigatória

**Data**: 2026-11-12  
**Status**: 🔴 ABERTO  
**Severidade**: ALTA  
**Categoria**: Exposição de dados sensíveis  

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
