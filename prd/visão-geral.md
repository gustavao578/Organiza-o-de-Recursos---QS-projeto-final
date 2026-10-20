# Visão Geral do Projeto

## Objetivo deste documento

Manter a visão atual e rastreável do projeto **Organização de Recursos**, reunindo seu propósito, escopo confirmado, estado de evolução, contexto técnico e próximos passos. Este documento deve acompanhar o projeto desde a definição dos requisitos até a entrega.

## Visão do produto

O sistema tem como propósito organizar a alocação de salas, professores e materiais para atividades, evitando conflitos de horário. Deve apoiar a consulta de disponibilidade, a criação e o acompanhamento de reservas e a administração dos recursos, respeitando os perfis e responsabilidades definidos para o projeto.

A aplicação é um projeto semestral de qualidade de software. Sua avaliação considera não apenas o funcionamento, mas também evidências objetivas de qualidade, segurança, rastreabilidade e desempenho.

## Objetivos do projeto

- Permitir a organização de reservas de salas, professores e materiais sem conflitos de horário.
- Apoiar os fluxos de solicitantes, responsáveis e administradores descritos na especificação.
- Desenvolver e validar a aplicação com evidências verificáveis de qualidade, segurança, rastreabilidade e desempenho.
- Aplicar ao projeto as técnicas de qualidade estudadas durante as aulas.

## Escopo confirmado

A especificação inicial estabelece os seguintes elementos:

- Consulta de disponibilidade de recursos.
- Criação, alteração e cancelamento de reservas pelo solicitante.
- Aprovação de solicitações especiais pelo responsável.
- Validação da alocação de docentes pelo responsável.
- Acompanhamento de retiradas e devoluções de materiais pelo responsável.
- Gerenciamento de salas, professores, materiais, usuários, bloqueios e períodos de manutenção pelo administrador.
- Prevenção de conflitos de horário envolvendo os recursos alocados.

Os detalhes das regras de negócio, permissões específicas, estados dos fluxos e critérios de conflito ainda precisam ser definidos e validados. Esta lista registra somente o que foi informado na especificação inicial; não substitui requisitos detalhados.

## Perfis envolvidos

- **Solicitante:** professor ou coordenador que consulta disponibilidade e cria, altera ou cancela suas reservas.
- **Responsável:** aprova solicitações especiais, valida a alocação de docentes e acompanha retiradas e devoluções de material.
- **Administrador:** gerencia salas, professores, materiais, usuários, bloqueios e períodos de manutenção.

Os perfis e suas necessidades estão detalhados em [personas.md](personas.md).

## Contexto técnico e entrega

- Linguagem prevista: Java 21.
- Framework previsto: Spring Boot 3.x.
- Entrega prevista: semana 46.
- O projeto será desenvolvido por equipes.

A arquitetura, o banco de dados, os mecanismos de autenticação, as integrações e as ferramentas de teste ainda não foram especificados neste documento.

## Qualidade e evidências

A entrega deve apresentar evidências objetivas relacionadas a:

- **Qualidade:** validações e resultados de testes pertinentes aos requisitos.
- **Segurança:** controles e verificações a definir conforme os perfis, dados e fluxos da aplicação.
- **Rastreabilidade:** vínculo verificável entre requisitos, implementação e testes.
- **Desempenho:** medições com cenários e critérios a definir.

Os métodos, limiares de aprovação e formatos dessas evidências são decisões pendentes; não devem ser tratados como definidos até serem acordados e documentados.

## Estado atual do projeto

**Fase:** documentação inicial e descoberta de requisitos.

**Baseline:** a especificação fornecida define o propósito, os perfis, as capacidades gerais, a stack prevista e o foco de avaliação. No momento da criação deste documento, o README do repositório contém apenas o título do projeto e não há implementação da aplicação registrada na estrutura visível do repositório.

**Decisões pendentes:** regras detalhadas de conflito e disponibilidade; fluxo e estados das reservas; significado e tratamento de solicitações especiais; permissões por perfil; regras para bloqueios e manutenção; ciclo de retirada e devolução de materiais; requisitos funcionais e não funcionais mensuráveis; arquitetura e persistência; critérios de segurança, desempenho e aceite.

## Registro de evolução

A visão geral deve ser atualizada sempre que uma implementação for iniciada ou concluída, ou quando arquivos do projeto forem criados, alterados ou removidos. Registrar a mudança de modo conciso, mantendo o estado atual coerente com o repositório. Quando aplicável, relacionar a mudança ao requisito, decisão ou evidência correspondente.

| Data | Mudança | Arquivos ou área | Requisito, decisão ou evidência relacionada |
|---|---|---|---|
| 2026-10-19 | Criação do baseline de visão geral e personas; sem implementação de aplicação registrada. | `prd/visão-geral.md`, `prd/personas.md` | Especificação inicial fornecida para o projeto. |

## Próximos passos

1. **Validar e detalhar requisitos:** transformar o escopo confirmado em requisitos funcionais e não funcionais, regras de negócio e critérios de aceite. Objetivo: remover ambiguidades antes da implementação e permitir rastreabilidade.
2. **Validar personas e permissões:** confirmar responsabilidades, ações permitidas e informações necessárias em cada fluxo. Objetivo: orientar autorização e comportamento do sistema por perfil.
3. **Definir regras de alocação:** especificar disponibilidade, intervalos, conflitos, bloqueios, manutenção e exceções. Objetivo: estabelecer critérios testáveis para impedir alocações incompatíveis.
4. **Documentar arquitetura e dados:** decidir e registrar a estrutura da aplicação, persistência e contratos necessários. Objetivo: oferecer uma base consistente para implementar os requisitos aprovados.
5. **Planejar qualidade e segurança:** definir estratégia de testes, rastreabilidade, controles de segurança e evidências de desempenho com critérios mensuráveis. Objetivo: tornar a avaliação da qualidade reproduzível.
6. **Implementar incrementalmente e atualizar este documento:** associar cada incremento aos requisitos e testes correspondentes. Objetivo: manter documentação, código e evidências alinhados até a entrega da semana 46.

## Histórico de atualização

- **2026-10-19:** criação do documento com base exclusivamente na especificação inicial e no estado visível do repositório.
