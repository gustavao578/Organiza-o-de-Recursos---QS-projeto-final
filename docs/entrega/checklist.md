# Checklist de Entrega

**Atualizado:** 2026-11-14

**Revisão publicada:** branch `fix/verify-profile-isolation`, commit `6106cc5` no [PR #11](https://github.com/gustavao578/Organiza-o-de-Recursos---QS-projeto-final/pull/11).

**Resultado local (2026-11-14, OpenJDK 21.0.13):** `./mvnw -B verify` — 235 testes, 0 falhas, 0 erros, 0 ignorados; JaCoCo acima dos limites configurados. CI e SonarCloud também passaram para `6106cc5`; revisão humana permanece pendente.

| Item | Existe | Atualizado | Caminho / evidência |
|---|---|---|---|
| Repositório com revisão por pares | Parcial — PR #11 aberto; revisão humana da alteração atual não ocorreu | Sim — PR publicado | [PR #11](https://github.com/gustavao578/Organiza-o-de-Recursos---QS-projeto-final/pull/11); decisão de revisão ainda pendente |
| Aplicação executável | Sim | Sim — smoke em `demo`: `/` e `/v3/api-docs` HTTP 200; perfil default `local`: `/` e login local HTTP 200 | `README.md`; `./mvnw spring-boot:run` |
| README de instalação, execução e login local | Sim | Sim | `README.md` |
| Documentação da API | Sim | Sim — OpenAPI e Swagger são servidos pela aplicação | `/v3/api-docs`, `/swagger-ui/index.html`; `README.md` |
| Matriz de rastreabilidade | Sim | Sim — RF/RNF/RC e limitações explícitas | `docs/rtm.md` |
| Diagrama de contexto | Sim | Sim | `docs/arquitetura/diagramas.md` |
| Diagrama de componentes | Sim | Sim | `docs/arquitetura/diagramas.md` |
| Diagramas de sequências críticas | Sim | Sim — criação de reserva/conflito e login local/demo | `docs/arquitetura/diagramas.md` |
| Plano e relatório de testes | Sim | Sim — inclui resultado local e limites de ambiente | `docs/testes/plano-testes.md`; `docs/testes/relatorio-testes.md` |
| Pipeline de CI | Sim | Sim — Java CI passou no commit `6106cc5` | `.github/workflows/ci.yml`; [run 43537064733](https://github.com/gustavao578/Organiza-o-de-Recursos---QS-projeto-final/actions/runs/43537064733) |
| Relatório / Quality Gate SonarCloud | Sim | Sim — Code Analysis passou no commit `6106cc5` | `docs/qualidade/sonarcloud.md`; [Quality Gate do PR #11](https://sonarcloud.io/dashboard?id=gustavao578_Organiza-o-de-Recursos---QS-projeto-final&pullRequest=11) |
| Relatório JaCoCo | Sim — relatório local e artifact de CI disponíveis | Sim — atualizado após verify em Java 21 | `docs/qualidade/relatorio-jacoco.md`; artifact [`jacoco-report`](https://github.com/gustavao578/Organiza-o-de-Recursos---QS-projeto-final/actions/runs/43537064733) |
| Plano e resultado de teste de carga | Sim | Sim — plano e relatório local atualizados; evidência não valida PostgreSQL/staging | `docs/performance/plano-01.md`; `docs/performance/resultado-01.md`; `docs/performance/resultado-02.md` |
| Registro de defeitos | Sim | Sim — DEF-002 de sobreposição confirmado resolvido; auditorias antigas marcadas como históricas | `docs/defeitos/README.md`; `docs/defeitos/DEF-002-sobreposicao-pendentes.md` |
| Relatório final de qualidade | Sim | Sim — resumo atual no início e snapshot antigo identificado como histórico | `docs/relatorio-qualidade.md`; `docs/testes/relatorio-testes.md` |

## Bloqueadores pendentes, em ordem

1. **Revisão por pares da alteração atual:** PR #11 está aberto, mas ainda não há aprovação humana; o gate anterior da story 2.2 em estado `CONCERNS` não equivale a essa revisão.
2. **RNF-03 sem aprovação representativa:** o resultado local em H2 usa `GET /reservas` como proxy; faltam confirmação dos limites e medição em PostgreSQL/staging.

CI e Quality Gate SonarCloud passaram para `6106cc5`. Consulte o PR #11 para conferir checks dos commits posteriores; não marque revisão humana ou RNF-03 como concluídos sem evidência correspondente.

## Correções dos erros de `./mvnw -B verify`

Na auditoria inicial houve 235 testes, 11 erros, 0 falhas e 0 ignorados: seis contextos sem `DataInitializer`, quatro violações de integridade referencial no teste de login e um contexto PostgreSQL sem tabela de conflitos. As correções estão aplicadas em código e passaram na verificação local: perfil `local` default; H2 randômico/isolation no teste; `AuthLoginIntegrationTest` com perfis e DB explícitos; Flyway habilitado em `prod`, sem desativação global. O relatório detalhado está em [relatorio-testes.md](../testes/relatorio-testes.md).
