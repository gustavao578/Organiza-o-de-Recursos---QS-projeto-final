# Cenários BDD — Aprovação, bloqueio e estados de reserva

**Data:** 2026-11-09
**Escopo:** cenários derivados de RF-01, RF-05, RF-07, RF-08 e das regras críticas de [visão geral](../visão-geral.md). As expectativas marcadas como abertas não devem ser convertidas em asserts até decisão de produto.

## Situação da implementação

Os cenários abaixo especificam comportamento requerido, não descrevem funcionalidade já entregue. Na implementação inspecionada, `Reserva` não possui estado, não há atributo/catálogo de recurso restrito e não existem endpoints de aprovação ou transição. O serviço cria reserva e rejeita conflito de manutenção; o controller expõe criação, leitura e exclusão.

## 1. Recurso restrito exige aprovação de Responsável

**Rastreabilidade:** RF-07; RF-01; RC-06.

```gherkin
Funcionalidade: Aprovação de reserva de recurso restrito

  Cenário: Solicitação de recurso restrito não é confirmada antes da aprovação
    Dado que um Solicitante pede um recurso classificado como restrito
    E a solicitação ainda não foi avaliada por um Responsável
    Quando o sistema registra a solicitação
    Então a reserva permanece no estado SOLICITADA
    E não é considerada confirmada
    E o sistema informa que a aprovação de um Responsável é necessária

  Cenário: Responsável aprova uma solicitação de recurso restrito
    Dado que existe uma reserva de recurso restrito no estado SOLICITADA
    Quando um Responsável aprova a solicitação
    Então a reserva muda para o estado APROVADA
    E somente então pode seguir para a confirmação final prevista no fluxo
```

A classificação de quais recursos são restritos e a associação entre recurso e Responsável ainda precisam ser definidas. O fluxo principal da visão geral dá suporte aos estados `SOLICITADA` e `APROVADA`, mas não detalha o mecanismo de confirmação final.

## 2. Solicitante não aprova a própria reserva

**Rastreabilidade:** RF-01, critério “solicitante não pode aprovar uma solicitação restrita”; RF-07; RC-06.

```gherkin
Funcionalidade: Separação entre solicitante e aprovador

  Cenário: Solicitante tenta aprovar a própria reserva restrita
    Dado que um Solicitante criou uma reserva de recurso restrito
    E a reserva permanece no estado SOLICITADA
    Quando o mesmo Solicitante tenta aprová-la
    Então a operação é negada
    E a reserva permanece no estado SOLICITADA
    E a reserva não é considerada confirmada
```

A regra de autorização é explícita no RF-01/RF-07. Autenticação, identificação do ator e endpoint de aprovação ainda não existem no código observado.

## 3. Recurso em manutenção não pode ser reservado

**Rastreabilidade:** RF-05, RF-08; RC-04.

```gherkin
Funcionalidade: Bloqueio de reserva durante manutenção

  Cenário: Reserva cujo período se sobrepõe à manutenção é rejeitada
    Dado que um recurso possui manutenção registrada entre 10:00:00 e 11:00:00
    Quando um Solicitante tenta reservá-lo entre 10:30:00 e 10:45:00
    Então a reserva é rejeitada com uma mensagem clara de indisponibilidade
    E nenhuma reserva é persistida para a solicitação

  Cenário: Reserva cruza o início da manutenção por um segundo
    Dado que um recurso possui manutenção registrada entre 10:00:00 e 11:00:00
    Quando um Solicitante tenta reservá-lo entre 09:59:59 e 10:00:01
    Então a reserva é rejeitada porque há sobreposição com a manutenção
    E nenhuma reserva é persistida para a solicitação
```

### Exemplos de fronteira do período

Considere manutenção de `10:00:00` a `11:00:00` no mesmo dia. Os horários são exemplos de análise, não definem fuso horário nem se os extremos do intervalo são inclusivos.

| Exemplo | Período da reserva | Resultado no código atual | Expectativa a fixar no BDD | Pergunta em aberto |
|---|---|---|---|---|
| Reserva termina exatamente no início da manutenção | `09:00:00–10:00:00` | A consulta JPA atual não identifica sobreposição (`inícioManutenção < fimReserva` é falso na igualdade); a reserva não é bloqueada por essa consulta. | **Pendente** | Um período que termina no instante exato do início da manutenção é adjacente e permitido, ou o limite é inclusivo e deve ser bloqueado? |
| Reserva começa exatamente no fim da manutenção | `11:00:00–12:00:00` | A consulta JPA atual não identifica sobreposição (`fimManutenção > inícioReserva` é falso na igualdade); a reserva não é bloqueada por essa consulta. | **Pendente** | Um período que começa no instante exato do fim da manutenção é adjacente e permitido, ou o limite é inclusivo e deve ser bloqueado? |
| Reserva cruza a manutenção por um segundo | `09:59:59–10:00:01` | A consulta atual identifica sobreposição; o serviço rejeita a reserva. | **Bloquear** | Não há dúvida para uma sobreposição que ocupa um intervalo positivo; confirmar apenas fuso/precisão temporal do contrato se necessário. |

