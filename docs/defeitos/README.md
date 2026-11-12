# Defeitos de Segurança - Matriz de Autorização

**Data**: 2026-11-12  
**Total de Defeitos**: 9  
**Status das correções**: implementadas localmente; aguardando revisão e merge do PR.

## Resumo Executivo

Análise de segurança da API REST identificou **9 divergências graves** entre a matriz de autorização documentada e o código implementado:

- **4 CRÍTICAS**: Falta total de autenticação → Qualquer pessoa acessa
- **5 ALTAS**: Falha de autorização → Usuários não-autorizado acessam dados/operações

## Distribuição por Categoria

| Categoria | Quantidade | Endpoints Afetados |
|-----------|-----------|------------------|
| Autenticação (sem @PreAuthorize) | 6 | Recursos (3), Usuários (2), Manutenções (1) |
| Autorização (perfil incorreto) | 2 | Manutenções (1), Reservas (1) |
| Exposição de dados sensíveis | 1 | Usuários (1) |

## Matriz de Defeitos

| ID | Endpoint | Método | Severidade | Status | Impacto |
|----|----------|--------|-----------|--------|---------|
| [DEF-001](./DEF-001-RECURSO-SEM-AUTENTICACAO-POST.md) | /recursos | POST | CRÍTICA | ✅ CORRIGIDO* | Criar recurso sem autenticação |
| [DEF-002](./DEF-002-RECURSO-SEM-AUTENTICACAO-PUT.md) | /recursos/{id} | PUT | CRÍTICA | ✅ CORRIGIDO* | Alterar qualquer recurso |
| [DEF-003](./DEF-003-RECURSO-SEM-AUTENTICACAO-DELETE.md) | /recursos/{id} | DELETE | CRÍTICA | ✅ CORRIGIDO* | Deletar qualquer recurso |
| [DEF-004](./DEF-004-USUARIOS-LISTAGEM-DESPROTEGIDA.md) | /usuarios | GET | ALTA | ✅ CORRIGIDO* | Expor lista de usuários |
| [DEF-005](./DEF-005-USUARIOS-DELETE-DESPROTEGIDO.md) | /usuarios/{id} | DELETE | CRÍTICA | ✅ CORRIGIDO* | Deletar qualquer usuário |
| [DEF-006](./DEF-006-USUARIOS-GET-DESPROTEGIDO.md) | /usuarios/{id} | GET | ALTA | ✅ CORRIGIDO* | Expor dados pessoais |
| [DEF-007](./DEF-007-MANUTENCAO-POST-DESPROTEGIDA.md) | /manutencoes | POST | CRÍTICA | ✅ CORRIGIDO* | DoS: manutenção indefinida |
| [DEF-008](./DEF-008-MANUTENCAO-GET-DESPROTEGIDA.md) | /manutencoes | GET | ALTA | ✅ CORRIGIDO* | Expor cronograma de manutenção |
| [DEF-009](./DEF-009-RESERVA-POST-AUTORIZACAO-INCORRETA.md) | /reservas | POST | ALTA | ✅ CORRIGIDO* | Admin/Responsável criam reservas |

`* CORRIGIDO` indica implementação e testes locais; fechamento definitivo após merge.
O registro técnico consolidado está em [FIX-EPIC-02-SEGURANCA-SPA.md](./FIX-EPIC-02-SEGURANCA-SPA.md).

## Próximos Passos

1. Revisar a implementação e evidências no registro [FIX-EPIC-02-SEGURANCA-SPA.md](./FIX-EPIC-02-SEGURANCA-SPA.md).
2. Executar os gates do pull request e confirmar a aplicação da migração PostgreSQL no ambiente alvo.
3. Após merge, atualizar o ciclo de vida individual dos documentos DEF-* para FECHADO.

## Testes Associados

- Arquivo: [src/test/java/br/edu/organizacaorecursos/security/MatrizAutorizacaoTest.java](../../src/test/java/br/edu/organizacaorecursos/security/MatrizAutorizacaoTest.java)
- Casos da matriz: 44 casos parametrizados para os perfis e resultados esperados.
- O teste PostgreSQL de exclusion constraint executa contra `tb_reservas` criada pelo Hibernate, aplica a migração versionada e valida o 409 da API.
- `./mvnw -B verify`: 179 testes, 0 falhas, 0 erros, 0 ignorados; gate JaCoCo aprovado.

## Fonte de Verdade

- Matriz de Autorização: [docs/seguranca/matriz-autorizacao.md](../seguranca/matriz-autorizacao.md)
- Checkpoint de Análise: [Análise de segurança completa da API](../../.copilot/session-state/b6b6418c-54c2-44de-8615-e1cd4fbd77e2/checkpoints/001-an-lise-de-seguran-a-completa.md)

## Convenções

Cada arquivo de defeito contém:
- **Status**: Ciclo de vida (ABERTO → RESOLVENDO → REVIEWANDO → FECHADO)
- **Severidade**: CRÍTICA (impacto imediato) | ALTA (risco significativo) | MÉDIA
- **Tabela de comparação**: Esperado vs. Atual
- **Código de correção**: Pronto para copiar/colar
- **Teste**: Referência aos casos que falham
