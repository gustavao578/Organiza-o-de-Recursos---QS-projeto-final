# Relatório de Testes — Verificação Local

**Data:** 2026-11-14

**Revisão:** commit `6106cc5` na branch `fix/verify-profile-isolation`; revisão humana permanece pendente.

**Comando:** `./mvnw -B verify` com OpenJDK 21.0.13

## Resultado

| Verificação | Resultado |
|---|---|
| Maven | `BUILD SUCCESS` |
| Testes JUnit/Spring/Cucumber | 235 testes, 0 falhas, 0 erros, 0 ignorados |
| JaCoCo — linhas | 93,14% (706 cobertas / 758) |
| JaCoCo — branches | 76,77% (228 cobertos / 297) |
| JaCoCo — instruções | 92,21% (3.127 cobertas / 3.391) |
| Relatório HTML/XML | `target/site/jacoco/index.html`, `jacoco.xml` |
| Smoke HTTP | perfil `demo`: `/` e `/v3/api-docs` HTTP 200; perfil default `local`: `/` e login local HTTP 200 |

Os relatórios Surefire/Failsafe e JaCoCo foram lidos após a execução. O Surefire registrou aviso de encerramento forçado de fork após o timeout de encerramento, mas Maven terminou com sucesso e os relatórios contabilizam zero falhas/erros/skips.

## Ambiente e escopo

- Linux dev container; Maven Wrapper 3.10.0; OpenJDK 21.0.13, alinhado ao workflow CI.
- H2 de teste com identificador aleatório e seed/login desabilitados na configuração de testes.
- Testes PostgreSQL usam Testcontainers quando Docker está disponível.
- O build registrou aviso não bloqueante de resolução DNS para metadados do repositório Flyway; as dependências disponíveis permitiram concluir a verificação.
- O smoke test foi executado com H2 em memória; não houve inspeção visual de navegador nesta rodada.
- O processo Spring Boot usado no smoke test foi encerrado intencionalmente após as respostas HTTP; o código de saída 143 veio do sinal de parada, não de falha de inicialização.

## Conclusão

A verificação local no runtime do CI satisfaz os limites Maven de teste e JaCoCo; Java CI e SonarCloud também passaram para `6106cc5`. Permanecem pendentes: revisão por pares humana, verificação visual/acessibilidade e aprovação de carga em ambiente representativo. Consulte [checklist de entrega](../entrega/checklist.md).

## Correções dos 11 erros da auditoria

- Seis falhas de inicialização: `local` agora é o perfil default, ativando `DataInitializer` conforme a execução documentada.
- Quatro violações de integridade em `AuthLoginIntegrationTest`: cada contexto recebe H2 em memória exclusivo e o teste ativa explicitamente os perfis `local` e `demo`.
- Um contexto PostgreSQL sem `tb_conflitos_reserva`: Flyway deixou de ser desativado globalmente e fica habilitado em `prod`, antes de `ddl-auto=validate`; perfis locais seguem com H2/Flyway desativado.
- `DataInitializer` e login local estão ativos em `local`/`demo` e ausentes em `prod`; a configuração isolada evita tocar no arquivo H2 local durante a suíte.
