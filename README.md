# Organização de Recursos

## Integração contínua

O workflow [.github/workflows/ci.yml](.github/workflows/ci.yml) executa
`./mvnw -B verify` em pull requests e pushes, usando Java 21 (Temurin).
A fase `verify` executa os testes JUnit e as features Cucumber, gera o relatório
JaCoCo e aplica os limites de cobertura configurados no Maven. O run
`42859680966`, no commit `eb2bced`, passou por Maven verify, SonarCloud e
publicação do relatório JaCoCo.

### Validação local

| Comando | Resultado | Run ID |
|---|---|---|
| `./mvnw -B verify` | Executa JUnit, Cucumber e o gate JaCoCo | Não se aplica (execução local) |

### SonarCloud

O passo SonarCloud do workflow requer o secret `SONAR_TOKEN` e as variáveis
`SONAR_ORGANIZATION` e `SONAR_PROJECT_KEY`. O scanner importa
`target/site/jacoco/jacoco.xml`; o Quality Gate passou no run listado acima.

## Autenticação da API

As rotas da API exigem `Authorization: Bearer <JWT>`. Configure `JWT_SECRET` com
uma chave aleatória de pelo menos 32 bytes para assinar/verificar tokens HS256.
Os tokens carregam o identificador do usuário em `sub` e o perfil em `profile`
(`SOLICITANTE`, `RESPONSAVEL` ou `ADMINISTRADOR`). A chave de teste em
`src/test/resources/application.properties` existe somente para testes e não
deve ser reutilizada em ambientes reais.

## Relatório de utilização

`GET /relatorios/utilizacao?inicio=2026-03-15T10:00:00&fim=2026-03-15T15:00:00`
retorna as horas reservadas por recurso (`porRecurso`), as horas agrupadas por
usuário associado à reserva (`porUsuario`) e a quantidade de recusas por
sobreposição no período (`conflitosEvitados`). Os parâmetros usam data/hora
ISO-8601; `inicio` é inclusivo e `fim` é exclusivo. Cada reserva contribui
somente com sua interseção com o período, e reservas `CANCELADA` não contam.
Os conflitos são filtrados pelo instante da tentativa recusada. Períodos sem
dados retornam listas vazias e zero conflitos; um fim igual ou anterior ao
início resulta em HTTP 400.

A rota requer Bearer JWT e está disponível somente para os perfis
`RESPONSAVEL` e `ADMINISTRADOR`. O contrato completo está publicado em
`/v3/api-docs` e no Swagger UI em `/swagger-ui/index.html`. `porUsuario` usa
`Reserva.usuario` como chave de agrupamento (para representar a carga horária
associada ao professor no modelo atual); isso não infere nem valida o perfil
Professor.
