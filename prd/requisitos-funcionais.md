# PRD — Requisitos Funcionais do Sistema de Organização de Recursos

## 1. Objetivo do documento

Este documento estabelece os requisitos funcionais da aplicação de organização de recursos, com foco em salas, professores e materiais, além das regras de autorização, disponibilidade, reservas, manutenção, aprovações e rastreabilidade. Ele complementa a visão geral do projeto e as personas definidas em [personas.md](personas.md).

A finalidade principal é transformar o escopo inicial em regras de negócio claras, observáveis e testáveis, permitindo que a implementação, a validação e a entrega do projeto sigam uma base consistente.

## 2. Escopo funcional

O sistema deve permitir:

- autenticação e autorização por perfil;
- cadastro e consulta de salas, professores e materiais;
- pesquisa por tipo, capacidade, localização, competência e disponibilidade;
- criação, alteração e cancelamento de reservas;
- verificação de sobreposição de horários, inclusive na agenda do professor;
- proteção contra dupla reserva em solicitações simultâneas;
- aprovação obrigatória para recursos restritos;
- bloqueio de recursos em manutenção;
- registro de retirada e devolução de materiais e equipamentos;
- histórico auditável de mudanças;
- notificação simulada ou integração com API externa;
- relatórios de utilização por recurso, carga horária alocada e conflitos evitados;
- interface responsiva com mensagens de erro compreensíveis;
- documentação dos fluxos públicos e da API.

## 3. Perfis e responsabilidades

### 3.1 Solicitante

O solicitante terá acesso para:

- consultar disponibilidade;
- criar reservas;
- alterar e cancelar suas próprias reservas;
- visualizar informações relevantes sobre o recurso solicitado.

### 3.2 Responsável

O responsável terá acesso para:

- avaliar solicitações especiais;
- validar a alocação de docentes;
- acompanhar a retirada e a devolução de materiais;
- visualizar histórico e pendências relacionadas aos recursos sob sua responsabilidade.

### 3.3 Administrador

O administrador terá acesso para:

- cadastrar e manter dados de salas, professores e materiais;
- gerenciar usuários;
- registrar bloqueios e períodos de manutenção;
- manter os dados e regras que impactam disponibilidade e reservas.

## 4. Requisitos funcionais

### RF-01 — Autenticação e autorização por perfil

**Descrição:** o sistema deve identificar o usuário autenticado e conceder acesso conforme o perfil associado, limitando ações e dados de acordo com a responsabilidade do papel.

**Regras de negócio:**

- cada usuário deve possuir um perfil único ou um conjunto de perfis autorizados;
- ações fora do escopo do perfil devem ser negadas com mensagem clara;
- autenticação deve impedir acesso não autorizado a dados e operações sensíveis;
- operações de gerenciamento devem exigir autorização específica do administrador ou do responsável.

**Critérios de aceite:**

- usuário sem autenticação não acessa recursos protegidos;
- solicitante não pode aprovar uma solicitação restrita;
- administrador não pode alterar o perfil de outro usuário sem ação autorizada;
- o sistema registra tentativas de acesso não autorizado.

---

### RF-02 — Cadastro e consulta de salas, professores e materiais

**Descrição:** o sistema deve permitir o cadastro, a consulta, a atualização e a manutenção de recursos e pessoas relevantes para a organização do uso dos ambientes e materiais.

**Dados mínimos esperados:**

- salas: identificador, nome, tipo, capacidade, localização e status;
- professores: identificador, nome, competência, disponibilidade e agenda;
- materiais: identificador, nome, tipo, quantidade, estado, localização e regras de empréstimo.

**Critérios de aceite:**

- recursos podem ser cadastrados com dados mínimos válidos;
- o sistema rejeita registros duplicados ou inconsistentes;
- consultas permitem filtros por tipo, capacidade, localização e disponibilidade;
- o cadastro de materiais e salas deve refletir seu status atual em reservas e manutenções.

---

### RF-03 — Pesquisa por tipo, capacidade, localização, competência e disponibilidade

**Descrição:** o sistema deve oferecer busca e filtros para encontrar recursos adequados ao uso solicitado.

**Filtros obrigatórios:**

