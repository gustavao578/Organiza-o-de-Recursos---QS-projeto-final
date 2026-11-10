# BDD — Aprovação e bloqueio de reservas

## Base e escopo

Foram consultados `prd/requisitos-funcionais.md`, `prd/rtm.md`, `prd/visão-geral.md` e o código de reservas.

O PRD define aprovação para recursos restritos (RF-07) e indisponibilidade durante manutenção (RF-08). Os cenários de aprovação, transições e manutenção estão implementados na feature `src/test/resources/features/aprovacao-bloqueio.feature` e são executados com MockMvc/JWT pelo runner Cucumber. Esta documentação descreve o baseline de 2026-11-10; não é apenas uma proposta para trabalho futuro.

Cada cenário contém exatamente um `Quando` e um `Então`. Os exemplos de manutenção, aprovação e transição são executáveis; transições candidatas ainda dependem de validação de produto. O endpoint atual de decisão é `PATCH /reservas/{id}/status`; aceita `APROVADA` ou `REJEITADA`, com autorização para `ADMINISTRADOR` ou o `RESPONSAVEL` associado ao recurso. A rota legada `/reservas/{id}/aprovar` é restrita a administrador.

## Regra 1 — Recurso restrito exige aprovação

### BDD-APR-01 — Solicitação restrita fica pendente

**Tipo:** cenário executado a partir do RF-07.

```gherkin
Cenário: Solicitação de recurso restrito aguarda aprovação
  Dado que o recurso está classificado como restrito
  E que o solicitante tem permissão para solicitar uma reserva
  Quando o solicitante envia uma solicitação válida para esse recurso
  Então a reserva é registrada como PENDENTE e não como confirmação final
```

### BDD-APR-02 — Responsável aprova a solicitação

```gherkin
Cenário: Responsável aprova uma solicitação restrita
  Dado que existe uma reserva restrita no estado PENDENTE
  E que o ator é o responsável autorizado pelo recurso
  Quando o ator aprova a reserva
  Então a reserva passa para o estado APROVADA
```

## Regra 2 — Solicitante não aprova a própria reserva

### BDD-APR-03 — Solicitante tenta aprovar a própria reserva

```gherkin
Cenário: Solicitante não pode aprovar a própria reserva
  Dado que existe uma reserva restrita no estado PENDENTE
  E que o ator é o solicitante dessa reserva
  Quando o ator tenta aprovar a reserva
  Então a operação é rejeitada e a reserva permanece PENDENTE
```

### BDD-APR-04 — Usuário que não é responsável tenta aprovar

```gherkin
Cenário: Usuário sem papel de responsável não aprova uma reserva restrita
  Dado que existe uma reserva restrita no estado PENDENTE
  E que o ator não é o responsável autorizado pelo recurso
  Quando o ator tenta aprovar a reserva
  Então a operação é rejeitada e a reserva permanece PENDENTE
```

## Regra 3 — Recurso em manutenção não pode ser reservado

### BDD-MAN-01 — Resultado nas fronteiras do período

**Premissa a validar:** considerar manutenção como intervalo semiaberto `[início, fim)`. O repositório atual usa comparações estritas e, portanto, produz os resultados candidatos abaixo. O PRD não define se os extremos são inclusivos.

```gherkin
Esquema do Cenário: Criar reserva na fronteira do período de manutenção
  Dado que o recurso tem manutenção de 2026-11-10T10:00:00 até 2026-11-10T11:00:00
  Quando o solicitante cria uma reserva de <inicio_reserva> até <fim_reserva>
  Então a resposta da API é <resultado_http> e a reserva <persistencia>

Exemplos:
  | caso                                      | inicio_reserva       | fim_reserva          | resultado_http | persistencia       |
  | reserva termina no início da manutenção   | 2026-11-10T09:00:00  | 2026-11-10T10:00:00  | 201              | é persistida       |
  | reserva começa no fim da manutenção      | 2026-11-10T11:00:00  | 2026-11-10T12:00:00  | 201              | é persistida       |
  | reserva cruza um segundo da manutenção   | 2026-11-10T09:00:00  | 2026-11-10T10:00:01  | 400              | não é persistida   |
```

## Regra 4 — Reserva só muda por transições válidas

### BDD-EST-01 — Transições do fluxo principal

```gherkin
Esquema do Cenário: Executar uma transição do fluxo principal
  Dado que a reserva está no estado <estado_origem>
  Quando um ator autorizado solicita a transição para <estado_destino>
  Então a operação é aceita e o estado persistido passa a <estado_destino>

Exemplos:
  | estado_origem | estado_destino |
  | PENDENTE      | APROVADA       |
  | APROVADA      | EM_USO         |
  | EM_USO        | CONCLUÍDA      |
```

### BDD-EST-02 — Estados alternativos

Os exemplos são exercitados pela feature atual. A autorização e o momento operacional de `NÃO_COMPARECEU` devem permanecer alinhados ao contrato completo de estados, que ainda precisa de validação de produto.

```gherkin
Esquema do Cenário: Registrar um estado alternativo permitido
  Dado que a reserva está no estado <estado_origem>
  Quando um ator autorizado solicita a transição para <estado_destino>
  Então a operação é aceita e o estado persistido passa a <estado_destino>

Exemplos atualmente exercitados:
  | estado_origem | estado_destino |
  | PENDENTE      | REJEITADA      |
  | PENDENTE      | CANCELADA      |
  | APROVADA      | NÃO_COMPARECEU |
```

### BDD-EST-03 — Transição fora do fluxo não é aceita

```gherkin
Esquema do Cenário: Rejeitar transição inválida
  Dado que a reserva está no estado <estado_origem>
  Quando um ator solicita a transição para <estado_destino>
  Então a operação é rejeitada e a reserva permanece em <estado_origem>

Exemplos:
  | estado_origem | estado_destino |
  | PENDENTE      | EM_USO         |
  | APROVADA      | CONCLUÍDA      |
  | CONCLUÍDA     | PENDENTE       |
```

## Perguntas em aberto

1. Quais papéis podem cadastrar/alterar o indicador `restrito` e quais critérios de produto determinam sua configuração?
2. Como o responsável pelo recurso é designado e como serão provisionados/emissos os JWTs? O backend compara o perfil persistido e o responsável associado, mas o fluxo de identidade/provisionamento permanece pendente.
3. Qual é a convenção dos extremos do intervalo de manutenção: `[início, fim)`, `[início, fim]` ou outra? Os resultados BDD-MAN-01 são candidatos compatíveis com o código atual, não uma decisão expressa no PRD.
4. Quais perfis podem executar cada transição de ciclo de vida e em que momento `NÃO_COMPARECEU` deve ser registrado?
5. O contrato OpenAPI deve documentar os status/códigos de erro e a diferença entre PATCH de status e a rota legada de aprovação; a documentação completa ainda está pendente.

## Destino dos testes

### Evidências de teste

- BDD-APR-01–04: criação em `PENDENTE`, aprovação/rejeição e bloqueio de autoaprovação/ator não autorizado via API com JWT.
- BDD-MAN-01: três exemplos de fronteira, verificando resposta HTTP e persistência.
- BDD-EST-01–03: fluxo principal, estados alternativos e transições inválidas, verificando estado persistido.
- `./mvnw -B verify` executa os cenários pelo runner Cucumber; o resultado mais recente no CI é o run `42859680966`.