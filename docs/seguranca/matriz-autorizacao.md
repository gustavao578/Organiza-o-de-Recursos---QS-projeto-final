# Matriz de Autorização - API de Organização de Recursos

**Data**: 2026-11-12  
**Versão**: 1.0  
**Escopo**: Todas as operações da API REST em `/reservas`, `/recursos`, `/usuarios`, `/manutencoes` e `/relatorios`

## Perfis de Usuário

| Perfil | Descrição |
|--------|-----------|
| **Anônimo** | Sem autenticação (sem token JWT) |
| **Solicitante** | Usuário autenticado com perfil COMUM ou SOLICITANTE |
| **Responsável** | Usuário autenticado com perfil RESPONSAVEL |
| **Administrador** | Usuário autenticado com perfil ADMINISTRADOR |

## Códigos de Resposta Esperados

- **200**: OK - Sucesso em operação de leitura ou modificação
- **201**: Created - Recurso criado com sucesso
- **204**: No Content - Sucesso em operação sem corpo (DELETE)
- **401**: Unauthorized - Token ausente ou inválido
- **403**: Forbidden - Usuário autenticado mas sem permissão
- **409**: Conflict - Conflito de negócio (ex: sobreposição de reservas)

## Regras Especiais

### Regra de Posse - Solicitante
- Solicitante **só altera suas próprias reservas** (`PUT /reservas/{id}`)
- Solicitante **só cancela suas próprias reservas** (`POST /reservas/{id}/cancelar`)
- Solicitante **pode deletar suas próprias reservas** (`DELETE /reservas/{id}`)
- Violação: Retorna **403 Forbidden**

### Regra de Posse - Recurso Restrito
- **Apenas o RESPONSÁVEL** (definido no campo `recursoId.responsavelId`) pode:
  - Aprovar reservas de recurso restrito: `POST /reservas/{id}/aprovar`
  - Iniciar execução: `POST /reservas/{id}/iniciar`
  - Concluir execução: `POST /reservas/{id}/concluir`
- Violação: Retorna **403 Forbidden**

### Auto-aprovação
- **Administrador não pode auto-aprovar sua própria reserva** em recurso restrito
- Violação: Retorna **400 Bad Request**

---

## Matriz de Autorização

### RESERVAS - Criação e Listagem

| Operação | Endpoint | HTTP | Anônimo | Solicitante | Responsável | Administrador | Observações |
|----------|----------|------|---------|-------------|-------------|---------------|-------------|
| Criar reserva | `POST /reservas` | POST | **401** | **201** | **201** | **201** | Cria com usuarioId do JWT |
| Listar reservas | `GET /reservas` | GET | **200** | **200** | **200** | **200** | Sem autenticação |
| Buscar por ID | `GET /reservas/{id}` | GET | **200** | **200** | **200** | **200** | Sem autenticação |
| Histórico auditorias | `GET /reservas/{id}/historico` | GET | **401** | **403** | **200** | **200** | ⚠️ **DIVERGÊNCIA**: Solicitante recebe 403, mas código permite se estiver autenticado |

### RESERVAS - Alteração e Cancelamento

| Operação | Endpoint | HTTP | Anônimo | Solicitante | Responsável | Administrador | Observações |
|----------|----------|------|---------|-------------|-------------|---------------|-------------|
| Atualizar reserva | `PUT /reservas/{id}` | PUT | **401** | **200**¹ | **403**¹ | **403**¹ | ¹ Requer posse (própria) - Responsável e Admin retornam 403 |
| Cancelar reserva | `POST /reservas/{id}/cancelar` | POST | **401** | **200**¹ | **403** | **403** | ¹ Requer posse (própria) |
| Deletar reserva | `DELETE /reservas/{id}` | DELETE | **401** | **204**¹ | **403** | **403** | ¹ Requer posse (própria) |

### RESERVAS - Transições de Status

