# Proposta de RNF de desempenho: reservas

**Estado:** Proposta para validação do grupo; ainda não aprovada.

## Requisitos propostos

1. **Consulta de disponibilidade/agenda:** sob 50 usuários virtuais simultâneos, o p95 da operação de consulta deve ser de até 300 ms e a taxa de erro técnico deve ficar abaixo de 1%.
2. **Criação de reserva:** sob o mesmo perfil de carga, o p95 deve ser de até 300 ms. Respostas `201` e `409` são resultados funcionais válidos; a taxa de respostas técnicas inválidas (timeouts, `5xx` ou status diferente de `201`/`409`) deve ficar abaixo de 1%.
3. **Contenção:** quando os usuários disputarem o mesmo recurso e intervalo, a aplicação deve manter a regra de exclusão: no máximo uma criação retorna `201`; as demais solicitações conflitantes retornam `409` e não criam reservas sobrepostas.

O p95 e a taxa de erro devem ser reportados separadamente para consulta e criação. A avaliação não inclui o bootstrap de dados nem a emissão local de tokens.

## Modelo de carga proposto

| Dimensão | Proposta |
|---|---|
| Usuários simultâneos | 50 usuários virtuais, cada qual associado a um usuário fictício distinto |
| Ramp-up | 60 segundos até atingir 50 usuários ativos |
| Duração estável | 300 segundos após o ramp-up; duração total do Thread Group: 360 segundos |
| Mistura de operações | 70% consulta de agenda e 30% tentativa de criar reserva |
| Recurso/intervalo | Um recurso local compartilhado e o mesmo intervalo futuro para todas as criações |
| Ritmo entre iterações | Pausa aleatória de 0,2 a 1 segundo para evitar um loop artificialmente apertado |
| Ambiente | Apenas aplicação local em `127.0.0.1`; não executar contra staging, produção ou serviço compartilhado |

## Lacunas para validação

- O contrato atual não oferece login. A API valida bearer JWT HS256 usando `JWT_SECRET`, sem emitir tokens. O plano JMeter usa somente um token fictício, assinado localmente com o segredo do processo, para testar a aplicação local; isso não mede autenticação real.
- Não existe endpoint de disponibilidade por recurso e intervalo. O GET existente `GET /reservas` lista reservas sem filtros. Até existir uma consulta dedicada (por exemplo, `GET /recursos/{id}/disponibilidade?inicio=...&fim=...`), o plano mede o GET de listagem como proxy de leitura da agenda e não deve ser interpretado como medição de uma consulta de disponibilidade seletiva.
- O grupo deve validar os limites de 300 ms, 1%, 50 usuários, a janela de duração e a mistura 70/30 antes de tratar este documento como RNF aprovado.

## Execução local

1. Inicie a aplicação em `127.0.0.1:8080` com H2 local e `JWT_SECRET` definido no processo. Use o mesmo segredo ao executar o JMeter; não reutilize segredo de produção.
2. Execute o plano apenas em CLI: `jmeter -n -t perf/reservas.jmx -l perf/resultados/rodada-01.jtl -e -o perf/relatorio-01`.
3. O Thread Group de setup cria um recurso local compartilhado; cada thread cria um usuário fictício da CSV. O sampler `LOGIN local simulado` emite JWT HS256 de teste e um JSON Extractor captura `access_token`. Não há senha validada nem serviço de autenticação envolvido.
4. Separe as amostras `SETUP`/`LOGIN` dos samplers de negócio ao calcular os limites. O GET medido é `GET /reservas`; ele é somente proxy de leitura, não um endpoint de disponibilidade seletiva.

Arquivos JTL e relatórios HTML ficam ignorados pelo Git. Versione o plano e a lista fictícia, nunca os resultados gerados.

## Resultado observado: rodada local

**Data:** 2026-11-11. **Ambiente:** aplicação local em H2 file-backed, JMeter CLI, 50 usuários, 60 s de ramp-up e 300 s estáveis. O relatório/JTL contém também amostras de setup e do emissor local de token; os números abaixo filtram apenas as duas operações de negócio.

| Operação | Amostras | Respostas | p50/p95/p99 | Erros técnicos |
|---|---:|---|---:|---:|
| Consulta proxy `GET /reservas` | 9.577 | 9.577 × 200 | 2/6/11 ms | 0 (0%) |
| Criação `POST /reservas` | 4.144 | 1 × 201; 4.143 × 409 | 3/10/19 ms | 0 (0%) |

A vazão do relatório JMeter foi 26,66 consultas/s e 11,58 criações/s (38,24 operações de negócio/s); a mistura foi 69,8%/30,2%. Nenhuma criação retornou 500 ou status diferente de 201/409. Os resultados atendem numericamente aos limites propostos nesta execução local; não aprovam o RNF, não substituem uma consulta de disponibilidade seletiva e não representam desempenho de PostgreSQL, staging ou produção. A repetição com H2 file-backed e a checagem SQL estão em [resultado-01.md](resultado-01.md).