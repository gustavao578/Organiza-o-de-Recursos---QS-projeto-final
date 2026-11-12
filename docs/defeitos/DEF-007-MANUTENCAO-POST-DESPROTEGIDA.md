# DEF-007: POST /manutencoes sem autenticação obrigatória

**Data**: 2026-11-12  
**Status**: 🔴 ABERTO  
**Severidade**: CRÍTICA  
**Categoria**: Falta de autenticação + Negação de serviço  

## Resumo

Endpoint `POST /manutencoes` permite qualquer usuário anônimo criar manutenções.

| Perfil | Esperado | Atual | Status |
|--------|----------|-------|--------|
| Anônimo | 401 | 201 | ⚠️ FALHA |
| Solicitante | 401 | 201 | ⚠️ FALHA |
| Responsável | 201 | 201 | ✅ OK |
| Administrador | 201 | 201 | ✅ OK |

## Impacto

**Crítico - Denial of Service (DoS):**
- Atacante cria manutenção indefinida em um recurso
- Recurso fica bloqueado para todos os usuários
- Ninguém consegue fazer reserva no recurso
- Sem forma de identificar quem criou a manutenção (sem autenticação)

## Cenário de ataque

```bash
curl -X POST http://localhost:8080/manutencoes \
  -H "Content-Type: application/json" \
  -d '{
    "recursoId": "550e8400-e29b-41d4-a716-446655440000",
    "dataInicio": "2026-11-12T00:00:00",
    "dataFim": "2099-12-31T23:59:59"  # 73 anos de manutenção!
  }'

# Retorna 201 Created (deveria ser 401)
# Recurso fica inutilizável
```

## Código de correção

```java
@PostMapping
@PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('RESPONSAVEL')")
public ResponseEntity<ManutencaoResponseDTO> criarManutencao(
    @Valid @RequestBody ManutencaoRequestDTO dto,
    @AuthenticationPrincipal Jwt jwt) {
    
    if (jwt == null) {
        throw new RegraNegocioException("Autenticação obrigatória");
    }
    // ... resto do código
}
```

## Teste

`testarCriarManutencaoDesprotegido[0]` - Anônimo deveria receber 401
