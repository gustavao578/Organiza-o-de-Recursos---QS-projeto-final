# Organização de Recursos

## Integração contínua

O workflow [.github/workflows/ci.yml](.github/workflows/ci.yml) executa
`mvn --batch-mode verify` em pull requests e pushes, usando Java 21 (Temurin).
A fase `verify` executa os testes JUnit e as features Cucumber, gera o relatório
JaCoCo e aplica os limites de cobertura configurados no Maven.

### Validação local

| Comando | Resultado | Run ID |
|---|---|---|
| `mvn test -B verify` | Executa JUnit, Cucumber e o gate JaCoCo | Não se aplica (execução local) |

### SonarCloud

O passo SonarCloud do workflow requer o secret `SONAR_TOKEN` e as variáveis
`SONAR_ORGANIZATION` e `SONAR_PROJECT_KEY`. O scanner espera o resultado do
Quality Gate e importa `target/site/jacoco/jacoco.xml`.

## Autenticação da API

As rotas da API exigem `Authorization: Bearer <JWT>`. Configure `JWT_SECRET` com
uma chave aleatória de pelo menos 32 bytes para assinar/verificar tokens HS256.
Os tokens carregam o identificador do usuário em `sub` e o perfil em `profile`
(`SOLICITANTE`, `RESPONSAVEL` ou `ADMINISTRADOR`). A chave de teste em
`src/test/resources/application.properties` existe somente para testes e não
deve ser reutilizada em ambientes reais.
