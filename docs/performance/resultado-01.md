# Resultado de desempenho — Rodada 01

> **Evidência histórica:** esta rodada foi executada em 2026-11-11 sobre worktree sujo e H2 file-backed. Ela não representa a revisão atual. A rodada posterior de 2026-11-14 está em [resultado-02.md](resultado-02.md); em ambos os casos, H2 local e `GET /reservas` proxy não aprovam desempenho de produção.

**Decisão:** **INCONCLUSIVO** para aprovação ampla do RNF. Nesta execução local, os limites numéricos propostos foram atendidos; a API não tem endpoint seletivo de disponibilidade e o banco usado foi H2, portanto o resultado não demonstra desempenho PostgreSQL, staging ou produção.

## Ambiente

| Item | Valor |
|---|---|
| Máquina | Dev container Ubuntu 24.04.5 LTS; AMD EPYC 7763; 2 vCPU expostas; 7,8 GiB RAM |
| Aplicação | Organização de Recursos `0.0.1-SNAPSHOT`; Spring Boot 3.2.4; Java 25.0.5 |
| Banco | H2 2.2.224, arquivo local em `/tmp/organizacao-recursos-perf-run-01` |
| Carga | Apache JMeter 5.6.3 em CLI, executado com Java 21.0.13 |
| Commit base | `5b2a825` (`docs: atualização de documentos`); worktree estava sujo durante a execução, portanto o commit isolado não reproduz o código efetivamente executado |
| Destino | Aplicação em `127.0.0.1:8080`; sem acesso a ambiente compartilhado |

## Carga

Executada com `jmeter -n -t perf/reservas.jmx -l perf/resultados/rodada-01.jtl -e -o perf/relatorio-01`: 50 usuários virtuais, ramp-up de 60 s, 300 s estáveis (360 s no Thread Group), mistura 70/30 de consulta/criação, pausa de 200–1000 ms. As amostras de setup e do emissor local de JWT foram excluídas das métricas de negócio.

O plano emite JWT HS256 local de teste porque não existe endpoint de login. A consulta é `GET /reservas`, que lista a coleção inteira e serve apenas como proxy; não existe endpoint de disponibilidade filtrada por recurso e intervalo.

## Resultados

Percentis calculados sobre as durações do JTL pelo método nearest-rank; p95 confere com `pct2ResTime` do relatório JMeter. `409` é uma resposta funcional esperada e não entra na taxa de erro técnico.

| Operação | Amostras | p50 | p95 | p99 | Vazão JMeter | Respostas | Erro técnico |
|---|---:|---:|---:|---:|---:|---|---:|
| Consulta proxy `GET /reservas` | 9.577 | 2 ms | 6 ms | 11 ms | 26,66 req/s | 9.577 × 200 | 0/9.577 (0%) |
| Criação `POST /reservas` | 4.144 | 3 ms | 10 ms | 19 ms | 11,58 req/s | 1 × 201; 4.143 × 409 | 0/4.144 (0%) |
| Total de negócio | 13.721 | — | — | — | 38,24 op/s | 1 aceita; conflitos recusados | 0% |

No JTL completo houve 27.524 amostras, incluindo setup e JWT local; o relatório JMeter marcou erro total zero. Nenhuma criação respondeu `500` ou código fora de `201/409`.

## Verificação SQL

Após a carga, consultei diretamente o H2 file-backed:

```sql
SELECT recurso.nome, reserva.data_inicio, reserva.data_fim,
       COUNT(*) AS linhas_no_intervalo,
       SUM(CASE WHEN reserva.status IN ('APROVADA', 'EM_USO') THEN 1 ELSE 0 END) AS reservas_aceitas
FROM tb_reservas reserva
JOIN tb_recursos recurso ON recurso.id = reserva.recurso_id
WHERE recurso.nome = 'Sala JMeter Local'
GROUP BY recurso.nome, reserva.data_inicio, reserva.data_fim;
```

Resultado: `Sala JMeter Local | 2026-11-13 01:00:00 | 2026-11-13 02:00:00 | linhas_no_intervalo=1 | reservas_aceitas=1`. Portanto, o intervalo disputado terminou com exatamente uma linha persistida e uma reserva aceita; as 4.143 respostas `409` não criaram linhas adicionais.

## Comparação e decisão

Os p95 observados (6 ms consulta, 10 ms criação) ficaram abaixo do limite proposto de 300 ms; erro técnico foi 0%, abaixo de 1%. A asserção de contenção também foi confirmada pela consulta SQL. Ainda assim, a decisão geral é **INCONCLUSIVO**: a consulta medida não é disponibilidade seletiva, o teste usa H2 local, e uma máquina compartilhando aplicação e gerador não representa o ambiente de produção. O RNF continua como proposta até validação do grupo.

## Hipótese de gargalo e próximo experimento

Hipótese: ao crescer o histórico, `GET /reservas` pode se tornar o próximo gargalo por listar todas as reservas, sem filtros. Nesta rodada o p95 foi 6 ms e só havia uma reserva persistida no recurso disputado; isso não sustenta uma afirmação de gargalo atual, mas mostra que a carga não exercitou crescimento de cardinalidade. O próximo experimento deve semear volumes graduais (por exemplo, 10 mil e 100 mil reservas em recursos variados), repetir o perfil e coletar p95, vazão, CPU/memória e plano SQL. A execução deve continuar local e não ser extrapolada para produção.