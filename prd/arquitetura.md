# Arquitetura — baseline inicial

> Esta é a seção inicial de análise arquitetural, não uma decisão de arquitetura detalhada. Requisitos e decisões são citados pelas fontes existentes; lacunas permanecem abertas. Este documento não altera os requisitos funcionais ou não funcionais.

## 1. Contexto e objetivo

O Sistema de Organização de Recursos deve apoiar a consulta, alocação e gestão de salas, professores e materiais, com reservas e prevenção de conflitos de horário. O projeto é semestral e sua avaliação inclui evidências de qualidade, segurança, rastreabilidade e desempenho. A visão geral registra Java 21 e Spring Boot 3.x como tecnologias previstas, mas informa que arquitetura, persistência, autenticação e integrações ainda dependem de definição ([visão-geral.md](visão-geral.md), “Visão do produto” e “Contexto técnico e entrega”; [requisitos-não-funcionais.md](requisitos-não-funcionais.md), §§ 2 e 4).

Esta análise organiza os fatores que condicionam a arquitetura e explicita o que ainda precisa de decisão, sem escolher tecnologias ou estratégias não aprovadas.

**Restrição explícita desta solicitação:** existe um cron job executado diariamente ao final do dia para treinar ou retreinar usando somente dados novos. Essa restrição foi fornecida pelo usuário nesta solicitação; ela não aparece nos requisitos funcionais, nos requisitos não funcionais ou na visão geral revisados. Seu domínio, dados, modelo e critérios operacionais ainda não estão definidos. Registrá-la aqui não altera o escopo funcional aprovado.

## 2. Stakeholders e personas

| Perfil / stakeholder | Responsabilidade ou interesse documentado | Origem |
|---|---|---|
| **Solicitante** | Professor ou coordenador; consulta disponibilidade, cria, altera e cancela as próprias reservas. | [personas.md](personas.md), § 1; RF-03, RF-04 |
| **Responsável** | Avalia solicitações especiais, valida alocação de docentes e acompanha retiradas e devoluções de materiais. | [personas.md](personas.md), § 2; RF-07, RF-09 |
| **Administrador** | Mantém cadastros de salas, professores, materiais e usuários, além de bloqueios e manutenções. | [personas.md](personas.md), § 3; RF-02, RF-08 |
| **Equipe do projeto** | Desenvolve o projeto em equipe e consulta documentação de fluxos e contratos. Não é um perfil funcional do sistema. | [visão-geral.md](visão-geral.md), “Contexto técnico e entrega”; RF-14 |

As permissões detalhadas, a designação do Responsável e a confirmação de que esses são todos os perfis permanecem pendentes ([personas.md](personas.md), “Pendências transversais para validação”; RF-01).

## 3. Requisitos arquiteturalmente significativos

Os identificadores ASR abaixo servem apenas para organizar esta análise; não substituem nem modificam os identificadores RF/RNF existentes.

| ID | Necessidade que condiciona a arquitetura | Origem e rastreabilidade |
|---|---|---|
| ASR-01 | Impedir sobreposição de reservas e preservar integridade sob solicitações simultâneas: no máximo uma reserva conflitante pode ser aceita. | RF-05, RF-06; regras prioritárias 1–3 de [requisitos-funcionais.md](requisitos-funcionais.md); RNF-02; “Tempo e concorrência” em [visão-geral.md](visão-geral.md). |
| ASR-02 | Autenticar usuários e aplicar autorização por perfil às operações e também às chamadas fora da interface. | RF-01; RNF-01; personas de [personas.md](personas.md). |
| ASR-03 | Não confirmar reservas de recursos restritos antes da aprovação do Responsável; bloquear recursos durante manutenção. | RF-07, RF-08; regras prioritárias 4–5 de [requisitos-funcionais.md](requisitos-funcionais.md). |
| ASR-04 | Manter histórico consultável e rastreável de operações relevantes e mudanças de estado, sem apagar reservas iniciadas. | RF-10; RNF-07; “Estados e auditoria” em [visão-geral.md](visão-geral.md). |
| ASR-05 | Produzir evidências de teste e qualidade rastreáveis: JUnit 5, testes de integração/API/end-to-end, Testcontainers, WireMock, JaCoCo, SonarCloud e CI em pull requests; medir desempenho com JMeter. | RNF-03 a RNF-06; estratégia de qualidade e metas em [visão-geral.md](visão-geral.md). |
| ASR-06 | Executar diariamente, ao final do dia, um cron job de treinamento ou retreinamento, usando somente dados novos. | Restrição explícita da solicitação atual do usuário; ausente dos artefatos de requisitos revisados. |
| ASR-07 | Oferecer notificações simuladas ou integração com API externa e documentar fluxos públicos e, quando aplicável, contratos de API. | RF-11, RF-14; RNF-09. A opção de integração ainda não foi decidida. |

## 4. Atributos de qualidade

