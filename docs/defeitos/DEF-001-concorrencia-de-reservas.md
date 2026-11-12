# DEF-001: Reservas concorrentes na mesma agenda

**Status:** ✅ Fechado
**Severidade:** Alta
**Fechado por**: PR #8 - fix(epic-02): secure reservation SPA and API  
**Merged em**: 2026-11-12T12:14:52-03:00  
**Commit**: 58411ac0904a248e1ccae74094f775e8611c3cf8

## Reprodução

1. Inicie o teste `ReservaConcorrenciaPostgresIntegrationTest` com Docker disponível.
2. Dispare 20 `POST /reservas` simultâneos para a mesma sala e intervalo, usando 20 solicitantes.
3. Repita com 20 salas diferentes associadas ao mesmo responsável e o mesmo intervalo.
4. Consulte `tb_reservas` por recurso/intervalo e por `responsavel_id`/intervalo.

Comando usado no dev container: `./mvnw -Dtest=ReservaConcorrenciaPostgresIntegrationTest test`.

## Evidência antes da correção

O teste JUnit repetiu os dois cenários cinco vezes em PostgreSQL 16 via Testcontainers:

| Cenário | Respostas observadas em cada ciclo | Linhas conflitantes consultadas |
|---|---|---:|
| Mesma sala | 1 × 201 e 19 × 400 | 1 |
| Mesmo responsável, salas distintas | 20 × 201 | 20 |

O teste já mantinha a sala serializada pelo lock pessimista existente, mas o handler devolvia 400 em vez de 409. Para a agenda do responsável, a verificação limitada a `recurso_id` permitia persistir as 20 reservas sobrepostas.

## Causa e correção

A criação verificava conflitos apenas no recurso solicitado e não serializava transações de recursos distintos pertencentes ao mesmo responsável. A exceção de conflito também era convertida para HTTP 400.

A correção mantém o lock do recurso, adquire lock pessimista no usuário responsável, consulta reservas aprovadas/em uso em todos os recursos desse responsável e retorna HTTP 409 para conflito. A aprovação de reservas pendentes aplica a mesma regra.