# DEF-009: POST /reservas com autorização incorreta

**Data**: 2026-11-12  
**Status**: ✅ FECHADO  
**Severidade**: ALTA  
**Categoria**: Controle de acesso incorreto  
**Fechado por**: PR #8 - fix(epic-02): secure reservation SPA and API  
**Merged em**: 2026-11-12T12:14:52-03:00  
**Commit**: 58411ac0904a248e1ccae74094f775e8611c3cf8  

## Resumo

Endpoint `POST /reservas` permite Responsável e Administrador criar reservas. Esperado: apenas Solicitante.

| Perfil | Esperado | Atual | Status |
|--------|----------|-------|--------|
| Anônimo | 401 | 401 | ✅ OK |
| Solicitante | 201 | 201 | ✅ OK |
| Responsável | 403 | 201 | ⚠️ FALHA |
| Administrador | 403 | 201 | ⚠️ FALHA |

## Impacto

- Responsável cria reserva em nome próprio (deveria apenas aprovar recursos restritos)
- Administrador cria reserva em nome próprio (deveria apenas administrar)
- Violação da separação de papéis
- Responsável/Admin podem monopolizar recursos

## Cenário de ataque

```bash
# Responsável cria reserva (deveria ser bloqueado)
curl -X POST http://localhost:8080/reservas \
  -H "Authorization: Bearer token-responsavel" \
  -H "Content-Type: application/json" \
  -d '{
    "recursoId": "550e8400-e29b-41d4-a716-446655440000",
    "dataInicio": "2026-11-15T10:00:00",
    "dataFim": "2026-11-15T11:00:00"
  }'

# Retorna 201 Created (deveria ser 403 Forbidden)
```

## Código de correção

```java
@PostMapping
@PreAuthorize("hasRole('SOLICITANTE')")  // APENAS Solicitante
public ResponseEntity<ReservaResponseDTO> criarReserva(
    @Valid @RequestBody ReservaRequestDTO dto,
    @AuthenticationPrincipal Jwt jwt) {
    
    String perfil = jwt.getClaimAsString("profile");
    if (!"SOLICITANTE".equals(perfil)) {
        throw new AccessDeniedException("Apenas solicitantes podem criar reservas");
    }
    
    // ... resto do código
}
```

## Teste

`testarCriarReservaAutorizacao[2]` (Responsável) e `[3]` (Admin) - Devem receber 403
