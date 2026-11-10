# Evidência TDD — Aprovação e bloqueio

## Execução

As linhas abaixo registram os resultados históricos dos ciclos RED/GREEN/REFACTOR quando foram executados; não devem ser interpretadas como a contagem atual nem como a presença do estado `SOLICITADA` no fluxo de criação atual.

**Snapshot atual (2026-11-10):** o projeto contém Maven Wrapper e o comando `./mvnw -B verify` passou com 92 testes, zero falhas/erros, 87% de cobertura de linhas e 71% de branches. O run CI `42859680966` no commit `eb2bced` passou por Maven verify, SonarCloud e publicação do relatório JaCoCo. As novas reservas restritas nascem `PENDENTE`; os registros abaixo preservam os nomes/status dos ciclos históricos.

| Cenário BDD | Commit red | Commit green | Commit refactor | Trecho da saída do teste |
|---|---|---|---|---|
| BDD-APR-01 — solicitação restrita fica pendente | `b534146` | `5980602` | `156b728` | RED: `40 testes, 1 falha, 0 erros`; `No value at JSON path "$.status"`. GREEN/refactor: `40 testes, 0 falhas, 0 erros`. |
| BDD-APR-02 — responsável aprova solicitação | `65bbd4d` | `8594f9d` | `2b52fa2` | RED: `41 testes, 1 falha, 0 erros`; `Status expected:<200> but was:<404>`. GREEN/refactor: `41 testes, 0 falhas, 0 erros`. |
| BDD-APR-03 — solicitante não aprova a própria reserva | `0d4452b` | `a2dd4f7` | `230004e` | RED: `42 testes, 1 falha, 0 erros`; `Status expected:<400> but was:<200>`. GREEN/refactor: `42 testes, 0 falhas, 0 erros`. |
| BDD-APR-04 — ator não responsável não aprova | N/A: comportamento já coberto pela autorização de BDD-APR-02 | `8594f9d` | N/A: nenhuma mudança de comportamento necessária | Teste `e250298`: `43 testes, 0 falhas, 0 erros`; naquele ciclo histórico, a reserva permaneceu `SOLICITADA`. |
| BDD-MAN-01 — três fronteiras da manutenção | N/A: a regra existente já produzia os resultados candidatos | N/A: nenhuma mudança de comportamento necessária | `8c2dbb1` | Teste parametrizado `944fc3d`: termina no início (`201`), começa no fim (`201`) e cruza um segundo (`400`); `46 testes, 0 falhas, 0 erros`. Após injetar `Clock`: `53 testes, 0 falhas, 0 erros`. |
| BDD-EST-01 — fluxo principal | `735b200` (APROVADA→EM_USO); `a24d78a` (EM_USO→CONCLUÍDA) | `a712c3f`; `e7ae8d8` | `422888d`; `f183472` | RED: endpoint ausente, `404`, sem erros de compilação. GREEN/refactor: primeiro `47`, depois `48` testes; ambos com `0 falhas, 0 erros`. SOLICITADA→APROVADA é coberto por BDD-APR-02. |
| BDD-EST-02 — estados alternativos propostos | `0e9f629` | `30338c0` | `451206a` | RED: as três variantes receberam `404`; `52 testes, 3 falhas, 0 erros`. GREEN/refactor: `52 testes, 0 falhas, 0 erros`. |
| BDD-EST-03 — transição inválida preserva estado | `dcef1d9` (REJEITADA→APROVADA) | `16ac2fb` | `38be66f` | O salto SOLICITADA→EM_USO já estava bloqueado: teste `618ad43`, `49 testes, 0 falhas, 0 erros`. RED reaprovação: `53 testes, 1 falha, 0 erros`; `Status expected:<400> but was:<200>`. GREEN/refactor: `53 testes, 0 falhas, 0 erros`. |

## Desvios do ciclo estrito

- BDD-APR-04, BDD-MAN-01 e o exemplo SOLICITADA→EM_USO de BDD-EST-03 já passavam antes de um teste específico. Não foi removido comportamento existente para fabricar uma falha vermelha.
- A transição REJEITADA→APROVADA revelou uma lacuna real: a API retornava `200`. O teste red correspondente foi adicionado e a regra passou a exigir origem SOLICITADA.
- Os estados alternativos e os atores associados são os candidatos registrados no BDD. O PRD ainda não define formalmente essas origens/permissões; precisam de confirmação do produto.
- A consulta de manutenção é uma comparação pura de intervalos e não precisa do horário atual. O `Clock` é fornecido por DI e usado pelos testes para construir períodos futuros; não há `Instant.now()` oculto nem `Thread.sleep` nesses cenários.