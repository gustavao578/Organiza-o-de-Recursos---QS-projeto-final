# EPIC-1: Relatórios de utilização — Brownfield Enhancement

**Status:** 📋 Planning
**Owner:** Produto
**Created:** 2026-11-11

---

## Objective

Disponibilizar a responsáveis e administradores um relatório consultável por período com horas de utilização por recurso, carga horária por professor e tentativas de reserva recusadas por sobreposição. O relatório dá visibilidade às alocações e à prevenção de conflitos descritas no RF-12.

## Business Value

Permitir que responsáveis e administradores acompanhem a alocação de recursos e avaliem quantas tentativas de sobreposição foram impedidas, sem depender de consulta manual às reservas.

## Scope

### In Scope

- Endpoint `GET /relatorios/utilizacao?inicio=&fim=`, restrito aos perfis Responsável e Administrador.
- Horas calculadas pela interseção do período consultado com cada reserva; todos os estados contam, exceto `CANCELADA`.
- Carga horária agrupada pelo usuário associado à reserva (`Reserva.usuario`), conforme decisão do solicitante para o modelo atual.
- Contagem de tentativas recusadas por sobreposição atribuída ao instante da tentativa; o evento precisa ser persistido para que possa ser consultado posteriormente.
- Testes HTTP black-box com pequena massa semeada, cálculos esperados documentados no próprio teste e cobertura dos casos de autorização e fronteira temporal solicitados.
- Documentação do endpoint e atualização da rastreabilidade/status do RF-12.

### Out of Scope

- Exportação, interface gráfica ou novas métricas não solicitadas.
- Criar uma entidade/perfil de Professor ou alterar o modelo de usuários.
- Contagem de eventos cuja causa não seja uma recusa por sobreposição.
- Relatórios de material, volume de reservas por estado ou quantidade de solicitações que exigiram aprovação, previstos no escopo mais amplo do RF-12 mas não pedidos nesta entrega.

## Existing System Context

- **Funcionalidade relevante:** API Spring Boot para reservas e recursos; a aplicação já recusa sobreposições com HTTP 409, mas não persiste as tentativas recusadas.
- **Tecnologia:** Java 21, Spring Boot, Spring Data JPA, H2 nos testes e JWT com perfis `SOLICITANTE`, `RESPONSAVEL` e `ADMINISTRADOR`.
- **Integração:** o relatório agrega `Reserva`, `Recurso` e `Usuario`; o registro de conflito deve sobreviver à resposta 409 para ser contabilizado em consultas futuras.
- **Rastreabilidade:** `RF-12` em `prd/requisitos-funcionais.md`; evidências e lacunas em `prd/rtm.md` e `prd/status-requisitos.md`.

## Stories

| ID | Title | Points | Priority | Status |
|----|-------|--------|----------|--------|
| 1.3 | Relatório de utilização por período | 8 | High | Ready |

**Assignment previsto:** executor `@dev`; quality gate de contrato/API `@architect`; QA gate do ciclo de vida `@qa`. O executor e o quality gate são diferentes.

## Success Criteria

- [ ] O endpoint retorna totais de horas por recurso, totais por usuário associado à reserva e a contagem de conflitos evitados para o período solicitado.
- [ ] Uma reserva que cruza a borda do período contribui apenas com a duração da interseção; `CANCELADA` não contribui com horas.
- [ ] Tentativas recusadas por sobreposição são persistidas e contadas pelo instante da tentativa.
- [ ] Sem token, a API responde 401; `SOLICITANTE` recebe 403; `RESPONSAVEL` e `ADMINISTRADOR` podem consultar.
- [ ] Testes API black-box cobrem os casos acima, inclusive período sem dados, com cálculos manuais em comentário no teste.
- [ ] A documentação pública e a rastreabilidade/status do RF-12 refletem exatamente o escopo entregue.

## Technical Requirements

- Manter o endpoint integrado aos padrões atuais de Spring MVC, JPA e autenticação JWT.
- Validar que `inicio` e `fim` formem um intervalo válido antes de consultar as métricas.
- Usar limites temporais consistentes com os intervalos de reserva semiabertos usados pelo domínio.
- Registrar a recusa de sobreposição de forma durável mesmo quando a operação de reserva retorna HTTP 409.
- Não modificar os contratos existentes de reservas.

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Tentativas 409 ainda não são persistidas; uma gravação na mesma transação revertida não seria contável. | High | Testar a persistência após a resposta 409 e validar a contagem via endpoint em uma requisição subsequente. |
| O modelo não distingue professores de outros usuários. | Medium | Agrupar pelo `Reserva.usuario`, conforme decisão explícita do solicitante; não inferir um perfil Professor. |
| O RF-12 é mais amplo que o recorte solicitado. | Medium | Manter este endpoint limitado às métricas expressamente pedidas e registrar como parcial qualquer escopo oficial ainda não coberto. |

## Dependencies

**Rastreabilidade:** `RF-12` em `prd/requisitos-funcionais.md`.

## Definition of Done

- [ ] Todos os critérios de sucesso da epic foram atendidos pela story vinculada.
- [ ] A suíte existente não regrediu e as validações Maven passam.
- [ ] Endpoint e critérios de autorização estão documentados.
- [ ] RTM e status do RF-12 registram evidência e lacunas remanescentes.
- [ ] Story 1.3 foi criada e validada por @sm/@po antes da implementação.
- [ ] QA concluiu a revisão da story antes do handoff para operações de PR.

## Change Log

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-11-11 | 1.0 | Epic brownfield criada para o recorte solicitado de RF-12 | @pm |
| 2026-11-11 | 1.1 | Removida dependência autorreferente, estimativa definida e quality gate alinhado ao fluxo de API | @pm |
| 2026-11-11 | 1.1.1 | Status da Story 1.3 sincronizado para Ready após validação GO do @po | @po |
