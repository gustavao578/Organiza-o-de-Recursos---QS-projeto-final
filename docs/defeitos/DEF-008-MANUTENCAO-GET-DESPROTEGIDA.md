# DEF-008: GET /manutencoes sem autorização obrigatória

**Data**: 2026-11-12  
**Status**: 🔴 ABERTO  
**Severidade**: ALTA  
**Categoria**: Falta de autorização  

## Resumo

Endpoint `GET /manutencoes` expõe cronograma de manutenções sem verificar se usuário tem permissão.

| Perfil | Esperado | Atual | Status |
|--------|----------|-------|--------|
| Anônimo | 401 | 200 | ⚠️ FALHA |
| Solicitante | 403 | 200 | ⚠️ FALHA |
| Responsável | 200 | 200 | ✅ OK |
| Administrador | 200 | 200 | ✅ OK |

## Impacto

- Solicitante vê cronograma de manutenção (informação do Responsável)
- Anônimo vê cronograma completo
- Risco de engenharia social: atacante sabe quando sistema estará em manutenção

## Código de correção

```java
@GetMapping
@PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('RESPONSAVEL')")  // Apenas admin/responsável
public ResponseEntity<List<ManutencaoResponseDTO>> listarManutencoes() {
    return ResponseEntity.ok(manutencaoService.listar());
}
```

## Teste

`testarListarManutencoesDesprotegido` - Testa permutações de autorização
