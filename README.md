<p align="center">
  <a href="https://www.gov.br/inpe/pt-br">
    <img src="docs/assets/inpe-logo.png" alt="Instituto Nacional de Pesquisas Espaciais — INPE" width="144" />
  </a>
</p>

<h1 align="center">INPE · CCS</h1>
<p align="center"><strong>CORA — Assistente Orbital · Nível 3</strong></p>
<p align="center">Fachada REST, cliente gRPC do orbital-dynamics e agente com ferramentas.</p>
<p align="center">
  <code>Java 25</code> &nbsp; <code>Quarkus</code> &nbsp; <code>gRPC client</code> &nbsp;
  <code>REST/JSON</code> &nbsp; <code>Prometheus</code>
</p>

---

## Identificação do serviço

| Campo | Valor |
| --- | --- |
| Nome do serviço | `cora` |
| Nível | **3** |
| Finalidade | Expor consultas orbitais por REST, consumir o L2 por gRPC e responder perguntas com um LLM |
| Responsabilidades | Filtrar satélites/estações, listar elementos, prever passagens, obter footprints e orquestrar ferramentas |
| Limites de atuação | Não coleta TLE, não mantém catálogo de estações e não propaga órbitas localmente |
| Consumidores previstos | Interface web, CLI e integrações de IA |
| Fontes de dados | `orbital-dynamics` (gRPC), que por sua vez usa os L1 |
| Situação | REST e ferramentas funcionais; LLM configurável por ambiente |

## Arquitetura

```text
cliente REST
   │
   ▼
CORA (Quarkus)
   ├── REST/JSON
   ├── LLM (LangChain4j, OpenAI-compatible)
   └── gRPC client (plaintext local)
          ▼
       orbital-dynamics:9090
          ├── ListSatellites
          ├── ListStations
          ├── PredictPasses
          ├── Propagate
          └── GetVisibilityFootprint
```

CORA é **stateless**: não usa banco de dados nesta versão. Isso evita PostgreSQL
e Flyway no caminho da demonstração. Persistência de conversas/auditoria pode ser
adicionada depois, quando houver um requisito concreto.

## Interface REST

| Método | Caminho | Uso |
| --- | --- | --- |
| `GET` | `/api/info` | Identidade do serviço |
| `GET` | `/api/elements/{catId}` | Elementos mais recentes de um satélite |
| `GET` | `/api/satellites` | Filtro por `name`, `catId`, `group`, `pageSize`, `pageToken` |
| `GET` | `/api/stations` | Filtro por `source`, `status`, `countryCode`, `operator`, `query` |
| `POST` | `/api/passes` | Passagens + ticks + footprint opcional; aceita `tlePolicy`/`maxTleAgeHours` |
| `POST` | `/api/orbit` | Propagação histórica com `tlePolicy`/`maxTleAgeHours` |
| `POST` | `/api/footprint` | Footprint WGS84 por altitude ou por objeto + instante |
| `POST` | `/api/chat` | Pergunta ao agente; requer LLM habilitado |

`GET /q/health/live`, `GET /q/health/ready` e `GET /q/metrics` continuam disponíveis.

### Exemplos

```bash
curl -s http://localhost:8090/api/info

curl -s 'http://localhost:8090/api/satellites?name=CBERS&pageSize=5'

curl -s 'http://localhost:8090/api/stations?source=ESA&status=ACTIVE&pageSize=5'

curl -s -X POST http://localhost:8090/api/passes \
  -H 'Content-Type: application/json' \
  -d '{
    "catId": 44883,
    "stationCode": "INPE-ALC",
    "start": "2026-10-27T00:00:00Z",
    "end": "2026-10-27T06:00:00Z",
    "tickSeconds": 60,
    "includeFootprint": true,
    "refraction": true,
    "footprintSamples": 36,
    "tlePolicy": "AS_OF",
    "maxTleAgeHours": 48
  }'

curl -s -X POST http://localhost:8090/api/orbit \
  -H 'Content-Type: application/json' \
  -d '{
    "catId": 25504,
    "start": "2026-01-02T22:00:00Z",
    "end": "2026-01-02T23:00:00Z",
    "stepSeconds": 300,
    "tlePolicy": "AS_OF",
    "maxTleAgeHours": 48
  }'

curl -s -X POST http://localhost:8090/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"message": "Quando o CBERS 4A passa por Alcântara nas próximas 24h?"}'

curl -s -X POST http://localhost:8090/api/footprint \
  -H 'Content-Type: application/json' \
  -d '{
    "stationCode": "INPE-ALC",
    "satelliteAltitudeM": 500000,
    "samples": 36,
    "refraction": true
  }'
```

