# Organização de Recursos

## Integração contínua

A configuração do workflow está preparada na branch local
`ci/maven-21-github-actions`, commit `f33aab9`. Ela executa
`./mvnw -B verify` em pull requests e em pushes para `main`, com Java 21
(Temurin), cache Maven, timeout e publicação dos relatórios Surefire/Failsafe
por três dias. **Essa configuração ainda não foi publicada na `main`**, então
não há workflow ou execução remota confirmada no GitHub.

### Validação local

| Comando | Resultado | Run ID |
|---|---|---|
| `./mvnw -B verify` | Passou: 29 testes, zero falhas ou erros | Não se aplica (execução local) |

### Execuções GitHub Actions

| Finalidade | Run ID | Estado |
|---|---|---|
| Verificação inicial em pull request | Ainda não existe | Pendente de publicação do workflow |
| Teste temporário com asserção intencionalmente falha | Ainda não existe | Não executado |
| Verificação após reversão da asserção | Ainda não existe | Não executado |

Os IDs devem ser preenchidos após as respectivas execuções remotas; uma
verificação local não possui Run ID do GitHub Actions.

### Proteção da branch `main`

Após publicar o workflow, um administrador deve configurar a regra de `main`:

1. Abra **Settings → Branches** no GitHub.
2. Crie ou edite a regra/ruleset que corresponde a `main`.
3. Habilite **Require status checks to pass before merging**.
4. Selecione o check **`verify`** e salve a regra.

O check `verify` e a proteção de branch ainda não foram confirmados no remoto.
