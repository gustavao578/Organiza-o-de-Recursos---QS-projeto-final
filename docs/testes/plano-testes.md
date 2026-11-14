# Plano de Testes

**Escopo:** validação da aplicação de Organização de Recursos após as correções de isolamento de perfis, configuração Flyway/PostgreSQL e login local.
**Execução de referência:** `./mvnw -B verify`.

## Estratégia

| Nível | Objetivo | Ferramentas / evidência |
|---|---|---|
| Unidade | Validar regras de domínio, DTOs, serviços e tratamento de erros | JUnit 5, Mockito e `src/test/java/` |
| API / integração | Verificar HTTP, autorização, persistência e perfis | Spring Test/MockMvc, H2 isolado e `src/test/resources/application-local.properties` |
| Banco PostgreSQL | Exercitar migrações e restrições que dependem do PostgreSQL | Testcontainers, Flyway e testes `*Postgres*IntegrationTest` |
| BDD | Executar fluxos de aprovação, manutenção e estado | Cucumber JUnit Platform e `src/test/resources/features/aprovacao-bloqueio.feature` |
| Frontend | Verificar sintaxe e lógica JavaScript automatizada existente | `node --check` e testes em `src/test/js/` |
| Cobertura | Medir linhas/branches e aplicar limites Maven | JaCoCo em `target/site/jacoco/`; limites definidos no `pom.xml` |
| Carga | Medir leitura/criação local, sem extrapolar para produção | JMeter CLI e `perf/reservas.jmx`; plano em [performance/plano-01.md](../performance/plano-01.md) |

## Execução

```bash
./mvnw -B verify
```

O workflow [.github/workflows/ci.yml](../../.github/workflows/ci.yml) usa Temurin Java 21. A execução local deve registrar Java/Maven efetivamente usados, contagem de testes, falhas/erros, JaCoCo e limitações. Os testes H2 recebem nome aleatório por execução para evitar contaminação por estado persistido. Os cenários Testcontainers precisam de Docker disponível.

## Critérios de saída

- `BUILD SUCCESS` e nenhuma falha/erro/skip inesperado em JUnit/Cucumber.
- Limites Maven JaCoCo satisfeitos (≥80% linhas e ≥70% branches).
- Registrar separadamente falhas de infraestrutura, análise SonarCloud, revisão e validação de carga; sucesso local não as substitui.
- Não declarar RNF-03 aprovado enquanto o perfil de carga e limites não forem ratificados e medidos em banco/ambiente representativo.

## Limitações conhecidas

Esta estratégia não demonstra testes E2E visuais, acessibilidade, DAST, desempenho em staging/produção nem integração com um provedor real de identidade. Esses itens são apontados no [relatório de execução](relatorio-testes.md) e na [RTM](../rtm.md).
