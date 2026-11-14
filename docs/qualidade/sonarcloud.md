# Relatório de Estado — SonarCloud

**Data:** 2026-11-14
**Estado da revisão local:** **PENDENTE — sem análise SonarCloud disponível para esta revisão.**

O workflow [.github/workflows/ci.yml](../../.github/workflows/ci.yml) executa Maven verify antes do scanner e exige `SONAR_TOKEN`, `SONAR_ORGANIZATION` e `SONAR_PROJECT_KEY`. A configuração importa `target/site/jacoco/jacoco.xml` e aguarda o Quality Gate. A branch com as correções de perfil/Flyway ainda precisa ser publicada e executada para produzir o resultado associado à revisão.

| Evidência | Estado |
|---|---|
| Configuração do scanner no CI | Existe |
| Relatório/Quality Gate para a revisão `fix/verify-profile-isolation` | Não disponível nesta execução |
| URL do projeto/rastreamento SonarCloud | Não fornecida pelos arquivos do repositório nem verificada remotamente |
| Conclusão de segurança/qualidade SonarCloud | Não declarada |

Não reutilize o Quality Gate de um commit histórico como resultado desta revisão. Após CI, registre aqui o SHA, URL da análise, estado do gate e data; preserve a evidência como artifact/link do run.