- tipo de sala, professor ou material;
- capacidade ou quantidade mínima necessária;
- localização física;
- competência do professor ou tipo de especialização;
- disponibilidade em data e horário.

**Critérios de aceite:**

- busca por capacidade retorna apenas recursos compatíveis com a demanda;
- professores podem ser filtrados por competência ou especialização;
- salas podem ser filtradas por tipo e localidade;
- a busca considera indisponibilidade por manutenção, conflito de horário e recurso bloqueado.

---

### RF-04 — Criação, alteração e cancelamento de reservas

**Descrição:** o sistema deve permitir ao solicitante registrar uma reserva e realizar ajustes no período solicitado.

**Fluxo esperado:**

- cadastro de nova reserva;
- validação do recurso e do período;
- confirmação ou rejeição da reserva conforme regras de negócio;
- alteração de data, horário, professor, material ou recurso associado;
- cancelamento com justificativa quando aplicável.

**Critérios de aceite:**

- reservas só podem ser alteradas pelo solicitante responsável ou por usuário com permissão atribuída;
- alterações devem manter rastreabilidade da operação;
- cancelamentos devem registrar motivo, responsável e horário da ação;
- reservas canceladas não podem permanecer disponíveis como vigentes.

---

### RF-05 — Detecção de sobreposição de horários, inclusive na agenda do professor

**Descrição:** o sistema deve impedir que a mesma sala, material, professor ou recurso seja alocado em horários conflitantes.

**Conflitos a detectar:**

- sobreposição de reservas de sala;
- sobreposição de horários de professor;
- sobreposição de materiais ou equipamentos;
- conflitos entre reserva e manutenção;
- conflito entre alocação de professor e agenda individual.

**Critérios de aceite:**

- sistema exibe aviso ao usuário quando uma reserva cria sobreposição;
- a validação inclui a agenda do professor, mesmo quando a sala e o material estejam disponíveis;
- reservas conflitantes são rejeitadas ou encaminhadas para revisão conforme regra definida;
- mensagem de erro deve indicar claramente o recurso e o intervalo conflitante.

---

### RF-06 — Proteção contra dupla reserva em solicitações simultâneas

**Descrição:** o sistema deve garantir consistência em cenários de concorrência, evitando criação de duas reservas válidas para o mesmo recurso no mesmo intervalo.

**Regras de negócio:**

- transações de criação e alteração de reserva devem ser atomicas;
- verificação de disponibilidade e gravação devem ocorrer em um único processo de validação;
- solicitações simultâneas devem ser tratadas por controle de concorrência ou bloqueio de registro;
- um conflito detectado por concorrência deve gerar resposta explícita ao usuário.

**Critérios de aceite:**

- duas requisições simultâneas não podem concluir duas reservas conflitantes para o mesmo recurso;
- leitura e escrita devem preservar integridade sem race condition;
- no caso de conflito, uma das operações é rejeitada e a outra permanece válida.

---

### RF-07 — Aprovação obrigatória para recursos restritos

**Descrição:** alguns recursos ou combinações de uso devem exigir aprovação antes da confirmação final da reserva.

**Exemplos de recursos restritos:**

- salas com utilização sensível;
- materiais de alto custo, risco ou uso controlado;
- combinações com professor ou competência específica;
- recursos reservados por regras institucionais ou administrativas.

**Critérios de aceite:**

- reserva de recurso restrito somente é confirmada após aprovação do responsável;
- status da reserva deve refletir pendência de aprovação, aprovada, recusada ou cancelada;
- o sistema deve informar o motivo da exigência de aprovação;
- recursos restritos não podem ser comprometidos antes da aprovação final.

---

### RF-08 — Bloqueio de recursos em manutenção

**Descrição:** recursos em manutenção ou indisponíveis temporariamente devem ser bloqueados para qualquer nova reserva ou uso.

**Regras de negócio:**

- manutenção deve ser registrada com data inicial, data final e descrição;
- qualquer intervalo em manutenção torna o recurso indisponível para reserva;
- o bloqueio deve refletir no calendário e nos filtros de disponibilidade;
- o status do recurso deve indicar manutenção ativa, cancelada ou concluída.

**Critérios de aceite:**

