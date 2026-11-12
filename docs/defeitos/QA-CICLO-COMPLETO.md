# Ciclo de QA - Testes Parametrizados de Autorização

**Data**: 2026-11-12  
**Executor**: Engenheiro de Qualidade (QA)  
**Artefato**: MatrizAutorizacaoTest.java + Defeitos documentados  

## Status da Suite de Testes

```
✅ Total: 38 testes
✅ Passando: 14 testes
⚠️ Falhando: 24 testes (ESPERADO - documentados em 9 DEFs)
⚠️ Erros: 2 (Display name pattern - será corrigido)
```

## Testes Passando (14 casos) ✅

| Grupo | Testes | Status |
|-------|--------|--------|
| **Autenticação Correta** | Anônimo recebe 401 em endpoints privados | ✅ |
| **GET /reservas** | Solicitante/Responsável/Admin têm acesso | ✅ |
| **Validação de Entrada** | Campos ausentes → 400 | ✅ |
| **ProblemDetail** | Respostas de erro sem stack trace | ✅ |

## Testes Falhando (24 casos) ⚠️

| Defeito | Método | Esperado | Atual | Casos |
|---------|--------|----------|-------|-------|
| DEF-001 | POST /recursos | 401 | 201 | 3 |
| DEF-002 | PUT /recursos | 401 | 200 | 3 |
| DEF-003 | DELETE /recursos | 401 | 204 | 3 |
| DEF-004 | GET /usuarios | 401 | 200 | 1 |
| DEF-005 | DELETE /usuarios | 401 | 204 | 3 |
| DEF-006 | GET /usuarios/{id} | 401 | 200 | 1 |
| DEF-007 | POST /manutencoes | 401 | 201 | 2 |
| DEF-008 | GET /manutencoes | 401/403 | 200 | 2 |
| DEF-009 | POST /reservas | 403 | 201 | 2 |

## Plano de Correção

Cada falhanda será resolvido em commit próprio:

1. **DEF-001**: Adicionar `@PreAuthorize("hasRole('ADMINISTRADOR')")` em POST /recursos
2. **DEF-002**: Adicionar `@PreAuthorize("hasRole('ADMINISTRADOR')")` em PUT /recursos/{id}
3. **DEF-003**: Adicionar `@PreAuthorize("hasRole('ADMINISTRADOR')")` em DELETE /recursos/{id}
4. **DEF-004**: Adicionar `@PreAuthorize("isAuthenticated()")` em GET /usuarios
5. **DEF-005**: Adicionar `@PreAuthorize("hasRole('ADMINISTRADOR')")` em DELETE /usuarios/{id}
6. **DEF-006**: Adicionar `@PreAuthorize("isAuthenticated()")` em GET /usuarios/{id}
7. **DEF-007**: Adicionar `@PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('RESPONSAVEL')")` em POST /manutencoes
8. **DEF-008**: Adicionar `@PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('RESPONSAVEL')")` em GET /manutencoes
9. **DEF-009**: Mudar para `@PreAuthorize("hasRole('SOLICITANTE')")` em POST /reservas

## Validações Implementadas

✅ **Cada teste valida:**
- Status HTTP esperado
- Regra de posse (Solicitante não altera reserva de outro)
- Validação de entrada (campos obrigatórios, tipos corretos)
- Conformidade com ProblemDetail (sem stack trace)

## Próximas Ações

1. ✅ Documentar cada defeito em `docs/defeitos/DEF-NNN.md`
2. ✅ Criar commits de documentação
3. ⏳ **PRÓXIMO**: Implementar correções em código (1 por vez)
4. ⏳ Re-executar suite após cada correção
5. ⏳ Marcar defito como RESOLVIDO → FECHADO
6. ⏳ Final: Confirmar que todos 38 testes passam

## Rastreamento por Defeito

| DEF | Arquivo | Commits |
|-----|---------|---------|
| 001 | DEF-001-RECURSO-SEM-AUTENTICACAO-POST.md | 8d5bbb2 |
| 002 | DEF-002-RECURSO-SEM-AUTENTICACAO-PUT.md | b40ad25 |
| 003 | DEF-003-RECURSO-SEM-AUTENTICACAO-DELETE.md | 1eb674f |
| 004 | DEF-004-USUARIOS-LISTAGEM-DESPROTEGIDA.md | df4c494 |
| 005 | DEF-005-USUARIOS-DELETE-DESPROTEGIDO.md | fc30ad7 |
| 006 | DEF-006-USUARIOS-GET-DESPROTEGIDO.md | 47f88ad |
| 007 | DEF-007-MANUTENCAO-POST-DESPROTEGIDA.md | 1633390 |
| 008 | DEF-008-MANUTENCAO-GET-DESPROTEGIDA.md | 85a4bad |
| 009 | DEF-009-RESERVA-POST-AUTORIZACAO-INCORRETA.md | f441f8a |
| README | docs/defeitos/README.md | 0e4fa0e |
