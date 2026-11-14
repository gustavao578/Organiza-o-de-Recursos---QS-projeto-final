# Relatório JaCoCo — Execução Local

**Data:** 2026-11-14

**Comando:** `./mvnw -B verify` com OpenJDK 21.0.13
**Escopo:** branch local `fix/verify-profile-isolation`, base `01bd469` acrescida das correções não publicadas na data do relatório.

| Métrica | Cobertos | Total | Cobertura | Limite Maven | Resultado |
|---|---:|---:|---:|---:|---|
| Linhas | 706 | 758 | 93,14% | 80% | Passou |
| Branches | 228 | 297 | 76,77% | 70% | Passou |
| Instruções | 3.127 | 3.391 | 92,21% | — | Informativo |

O HTML está em `target/site/jacoco/index.html`; a entrada XML consumida pelo SonarCloud é `target/site/jacoco/jacoco.xml`. Esses arquivos são gerados pelo build e não versionados. O workflow publica `target/site/jacoco/` como artifact `jacoco-report` mesmo quando uma etapa posterior falha.

Os contadores foram confirmados em `target/site/jacoco/jacoco.xml` após a execução completa: 235 testes, sem falhas, erros ou skips. Esta é uma medição local, não um artifact de CI nem confirmação de Quality Gate. Para a execução completa, veja [relatório de testes](../testes/relatorio-testes.md).
