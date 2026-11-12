# Migração PostgreSQL — EPIC-02 Post-Merge

**Data:** 2026-11-12  
**Status:** ✅ **PRONTO PARA EXECUÇÃO**  
**PR:** #8 - fix(epic-02): secure reservation SPA and API  
**Merged em:** 2026-11-12T12:14:52-03:00  
**Commit:** 58411ac0904a248e1ccae74094f775e8611c3cf8  

---

## 🎯 Objetivo

Aplicar a migração Flyway para PostgreSQL que implementa a constraint de exclusão (EXCLUDE) na tabela `tb_reservas`, prevenindo sobreposição de reservas em status PENDENTE, APROVADA e EM_USO.

---

## ✅ Pré-Requisitos

- [ ] PostgreSQL 14+ instalado no ambiente alvo
- [ ] Banco de dados `organizacao_recursos` criado
- [ ] Usuário com permissões DDL no banco
- [ ] Backup recente do banco de dados
- [ ] Maven 3.8.1+ instalado
- [ ] Arquivo `.env` ou variáveis configuradas:
  - `DATABASE_URL` (ex: `jdbc:postgresql://host:5432/organizacao_recursos`)
  - `DATABASE_USERNAME`
  - `DATABASE_PASSWORD`

---

## 🚀 Procedimento de Execução

### Passo 1: Fazer Backup do Banco Atual

```bash
# Variáveis do ambiente
export TARGET_DB_HOST="seu-host-postgres"
export TARGET_DB_USER="seu-usuario"
export TARGET_DB_NAME="organizacao_recursos"
export TARGET_DB_PORT="5432"

# Criar backup
pg_dump -h $TARGET_DB_HOST \
        -U $TARGET_DB_USER \
        -p $TARGET_DB_PORT \
        -d $TARGET_DB_NAME \
        > backup-pre-migration-$(date +%Y%m%d_%H%M%S).sql

# Verificar tamanho do backup
ls -lh backup-pre-migration-*.sql
```

**Esperado:** Arquivo SQL gerado com sucesso contendo DDL e dados.

---

### Passo 2: Verificar Conexão com PostgreSQL

```bash
# Testar conexão
psql -h $TARGET_DB_HOST \
     -U $TARGET_DB_USER \
     -p $TARGET_DB_PORT \
     -d $TARGET_DB_NAME \
     -c "SELECT version();"

# Esperado: PostgreSQL 14 (ou superior)
```

---

### Passo 3: Verificar Estado Atual da Tabela `tb_reservas`

```bash
# Listar constraints existentes
psql -h $TARGET_DB_HOST \
     -U $TARGET_DB_USER \
     -p $TARGET_DB_PORT \
     -d $TARGET_DB_NAME \
     -c "SELECT constraint_name, constraint_type 
         FROM information_schema.table_constraints 
         WHERE table_name='tb_reservas' 
         ORDER BY constraint_name;"

# Contar registros
psql -h $TARGET_DB_HOST \
     -U $TARGET_DB_USER \
     -p $TARGET_DB_PORT \
     -d $TARGET_DB_NAME \
     -c "SELECT COUNT(*) as total_reservas, status, COUNT(*) as count_by_status
         FROM tb_reservas 
         GROUP BY status 
         ORDER BY status;"

# Exemplo de saída esperada:
# total_reservas | status    | count_by_status
# ============================================================
#              42 | PENDENTE  |              12
#              42 | APROVADA  |              20
#              42 | EM_USO    |               8
#              42 | CONCLUIDA |               2
```

---

### Passo 4: Aplicar Migração Flyway