| Operação | Endpoint | HTTP | Anônimo | Solicitante | Responsável | Administrador | Observações |
|----------|----------|------|---------|-------------|-------------|---------------|-------------|
| Atualizar status | `PATCH /reservas/{id}/status` | PATCH | **401** | **403** | **200** | **200** | Transições: PENDENTE→APROVADA (requer validação) |
| Aprovar reserva | `POST /reservas/{id}/aprovar` | POST | **401** | **403** | **403**² | **200** | ² Para recurso restrito: só Responsável (do recurso) pode aprovar |
| Rejeitar reserva | `POST /reservas/{id}/rejeitar` | POST | **401** | **403** | **403** | **200** | Apenas Admin pode rejeitar |
| Marcar não comparecimento | `POST /reservas/{id}/nao-compareceu` | POST | **401** | **403** | **200** | **403** | ⚠️ **DIVERGÊNCIA**: Apenas Responsável, não Admin |

### RESERVAS - Execução (Transições EM_USO → CONCLUÍDA)

| Operação | Endpoint | HTTP | Anônimo | Solicitante | Responsável | Administrador | Observações |
|----------|----------|------|---------|-------------|-------------|---------------|-------------|
| Iniciar reserva | `POST /reservas/{id}/iniciar` | POST | **401** | **403** | **200**² | **403** | ² Requer Responsável do recurso (recurso.responsavelId) |
| Concluir reserva | `POST /reservas/{id}/concluir` | POST | **401** | **403** | **200**² | **403** | ² Requer Responsável do recurso (recurso.responsavelId) |

---

### RECURSOS - Gerenciamento

| Operação | Endpoint | HTTP | Anônimo | Solicitante | Responsável | Administrador | Observações |
|----------|----------|------|---------|-------------|-------------|---------------|-------------|
| Criar recurso | `POST /recursos` | POST | **201** | **201** | **201** | **201** | ⚠️ **DIVERGÊNCIA**: Sem autenticação obrigatória |
| Listar recursos | `GET /recursos` | GET | **200** | **200** | **200** | **200** | Sem autenticação |
| Buscar por ID | `GET /recursos/{id}` | GET | **200** | **200** | **200** | **200** | Sem autenticação |
| Atualizar recurso | `PUT /recursos/{id}` | PUT | **200** | **200** | **200** | **200** | ⚠️ **DIVERGÊNCIA**: Sem autenticação obrigatória |
| Deletar recurso | `DELETE /recursos/{id}` | DELETE | **204** | **204** | **204** | **204** | ⚠️ **DIVERGÊNCIA**: Sem autenticação obrigatória |

---

### USUÁRIOS - Gerenciamento

| Operação | Endpoint | HTTP | Anônimo | Solicitante | Responsável | Administrador | Observações |
|----------|----------|------|---------|-------------|-------------|---------------|-------------|
| Criar usuário | `POST /usuarios` | POST | **401** | **201** | **201**¹ | **201**¹ | ¹ Apenas Admin pode definir perfil; outros criam como SOLICITANTE |
| Listar usuários | `GET /usuarios` | GET | **200** | **200** | **200** | **200** | ⚠️ **DIVERGÊNCIA**: Sem autenticação obrigatória |
| Buscar por ID | `GET /usuarios/{id}` | GET | **200** | **200** | **200** | **200** | ⚠️ **DIVERGÊNCIA**: Sem autenticação obrigatória |
| Atualizar usuário | `PUT /usuarios/{id}` | PUT | **401** | **200** | **200** | **200** | Qualquer autenticado pode atualizar (falta validação de posse) |
| Deletar usuário | `DELETE /usuarios/{id}` | DELETE | **204** | **204** | **204** | **204** | ⚠️ **DIVERGÊNCIA**: Sem autenticação obrigatória |

---

### MANUTENÇÕES - Gerenciamento

| Operação | Endpoint | HTTP | Anônimo | Solicitante | Responsável | Administrador | Observações |
|----------|----------|------|---------|-------------|-------------|---------------|-------------|
| Criar manutenção | `POST /manutencoes` | POST | **201** | **201** | **201** | **201** | ⚠️ **DIVERGÊNCIA**: Sem autenticação obrigatória |
| Listar manutenções | `GET /manutencoes` | GET | **200** | **200** | **200** | **200** | Sem autenticação |

---

### RELATÓRIOS - Análise e Consultas

| Operação | Endpoint | HTTP | Anônimo | Solicitante | Responsável | Administrador | Observações |
|----------|----------|------|---------|-------------|-------------|---------------|-------------|
| Utilização por período | `GET /relatorios/utilizacao` | GET | **401** | **403** | **200** | **200** | Requer query params: `inicio`, `fim` (ISO-8601) |