As respostas são o JSON canônico dos protobufs do L2 (`JsonFormat`), com
`camelCase` e timestamps RFC 3339.

Entradas inválidas retornam `400`. Falhas conhecidas do L2 são traduzidas para
HTTP (`400` para argumento inválido, `404` para recurso ausente, `503` para
indisponibilidade e `504` para deadline/timeout); falhas inesperadas do upstream
retornam `502`. A resposta de erro tem os campos `error` e `message`.

## Agente de IA

O `/api/chat` usa **LangChain4j** com um provedor OpenAI-compatible. O provedor
padrão é a **DeepSeek** (`deepseek-chat`), configurável por ambiente. O modelo
recebe ferramentas que chamam o L2 e ferramentas de seleção da UI:

- `listSatellites`
- `listStations`
- `predictPasses`
- `propagate`
- `getVisibilityFootprint`
- `selectSatellites`, `followSatellite`, `selectStations`, `focusStation`, `clearSelection`
- `stopFollowing`, `setCamera`, `setLayers`, `setTime`, `setPlayback`, `goLive`, `openPanel`

O chat aceita a seleção atual e devolve ações para a UI:

```json
{
  "message": "Mostre o CBERS 4A em Alcântara.",
  "selection": {"satellites": ["25504"], "stations": ["INPE-CBA"]}
}
```

```json
{
  "answer": "Feito...",
  "actions": [
    {"type": "selectSatellites", "ids": ["44883"]},
    {"type": "focusStation", "ids": ["INPE-ALC"]}
  ]
}
```

O CORA não altera o estado do navegador diretamente; o cliente aplica as ações.

Para ver ou seguir um satélite específico, o agente consulta seu NORAD/catId e
emite `followSatellite`. A UI adiciona o objeto à seleção, encerra o foco de estação
e acompanha continuamente a posição no horário visualizado. Exemplo de pedido:

```json
{"message":"Quero ver o AMAZONIA 1", "selection":{"satellites":[],"stations":[]}}
```

A ação esperada é:

```json
{"type":"followSatellite","ids":["47699"]}
```

`selectSatellites` adiciona objetos sem iniciar acompanhamento. `focusStation` e
`clearSelection` encerram o acompanhamento do satélite. A UI oferece também
“Parar de seguir” e interrompe o acompanhamento quando o usuário move a câmera.

Configuração DeepSeek (padrão):

```bash
export CORA_AI_ENABLED=true
export CORA_AI_BASE_URL=https://api.deepseek.com/v1
export CORA_AI_MODEL=deepseek-chat
export CORA_AI_API_KEY='...'
```

Alternativa local com Ollama:

```bash
ollama serve
ollama pull qwen2.5:3b
export CORA_AI_ENABLED=true
export CORA_AI_BASE_URL=http://localhost:11434/v1
export CORA_AI_MODEL=qwen2.5:3b
export CORA_AI_API_KEY=ollama
```

A IA fica **desabilitada** por padrão. Sem `CORA_AI_ENABLED=true` ou sem
`CORA_AI_API_KEY`, o `/api/chat` retorna `503` e o restante da API continua
funcionando.

## Configuração

| Variável | Uso | Padrão |
| --- | --- | --- |
| `HTTP_PORT` | REST, health e métricas | `8080` |
| `GRPC_PORT` | Porta do servidor gRPC do CORA | `9000` |
| `ORBITAL_DYNAMICS_HOST` | Host do L2 | `localhost` |
| `ORBITAL_DYNAMICS_PORT` | Porta gRPC do L2 | `9090` |
| `JAVA_MAX_MEM_RATIO` | Heap máximo em relação ao limite do container | `50` |
| `CORA_AI_ENABLED` | Habilita `/api/chat` | `false` |
| `CORA_AI_BASE_URL` | Endpoint OpenAI-compatible | `https://api.deepseek.com/v1` |
| `CORA_AI_MODEL` | Modelo | `deepseek-chat` |
| `CORA_AI_API_KEY` | Chave do provedor | vazio |

