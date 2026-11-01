# PRD — Requisitos Não Funcionais do Sistema de Organização de Recursos

## 1. Objetivo

Este documento define os requisitos de qualidade e as restrições técnicas do Sistema de Organização de Recursos. Os requisitos foram derivados da [visão geral do projeto](visão-geral.md), das [personas](personas.md) e dos [requisitos funcionais](requisitos-funcionais.md), sem acrescentar políticas de negócio não aprovadas.

Os requisitos não funcionais (RNFs) estabelecem como o sistema deve proteger e manter a integridade dos dados, responder sob carga, ser verificado e apresentar seus fluxos. Cada requisito indica evidências que podem ser usadas para sua validação.

## 2. Contexto e convenções

- **Escopo:** aplicação de reservas e gestão de salas, professores e materiais, usada por solicitantes, responsáveis e administradores.
- **Stack prevista:** Java 21 e Spring Boot 3.x.
- **Obrigatoriedade:** “deve” indica requisito do projeto; critérios ainda sem parâmetro aprovado estão identificados como pendências, não como metas implícitas.
- **Rastreabilidade:** os RNFs referenciam requisitos funcionais (RF) quando reforçam ou qualificam um comportamento já especificado.

## 3. Requisitos não funcionais

### RNF-01 — Segurança e autorização

O sistema deve proteger operações e dados conforme a identidade autenticada e as permissões do perfil do usuário.

**Critérios de verificação:**

- operações protegidas não podem ser acessadas sem autenticação;
- operações não autorizadas para o perfil devem ser negadas, inclusive quando chamadas fora da interface;
- entradas devem ser validadas antes de serem processadas;
- erros apresentados ao usuário não devem expor detalhes técnicos ou informações sensíveis;
- tentativas de acesso não autorizado devem ser registradas;
- a entrega não deve conter vulnerabilidades críticas conhecidas.

**Rastreabilidade:** RF-01; personas Solicitante, Responsável e Administrador; avaliação de autorização, validação de entrada e tratamento seguro de erros definida na visão geral.

### RNF-02 — Integridade e concorrência

O sistema deve preservar a consistência das reservas quando operações conflitantes forem solicitadas ao mesmo tempo.

**Critérios de verificação:**

- em solicitações simultâneas conflitantes para o mesmo recurso e período, no máximo uma reserva pode ser aceita;
- validação de disponibilidade e gravação da reserva devem preservar a atomicidade da operação;
- uma operação rejeitada por conflito não pode deixar alterações parciais nem comprometer a disponibilidade;
- deve existir teste automatizado de concorrência que demonstre esse comportamento.

**Rastreabilidade:** RF-04, RF-05 e RF-06; regra crítica de tempo e concorrência na visão geral.

### RNF-03 — Desempenho mensurável

O desempenho deve ser avaliado com JMeter nos fluxos de consulta de disponibilidade e criação de reserva, incluindo cenários de concorrência relevantes.

**Critérios de verificação:**

- os testes devem registrar, no mínimo, duração das requisições, volume de requisições e falhas observadas;
- o relatório deve identificar os cenários executados, a carga aplicada e os resultados;
- limites de tempo de resposta, volume de usuários simultâneos, volume de dados e taxa de erro devem ser aprovados antes de declarar conformidade de desempenho.

**Rastreabilidade:** RF-03, RF-04 e RF-06; avaliação de desempenho com JMeter prevista na visão geral.

**Parâmetros pendentes de aprovação:** carga e perfil de uso, limite de tempo de resposta e taxa de erro aceitável. Não há valores quantitativos definidos nos documentos atuais.

### RNF-04 — Estratégia e evidência de testes

O projeto deve validar os comportamentos funcionais e os atributos de qualidade com testes automatizados adequados às respectivas fronteiras.

**Critérios de verificação:**

- devem ser usados JUnit 5 e testes unitários, incluindo testes parametrizados;
- devem existir testes de integração, testes de API em caixa-preta e testes end-to-end;
- persistência deve ser exercitada com banco containerizado por Testcontainers;
- integrações externas devem ser simuladas com WireMock; mocks ficam restritos a testes unitários isolados;
- pelo menos uma funcionalidade nova deve ter evidências de desenvolvimento com TDD ou BDD;
- testes devem cobrir cenários válidos, inválidos e de conflito pertinentes aos requisitos implementados.

**Rastreabilidade:** RF-01 a RF-14 conforme aplicável; estratégia obrigatória de qualidade na visão geral.

### RNF-05 — Cobertura e rastreabilidade de qualidade

