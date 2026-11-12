# Rotas Sem Proteção de Segurança

**Data de Análise**: 2026-11-12  
**Total de Rotas Analisadas**: 28  
**Rotas Desprotegidas**: 15  
**Taxa de Proteção**: 46%

---

## Legenda

| Símbolo | Significado |
|---------|-----------|
| ❌ | Sem qualquer proteção (público) |
| ⚠️ | Proteção parcial ou incompleta |
| ✅ | Protegido corretamente |

---

## Rotas Desprotegidas por Controlador

### ❌ RecursoController - 5/5 (100% desprotegido)

**Arquivo**: [RecursoController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/RecursoController.java)

| # | HTTP | Endpoint | Método | Proteção | Problema | Impacto |
|---|------|----------|--------|----------|----------|---------|
| 1 | POST | `/recursos` | `criarRecurso()` | ❌ Nenhuma | Sem `@AuthenticationPrincipal Jwt jwt` | Qualquer pessoa cria recursos |
| 2 | GET | `/recursos` | `listarRecursos()` | ❌ Nenhuma | Sem `@AuthenticationPrincipal Jwt jwt` | Informações públicas (aceitável?) |
| 3 | GET | `/recursos/{id}` | `buscarRecurso()` | ❌ Nenhuma | Sem `@AuthenticationPrincipal Jwt jwt` | Informações públicas (aceitável?) |
| 4 | PUT | `/recursos/{id}` | `atualizarRecurso()` | ❌ Nenhuma | Sem `@AuthenticationPrincipal Jwt jwt` | Qualquer pessoa modifica recursos |
| 5 | DELETE | `/recursos/{id}` | `deletarRecurso()` | ❌ Nenhuma | Sem `@AuthenticationPrincipal Jwt jwt` | **CRÍTICO**: Qualquer pessoa deleta |

**Ação obrigatória**: Adicionar `@AuthenticationPrincipal Jwt jwt` e exigir `ADMINISTRADOR` em POST/PUT/DELETE.

---

### ❌ UsuarioController - 4/5 (80% desprotegido)

**Arquivo**: [UsuarioController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/UsuarioController.java)

| # | HTTP | Endpoint | Método | Proteção | Problema | Impacto |
|---|------|----------|--------|----------|----------|---------|
| 6 | POST | `/usuarios` | `criarUsuario()` | ⚠️ Parcial | Tem JWT mas não valida perfil | Qualquer autenticado cria Admin |
| 7 | GET | `/usuarios` | `listarUsuarios()` | ❌ Nenhuma | Sem `@AuthenticationPrincipal Jwt jwt` | Exposição de usuários (emails) |
| 8 | GET | `/usuarios/{id}` | `buscarUsuario()` | ❌ Nenhuma | Sem `@AuthenticationPrincipal Jwt jwt` | Exposição de usuário específico |
| 9 | PUT | `/usuarios/{id}` | `atualizarUsuario()` | ⚠️ Falha | Tem JWT mas sem posse | **Escalação de privilégio** |
| 10 | DELETE | `/usuarios/{id}` | `deletarUsuario()` | ❌ Nenhuma | Sem `@AuthenticationPrincipal Jwt jwt` | **CRÍTICO**: Qualquer pessoa deleta |

**Ação obrigatória**: 
- Adicionar autenticação em GET/DELETE
- Adicionar validação de posse em PUT
- Restringir POST a ADMINISTRADOR apenas

---

### ❌ ManutencaoController - 2/2 (100% desprotegido)

**Arquivo**: [ManutencaoController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/ManutencaoController.java)

| # | HTTP | Endpoint | Método | Proteção | Problema | Impacto |
|---|------|----------|--------|----------|----------|---------|
| 11 | POST | `/manutencoes` | `criarManutencao()` | ❌ Nenhuma | Sem `@AuthenticationPrincipal Jwt jwt` | **CRÍTICO**: Qualquer pessoa bloqueia recurso |
| 12 | GET | `/manutencoes` | `listarManutencoes()` | ❌ Nenhuma | Sem `@AuthenticationPrincipal Jwt jwt` | Exposição de agenda de manutenção |

**Ação obrigatória**: Adicionar `@AuthenticationPrincipal Jwt jwt` e exigir `RESPONSAVEL` ou `ADMINISTRADOR` em POST.

---

### ⚠️ ReservaController - 2/13 (15% com problemas)

**Arquivo**: [ReservaController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/ReservaController.java)

| # | HTTP | Endpoint | Método | Proteção | Problema | Impacto |
|---|------|----------|--------|----------|----------|---------|
| 13 | GET | `/reservas/{id}/historico` | `historico()` | ⚠️ Parcial | Exclui SOLICITANTE de propósito | Solicitante não vê seu histórico |
| 14 | POST | `/reservas/{id}/nao-compareceu` | `marcarNaoComparecimento()` | ⚠️ Parcial | Rejeita ADMINISTRADOR | Admin não consegue marcar não comparecimento |