O servidor gRPC do CORA sobe por herança do template, mas ainda não publica
serviço de domínio. A interface externa desta versão é REST.

## Desenvolvimento

```bash
./gradlew quarkusDev
./gradlew clean test quarkusBuild
java -jar build/quarkus-app/quarkus-run.jar
```

Em dev, `ORBITAL_DYNAMICS_HOST`/`PORT` apontam para `localhost:9090`. O L2 precisa
estar rodando e saudável.

## Docker Compose

No stack raiz, o serviço `cora` é construído a partir de
`l3/CORA/src/main/docker/Dockerfile.jvm` e publicado em `localhost:8090`
(HTTP) e `localhost:9091` (gRPC). O `up.sh` executa `quarkusBuild` antes do
`docker compose build`.

```bash
docker compose up -d --no-build --wait cora
curl -s http://localhost:8090/api/info
```

## Testes

- `CoraResourceTest`: identidade, validação REST, defaults protobuf e tradução de erros gRPC com L2 simulado.
- `InstrumentationTest`: health e métricas JVM/HTTP com labels de identidade.
- `SetupTest`: personalização do template.
- `CoraAssistantServiceTest`: coleta e descarte das ações após sucesso ou falha do provedor.
- `CoraApplicationIT`: smoke test do pacote.

Os testes **não** chamam o L2 real; as chamadas gRPC são validadas no smoke test
manual/compose.

## Limitações

- O LLM é opcional e desabilitado por padrão; sem provedor configurado, `/api/chat` retorna `503`.
- Não há banco, autenticação ou TLS.
- O client gRPC usa `plaintext` no ambiente local.
- As respostas de passagens com `includeTickFootprints` podem ser grandes; para
  UI, prefira footprints no TCA ou endpoints de resumo.

## Eventos para controlar a interface

As ações mantêm `type` e `ids`, com o campo adicional `parameters` (objeto, vazio
nas ações de seleção). O cliente aceita apenas os tipos e valores documentados,
aplica os efeitos e registra na conversa o resultado. São comandos de visualização;
não alteram dados orbitais, agendamentos ou antenas reais.

| Ação | Parâmetros | Exemplo de pedido |
| --- | --- | --- |
| `followSatellite` | `ids: ["47699"]` | Quero ver o AMAZONIA 1 |
| `stopFollowing` | nenhum | Pare de seguir o satélite |
| `focusStation` | `ids: ["INPE-ALC"]` | Foque Alcântara |
| `setCamera` | `view: world/brazil/home` | Mostre o Brasil |
| `setLayers` | `orbits` e/ou `coverage`, booleanos | Oculte as trajetórias |
| `setTime` | `at`, ISO 8601 com fuso | Mostre 2/11/2026 às 18h UTC |
| `setPlayback` | `rate: 0/1/30/120` | Acelere a reprodução para 30 vezes |
| `goLive` | nenhum | Volte ao horário atual |
| `openPanel` | `panel: stations/satellites/visualization/passes` | Abra o catálogo de satélites |

`openPanel(passes)` requer uma estação em foco; o agente deve emitir `focusStation`
antes. `setTime` pausa a reprodução e usa os elementos já carregados pela UI,
sem prometer reconstrução histórica com TLE adequado. O agente pede esclarecimento
quando data/hora ou fuso são ambíguos. Exemplo de combinação:

```json
{
  "answer": "Reprodução em 30 vezes, com trajetórias ocultas.",
  "actions": [
    {"type":"setPlayback","ids":[],"parameters":{"rate":30}},
    {"type":"setLayers","ids":[],"parameters":{"orbits":false}}
  ]
}
```

Os filtros das ferramentas de catálogo são opcionais no schema enviado ao modelo.
Para buscar por nome, o agente omite NORAD e grupo em vez de preencher números
fictícios; zero no NORAD opcional é tratado como filtro ausente. Parâmetros
opcionais de propagação, passagens e camadas também podem ser omitidos.
