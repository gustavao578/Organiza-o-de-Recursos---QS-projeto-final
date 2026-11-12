# Registro de Correções — EPIC-02 Segurança e SPA

**Data:** 2026-11-12
**Status:** Implementado localmente; aguardando revisão/merge
**Story:** [2.1-seguranca-e-spa-de-reservas](../stories/2.1-seguranca-e-spa-de-reservas.md)

Este registro consolida as correções dos defeitos DEF-001 a DEF-009, a prevenção de
sobreposição de reservas pendentes e a criação da SPA, além da correção de
inicialização de JWT_SECRET.

## Controles de acesso

- `POST`, `PUT` e `DELETE /recursos` exigem a role `ADMINISTRADOR`; os métodos
  recebem o JWT via `@AuthenticationPrincipal` e verificam sua presença.
- `GET /usuarios` e `GET /usuarios/{id}` exigem usuário autenticado.
  `DELETE /usuarios/{id}` exige `ADMINISTRADOR`.
- `POST` e `GET /manutencoes` exigem `ADMINISTRADOR` ou `RESPONSAVEL`.
- `POST /reservas` exige exclusivamente claim `profile=SOLICITANTE`; aliases
  legados não autorizam essa operação.
- `SecurityConfig` habilita method security. Os caminhos estáticos da SPA são
  públicos por método GET, mas as rotas da API continuam autenticadas.
- Testes em `MatrizAutorizacaoTest` cobrem os perfis e resultados 401/403/2xx.

## Inicialização e segredo JWT

- `application.properties` não contém fallback JWT e declara
  `spring.profiles.default=local`.
- Sem `JWT_SECRET`, `SecurityConfig` gera uma chave HS256 aleatória de 256 bits
  em memória. Ela não é persistida/compartilhada e muda a cada inicialização;
  ambientes com tokens emitidos por provedor externo devem configurar o mesmo
  segredo em todas as instâncias.
- Segredos configurados são validados em bytes no startup e rejeitados se
  menores que 256 bits. Testes usam a chave efêmera do próprio contexto e não
  armazenam segredos em arquivos `.properties`.
- H2 e suas configurações foram limitados ao perfil local, sem senha textual e
  com console desativado. O perfil `prod` usa PostgreSQL com credenciais via
  `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD`, desativa o console
  H2 e valida o schema sem atualizá-lo.

## Vulnerabilidade SonarCloud e cobertura

- `ManutencaoController` recebe `ManutencaoRequestDTO` em vez da entidade JPA;
  o identificador é sempre gerado no servidor, impedindo merge/edição por
  `id` enviado pelo cliente (SonarCloud `java:S4684`).
- `RelatorioUtilizacaoService` converte as datas locais para instantes usando o
  fuso do `Clock` antes de calcular durações. Isso evita contabilizar
  incorretamente reservas que atravessam transições de horário de verão
  (SonarCloud `java:S8700`).
- Testes focados cobrem chave JWT efêmera/configurada, rejeição de JWT expirado,
  guards de principal ausente, tentativa de alterar manutenção enviando um
  `id` existente e duração durante transição de horário de verão.

## Sobreposição de reservas

- A verificação de conflito na criação ocorre antes de selecionar o status
  inicial e considera `PENDENTE`, `APROVADA` e `EM_USO`.
- O PostgreSQL recebe a constraint `EXCLUDE USING gist` definida em
  `src/main/resources/db/migration/V1__reservas_sem_sobreposicao_ativa.sql`,
  usando `btree_gist`, o recurso e um `tsrange` semiaberto `[início, fim)`.
- Períodos adjacentes são válidos. A constraint não se aplica a status inativos;
  violações retornam HTTP 409. Testcontainers executa a migração sobre a tabela
  `tb_reservas` real criada pelo Hibernate. O teste de API insere primeiro uma
  reserva conflitante e desativa de modo controlado a consulta preventiva da
  aplicação, sem corrida, para alcançar a exclusion constraint e confirmar HTTP
  409 por `DataIntegrityViolationException`.
- A migração precisa ser aplicada a schemas PostgreSQL existentes após a
  revisão dos dados; ela não apaga nem corrige registros conflitantes.

## SPA

- A aplicação serve um único `index.html`, `styles.css` e os módulos
  `api.js`, `auth.js`, `app.js`, `recursos.js` e `reservas.js` em
  `src/main/resources/static/`.
- `api.js` centraliza Bearer JWT e tratamento de respostas; `auth.js` mantém
  token externo em `sessionStorage`; `app.js` alterna as telas e as integrações
  exibem recursos/reservas e ações permitidas por perfil.
- A API não emite JWT. A interface recebe um token de provedor externo. O
  backend permanece a autoridade de autorização, inclusive em chamadas fora
  da SPA.
- O tratamento de 401 encerra a sessão; 403 e 409 apresentam mensagens
  explícitas ao usuário.

## Evidências e arquivos de referência

- Testes de integração e autorização: `src/test/java/br/edu/organizacaorecursos/`.
- `./mvnw -B clean verify`: 189 testes, 0 falhas/erros/ignorados; JaCoCo
  registra 92% de cobertura de linhas e 74% de branches, acima dos limites do
  build (80% linhas, 70% branches). A reanálise SonarCloud após o push ainda é
  necessária para confirmar a métrica de cobertura em código novo do PR.
- O smoke browser do AC14 permanece pendente: não há Chromium, Playwright ou
  Puppeteer/runner de browser instalado no ambiente. Não considerar essa
  validação de browser cumprida.
- Como operar a SPA, configurar segredo e aplicar a migração: seção pertinente
  do `README.md`.