O projeto deve produzir evidências verificáveis de cobertura, rastreabilidade e análise de qualidade.

**Critérios de verificação:**

- JaCoCo deve medir cobertura de linhas e branches;
- metas mínimas do projeto: 80% de cobertura de linhas e 70% de cobertura de branches;
- 100% dos requisitos críticos devem estar cobertos pela matriz de rastreabilidade (RTM), ligados às evidências de teste correspondentes;
- o projeto deve estar integrado ao SonarCloud;
- a cobertura deve representar testes de comportamento relevante, e não somente testes triviais criados para elevar percentuais.

**Rastreabilidade:** metas mensuráveis e estratégia obrigatória de qualidade na visão geral.

### RNF-06 — Integração contínua

As verificações de qualidade devem ser executadas automaticamente em pull requests.

**Critérios de verificação:**

- GitHub Actions deve executar as verificações configuradas para o projeto em pull requests;
- os resultados da execução devem estar disponíveis para consulta na verificação do pull request;
- os testes automatizados e as verificações de qualidade previstas devem ser incluídos no fluxo de CI.

**Rastreabilidade:** estratégia de CI, análise estática e requisitos não funcionais na visão geral.

### RNF-07 — Auditoria e rastreabilidade de alterações

O sistema deve manter evidências das mudanças de estado e das operações relevantes necessárias para acompanhar reservas e recursos.

**Critérios de verificação:**

- toda mudança de estado de reserva deve gerar um registro de auditoria;
- o registro deve permitir identificar a operação, o usuário responsável e quando ela ocorreu;
- reservas iniciadas não podem ser apagadas;
- ações relevantes especificadas no RF-10 devem manter histórico consultável por usuários autorizados.

**Rastreabilidade:** RF-04, RF-07, RF-09 e RF-10; regras de estados e auditoria na visão geral.

### RNF-08 — Responsividade e clareza da interface

As interfaces dos fluxos cobertos pelo sistema devem se adaptar a desktop, tablet e dispositivos móveis e comunicar claramente o resultado das operações.

**Critérios de verificação:**

- os fluxos devem permanecer utilizáveis nos formatos desktop, tablet e mobile;
- erros, conflitos, bloqueios, rejeições e confirmações devem ser apresentados em linguagem compreensível;
- a interface deve fornecer feedback explícito após operações bem-sucedidas ou rejeitadas.

**Rastreabilidade:** RF-13; necessidades de consulta, reserva e acompanhamento das três personas.

### RNF-09 — Documentação dos contratos e fluxos

A documentação disponibilizada para integrantes do projeto ou usuários finais deve corresponder aos fluxos e contratos públicos implementados.

**Critérios de verificação:**

- os principais fluxos públicos devem estar documentados;
- quando houver API, a documentação deve refletir operações, autenticação, autorização, exemplos e erros previstos;
- alterações nos contratos ou fluxos devem ser acompanhadas pela atualização da documentação pertinente.

**Rastreabilidade:** RF-14.

## 4. Restrições técnicas

- A linguagem prevista para o projeto é Java 21.
- O framework previsto é Spring Boot 3.x.
- Os requisitos de teste e qualidade definidos na visão geral — JUnit 5, Testcontainers, WireMock, GitHub Actions, JaCoCo, SonarCloud e JMeter — devem ser considerados no planejamento e na validação da solução.

Estas restrições registram decisões existentes; não definem arquitetura, banco de dados ou provedores de integração ainda não escolhidos.

## 5. Pontos de qualidade ainda a definir

Os documentos atuais não fixam valores ou políticas para os itens abaixo. Eles devem ser acordados antes de serem usados como critérios quantitativos de aceite:

1. **Desempenho:** carga concorrente, volume de dados, tempos máximos de resposta e taxa de erro aceitável para cada fluxo medido.
2. **Disponibilidade e recuperação:** metas de disponibilidade, backup, recuperação e perda de dados aceitável, caso façam parte do escopo de entrega.
3. **Retenção de auditoria:** período de retenção e regras de acesso aos registros, além das responsabilidades e permissões já descritas nos RFs.
4. **Compatibilidade de interface:** navegadores e versões que serão oficialmente suportados.

Esses pontos são pendências de decisão, não requisitos adicionais aprovados por este documento.

## 6. Histórico de atualização

- **2026-11-01:** criação dos requisitos não funcionais com base na visão geral, nas personas e nos requisitos funcionais existentes. Metas de qualidade já aprovadas foram registradas; métricas quantitativas ausentes foram mantidas como pendências.