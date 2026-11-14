# Relatório de Estado — SonarCloud

**Data:** 2026-11-14
**Estado:** **PASS** para o commit `6106cc5` no PR #11. A análise está vinculada ao pull request e deve ser conferida novamente para cada commit posterior.

O workflow [.github/workflows/ci.yml](../../.github/workflows/ci.yml) executa Maven verify antes do scanner e exige `SONAR_TOKEN`, `SONAR_ORGANIZATION` e `SONAR_PROJECT_KEY`. A configuração importa `target/site/jacoco/jacoco.xml` e aguarda o Quality Gate; a execução da branch publicada está registrada abaixo.

| Evidência | Estado |
|---|---|
| Configuração do scanner no CI | Existe |
| Revisão | [PR #11](https://github.com/gustavao578/Organiza-o-de-Recursos---QS-projeto-final/pull/11), commit `6106cc5` |
| Análise | [SonarCloud Code Analysis — PASS](https://sonarcloud.io/dashboard?id=gustavao578_Organiza-o-de-Recursos---QS-projeto-final&pullRequest=11) |
| Java CI | [Run 43537064733 — PASS](https://github.com/gustavao578/Organiza-o-de-Recursos---QS-projeto-final/actions/runs/43537064733) |

O estado acima comprova a análise de `6106cc5`; não representa uma aprovação de revisão por pares. Use o link do PR para verificar o Quality Gate do head atual após novos pushes.