---

## Resumo de Divergências Identificadas

### ⚠️ CRÍTICAS (Violam regras de negócio)

1. **Recurso Controller - Sem autenticação**
   - ❌ `POST /recursos` - Qualquer pessoa (anônima) pode criar
   - ❌ `PUT /recursos/{id}` - Qualquer pessoa pode atualizar
   - ❌ `DELETE /recursos/{id}` - Qualquer pessoa pode deletar
   - **Impacto**: Recursos podem ser modificados/deletados por usuários não autorizados
   - **Esperado**: Todas essas operações devem exigir autenticação e provavelmente ADMINISTRADOR
   - **Código**: [RecursoController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/RecursoController.java:30-65)

2. **Usuário Controller - Sem autenticação em listagem e deleção**
   - ❌ `GET /usuarios` - Qualquer pessoa pode listar usuários
   - ❌ `GET /usuarios/{id}` - Qualquer pessoa pode buscar usuário por ID
   - ❌ `DELETE /usuarios/{id}` - Qualquer pessoa pode deletar usuário
   - **Impacto**: Dados sensíveis expostos (emails, matrículas); deleção de usuários sem controle
   - **Esperado**: Autenticação obrigatória; deleção apenas para ADMINISTRADOR
   - **Código**: [UsuarioController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/UsuarioController.java:45-80)

3. **Manutenção Controller - Sem autenticação**
   - ❌ `POST /manutencoes` - Qualquer pessoa pode criar manutenção
   - ❌ `GET /manutencoes` - Qualquer pessoa pode listar manutenções
   - **Impacto**: Manutenções falsas podem ser criadas, bloqueando recursos
   - **Esperado**: Autenticação obrigatória; provavelmente RESPONSAVEL ou ADMINISTRADOR
   - **Código**: [ManutencaoController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/ManutencaoController.java:27-42)

4. **Reserva Controller - Erro de lógica em `POST /reservas/{id}/nao-compareceu`**
   - ❌ Método exige perfil RESPONSAVEL, mas retorna 403 para ADMINISTRADOR
   - **Esperado**: ADMINISTRADOR também deveria poder marcar não comparecimento
   - **Código**: [ReservaController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/ReservaController.java:119-126)

### ⚠️ MODERADAS (Regras de negócio não implementadas)

5. **Reserva Controller - Falta validação de posse em `PUT /usuarios/{id}`**
   - ❌ Qualquer usuário autenticado pode atualizar qualquer outro usuário
   - **Esperado**: Usuário só deve poder atualizar a si mesmo (ou ADMINISTRADOR qualquer um)
   - **Código**: [UsuarioController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/UsuarioController.java:56-70)

6. **Reserva Controller - `GET /reservas/{id}/historico` permite Solicitante com erro**
   - ❌ Código exige autenticação mas depois verifica se é RESPONSAVEL ou ADMINISTRADOR
   - **Esperado**: Solicitante deveria poder ver histórico de suas próprias reservas
   - **Código**: [ReservaController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/ReservaController.java:139-147)

---

## Rotas SEM Regras de Segurança Implementadas

As seguintes rotas não possuem `@AuthenticationPrincipal Jwt jwt` ou validações:

1. **RecursoController**
   - ❌ `POST /recursos` - criarRecurso() - Sem validação
   - ❌ `GET /recursos` - listarRecursos() - Sem validação
   - ❌ `GET /recursos/{id}` - buscarRecurso() - Sem validação
   - ❌ `PUT /recursos/{id}` - atualizarRecurso() - Sem validação
   - ❌ `DELETE /recursos/{id}` - deletarRecurso() - Sem validação

2. **UsuarioController**
   - ⚠️ `GET /usuarios` - listarUsuarios() - Sem validação
   - ⚠️ `GET /usuarios/{id}` - buscarUsuario() - Sem validação
   - ✅ `POST /usuarios` - criarUsuario() - **HAS JWT** - Mas não obriga ser ADMIN
   - ⚠️ `DELETE /usuarios/{id}` - deletarUsuario() - Sem validação

3. **ManutencaoController**
   - ❌ `POST /manutencoes` - criarManutencao() - Sem validação
   - ❌ `GET /manutencoes` - listarManutencoes() - Sem validação

