# Plano de Teste de Carga — Rodada Local

**Finalidade:** produzir evidência local repetível para RNF-03 sem alegar desempenho de produção. Os parâmetros de p95 e erro abaixo são propostas ainda sujeitas à aprovação do grupo.

## Perfil

| Parâmetro | Valor |
|---|---|
| Ferramenta | Apache JMeter 5.6.3, modo CLI |
| Runtime Java | Java 21 (alinhado ao CI; JMeter 5.6.3/Groovy não executa os scripts do plano com Java 25) |
| Destino | Aplicação local em `127.0.0.1:8080`, perfil `demo` |
| Banco | H2 em memória |
| Usuários | 50 virtuais, cada qual com identidade seed fictícia |
| Ramp-up | 60 s |
| Duração total do Thread Group | 360 s (60 s ramp-up + 300 s de carga) |
| Mistura | Aproximadamente 70% `GET /reservas`, 30% `POST /reservas` |
| Intervalo de iteração | Pausa aleatória entre 200 e 1.000 ms |
| Limites propostos | p95 ≤ 300 ms; erro técnico < 1%; no máximo um aceite na disputa |

O GET mede a listagem integral de reservas como proxy, não disponibilidade seletiva. O plano assina JWT HS256 localmente; o segredo deve ser temporário, igual ao do processo da aplicação e nunca ser usado fora do teste. O perfil `demo` é isolado de dados persistidos e produção.

## Execução

Inicie a aplicação em terminal separado com Java 21, `SPRING_PROFILES_ACTIVE=demo` e `JWT_SECRET` temporário. Depois, na raiz do repositório, rode JMeter também com Java 21:

```bash
export JAVA_HOME=/caminho/para/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
export SPRING_PROFILES_ACTIVE=demo
export JWT_SECRET="<segredo-temporario-local>"
./mvnw -B spring-boot:run
```

Em outro terminal, com o mesmo JDK e segredo:

```bash
jmeter -n -t perf/reservas.jmx \
  -l /tmp/organizacao-recursos-rodada-01.jtl \
  -e -o /tmp/organizacao-recursos-relatorio-01 \
  -Jperf.threads=50 -Jperf.rampUpSeconds=60 -Jperf.durationSeconds=360
```

Defina o mesmo `JWT_SECRET` para a aplicação e o JMeter sem registrá-lo em arquivos ou logs. O plano cria os dados de execução; JTL e HTML ficam fora do versionamento. Caso o relatório HTML exista, use diretório de saída vazio. Java 25 não é compatível com a versão Groovy empacotada no JMeter 5.6.3; essa incompatibilidade causou uma tentativa sem amostras e sem resultado válido.

## Critérios de análise e limitações

Excluir setup e emissão local de token dos agregados de negócio. Reportar p50/p95/p99, vazão, amostras e respostas por operação. `201` e `409` são funcionais para criação; timeout, 5xx ou qualquer outro status são erro técnico. Conferir a disputa de intervalo separadamente.

Uma rodada local com H2 é somente diagnóstico. A aprovação do RNF requer confirmação dos parâmetros, consulta de disponibilidade seletiva e execução com PostgreSQL/ambiente representativo, volume de dados declarado e métricas de recursos do servidor.