| Atributo | Expectativa documentada | Fonte / limite conhecido |
|---|---|---|
| **Segurança e autorização** | Restringir dados e operações conforme identidade e perfil; validar entradas e não expor detalhes sensíveis em erros. | RNF-01, RF-01; permissões por perfil ainda precisam de detalhamento. |
| **Integridade e concorrência** | Evitar dupla reserva e alterações parciais em operações conflitantes. | RNF-02, RF-05, RF-06. |
| **Desempenho mensurável** | Avaliar consulta de disponibilidade e criação de reserva com JMeter; registrar duração, volume e falhas. | RNF-03. Limites de resposta, carga, dados e taxa de erro não foram aprovados. |
| **Testabilidade e evidência** | Testes automatizados nas fronteiras pertinentes, cobertura com JaCoCo, RTM para requisitos críticos e execução de CI em pull requests. | RNF-04 a RNF-06; metas registradas na visão geral: 80% de linhas, 70% de branches e 100% de requisitos críticos cobertos pela RTM. |
| **Auditabilidade** | Identificar operação, usuário responsável e momento da ação; manter histórico consultável por usuários autorizados. | RNF-07, RF-10. Retenção e permissões de consulta ainda não estão definidas. |
| **Usabilidade e responsividade** | Manter fluxos utilizáveis em desktop, tablet e mobile, com mensagens compreensíveis e feedback explícito. | RNF-08, RF-13. Navegadores suportados ainda não foram definidos. |
| **Documentação** | Manter documentação dos fluxos públicos e contratos coerente com o comportamento implementado. | RNF-09, RF-14. |

## 5. Drivers arquiteturais priorizados

Prioridade abaixo é uma organização inicial para orientar decisões; não é uma nova priorização de negócio nem altera o status dos requisitos.

| Prioridade | Driver | Origem |
|---|---|---|
| **P1 — obrigatório** | Integridade de reservas sob conflito e concorrência. | RF-05, RF-06; regras prioritárias 1–3 de [requisitos-funcionais.md](requisitos-funcionais.md); RNF-02; regras críticas da [visão-geral.md](visão-geral.md). |
| **P1 — obrigatório** | Rotina diária de treinamento/retreinamento ao final do dia, limitada a dados novos. | Solicitação explícita do usuário nesta tarefa; não consta do baseline dos RF/RNF. |
| **P1 — obrigatório** | Autenticação, autorização por perfil e proteção de dados/operações. | RF-01; RNF-01; personas de [personas.md](personas.md). |
| **P1 — obrigatório** | Auditabilidade de reservas, recursos, decisões e movimentações de materiais. | RF-09, RF-10; RNF-07; regras de estados e auditoria da [visão-geral.md](visão-geral.md). |
| **P2 — evidência exigida** | Verificabilidade por testes, cobertura, rastreabilidade e CI. | RNF-04 a RNF-06; estratégia e metas da [visão-geral.md](visão-geral.md). |
| **P2 — mensuração exigida; limites pendentes** | Medir desempenho nos fluxos de disponibilidade e reserva. | RF-03, RF-04, RF-06; RNF-03; JMeter previsto na [visão-geral.md](visão-geral.md). |
| **P3 — comportamento exigido** | Fluxos responsivos e mensagens claras para os três perfis. | RF-13; RNF-08; necessidades em [personas.md](personas.md). |
| **P3 — decisão de integração pendente** | Notificar usuários e documentar fluxos/contratos públicos. | RF-11, RF-14; RNF-09. |

## 6. Glossário

| Termo | Significado no contexto documentado |
|---|---|
| **Recurso** | Sala, professor, material ou equipamento gerenciado ou alocado pelo sistema. |
| **Reserva** | Solicitação para alocar recursos em um período; pode ter estados e aprovação conforme as regras aplicáveis. |
| **Recurso restrito** | Recurso ou combinação de uso que exige aprovação do Responsável antes da confirmação (RF-07); os critérios para classificá-lo ainda não foram definidos. |
| **Conflito / sobreposição** | Interseção de horários de reservas ou indisponibilidades de um mesmo recurso, incluindo a agenda do professor (RF-05). |
| **RTM** | Matriz de rastreabilidade de requisitos, usada para relacionar requisitos críticos às evidências de teste (RNF-05). |
| **Cron job** | Tarefa agendada referida na solicitação atual para executar diariamente ao final do dia o treinamento/retreinamento usando somente dados novos. |
| **Dados novos** | Dados que a rotina diária pode usar conforme a restrição explícita do usuário. Critério de novidade, origem, janela e tratamento de repetição ainda não definidos. |

## 7. Riscos iniciais

