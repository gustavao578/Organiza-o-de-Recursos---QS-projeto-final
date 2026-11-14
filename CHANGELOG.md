# Changelog

Todos os mudanças notáveis para este projeto serão documentadas neste arquivo.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto segue [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [1.0.0-rc1] - 2026-11-14

Release Candidate 1 - Primeira versão candidata para release.

### Added

- **Autenticação com JWT**: Suporte a login com JWT (HS256) usando email ou matrícula como identificador
- **SPA Frontend**: Aplicação front-end em HTML/CSS/JavaScript com:
  - Listagem de recursos disponíveis
  - Solicitação de reservas com visualização de disponibilidade
  - Dashboard de responsáveis com gerenciamento de aprovações/bloqueios
  - Dashboard administrativo com gerenciamento de usuários e recursos
  - Interface responsiva com temas claro/escuro
  - Validações em tempo real
- **API REST**: Endpoints completos para:
  - Autenticação (`POST /auth/login`)
  - Listagem de recursos (`GET /api/recursos`)
  - Gerenciamento de reservas (`GET/POST/PATCH /api/reservas`)
  - Gerenciamento de usuários (`GET/POST/PATCH /api/usuarios`) - Admin only
  - Gerenciamento de recursos (`GET/POST/PATCH /api/recursos`) - Admin only
- **Banco de dados**: Suporte dual:
  - H2 em memória para desenvolvimento/teste (perfil `local`)
  - PostgreSQL para produção (perfil `prod`)
  - Flyway para versionamento de schema
  - JPA com Hibernate para ORM
- **Segurança**:
  - Spring Security com autenticação baseada em JWT
  - Restrição de CORS configurável por perfil
  - Validação de perfil de usuário (ADMINISTRADOR, RESPONSAVEL, SOLICITANTE)
  - Isolamento de dados com RLS conceitual (profile-based)
- **Dados de demonstração**: Seed automático com usuários e recursos de exemplo (perfis `local` e `demo`)
- **Documentação**: Swagger/OpenAPI (springdoc-openapi)
- **Testes**:
  - 235+ testes unitários e de integração
  - Cobertura de código com JaCoCo
  - BDD com Cucumber para testes de aceitação
  - Testes de performance com JMeter
  - Validação com SonarCloud

### Fixed

- **Profiles Spring**: Corrigida sintaxe de `@Profile` que usava operadores inválidos (`&`, `|`)
  - `@Profile("(local | demo) & !prod")` → `@Profile({"local", "demo"})`
  - Afeta: `AuthController`, `DataInitializer`
- **Git LFS**: Adicionado `.worktrees/` ao `.gitignore` para evitar erros de submodule no CI

### Technical Details

- **Linguagem**: Java 21
- **Framework**: Spring Boot 3.3.5
- **Build**: Maven 3.9+
- **Cobertura de Código**: JaCoCo
- **Análise de Qualidade**: SonarCloud
- **Documentação Técnica**: Suporte a OpenAPI 3.0
- **Banco de Dados**: H2 (dev) / PostgreSQL 15+ (prod)

### Quality Metrics (rc1)

- **Testes**: 235/235 passando ✅
- **Build**: Maven verify ✅
- **Análise SonarCloud**: Aguardando análise pós-release
- **Cobertura JaCoCo**: Gerado em `target/site/jacoco/`

### Security

- JWT com HS256 para autenticação stateless
- Validação de token em cada requisição
- Restrição de endpoints por perfil de usuário
- CORS configurável (restritivo em prod)
- Sem armazenamento de senhas em texto plano (demonstração usa fake auth)

### Known Issues / Limitações

- v1.0.0-rc1: Autenticação de demonstração usa apenas identificador (email/matrícula), sem suporte a senha hash
- Análise SonarCloud será executada após merge na main e push da tag
- Dados de demonstração são perdidos ao reiniciar (H2 em memória) - use PostgreSQL para persistência

### Contribuições / Commits

- `c266f77`: fix: corrige profiles Spring para desabilitar seed em prod
- `3547133`: fix: corrige profiles Spring e gitignore para CI/CD
- `9c70364`: docs: record CI and SonarCloud evidence [Story 2.2]
- `6106cc5`: fix: restore Maven verification and update delivery evidence [Story 2.2]

### Links de Evidência

- **CI Workflow**: [GitHub Actions - Java CI](https://github.com/gustavao578/Organiza-o-de-Recursos---QS-projeto-final/actions/workflows/ci.yml)
- **Cobertura de Código**: `docs/evidencias/` (JaCoCo report)
- **Quality Gate SonarCloud**: `docs/evidencias/` (link e status)
- **Requisitos/RTM**: `docs/rtm.md`
- **Relatório de Qualidade**: `docs/relatorio-qualidade.md`
- **Testes de Desempenho**: `docs/performance/resultado-01.md`
- **Registro de Defeitos**: `docs/defeitos/`

---

## Versões Anteriores

Este é o primeiro release candidate. Não há versões anteriores listadas.

### Como Usar Este Changelog

- **Added** para novas funcionalidades
- **Changed** para mudanças em funcionalidades existentes
- **Deprecated** para funcionalidades que serão removidas em breve
- **Removed** para funcionalidades removidas nesta versão
- **Fixed** para correção de bugs
- **Security** para vulnerabilidades

---

**Nota**: Esta tag (v1.0.0-rc1) será criada no commit `c266f77` após validação do CI verde na main.