```bash
# Navegar ao diretório raiz do projeto
cd /workspaces/Organiza-o-de-Recursos---QS-projeto-final

# Configurar variáveis de ambiente (se necessário)
export FLYWAY_URL="jdbc:postgresql://$TARGET_DB_HOST:$TARGET_DB_PORT/$TARGET_DB_NAME"
export FLYWAY_USER="$TARGET_DB_USER"
export FLYWAY_PASSWORD="$TARGET_DB_PASSWORD"

# Executar migração
./mvnw flyway:info

# Esperado: Listar todas as migrações e status de execução
# +-----------+---------+-----+------+------+------+
# | Category  | Version | Ins | Exe | Suc  | Fai  |
# +-----------+---------+-----+------+------+------+
# | Versioned | 1       | ... | ... | 1    | 0    |
# +-----------+---------+-----+------+------+------+

# Aplicar a migração
./mvnw flyway:migrate

# Esperado:
# [INFO] Flyway Community Edition 9.x.x
# [INFO] Database: PostgreSQL 14.x
# [INFO] Successfully validated 1 migration
# [INFO] Creating schema "public" ...
# [INFO] Running migration V1__reservas_sem_sobreposicao_ativa.sql ...
# [INFO] Migration V1__reservas_sem_sobreposicao_ativa.sql (committed)
```

**Possíveis Erros:**
- `ERROR: relation "btree_gist" does not exist` → Instalar extensão manualmente:
  ```sql
  CREATE EXTENSION IF NOT EXISTS btree_gist;
  ```
- `ERROR: conflicting key value violates exclusion constraint` → Dados existentes violam constraint. Consultar seção "Resolução de Conflitos" abaixo.

---

### Passo 5: Validar Constraint Foi Criada

```bash
# Listar constraints de exclusão
psql -h $TARGET_DB_HOST \
     -U $TARGET_DB_USER \
     -p $TARGET_DB_PORT \
     -d $TARGET_DB_NAME \
     -c "SELECT constraint_name, constraint_type 
         FROM information_schema.table_constraints 
         WHERE table_name='tb_reservas' 
         AND constraint_type='EXCLUDE';"

# Esperado:
# constraint_name | constraint_type
# ====================================
# ex_tb_reservas_sem_sobreposicao_ativa | EXCLUDE
```

---

### Passo 6: Validar Dados Migraram Corretamente

```bash
# Contar registros após migração
psql -h $TARGET_DB_HOST \
     -U $TARGET_DB_USER \
     -p $TARGET_DB_PORT \
     -d $TARGET_DB_NAME \
     -c "SELECT COUNT(*) as total_reservas FROM tb_reservas;"

# Esperado: Mesmo número de registros que antes

# Verificar integridade de foreign keys
psql -h $TARGET_DB_HOST \
     -U $TARGET_DB_USER \
     -p $TARGET_DB_PORT \
     -d $TARGET_DB_NAME \
     -c "SELECT COUNT(*) as orfaos_reservas 
         FROM tb_reservas r 
         WHERE NOT EXISTS (
           SELECT 1 FROM tb_recursos 
           WHERE id = r.recurso_id
         );"

# Esperado: 0 (zero registros órfãos)
```

---

### Passo 7: Testar Constraint com Dados

```bash
# Inserir reserva de teste (deve falhar se houver sobreposição)

-- Reserva 1: 2026-11-13 09:00-10:00
INSERT INTO tb_reservas (id, recurso_id, data_inicio, data_fim, status, usuario_id, criado_em)
VALUES (
  'test-001-unique-id',
  (SELECT id FROM tb_recursos LIMIT 1),  -- Usar recurso existente
  '2026-11-13 09:00:00',
  '2026-11-13 10:00:00',
  'APROVADA',
  (SELECT id FROM tb_usuarios LIMIT 1),
  NOW()
);
-- Esperado: INSERT 1 (sucesso)

-- Reserva 2: Sobreposta (09:30-10:30) — deve FALHAR
INSERT INTO tb_reservas (id, recurso_id, data_inicio, data_fim, status, usuario_id, criado_em)
VALUES (
  'test-002-overlap-id',
  (SELECT id FROM tb_recursos LIMIT 1),  -- Mesmo recurso
  '2026-11-13 09:30:00',
  '2026-11-13 10:30:00',
  'APROVADA',
  (SELECT id FROM tb_usuarios LIMIT 1),
  NOW()
);
-- Esperado: ERROR: conflicting key value violates exclusion constraint "ex_tb_reservas_sem_sobreposicao_ativa"
```

---

## ⚠️ Resolução de Conflitos

