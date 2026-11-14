# Checklist de Entrega

**Atualizado:** 2026-11-14

**Revisão em avaliação:** branch `fix/verify-profile-isolation`, base `01bd469` mais alterações locais ainda não publicadas.

**Resultado local (2026-11-14, OpenJDK 21.0.13):** `./mvnw -B verify` — 235 testes, 0 falhas, 0 erros, 0 ignorados; JaCoCo acima dos limites configurados. Isso não comprova CI, peer review ou Quality Gate remoto.

| Item | Existe | Atualizado | Caminho / evidência |
|---|---|---|---|
| Repositório com revisão por pares | Parcial — repositório e PRs anteriores existem; revisão da alteração atual não ocorreu | Não | Branch atual ainda sem PR/review |
| Aplicação executável | Sim | Sim — smoke em `demo`: `/` e `/v3/api-docs` HTTP 200; perfil default `local`: `/` e login local HTTP 200 | `README.md`; `./mvnw spring-boot:run` |
| README de instalação, execução e login local | Sim | Sim | `README.md` |
| Documentação da API | Sim | Sim — OpenAPI e Swagger são servidos pela aplicação | `/v3/api-docs`, `/swagger-ui/index.html`; `README.md` |
| Matriz de rastreabilidade | Sim | Sim — RF/RNF/RC e limitações explícitas | `docs/rtm.md` |
| Diagrama de contexto | Sim | Sim | `docs/arquitetura/diagramas.md` |
| Diagrama de componentes | Sim | Sim | `docs/arquitetura/diagramas.md` |
| Diagramas de sequências críticas | Sim | Sim — criação de reserva/conflito e login local/demo | `docs/arquitetura/diagramas.md` |
| Plano e relatório de testes | Sim | Sim — inclui resultado local e limites de ambiente | `docs/testes/plano-testes.md`; `docs/testes/relatorio-testes.md` |
| Pipeline de CI | Sim | Sim — definição conferida; execução verde no commit publicado pendente | `.github/workflows/ci.yml` |
| Relatório / Quality Gate SonarCloud | Parcial — há relatório de estado, sem resultado para esta revisão | Não | `docs/qualidade/sonarcloud.md` |
| Relatório JaCoCo | Sim — relatório local existe; artifact de CI desta revisão ainda não | Sim — atualizado após verify em Java 21 | `docs/qualidade/relatorio-jacoco.md`; `target/site/jacoco/` |
| Plano e resultado de teste de carga | Sim | Sim — plano e relatório local atualizados; evidência não valida PostgreSQL/staging | `docs/performance/plano-01.md`; `docs/performance/resultado-01.md`; `docs/performance/resultado-02.md` |
| Registro de defeitos | Sim | Sim — DEF-002 de sobreposição confirmado resolvido; auditorias antigas marcadas como históricas | `docs/defeitos/README.md`; `docs/defeitos/DEF-002-sobreposicao-pendentes.md` |
| Relatório final de qualidade | Sim | Sim — resumo atual no início e snapshot antigo identificado como histórico | `docs/relatorio-qualidade.md`; `docs/testes/relatorio-testes.md` |

## Bloqueadores pendentes, em ordem

1. **CI da revisão não confirmado:** a verificação local passou, mas a branch ainda não foi publicada para executar `.github/workflows/ci.yml` no SHA final.
2. **Revisão por pares da alteração atual:** requer PR e revisão humana; o gate anterior da story 2.2 em estado `CONCERNS` não equivale a revisão desta revisão.
3. **SonarCloud sem Quality Gate atual:** o relatório local registra a ausência. O workflow depende dos secrets/variables e da execução após push.
4. **RNF-03 sem aprovação representativa:** o resultado local em H2 usa `GET /reservas` como proxy; falta confirmação dos limites e medição em PostgreSQL/staging.

Não marcar os itens acima como concluídos com base em resultados históricos. Atualize esta tabela depois da execução remota da branch e da revisão humana.

## Correções dos erros de `./mvnw -B verify`

Na auditoria inicial houve 235 testes, 11 erros, 0 falhas e 0 ignorados: seis contextos sem `DataInitializer`, quatro violações de integridade referencial no teste de login e um contexto PostgreSQL sem tabela de conflitos. As correções estão aplicadas em código e passaram na verificação local: perfil `local` default; H2 randômico/isolation no teste; `AuthLoginIntegrationTest` com perfis e DB explícitos; Flyway habilitado em `prod`, sem desativação global. O relatório detalhado está em [relatorio-testes.md](../testes/relatorio-testes.md).