4. **ReservaController - Parcialmente protegido**
   - ✅ `GET /reservas/{id}` - buscarPorId() - **SEM validação (é público)**
   - ✅ `GET /reservas` - listarReservas() - **SEM validação (é público)**
   - ✅ Outros endpoints têm validações

---

## Endpoints Públicos Intencionais

Baseado na SecurityConfig, os seguintes grupos de rotas são intencionalmente públicos:

- `/v3/api-docs/**` - Documentação OpenAPI (Swagger)
- `/swagger-ui/**` - Interface Swagger
- `/swagger-ui.html` - Interface Swagger
- `/h2-console/**` - Banco H2 (Desenvolvimento)

**Confirmados como públicos pela análise de testes:**
- `GET /reservas` - Listar reservas (sem autenticação)
- `GET /reservas/{id}` - Buscar reserva (sem autenticação)
- `GET /recursos` - Listar recursos (sem autenticação)
- `GET /usuarios` - Listar usuários (sem autenticação)
- `GET /manutencoes` - Listar manutenções (sem autenticação)

---

## Recomendações de Segurança (Ordem de Prioridade)

### P0 - Crítico (Fazer imediatamente)

- [ ] **RecursoController**: Adicionar validação `@AuthenticationPrincipal Jwt jwt` em TODOS os endpoints
  - Criar: Apenas ADMINISTRADOR
  - Atualizar: Apenas ADMINISTRADOR
  - Deletar: Apenas ADMINISTRADOR

- [ ] **ManutencaoController**: Adicionar validação em ambos endpoints
  - Criar: Apenas ADMINISTRADOR ou RESPONSAVEL
  - Listar: Apenas RESPONSAVEL ou ADMINISTRADOR

- [ ] **UsuarioController**: Remover acesso público
  - Listar: Apenas ADMINISTRADOR
  - Buscar por ID: Próprio ou ADMINISTRADOR
  - Deletar: Apenas ADMINISTRADOR
  - Atualizar: Próprio usuário ou ADMINISTRADOR

### P1 - Alto (Fazer em breve)

- [ ] **ReservaController**: Corrigir `POST /reservas/{id}/nao-compareceu`
  - Aceitar RESPONSAVEL e ADMINISTRADOR (ou remover ADMINISTRADOR conforme especificação)

- [ ] **ReservaController**: Melhorar `GET /reservas/{id}/historico`
  - Permitir Solicitante ver histórico de suas próprias reservas
  - Permitir RESPONSAVEL e ADMINISTRADOR ver qualquer histórico

### P2 - Médio (Consideração futura)

- [ ] Definir política clara sobre quem pode listar/buscar recursos, usuários e manutenções
  - Versão pública? (Qualquer um)
  - Apenas autenticados?
  - Apenas RESPONSAVEL/ADMINISTRADOR?

- [ ] Implementar busca/filtro com paginação em listagens públicas (se mantidas públicas)

---

## Configuração de Segurança Atual

**Arquivo**: [SecurityConfig.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/config/SecurityConfig.java)

```java
// Regra: Qualquer rota fora dos APIs requer autenticação
.authorizeHttpRequests(authorize -> authorize
    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/h2-console/**")
    .permitAll()
    .anyRequest().authenticated())
```

**Consequência**: Qualquer rota `/recursos`, `/usuarios`, `/manutencoes` que não tenha validação manual permite requisições anônimas (sem Bearer token).

---

## Matriz de Teste Sugerida

Para validar as correções, criar testes seguindo este padrão:

```gherkin
Given um usuário [ANÔNIMO|SOLICITANTE|RESPONSÁVEL|ADMINISTRADOR]
When executa [OPERAÇÃO] em [ENDPOINT]
Then recebe status [200|201|204|401|403]
And a entidade [não foi alterada|foi alterada como esperado]
```

Exemplos:
- `POST /recursos` sem token → 401 ✗ (atualmente 201)
- `DELETE /usuarios/{id}` sem token → 401 ✗ (atualmente 204)
- `PUT /reservas/{id}` como Responsável (posse de outro) → 403 ✓ (validar)

---

## Versioning

| Versão | Data | Autor | Mudanças |
|--------|------|-------|----------|
| 1.0 | 2026-11-12 | Análise de Segurança | Versão inicial - Matriz completa e divergências documentadas |

