# Análise de Segurança - Organização de Recursos

**Data**: 2026-11-12  
**Status**: 🔴 DIVERGÊNCIAS CRÍTICAS IDENTIFICADAS  
**Severidade Máxima**: CRÍTICA  
**Prioridade**: IMEDIATO

---

## 📋 Documentação

Esta pasta contém a análise completa de segurança da API:

### 1. 📊 [Matriz de Autorização](./matriz-autorizacao.md)
- **Conteúdo**: Matriz completa operação × perfil
- **Formato**: Tabelas por controlador
- **Inclui**: Códigos HTTP esperados (200/201/204/401/403)
- **Regras especiais**: Posse, recurso restrito, auto-aprovação
- **Divergências marcadas**: ⚠️ Operações onde código diverge da matriz
- **Uso**: Referência para implementar ou validar segurança

### 2. 🚨 [Divergências de Segurança](./DIVERGENCIAS.md)
- **Conteúdo**: Análise detalhada de cada divergência
- **Organização**: Por severidade (crítica/alta/média)
- **Para cada divergência**:
  - Status atual vs esperado
  - Impacto no negócio
  - Arquivo e linha do código
  - Cenário de ataque (se aplicável)
  - Correção sugerida (com código)
- **Plano de correção**: Fases 1/2/3 com tempo estimado
- **Testes críticos**: Casos que comprovam a vulnerabilidade
- **Uso**: Documento executivo para aprovação + roadmap de correções

### 3. 🔓 [Rotas Sem Proteção](./ROTAS-DESPROTEGIDAS.md)
- **Conteúdo**: Inventário de endpoints sem autenticação
- **Organização**: Por controlador + análise de risco
- **Detalhes**: Problema específico, impacto, severidade
- **Análise de risco**: Score de impacto (Confidencialidade/Integridade/Disponibilidade)
- **Vetor de ataque**: Cenário sequencial completo
- **Checklist**: Remediação imediata/semana/sprint
- **Templates**: Código pronto para copiar/colar
- **Uso**: Guia de implementação + gestão de vulnerabilidades

---

## 📈 Estatísticas Rápidas

| Métrica | Valor | Status |
|---------|-------|--------|
| **Total de rotas** | 28 | — |
| **Rotas protegidas** | 13 | ✅ 46% |
| **Rotas desprotegidas** | 15 | ❌ 54% |
| **Divergências críticas** | 4 | 🔴 |
| **Divergências altas** | 1 | 🟠 |
| **Divergências médias** | 4 | 🟡 |
| **Controllers críticos** | 3/5 | ❌ |

---

## 🎯 Achados Principais

### 🔴 CRÍTICOS (Implementar HOJE)

1. **RecursoController** - 5/5 endpoints sem autenticação
   - Qualquer pessoa cria/modifica/deleta recursos
   - **Impacto**: Recursos falsos podem bloquear sistema

2. **ManutencaoController** - 2/2 endpoints sem autenticação
   - Qualquer pessoa cria manutenção indefinida
   - **Impacto**: Negócio parado (recurso 100% bloqueado)

3. **UsuarioController** - Múltiplas falhas
   - Escalação de privilégio em `PUT /usuarios/{id}`
   - Deleção sem autenticação em `DELETE /usuarios/{id}`
   - **Impacto**: Invasão total do sistema

### 🟠 ALTOS (Implementar esta semana)

- ReservaController: Lógica confusa em historico + nao-compareceu

### 🟡 MÉDIOS (Próximo sprint)

- Testes e auditoria de segurança automatizada

---

## 🔧 Ações Imediatas

### Prioridade 1️⃣ (2-3 horas)

- [ ] Adicionar `@AuthenticationPrincipal Jwt jwt` em **RecursoController**
- [ ] Adicionar autenticação obrigatória em **ManutencaoController**
- [ ] Adicionar validação de posse em **UsuarioController** (`PUT`)
- [ ] Bloquear deleção anônima em **UsuarioController** (`DELETE`)
- [ ] Rodar testes de segurança (SecurityConfigTest)

### Prioridade 2️⃣ (1-2 horas)

- [ ] Corrigir `GET /reservas/{id}/historico` - Permitir Solicitante
- [ ] Definir política para `POST /reservas/{id}/nao-compareceu`
- [ ] Adicionar autenticação em `GET /usuarios` e `GET /usuarios/{id}`

### Prioridade 3️⃣ (Próximo sprint)

- [ ] Implementar auditoria de operações sensíveis
- [ ] Criar testes de segurança automatizados (coverage 100%)
- [ ] Documento de política de autenticação/autorização