- recurso em manutenção não aparece como disponível para reserva;
- reserva atual que cruza com manutenção deve ser sinalizada ou rejeitada conforme regra;
- manutenção cadastrada em data passada deve ser registrada como encerrada;
- o administrador pode rastrear o histórico do bloqueio.

---

### RF-09 — Registro de retirada e devolução de materiais e equipamentos

**Descrição:** o sistema deve registrar o ciclo de empréstimo, retirada, uso e devolução de materiais e equipamentos.

**Informações obrigatórias:**

- material ou equipamento;
- responsável pela retirada;
- data e hora da retirada;
- data e hora da devolução;
- condição do material no retorno;
- observações e pendências.

**Critérios de aceite:**

- a retirada só pode ocorrer com autenticação e autorização do responsável;
- devolução com atraso ou dano deve ser identificada no histórico;
- materiais não devolvidos aparecem como pendentes;
- o sistema mantém o status de cada item e o histórico de movimentação.

---

### RF-10 — Histórico auditável de mudanças

**Descrição:** todas as alterações relevantes no sistema devem ser registradas em histórico auditável para rastreabilidade.

**Eventos que devem ser registrados:**

- cadastro, alteração e exclusão de recursos;
- criação, alteração e cancelamento de reservas;
- aprovação ou rejeição de solicitações;
- bloqueios e manutenção;
- retirada e devolução de materiais;
- mudanças de perfil, permissões e dados sensíveis.

**Critérios de aceite:**

- cada ação registra usuário, data, hora, operação e antes/depois quando aplicável;
- o histórico é consultável por usuário autorizado;
- alterações críticas não podem ser apagadas sem justificativa e auditoria;
- o histórico sustenta a análise de incidentes e controvérsias.

---

### RF-11 — Notificação simulada ou integração com API externa

**Descrição:** o sistema deve informar usuários sobre o estado das reservas, aprovações e pendências por meio de mecanismo de notificação.

**Opções aceitáveis:**

- notificação simulada no sistema;
- integração com API externa de e-mail, push ou webhook.

**Critérios de aceite:**

- o usuário recebe confirmação de criação, alteração, cancelamento ou aprovação;
- notificação é enviada em caso de conflito ou rejeição;
- pendências relevantes são informadas ao responsável ou solicitante;
- a notificação pode ser registrada no histórico da operação.

---

### RF-12 — Relatório de utilização por recurso, carga horária alocada e conflitos evitados

**Descrição:** o sistema deve gerar relatórios que permitam avaliar a utilização dos recursos e o impacto das regras de prevenção de conflitos.

**Métricas esperadas:**

- utilização por recurso;
- carga horária alocada por sala, professor e material;
- volume de reservas aprovadas e canceladas;
- número de conflitos evitados por validação automática;
- quantidade de solicitações que exigiram aprovação.

**Critérios de aceite:**

- relatórios podem ser consultados por administrador ou responsável autorizados;
- métricas refletem períodos selecionados pelo usuário;
- dados de utilização e prevenção de conflitos são compreensíveis e comparáveis;
- o sistema oferece exportação ou visualização simples dos resultados.

---

### RF-13 — Interface responsiva e mensagens de erro compreensíveis

**Descrição:** a aplicação deve oferecer uma interface adaptável a diferentes dispositivos, com clareza na comunicação e orientação ao usuário.

**Requisitos de interface:**

- layout responsivo para desktop, tablet e mobile;
- mensagens claras para erros, conflitos, bloqueios e rejeições;
- indicação de passos necessários para correção de inconsistências;
- feedback explícito após operações bem-sucedidas ou rejeitadas.

**Critérios de aceite:**

- a interface exibe erros em linguagem compreensível, sem jargões técnicos;
- campos críticos são identificados visualmente;
- confirmações e rejeições têm status visual adequado;
- o fluxo é utilizável em telas de diferentes tamanhos.

---

### RF-14 — Documentação de API ou dos fluxos públicos do sistema

**Descrição:** o sistema deve possuir documentação dos fluxos públicos e, quando aplicável, da API do projeto.

**Documentação mínima esperada:**

- descrição dos principais fluxos de uso;
- endpoints ou operações públicas;
- regras de autenticação e autorização;
- exemplos de requisições e respostas;
- erros e códigos esperados.