**Ações necessárias**:
- Permitir Solicitante ver histórico de suas próprias reservas
- Decidir se Admin deve poder marcar não comparecimento

---

### ✅ RelatorioController - 1/1 (100% protegido)

**Arquivo**: [RelatorioController.java](/workspaces/Organiza-o-de-Recursos---QS-projeto-final/src/main/java/br/edu/organizacaorecursos/controller/RelatorioController.java)

| # | HTTP | Endpoint | Método | Proteção | Status |
|---|------|----------|--------|----------|--------|
| 15 | GET | `/relatorios/utilizacao` | `obterUtilizacao()` | ✅ Correta | Exige RESPONSAVEL ou ADMINISTRADOR |

---

## Análise Detalhada - Rotas Público-Intencionales vs Desprotegidas

### ✅ Público Intencional (Sem proteção é OK)

Baseado em testes e documentação:

| Endpoint | Motivo | Recomendação |
|----------|--------|-------------|
| `GET /reservas` | Catálogo público | Manter público |
| `GET /reservas/{id}` | Consulta pública | Manter público |
| `GET /recursos` | Catálogo público | Manter público |
| `GET /recursos/{id}` | Consulta pública | Manter público |
| `GET /manutencoes` | Agenda pública? | **Revisar**: Deve ser público? |
| `GET /usuarios` | Lista pública? | **Revisar**: Exposição de dados sensíveis |
| `GET /usuarios/{id}` | Perfil público? | **Revisar**: Exposição de email/matrícula |

---

### ❌ Desprotegidas Sem Justificativa

| # | Endpoint | Razão de Estar Desprotegido | Severidade | Deve ser Protegido |
|---|----------|---------------------------|-----------|------------------|
| 1-5 | RecursoController (POST/PUT/DELETE) | Falta implementação | 🔴 CRÍTICA | SIM - Apenas ADMIN |
| 6 | `POST /usuarios` | Falta validação de perfil | 🔴 CRÍTICA | SIM - Forçar ADMIN para criar Admin |
| 7-8 | `GET /usuarios` e `GET /usuarios/{id}` | Falta autenticação | 🟠 ALTA | Sim - Validar posse ou ADMIN |
| 9 | `PUT /usuarios/{id}` | Falta validação de posse | 🔴 CRÍTICA | SIM - Adicionar posse + ADMIN exception |
| 10 | `DELETE /usuarios/{id}` | Falta autenticação | 🔴 CRÍTICA | SIM - Apenas ADMIN |
| 11-12 | ManutencaoController | Falta implementação | 🔴 CRÍTICA | SIM - Apenas ADMIN/RESPONSAVEL |

---

## Vetor de Ataque Principal

### Cenário: Ataque Sequencial

```
1. Atacante (anônimo) descobre UUID de um recurso crítico
   POST /recursos → Cria recurso fake (sem autenticação)
   
2. Cria manutenção contínua nesse recurso
   POST /manutencoes → Bloqueia indefinidamente
   
3. Escalação: Cria usuário ADMIN
   POST /usuarios com perfil=ADMINISTRADOR
   
4. Remove todos os usuários legítimos
   DELETE /usuarios/{id} × N vezes
   
5. Deleta recursos da empresa
   DELETE /recursos/{id} × N vezes
   
RESULTADO: Sistema comprometido, negócio parado
```

---

## Rotas COM Proteção (Corretas)

### ✅ ReservaController - Bem Protegidas (11/13)

| HTTP | Endpoint | Proteção |
|------|----------|----------|
| POST | `/reservas` | ✅ Requer COMUM/SOLICITANTE |
| POST | `/reservas/{id}/aprovar` | ✅ Requer ADMINISTRADOR |
| POST | `/reservas/{id}/iniciar` | ✅ Requer RESPONSAVEL |
| POST | `/reservas/{id}/concluir` | ✅ Requer RESPONSAVEL |
| POST | `/reservas/{id}/rejeitar` | ✅ Requer ADMINISTRADOR |
| PATCH | `/reservas/{id}/status` | ✅ Requer ADMIN/RESPONSAVEL |
| PUT | `/reservas/{id}` | ✅ Requer COMUM/SOLICITANTE + posse |
| POST | `/reservas/{id}/cancelar` | ✅ Requer COMUM/SOLICITANTE + posse |
| DELETE | `/reservas/{id}` | ✅ Requer posse |

### ✅ RelatorioController - Bem Protegidas (1/1)

| HTTP | Endpoint | Proteção |
|------|----------|----------|
| GET | `/relatorios/utilizacao` | ✅ Requer RESPONSAVEL/ADMINISTRADOR |

---

## Matriz de Risco

### Risco por Endpoint Desprotegido

