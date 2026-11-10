# BDD — Aprovação e bloqueio de reservas

## Base e escopo

Os caminhos `docs/prd.md` e `docs/rtm.md` citados na solicitação não existem neste workspace. Foram consultados `prd/requisitos-funcionais.md`, `prd/rtm.md`, `prd/visão-geral.md` e o código de reservas.

O PRD define aprovação para recursos restritos (RF-07) e indisponibilidade durante a manutenção (RF-08). A RTM classifica RF-07/RC-06 e RC-05 (transições de estado) como bloqueados; RC-04 (manutenção) é parcial. Os cenários abaixo são especificações para testes futuros, não afirmações de que esses fluxos já estejam implementados.

Cada cenário contém exatamente um `Quando` e um `Então`. Respostas HTTP indicadas servem como resultados observáveis no endpoint de criação existente; para endpoints de aprovação e transição, que ainda não existem, o código HTTP permanece pendente de contrato.

## Regra 1 — Recurso restrito exige aprovação

### BDD-APR-01 — Solicitação restrita fica pendente

**Tipo:** cenário proposto a partir do RF-07.

```gherkin
Cenário: Solicitação de recurso restrito aguarda aprovação
  Dado que o recurso está classificado como restrito
  E que o solicitante tem permissão para solicitar uma reserva
  Quando o solicitante envia uma solicitação válida para esse recurso
  Então a reserva é registrada como SOLICITADA e não como confirmação final
```

### BDD-APR-02 — Responsável aprova a solicitação

```gherkin
Cenário: Responsável aprova uma solicitação restrita
  Dado que existe uma reserva restrita no estado SOLICITADA
  E que o ator é o responsável autorizado pelo recurso
  Quando o ator aprova a reserva
  Então a reserva passa para o estado APROVADA
```

## Regra 2 — Solicitante não aprova a própria reserva

### BDD-APR-03 — Solicitante tenta aprovar a própria reserva

```gherkin
Cenário: Solicitante não pode aprovar a própria reserva
  Dado que existe uma reserva restrita no estado SOLICITADA
  E que o ator é o solicitante dessa reserva
  Quando o ator tenta aprovar a reserva
  Então a operação é rejeitada e a reserva permanece SOLICITADA
```

### BDD-APR-04 — Usuário que não é responsável tenta aprovar

```gherkin
Cenário: Usuário sem papel de responsável não aprova uma reserva restrita
  Dado que existe uma reserva restrita no estado SOLICITADA
  E que o ator não é o responsável autorizado pelo recurso
  Quando o ator tenta aprovar a reserva
  Então a operação é rejeitada e a reserva permanece SOLICITADA
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
  | SOLICITADA    | APROVADA       |
  | APROVADA      | EM_USO         |
  | EM_USO        | CONCLUÍDA      |
```

### BDD-EST-02 — Estados alternativos

**Proposta sujeita à confirmação:** o PRD e a visão geral nomeiam `REJEITADA`, `CANCELADA` e `NÃO_COMPARECEU`, mas não especificam de quais estados cada um pode ser alcançado nem quem pode executar a transição.

```gherkin
Esquema do Cenário: Registrar um estado alternativo permitido
  Dado que a reserva está no estado <estado_origem>
  Quando um ator autorizado solicita a transição para <estado_destino>
  Então a operação é aceita e o estado persistido passa a <estado_destino>

Exemplos candidatos, pendentes de validação:
  | estado_origem | estado_destino |
  | SOLICITADA    | REJEITADA      |
  | SOLICITADA    | CANCELADA      |
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
  | SOLICITADA    | EM_USO         |
  | APROVADA      | CONCLUÍDA      |
  | CONCLUÍDA     | SOLICITADA     |
```

## Perguntas em aberto

1. Quais tipos de recurso ou combinações tornam uma reserva restrita e como essa classificação é configurada?
2. Como identificar o responsável autorizado por um recurso e qual mecanismo comprova o papel do ator? O RF-07 exige aprovação pelo responsável, mas perfis/permissões ainda precisam de validação.
3. Qual é a convenção dos extremos do intervalo de manutenção: `[início, fim)`, `[início, fim]` ou outra? Os resultados BDD-MAN-01 são candidatos compatíveis com o código atual, não uma decisão expressa no PRD.
4. Quais estados de origem permitem `REJEITADA`, `CANCELADA` e `NÃO_COMPARECEU`, quem pode realizar cada transição e em que momento `NÃO_COMPARECEU` deve ser registrado?
5. Quais códigos HTTP e formato de erro devem ser usados para aprovação negada e transição inválida? O contrato público ainda está pendente segundo a RTM.

## Destino dos testes

### Testes de unidade

- BDD-APR-01: estado inicial de solicitação restrita e ausência de confirmação final.
- BDD-APR-02: aprovação válida pelo responsável.
- BDD-APR-03: bloqueio de autoaprovação.
- BDD-APR-04: bloqueio de aprovação por ator sem papel autorizado.
- BDD-EST-01: cada uma das três transições principais.
- BDD-EST-02: transições alternativas, após confirmação das origens e atores permitidos.
- BDD-EST-03: rejeição de transições inválidas e preservação do estado.

### Testes de API

- BDD-APR-01: criação da solicitação restrita e estado retornado.
- BDD-APR-02: aprovação pelo responsável e estado retornado.
- BDD-APR-03: tentativa de autoaprovação rejeitada, sem alterar o estado.
- BDD-APR-04: tentativa por usuário não responsável rejeitada, sem alterar o estado.
- BDD-MAN-01: os três exemplos de fronteira, verificando resposta HTTP e persistência.
- BDD-EST-01: as três transições principais pelo endpoint público de estado.
- BDD-EST-02: transições alternativas pelo endpoint público, após definição do contrato.
- BDD-EST-03: transições inválidas rejeitadas e estado preservado.