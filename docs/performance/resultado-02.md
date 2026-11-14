# Resultado de desempenho — Rodada 02

**Data:** 2026-11-14
**Decisão:** **INCONCLUSIVO** para aprovação do RNF em produção. Esta rodada confirma apenas um diagnóstico local com H2 em memória.

## Ambiente e escopo

| Item | Valor |
|---|---|
| Aplicação | Organização de Recursos `0.0.1-SNAPSHOT`; Spring Boot 3.2.4 |
| Runtime | OpenJDK 21.0.13 |
| Banco | H2 em memória, perfil `demo` |
| Carga | Apache JMeter 5.6.3 em CLI |
| Destino | Aplicação local em `127.0.0.1:8080` |
| Perfil | 50 usuários virtuais; ramp-up de 60 s; 300 s estáveis; mistura aproximada 70/30 |
| Revisão | Branch local `fix/verify-profile-isolation`, base `01bd469` mais alterações não commitadas |

O JTL contém 27.552 amostras incluindo setup e emissão local de JWT. As métricas abaixo consideram apenas os samplers de negócio. O GET medido é `GET /reservas`, uma listagem integral usada como proxy; não existe endpoint de disponibilidade seletiva.

## Resultados

Percentis nearest-rank calculados sobre as durações do JTL. `201` e `409` são resultados funcionais esperados na criação; falhas técnicas foram contabilizadas separadamente.

| Operação | Amostras | p50 | p95 | p99 | Vazão JMeter | Respostas | Erro técnico |
|---|---:|---:|---:|---:|---:|---|---:|
| Consulta proxy `GET /reservas` | 9.574 | 1 ms | 6 ms | 13 ms | 26,73 req/s | 9.574 × 200 | 0/9.574 (0%) |
| Criação `POST /reservas` | 4.163 | 3 ms | 12 ms | 26 ms | 11,70 req/s | 1 × 201; 4.162 × 409 | 0/4.163 (0%) |
| Total de negócio | 13.737 | — | — | — | 38,44 op/s | 1 aceita; conflitos recusados | 0% |

## Decisão e limitações

Os p95 observados ficaram abaixo do limite proposto de 300 ms e não houve erro técnico nesta execução. Isso não aprova o RNF: a consulta não é seletiva, o banco é H2 em memória, a execução é local e não foram coletadas métricas de recursos do servidor nem feita validação em PostgreSQL/staging. A aprovação exige limites acordados e nova rodada representativa com volume de dados declarado.

A rodada histórica com H2 file-backed e consulta SQL está em [resultado-01.md](resultado-01.md); seus números não devem ser combinados com esta execução. O plano, as limitações e os critérios de análise estão em [plano-01.md](plano-01.md) e [rnf.md](rnf.md).
