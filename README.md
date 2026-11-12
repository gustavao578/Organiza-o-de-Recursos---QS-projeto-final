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

As rotas da API exigem `Authorization: Bearer <JWT>`. Em desenvolvimento local,
o perfil `local` é o padrão quando nenhum perfil é selecionado. Somente
`application-local.properties` oferece um segredo JWT de fallback destinado
exclusivamente ao desenvolvimento. Para tokens HS256 próprios, configure
`JWT_SECRET` com uma chave aleatória de pelo menos 32 bytes; a aplicação valida
o tamanho durante a inicialização.

Em produção, ative o perfil `prod` e injete `JWT_SECRET`. O perfil exige a
variável, não herda o fallback local e a aplicação falha ao iniciar se o segredo
estiver ausente ou vazio:

```bash
SPRING_PROFILES_ACTIVE=prod JWT_SECRET="<chave-aleatoria-com-pelo-menos-32-bytes>" ./mvnw spring-boot:run
```

Nunca use o segredo local padrão em ambientes compartilhados ou de produção.
Garanta que implantações de produção ativem explicitamente `prod`; não defina
`JWT_SECRET` com o fallback documentado.
Os tokens carregam o identificador do usuário em `sub` e o perfil em `profile`
(`SOLICITANTE`, `RESPONSAVEL` ou `ADMINISTRADOR`). A chave de teste em
`src/test/resources/application.properties` existe somente para testes e não
deve ser reutilizada em ambientes reais.

## SPA de recursos e reservas

Inicie a aplicação com `./mvnw spring-boot:run` e abra
`http://localhost:8080/`. A interface é servida pelo próprio Spring Boot na
mesma origem da API; os arquivos ficam em `src/main/resources/static/` e não
exigem instalação de dependências de frontend.

A API não emite JWT nem oferece login por senha. Na tela inicial, informe um
JWT válido emitido pelo provedor de identidade confiável da organização. O
token deve conter `sub` (UUID do usuário) e `profile`, com um dos valores
`SOLICITANTE`, `RESPONSAVEL` ou `ADMINISTRADOR`. A interface lê esses claims
somente para exibir a identidade e adaptar as ações; assinatura, validade e
autorização são sempre verificadas pelo backend. O token permanece em
`sessionStorage` durante a sessão desta aba e é removido ao encerrar a sessão
ou ao receber HTTP 401. `sessionStorage` não protege contra XSS: mantenha a
origem protegida, não adicione scripts de terceiros e trate os dados exibidos
como não confiáveis.

Todos os endpoints da API continuam protegidos por Bearer JWT. Os arquivos
estáticos da SPA podem ser carregados sem token, mas isso não libera rotas de
negócio. A SPA permite listar recursos e reservas a usuários autenticados,
oferece criação de recursos somente a `ADMINISTRADOR` e criação de reservas
somente a `SOLICITANTE`.

## Migração PostgreSQL de conflitos de reservas

Antes de liberar o backend contra PostgreSQL, garanta que o schema da aplicação
já contenha `tb_reservas` e aplique a migração versionada:

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 \
  -f src/main/resources/db/migration/V1__reservas_sem_sobreposicao_ativa.sql
```

A migração habilita `btree_gist` e cria uma constraint GiST que impede
sobreposição no mesmo recurso nos estados `PENDENTE`, `APROVADA` e `EM_USO`,
usando intervalos semiabertos `[início, fim)`. Reservas adjacentes são
permitidas. O script não remove nem altera registros preexistentes; se a
constraint não puder ser criada devido a conflitos, a migração falha e os dados
devem ser revisados antes de reaplicá-la. H2 continua sendo usado nos testes que
não validam recursos específicos do PostgreSQL; a garantia física é testada
com Testcontainers/PostgreSQL.

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