A regra RF-08 declara indisponível qualquer intervalo em manutenção, mas o PRD não decide a semântica exata dos extremos. Não se deve transformar o comportamento observado do SQL em política de produto sem responder às duas perguntas acima. A comparação implementada é estrita: `inícioManutenção < fimReserva` e `fimManutenção > inícioReserva`.

## 4. Reserva muda somente pelas transições declaradas

**Rastreabilidade:** regras críticas “Estados e auditoria”; RF-07; RNF-07; RC-05 e RC-08.

```gherkin
Funcionalidade: Transições de estado da reserva

  Esquema do Cenário: Avançar pelo fluxo principal
    Dado que a reserva está no estado <estado_atual>
    Quando a transição para <proximo_estado> é solicitada
    Então a mudança é permitida
    E o novo estado é <proximo_estado>
    E a mudança gera um registro de auditoria

    Exemplos:
      | estado_atual | proximo_estado |
      | SOLICITADA   | APROVADA       |
      | APROVADA     | EM_USO         |
      | EM_USO       | CONCLUÍDA      |

  Cenário: Não permitir saltar etapas do fluxo principal
    Dado que a reserva está no estado SOLICITADA
    Quando é solicitada a transição direta para EM_USO
    Então a transição é rejeitada
    E a reserva permanece no estado SOLICITADA
    E nenhum evento de mudança de estado é registrado como concluído
```

A visão geral enumera `REJEITADA`, `CANCELADA` e `NÃO_COMPARECEU` como estados alternativos, mas não define de quais estados cada um pode ser alcançado, por qual ator, nem as condições para a transição. Por isso, os seguintes rascunhos ainda não têm resultado assertivo e não são testes executáveis:

```gherkin
Cenário pendente: Rejeitar uma solicitação
  Dado que existe uma reserva em um estado ainda não determinado
  Quando o ator autorizado solicita REJEITADA
  Então a transição e o ator permitido aguardam decisão de produto

Cenário pendente: Cancelar uma reserva
  Dado que existe uma reserva em um estado ainda não determinado
  Quando um ator solicita CANCELADA
  Então os estados de origem permitidos, o ator e a justificativa aguardam decisão

Cenário pendente: Registrar não comparecimento
  Dado que existe uma reserva em um estado ainda não determinado
  Quando um ator solicita NÃO_COMPARECEU
  Então o estado de origem, o ator e o momento permitido aguardam decisão
```

**Perguntas abertas para as transições alternativas:**

- De quais estados pode ocorrer `REJEITADA`? Quem pode rejeitar e é obrigatório registrar motivo?
- Em quais estados pode ocorrer `CANCELADA`, por qual ator e com que justificativa?
- Quando pode ocorrer `NÃO_COMPARECEU`, quem registra e como distinguir uma reserva concluída?
- Tentativas repetidas, concorrentes ou fora de ordem devem ser rejeitadas com qual resultado e mensagem?
- Toda tentativa negada também deve gerar evento de auditoria? RF-01 exige registro de acesso não autorizado, mas a política específica de auditoria de transição não está descrita.

## 5. Destino dos testes

| Cenário | Tipo de teste indicado | Evidência/assert principal |
|---|---|---|
| Recurso restrito aguarda aprovação; Responsável aprova | Unidade de domínio/serviço para regra de autorização e transição, quando implementada. | Estado só avança de `SOLICITADA` para `APROVADA` após aprovação do perfil correto. |
| Solicitante tenta aprovar a própria reserva | Unidade para política de autorização se isolável; também API para validar o ator autenticado e o contrato HTTP. | Operação negada; estado continua `SOLICITADA`; sem confirmação nem mutação parcial. |
| Transições principais permitidas e salto de etapa negado | Unidade para a máquina/política de estados; API para validar que os endpoints aplicam a mesma regra. | Transições autorizadas mudam o estado; transição inválida mantém estado e não registra auditoria de sucesso. |
| `REJEITADA`, `CANCELADA`, `NÃO_COMPARECEU` | Não escrever teste de resultado até as perguntas abertas serem decididas; depois, unidade para a regra de transição e API para autorização/contrato. | Registrar origem, ator, pré-condições, motivo e resultado esperado antes de fixar asserts. |
| Reserva em manutenção, inclusive cruzamento por um segundo | API com persistência de manutenção e reserva para verificar resposta e ausência de gravação; teste de integração de repositório para isolar a consulta temporal. | Sobreposição positiva rejeitada; verificar status/mensagem e que não houve persistência. |
| Reserva termina exatamente no início ou começa exatamente no fim da manutenção | Manter como testes pendentes de decisão; após decisão, preferir API com dados persistidos e também cobrir a consulta de repositório. | Assert de aceitar/rejeitar só depois de aprovada a semântica inclusiva/exclusiva. |

Nenhum cenário acima foi executado porque as regras de aprovação/estado não estão implementadas; este artefato descreve cobertura futura e questões de produto, não altera o comportamento de produção.