---

## 📂 Mapa de Arquivos de Código

### Controllers
- [ReservaController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/ReservaController.java) - Bem protegido (11/13 ✅)
- [RecursoController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/RecursoController.java) - Desprotegido (0/5 ❌)
- [UsuarioController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/UsuarioController.java) - Parcialmente protegido (1/5 ⚠️)
- [ManutencaoController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/ManutencaoController.java) - Desprotegido (0/2 ❌)
- [RelatorioController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/RelatorioController.java) - Bem protegido (1/1 ✅)

### Configuração
- [SecurityConfig.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/config/SecurityConfig.java) - Configuração central (boa base, mas controllers não validam)

### Testes de Segurança
- [SecurityConfigTest.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/test/java/br/edu/organizacaorecursos/SecurityConfigTest.java) - Testes básicos ✅
- [ReservaApiIntegrationTest.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/test/java/br/edu/organizacaorecursos/ReservaApiIntegrationTest.java) - Testes detalhados ✅

---

## 📊 Leitura Recomendada

### Para Developers
1. Comece por [ROTAS-DESPROTEGIDAS.md](./ROTAS-DESPROTEGIDAS.md)
   - Veja qual endpoint precisa corrigir
   - Use os templates de correção
   - Execute o checklist

2. Consulte [matriz-autorizacao.md](./matriz-autorizacao.md)
   - Confirme o comportamento esperado
   - Implemente conforme especificado

### Para Arquitetos
1. Leia [DIVERGENCIAS.md](./DIVERGENCIAS.md)
   - Entenda o impacto de cada falha
   - Veja os cenários de ataque
   - Planeje as fases de correção

2. Revise [matriz-autorizacao.md](./matriz-autorizacao.md)
   - Defina política para casos ambíguos
   - Aprove ou revise regras de negócio

### Para QA/Testes
1. Use casos de teste em [DIVERGENCIAS.md](./DIVERGENCIAS.md)
   - Teste 4 casos críticos fornecidos
   - Adicione testes para cada correção

2. Implemente testes de segurança automatizados
   - Templates em [ROTAS-DESPROTEGIDAS.md](./ROTAS-DESPROTEGIDAS.md)
   - Cobertura 100% dos endpoints

---

## 🛡️ Checklist de Aprovação

### Developer
- [ ] Implementar correções da Prioridade 1️⃣
- [ ] Passar em testes de segurança (SecurityConfigTest)
- [ ] Adicionar testes de segurança para cada correção
- [ ] Code review com checklist de segurança

### QA
- [ ] Executar testes de segurança manuais
- [ ] Testar casos do DIVERGENCIAS.md
- [ ] Verificar que nenhuma rota pública foi protegida por acidente

### Arquiteto
- [ ] Revisar matriz de autorização
- [ ] Validar regras de negócio
- [ ] Aprovar política de autenticação/autorização

### DevSecOps
- [ ] Revisar SecurityConfig
- [ ] Validar tokens JWT
- [ ] Verificar rate limiting (não implementado - considerar)

---

## 📞 Perguntas Frequentes

**P: Por que tantos endpoints estão sem proteção?**  
R: Parecem ser endpoints iniciais que foram desenvolvidos sem considerar segurança. A configuração de segurança (SecurityConfig) exige autenticação global, mas os controllers não validam/não passam JWT.

**P: Os testes estão passando apesar das vulnerabilidades?**  
R: Sim. Os testes usam `@AutoConfigureMockMvc(addFilters = false)` que desativa os filtros de segurança. É por isso que não detectam as vulnerabilidades.

**P: Qual é a ordem de correção?**  
R: Prioridade 1️⃣ (hoje) > Prioridade 2️⃣ (esta semana) > Prioridade 3️⃣ (sprint). Veja [DIVERGENCIAS.md](./DIVERGENCIAS.md#plano-de-correção-recomendado).

**P: Preciso corrigir todos os endpoints públicos?**  
R: Depende da política. Alguns GET podem ser públicos por design. Defina com o time qual é a política e documente em [matriz-autorizacao.md](./matriz-autorizacao.md).

---

## 📚 Referências Externas

- [OWASP Top 10 2021 - A01 Broken Access Control](https://owasp.org/Top10/A01_2021-Broken_Access_Control/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)

---

## 👤 Autor

**Análise de Segurança Automatizada**  
Data: 2026-11-12  
Ferramenta: Static Code Analysis + Manual Review

---

## 📝 Histórico

| Data | Versão | Mudanças |
|------|--------|----------|
| 2026-11-12 | 1.0 | Análise inicial - 3 documentos criados |

