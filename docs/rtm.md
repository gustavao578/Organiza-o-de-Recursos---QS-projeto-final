# Rastreabilidade de desempenho

Esta página liga a proposta de desempenho, o plano executável e a evidência da rodada. A matriz funcional canônica continua em [prd/rtm.md](../prd/rtm.md), linha RNF-03.

| ID | Requisito | Plano e dados | Resultado | Estado |
|---|---|---|---|---|
| RNF-03 / RNF-PERF-01 | p95 ≤ 300 ms e erro técnico < 1% sob 50 usuários; disputa de um recurso/intervalo permite no máximo uma criação aceita. Proposta pendente de validação. | [Proposta e perfil](performance/rnf.md); [plano JMeter](../perf/reservas.jmx); [usuários fictícios](../perf/usuarios.csv). | [Resultado-01: métricas, SQL, decisão e limitações](performance/resultado-01.md). | **INCONCLUSIVO** para aprovar o RNF: medição local em H2 e GET de listagem usado como proxy de disponibilidade. |