### Cenário: Dados Existentes Violam Constraint

Se houver reservas sobrepostas existentes no banco alvo, a migração falhará:

```
ERROR: conflicting key value violates exclusion constraint
```

**Solução:**

1. **Opção A: Limpar dados conflitantes** (se aceitável)
   ```sql
   -- Deletar reservas sobrepostas mantendo a mais antiga
   DELETE FROM tb_reservas
   WHERE id IN (
     SELECT r2.id
     FROM tb_reservas r1
     JOIN tb_reservas r2 ON r1.recurso_id = r2.recurso_id
       AND r1.id < r2.id
       AND r1.status IN ('PENDENTE', 'APROVADA', 'EM_USO')
       AND r2.status IN ('PENDENTE', 'APROVADA', 'EM_USO')
       AND tsrange(r1.data_inicio, r1.data_fim, '[)') && tsrange(r2.data_inicio, r2.data_fim, '[)')
   );
   ```

2. **Opção B: Marcar como CONCLUIDA** (preservar dados)
   ```sql
   -- Marcar reservas duplicadas como CONCLUIDA (inativa)
   UPDATE tb_reservas
   SET status = 'CONCLUIDA'
   WHERE id IN (
     SELECT r2.id FROM tb_reservas r1
     JOIN tb_reservas r2 ON r1.recurso_id = r2.recurso_id
       AND r1.id < r2.id
       AND r1.status IN ('PENDENTE', 'APROVADA', 'EM_USO')
       AND r2.status IN ('PENDENTE', 'APROVADA', 'EM_USO')
       AND tsrange(r1.data_inicio, r1.data_fim, '[)') && tsrange(r2.data_inicio, r2.data_fim, '[)')
   );
   ```

3. **Opção C: Restaurar backup e tentar novamente**
   ```bash
   # Restaurar banco
   psql -h $TARGET_DB_HOST \
        -U $TARGET_DB_USER \
        -p $TARGET_DB_PORT \
        -d $TARGET_DB_NAME \
        < backup-pre-migration-YYYYMMDD_HHMMSS.sql
   
   # Então aplicar opção A ou B e retry
   ```

---

## ✅ Validação Pós-Migração

Checklist de verificação:

- [ ] Migração Flyway executada sem erros
- [ ] Constraint `ex_tb_reservas_sem_sobreposicao_ativa` criada em `tb_reservas`
- [ ] Todos os registros anteriores preservados (COUNT ✅)
- [ ] Nenhum registro órfão (FK validation ✅)
- [ ] Teste de exclusão funciona (INSERT conflitante retorna erro 409)
- [ ] Aplicação iniciada com sucesso (`./mvnw spring-boot:run`)
- [ ] Testes de integração passam (`./mvnw test`)

---

## 📋 Rollback (Se Necessário)

Se a migração precisar ser desfeita:

```bash
# Restaurar do backup
psql -h $TARGET_DB_HOST \
     -U $TARGET_DB_USER \
     -p $TARGET_DB_PORT \
     -d $TARGET_DB_NAME \
     < backup-pre-migration-YYYYMMDD_HHMMSS.sql

# Flyway marcará a migração como mal-sucedida e poderá ser retentada
./mvnw flyway:undo  # (se Flyway Enterprise)
```

---

## 📚 Referências

- **Migração Flyway:** `src/main/resources/db/migration/V1__reservas_sem_sobreposicao_ativa.sql`
- **Constraint Details:** Exclusion constraint GIST com `tsrange` semi-aberto `[inicio, fim)`
- **Extensão PostgreSQL:** `btree_gist` (requerida para GIST indexes com tipos compostos)
- **Documentação:** [Flyway PostgreSQL](https://flywaydb.org/documentation/database/postgresql)

---

## 🔗 Relacionados

- PR #8: fix(epic-02): secure reservation SPA and API
- DEF-002: Sobreposição de Reservas PENDENTES
- RC-02: Requisito de Controle — Reservas do mesmo recurso não se sobrepõem

---

**Status:** ✅ PRONTO PARA EXECUÇÃO EM AMBIENTE ALVO
