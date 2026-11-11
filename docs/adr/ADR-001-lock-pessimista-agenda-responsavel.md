# ADR-001: Lock pessimista para agenda do responsável

**Status:** Aceito

## Contexto

Conflitos de agenda podem atravessar recursos distintos vinculados ao mesmo responsável. Uma exclusion constraint por `recurso_id` não cobre essa regra relacional; indexar a regra por responsável exigiria duplicar esse identificador na reserva e manter sua consistência.

## Decisão

Manter o lock pessimista do recurso e, quando há responsável, bloquear também a linha do usuário responsável durante a transação. Após adquirir o lock, consultar reservas `APROVADA` ou `EM_USO` em todos os recursos associados àquele responsável. Aplicar a mesma sequência ao aprovar uma reserva pendente ou atualizar uma reserva para um estado aprovado.

## Consequências

Transações para o mesmo responsável são serializadas, preservando a verificação de intervalo existente sem schema específico do PostgreSQL. O custo é serialização por responsável, aceitável para a agenda e restrita somente aos recursos que possuem responsável.