| ID | Risco | Evidência / origem |
|---|---|---|
| R-01 | A rotina de treinamento/retreinamento pode estar fora do escopo atual de organização de recursos; não há RF/RNF que descreva domínio de modelo ou dados de treinamento. | Comparação da restrição da solicitação atual com o escopo de [visão-geral.md](visão-geral.md), RF-01 a RF-14 e RNF-01 a RNF-09. |
| R-02 | “Final do dia” e “somente dados novos” não definem fuso horário, corte temporal, fonte, deduplicação, falhas ou recuperação; implementações diferentes podem violar a restrição. | Solicitação atual do usuário; sem especificação operacional correspondente nos artefatos consultados. |
| R-03 | Sem estratégia arquitetural já decidida para concorrência, a aplicação pode aceitar reservas conflitantes ou deixar gravações parciais. | RF-05, RF-06; RNF-02; estratégia ainda pendente em RF, § 6. |
| R-04 | Ausência de limites quantitativos de desempenho impede um aceite objetivo de latência, carga ou taxa de erro. | RNF-03, seção “Parâmetros pendentes de aprovação”. |
| R-05 | Perfis, retenção de auditoria, política para reservas existentes diante de manutenção e comportamento da notificação ainda não estão definidos, afetando autorização, dados e contratos. | RF, § 6; RNF, § 5; pendências em [personas.md](personas.md). |

## 8. Fatos, lacunas, conflitos e suposições

### Fatos documentados

- O sistema organiza salas, professores e materiais e deve evitar conflitos de horário (visão geral; RF-05 e RF-06).
- Os perfis documentados são Solicitante, Responsável e Administrador ([personas.md](personas.md), §§ 1–3).
- Java 21 e Spring Boot 3.x são a stack **prevista**, não uma decisão detalhada de componentes ([visão-geral.md](visão-geral.md), “Contexto técnico e entrega”; RNF, § 4).
- Testes, ferramentas e metas de qualidade estão especificados nos RNF e na visão geral; metas de desempenho quantitativo não estão fixadas (RNF-03 a RNF-06).
- A rotina diária de treinamento usando somente dados novos é uma restrição expressa pelo usuário nesta solicitação, não um requisito existente nos arquivos de requisitos.

### Lacunas

- Banco de dados, persistência, autenticação concreta, arquitetura de implantação e contratos de integração não foram decididos (visão geral; RNF, § 4).
- Os limites de desempenho e as políticas de disponibilidade, recuperação, retenção e compatibilidade de navegadores permanecem pendentes (RNF, § 5).
- Os critérios de negócio e as permissões ainda não estão completos (RF, § 6; [personas.md](personas.md), “Pendências transversais para validação”).
- Para a rotina agendada, não estão especificados dados, modelo, critério de novidade, fuso/horário exato ou comportamento em falhas (solicitação atual).

### Conflitos e ambiguidades identificados

- **Não foi encontrado conflito textual inequívoco** entre os RF e RNF consultados. Há, porém, pontos que exigem conciliação antes de decisões arquiteturais:
  - A visão geral apresenta `SOLICITADA → APROVADA → EM_USO → CONCLUÍDA`, enquanto RF-07 condiciona aprovação a recursos restritos. Não está explícito como reservas sem restrição percorrem os estados.
  - RF-05 deixa a rejeição ou revisão de conflitos conforme regra a definir; RF-06 exige que, em concorrência conflitante, apenas uma operação seja aceita. A política para encaminhamento à revisão não está especificada.
  - O treinamento/retreinamento diário foi solicitado agora, mas o escopo documental existente é a organização de recursos. A relação da rotina com o produto precisa ser confirmada; a restrição é registrada sem presumir integração ou funcionalidade adicional.

### Suposições

- Nenhuma suposição de tecnologia, volume, disponibilidade, SLA, fonte de dados ou comportamento de negócio foi adotada.
- Java 21 e Spring Boot 3.x são tratados somente como tecnologias previstas nos documentos, não como decisões adicionais desta análise.
- A restrição do cron job é tratada como obrigatória porque foi explicitamente imposta pelo usuário; detalhes não fornecidos permanecem em aberto.

## 9. Perguntas abertas

1. A rotina de treinamento/retreinamento pertence ao escopo deste sistema? Qual dado/modelo será treinado e qual origem define “dados novos”?
2. Qual horário e fuso representam “ao final do dia”? O que fazer se a rotina falhar ou encontrar dados já processados?
3. Como o fluxo de estados se aplica a reservas que não exigem aprovação?
4. Em que casos um conflito deve ser rejeitado imediatamente ou encaminhado para revisão, sem permitir duas reservas válidas?
5. Quais perfis e permissões finais se aplicam, inclusive à consulta de auditoria?
6. Quais limites quantitativos de desempenho, carga e taxa de erro serão aprovados?
7. São necessários requisitos de disponibilidade, backup e recuperação? Se sim, quais metas?
8. Qual política de retenção e acesso será aplicada ao histórico auditável?
9. Qual mecanismo de notificação será usado: simulação no sistema ou integração externa?
10. Qual banco de dados, mecanismo de autenticação e arquitetura de implantação serão escolhidos?

## Referências consultadas

- [personas.md](personas.md)
- [requisitos-funcionais.md](requisitos-funcionais.md)
- [requisitos-não-funcionais.md](requisitos-não-funcionais.md)
- [visão-geral.md](visão-geral.md)
