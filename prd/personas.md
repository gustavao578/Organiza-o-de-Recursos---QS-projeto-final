# Personas

## Objetivo deste documento

Registrar os perfis que interagem com o sistema de Organização de Recursos, suas responsabilidades, objetivos, necessidades de informação e perguntas que precisam responder ao usar a aplicação. As personas orientam a elicitação de requisitos, os fluxos, as permissões e os testes; não substituem regras de negócio ainda não definidas.

## 1. Solicitante

### Perfil

Professor ou coordenador que consulta a disponibilidade de recursos e gerencia as próprias reservas. Esse perfil foi definido na especificação inicial; não estão definidos outros atributos demográficos ou organizacionais.

### Objetivos

- Consultar a disponibilidade de salas, professores e materiais relevantes à reserva.
- Criar uma reserva para uma necessidade de atividade.
- Alterar ou cancelar suas reservas.
- Evitar solicitar recursos indisponíveis ou gerar conflito de horário.

### Necessidades de informação

- Disponibilidade dos recursos para o período consultado.
- Recursos associados à reserva e seu estado atual.
- Resultado da solicitação e eventuais conflitos ou impedimentos.
- Situação de uma reserva após criação, alteração ou cancelamento.

Os detalhes de filtros, informações exibidas e estados da reserva precisam ser confirmados nos requisitos.

### Perguntas que a aplicação precisa responder

- Quais salas, professores e materiais estão disponíveis no período desejado?
- A reserva que pretendo criar pode ser atendida sem conflito?
- Quais recursos e horários estão associados à minha reserva?
- Minha solicitação, alteração ou cancelamento foi registrado?
- Há alguma pendência ou impedimento para a minha reserva?

A última pergunta depende da definição de estados, aprovações e mensagens do fluxo.

## 2. Responsável

### Perfil

Pessoa responsável por aprovar solicitações especiais, validar a alocação de docentes e acompanhar retiradas e devoluções de materiais. A especificação não define como esse perfil é designado nem quais unidades ou recursos ficam sob sua responsabilidade.

### Objetivos

- Avaliar e aprovar ou encaminhar solicitações especiais conforme regras a definir.
- Validar a alocação de docentes.
- Acompanhar a retirada e a devolução de materiais.
- Identificar pendências relacionadas às atividades sob sua responsabilidade.

### Necessidades de informação

- Solicitações especiais que aguardam análise e seus dados relevantes.
- Alocações de docentes que precisam de validação.
- Materiais retirados, devoluções esperadas e situação do acompanhamento.
- Histórico das decisões e atualizações necessárias para rastreabilidade.

A lista de dados, alertas, prazos e histórico disponível para este perfil ainda precisa ser validada.

### Perguntas que a aplicação precisa responder

- Quais solicitações especiais estão aguardando minha análise?
- Quais dados e recursos estão envolvidos em cada solicitação?
- A alocação de docentes foi validada?
- Quais materiais foram retirados e quais precisam ser devolvidos?
- Qual é o estado atual e o histórico das ações que acompanho?

Os critérios de aprovação, validação e acompanhamento ainda precisam ser definidos.

## 3. Administrador

### Perfil

Pessoa que mantém os cadastros e configurações operacionais descritos na especificação: salas, professores, materiais, usuários, bloqueios e períodos de manutenção. A especificação não detalha níveis administrativos ou processo de gestão de usuários.

### Objetivos

- Gerenciar salas, professores e materiais disponíveis no sistema.
- Gerenciar usuários.
- Registrar bloqueios e períodos de manutenção que afetem a disponibilidade.
- Manter os dados necessários para que os demais perfis consultem e solicitem recursos.

### Necessidades de informação

- Dados e situação dos recursos e usuários sob gestão.
- Bloqueios e períodos de manutenção cadastrados.
- Efeito das alterações administrativas sobre a disponibilidade e as reservas.
- Registro das mudanças para permitir acompanhamento e rastreabilidade.

Os campos, operações, validações e visibilidade do histórico precisam ser decididos nos requisitos.

### Perguntas que a aplicação precisa responder

- Quais salas, professores, materiais e usuários estão cadastrados?
- Quais recursos estão bloqueados ou em manutenção e em quais períodos?
- Uma alteração administrativa afeta reservas ou disponibilidades existentes?
- A alteração realizada foi registrada e pode ser rastreada?

As regras para alterações que afetem reservas existentes e o nível de auditoria ainda não foram especificados.

## Pendências transversais para validação

- Quais ações cada perfil pode executar e sobre quais reservas ou recursos?
- Como o sistema autentica e identifica cada perfil?
- Quais dados são obrigatórios para consultar disponibilidade e solicitar uma reserva?
- Quais são os estados possíveis de uma reserva e suas transições?
- Quais critérios definem conflito, disponibilidade e solicitação especial?
- Como são registrados decisões, alterações, retiradas e devoluções?
- Quais informações devem ser protegidas e quais evidências de segurança são esperadas?

Esses itens são perguntas para detalhamento; não representam funcionalidades ou políticas já aprovadas.

## Próximos passos

1. Revisar os três perfis com a equipe e confirmar se representam todos os usuários do escopo. Objetivo: evitar lacunas de atores antes da definição dos requisitos.
2. Mapear cenários principais e exceções de cada perfil, incluindo consulta, reserva, aprovação, validação, manutenção e gestão de materiais. Objetivo: identificar requisitos e critérios de aceite observáveis.
3. Definir permissões e dados acessíveis por perfil. Objetivo: orientar os requisitos de segurança e os testes de autorização.
4. Converter as perguntas de cada persona em requisitos rastreáveis e testes. Objetivo: verificar que a aplicação responde às necessidades reais dos usuários.
5. Atualizar este documento quando responsabilidades ou necessidades forem validadas ou alteradas. Objetivo: manter as decisões de produto e a documentação alinhadas.

## Histórico de atualização

- **2026-10-19:** criação das personas iniciais com base exclusivamente nos três perfis da especificação fornecida; detalhes não especificados foram mantidos como pendências de validação.
