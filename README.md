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
o perfil `local` é o padrão quando nenhum perfil é selecionado e usa H2 sem
expor o console web. Se `JWT_SECRET` não estiver definido, o backend gera uma
chave HS256 aleatória em memória para aquele processo. Essa chave efêmera não é
compartilhada entre instâncias e muda a cada reinício; configure uma chave
aleatória de pelo menos 32 bytes em `JWT_SECRET` para validar tokens do provedor
de identidade e manter compatibilidade entre reinícios/instâncias.

Em produção, ative o perfil `prod`, configure a conexão PostgreSQL e injete
`JWT_SECRET`:

```bash
SPRING_PROFILES_ACTIVE=prod \
DATABASE_URL="jdbc:postgresql://<host>:5432/<database>" \
DATABASE_USERNAME="<usuario>" \
DATABASE_PASSWORD="<senha>" \
JWT_SECRET="<chave-aleatoria-com-pelo-menos-32-bytes>" \
./mvnw spring-boot:run
```

O perfil de produção desativa o console H2. Flyway aplica as migrações
PostgreSQL versionadas antes de o Hibernate validar o schema; a aplicação não
altera nem remove dados existentes para contornar uma migração incompatível.
Se já houver sobreposições impeditivas, a V2 falha e exige revisão dos dados.
A dependência PostgreSQL está disponível no runtime.
Os tokens carregam o identificador do usuário em `sub` e o perfil em `profile`
(`SOLICITANTE`, `RESPONSAVEL` ou `ADMINISTRADOR`). A chave de teste em
execução de testes é gerada em memória e não deve ser reutilizada em ambientes
reais.

## SPA de recursos e reservas

Inicie a aplicação com `./mvnw spring-boot:run` e abra
`http://localhost:8080/`. A interface é servida pelo próprio Spring Boot na
mesma origem da API; os arquivos ficam em `src/main/resources/static/` e não
exigem instalação de dependências de frontend.

No perfil `demo`, a SPA permite entrar usando e-mail ou matrícula de um usuário
cadastrado. Esse fluxo é somente para demonstração local: não verifica a posse
do identificador e, portanto, não deve ser habilitado em produção. Para
iniciá-lo, use `SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run`. O perfil
`demo` inclui o armazenamento H2 local; não o use em ambientes compartilhados.
O Swagger documenta `POST /auth/login` apenas quando esse perfil está ativo;
o login é público e as operações de negócio herdam o esquema Bearer JWT.

Em produção, o endpoint de login por identificador fica desabilitado. Na tela
inicial, informe um JWT emitido por um provedor de identidade confiável. O
token deve conter `sub` (UUID do usuário), `profile` (`SOLICITANTE`,
`RESPONSAVEL` ou `ADMINISTRADOR`) e `exp`. A interface lê esses claims somente
para exibir a identidade e adaptar as ações; assinatura, validade e autorização
são sempre verificadas pelo backend. O token permanece em
`sessionStorage` durante a sessão desta aba e é removido ao encerrar a sessão
ou ao receber HTTP 401. `sessionStorage` não protege contra XSS: mantenha a
origem protegida, não adicione scripts de terceiros e trate os dados exibidos
como não confiáveis.

Todos os endpoints de negócio da API continuam protegidos por Bearer JWT. Os
arquivos estáticos da SPA podem ser carregados sem token, mas isso não libera
rotas de negócio. As reservas usam exclusivamente o caminho `/reservas`; o
alias anterior `/api/reservas` não está mais disponível. A SPA permite listar recursos e reservas a usuários
autenticados, oferece gestão de recursos e usuários a `ADMINISTRADOR` e mostra
as ações de reservas conforme perfil, vínculo e estado.

## Migração PostgreSQL de conflitos de reservas

Ao iniciar com `SPRING_PROFILES_ACTIVE=prod`, o Flyway aplica automaticamente
as migrações versionadas antes de `ddl-auto=validate`. A sequência é:

```bash
src/main/resources/db/migration/V1__criar_tabelas.sql
src/main/resources/db/migration/V2__reservas_sem_sobreposicao_ativa.sql
```

A V1 cria as tabelas e índices do domínio; a V2 habilita `btree_gist` e cria
uma constraint GiST que impede
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