**Critérios de aceite:**

- a documentação está disponível para consulta por integrantes do projeto ou usuários finais;
- os fluxos principais estão registrados e consistentes com a implementação;
- a API ou os contratos públicos refletem exatamente os comportamentos previstos dos requisitos.

## 5. Regras de negócio prioritárias

As regras abaixo devem ser consideradas obrigatórias para a implementação inicial:

1. Reserva só pode ser criada quando o recurso está disponível no período solicitado.
2. Sobreposição de horários deve ser bloqueada, incluindo conflitos na agenda do professor.
3. Solicitações simultâneas devem ser tratadas de forma segura para evitar dupla reserva.
4. Recursos restritos exigem aprovação do responsável antes da confirmação final.
5. Recursos em manutenção devem ser considerados indisponíveis até a data de fim do bloqueio.
6. A retirada e a devolução de materiais devem ser registradas e rastreadas.
7. Alterações relevantes devem ser armazenadas em histórico auditável.
8. Notificações e erros devem ser claros e compreensíveis para o usuário.

## 6. Pontos que precisam de validação

Os itens abaixo precisam ser confirmados antes da implementação final do sistema, porque influenciam regras, permissões, UX e arquitetura:

1. **Definição exata dos perfis e permissões**: confirmar papéis, responsabilidades e limites de ação de solicitante, responsável e administrador.
2. **Regra de aprovação de recursos restritos**: definir quais recursos são restritos, quem aprova, quais prazos e como a rejeição é tratada.
3. **Critérios de conflito**: validar como o sistema classifica sobreposição de horários e quando a reserva deve ser rejeitada ou encaminhada para aprovação.
4. **Concorrência e integridade**: confirmar a estratégia de locking, transactionality e tratamento de requests em paralelo.
5. **Manutenção e bloqueios**: estabelecer como o administrador registra bloqueios, como os conflitos são exibidos e como o sistema trata reservas existentes no período de manutenção.
6. **Retirada e devolução de materiais**: validar regras de disponibilidade do item, status de dano, atraso e pendências de devolução.
7. **Histórico auditável**: confirmar quem pode consultar o log, quais dados são armazenados e por quanto tempo.
8. **Notificações**: decidir se as notificações serão simuladas em interface ou por integração com API externa.
9. **Relatórios e métricas**: validar quais indicadores serão avaliados e como eles serão exibidos.
10. **Contrato de API e documentação pública**: definir público-alvo, endpoints, autenticação e formato das respostas.
11. **Experiência do usuário**: confirmar mensagens e fluxos para evitar erros incompreensíveis e atender ao requisito de responsividade.

## 7. Próximos passos para continuidade do projeto

1. **Validar perfis e permissões** com usuários reais e com as personas definidas em [personas.md](personas.md).
2. **Definir regras de negócio detalhadas** para cada recurso, incluindo manutenção, aprovação e conflitos.
3. **Especificar o modelo de dados** para salas, professores, materiais, reservas, históricos e notificações.
4. **Definir a arquitetura do sistema** com autenticação, autorização, persistência e integração de notificações.
5. **Mapear fluxos de usuários** para criação, alteração, cancelamento, aprovação e devolução.
6. **Estabelecer critérios de aceite e testes** para cada requisito funcional priorizado.
7. **Documentar a API ou fluxo público** antes da implementação completa, mantendo alinhamento com o código final.
8. **Implementar em ciclos incrementais**, começando pelos casos mais críticos: autenticação, disponibilidade, conflito e reservas.
9. **Validar com evidências de qualidade** por meio de testes, revisão de regras e análise de riscos.
10. **Atualizar continuamente este PRD** conforme decisões de negócio forem confirmadas.

## 8. Conclusão

Este PRD consolida os requisitos mínimos exigidos para o projeto de organização de recursos, incluindo autenticação, controle de reservas, manutenção, autorização, auditoria, relatórios e interface. A maior parte do valor do sistema está na capacidade de impedir conflitos, garantir integridade de alocações e oferecer rastreabilidade para decisões e ações do negócio.

A etapa seguinte deve priorizar a validação dos pontos críticos listados acima, de modo a transformar este documento em base para a implementação e para a verificação de qualidade do sistema.
