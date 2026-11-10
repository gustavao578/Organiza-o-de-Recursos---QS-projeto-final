# language: pt
Funcionalidade: Aprovação e bloqueio de reservas
  Como pessoa usuária da Organização de Recursos
  Quero que aprovação, manutenção e transições de estado sejam respeitadas
  Para que reservas restritas e recursos indisponíveis sejam controlados

  Cenário: Solicitação de recurso restrito aguarda aprovação
    Dado que o recurso está classificado como restrito
    E que o solicitante tem permissão para solicitar uma reserva
    Quando o solicitante envia uma solicitação válida para esse recurso
    Então a reserva é registrada como SOLICITADA e não como confirmação final

  Cenário: Responsável aprova uma solicitação restrita
    Dado que existe uma reserva restrita no estado SOLICITADA
    E que o ator é o responsável autorizado pelo recurso
    Quando o ator aprova a reserva
    Então a reserva passa para o estado APROVADA

  Cenário: Solicitante não pode aprovar a própria reserva
    Dado que existe uma reserva restrita no estado SOLICITADA
    E que o ator é o solicitante dessa reserva
    Quando o ator tenta aprovar a reserva
    Então a operação é rejeitada e a reserva permanece SOLICITADA

  Cenário: Usuário sem papel de responsável não aprova uma reserva restrita
    Dado que existe uma reserva restrita no estado SOLICITADA
    E que o ator não é o responsável autorizado pelo recurso
    Quando o ator tenta aprovar a reserva
    Então a operação é rejeitada e a reserva permanece SOLICITADA

  Esquema do Cenário: Criar reserva na fronteira do período de manutenção
    Dado que o recurso tem manutenção de 2026-11-10T10:00:00 até 2026-11-10T11:00:00
    Quando o solicitante cria uma reserva de <inicio_reserva> até <fim_reserva>
    Então a resposta da API é <resultado_http> e a reserva <persistencia>

    Exemplos:
      | caso                                    | inicio_reserva      | fim_reserva         | resultado_http | persistencia       |
      | reserva termina no início da manutenção | 2026-11-10T09:00:00 | 2026-11-10T10:00:00 | 201            | é persistida       |
      | reserva começa no fim da manutenção     | 2026-11-10T11:00:00 | 2026-11-10T12:00:00 | 201            | é persistida       |
      | reserva cruza um segundo da manutenção  | 2026-11-10T09:00:00 | 2026-11-10T10:00:01 | 400            | não é persistida   |

  Esquema do Cenário: Executar uma transição do fluxo principal
    Dado que a reserva está no estado <estado_origem>
    Quando um ator autorizado solicita a transição para <estado_destino>
    Então a operação é aceita e o estado persistido passa a <estado_destino>

    Exemplos:
      | estado_origem | estado_destino |
      | SOLICITADA    | APROVADA       |
      | APROVADA      | EM_USO         |
      | EM_USO        | CONCLUÍDA      |

  Esquema do Cenário: Registrar um estado alternativo permitido
    Dado que a reserva está no estado <estado_origem>
    Quando um ator autorizado solicita a transição para <estado_destino>
    Então a operação é aceita e o estado persistido passa a <estado_destino>

    # Exemplos candidatos, pendentes de validação
    Exemplos:
      | estado_origem | estado_destino |
      | SOLICITADA    | REJEITADA      |
      | SOLICITADA    | CANCELADA      |
      | APROVADA      | NÃO_COMPARECEU |

  Esquema do Cenário: Rejeitar transição inválida
    Dado que a reserva está no estado <estado_origem>
    Quando um ator solicita a transição para <estado_destino>
    Então a operação é rejeitada e a reserva permanece em <estado_origem>

    Exemplos:
      | estado_origem | estado_destino |
      | SOLICITADA    | EM_USO         |
      | APROVADA      | CONCLUÍDA      |
      | CONCLUÍDA     | SOLICITADA     |