| Endpoint | Confidencialidade | Integridade | Disponibilidade | Score |
|----------|------------------|-----------|-----------------|-------|
| `POST /recursos` | Alto | CRÍTICO | Alto | 🔴 9/10 |
| `DELETE /recursos/{id}` | Médio | CRÍTICO | CRÍTICO | 🔴 10/10 |
| `POST /usuarios` | Médio | CRÍTICO | Médio | 🔴 9/10 |
| `DELETE /usuarios/{id}` | Médio | CRÍTICO | CRÍTICO | 🔴 10/10 |
| `PUT /usuarios/{id}` | Alto | CRÍTICO | Médio | 🔴 9/10 |
| `POST /manutencoes` | Médio | CRÍTICO | CRÍTICO | 🔴 10/10 |
| `GET /usuarios` | CRÍTICO | Baixo | Baixo | 🟠 7/10 |
| `GET /recursos/{id}` | Baixo | Baixo | Baixo | 🟢 2/10 |

---

## Checklist de Remediação

### Imediato (Hoje)

- [ ] Adicionar `@AuthenticationPrincipal Jwt jwt` em RecursoController
  - [ ] `criarRecurso()` - Validar ADMINISTRADOR
  - [ ] `atualizarRecurso()` - Validar ADMINISTRADOR
  - [ ] `deletarRecurso()` - Validar ADMINISTRADOR

- [ ] Adicionar `@AuthenticationPrincipal Jwt jwt` em ManutencaoController
  - [ ] `criarManutencao()` - Validar RESPONSAVEL/ADMINISTRADOR
  - [ ] `listarManutencoes()` - Validar autenticação mínima

- [ ] Corrigir UsuarioController
  - [ ] `criarUsuario()` - Validar apenas ADMINISTRADOR pode criar perfil ADMIN
  - [ ] `deletarUsuario()` - Validar ADMINISTRADOR
  - [ ] `atualizarUsuario()` - Adicionar posse + blocar change perfil sem ADMIN

- [ ] Executar testes de segurança após cada mudança

### Esta Semana

- [ ] UsuarioController - `GET /usuarios` e `GET /usuarios/{id}` - Adicionar autenticação
- [ ] ReservaController - `GET /reservas/{id}/historico` - Permitir Solicitante ver seus próprios
- [ ] ReservaController - `POST /reservas/{id}/nao-compareceu` - Decidir política para Admin

### Próximo Sprint

- [ ] Implementar auditoria de segurança para operações sensíveis
- [ ] Adicionar testes de segurança automatizados (SecurityConfigTest + novos)
- [ ] Revisão de código com foco em autenticação/autorização

---

## Templates de Correção

### Template 1: Endpoint que Exige Autenticação

```java
@PostMapping
public ResponseEntity<...> criar(
        @Valid @RequestBody ... request,
        @AuthenticationPrincipal Jwt jwt) {
    
    // ✅ Validar autenticação
    if (jwt == null) {
        throw new AccessDeniedException("Autenticação obrigatória");
    }
    
    // ✅ Validar perfil
    exigirAdmin(jwt);  // ou exigirPerfil(jwt, "RESPONSAVEL")
    
    // ... resto do código
}
```

### Template 2: Endpoint com Validação de Posse

```java
@PutMapping("/{id}")
public ResponseEntity<...> atualizar(
        @PathVariable UUID id,
        @Valid @RequestBody ... request,
        @AuthenticationPrincipal Jwt jwt) {
    
    // ✅ Validar autenticação
    if (jwt == null) {
        throw new AccessDeniedException("Autenticação obrigatória");
    }
    
    UUID usuarioId = UUID.fromString(jwt.getSubject());
    String perfil = perfilNormalizado(jwt.getClaimAsString("profile"));
    
    // ✅ Validar posse (Solicitante só atualiza a si mesmo)
    boolean isAdmin = "ADMINISTRADOR".equals(perfil);
    if (!isAdmin && !id.equals(usuarioId)) {
        throw new AccessDeniedException("Usuário só pode atualizar a si mesmo");
    }
    
    // ... resto do código
}
```

### Template 3: Endpoint Público (Sem Proteção)

```java
@GetMapping
public ResponseEntity<...> listar() {
    // Sem @AuthenticationPrincipal Jwt jwt
    // Sem validação de autenticação
    // Apenas para endpoints verdadeiramente públicos
    
    return ResponseEntity.ok(service.listar());
}
```

---

## Referências

- 📄 [Matriz de Autorização Completa](./matriz-autorizacao.md)
- 📄 [Divergências de Segurança](./DIVERGENCIAS.md)
- 📄 [OWASP Top 10 - A01:2021 Broken Access Control](https://owasp.org/Top10/A01_2021-Broken_Access_Control/)
- 📄 [Spring Security Reference](https://docs.spring.io/spring-security/reference/)

---

**Última atualização**: 2026-11-12

