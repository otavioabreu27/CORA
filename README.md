<p align="center">
  <a href="https://www.gov.br/inpe/pt-br">
    <img src="docs/assets/inpe-logo.png" alt="Instituto Nacional de Pesquisas Espaciais — INPE" width="144" />
  </a>
</p>

<h1 align="center">INPE · CCS</h1>
<p align="center"><strong>Plataforma de serviços · Nível 1</strong></p>
<p align="center">Contratos explícitos. Integrações documentadas. Instrumentação desde o início.</p>
<p align="center">
  <code>Java 25</code> &nbsp; <code>Quarkus</code> &nbsp; <code>gRPC</code> &nbsp;
  <code>PostgreSQL</code> &nbsp; <code>RabbitMQ</code> &nbsp; <code>Prometheus</code>
</p>

---

## Identificação do serviço

| Campo | Valor |
| --- | --- |
| Nome do serviço | <!-- service-name -->A preencher<!-- /service-name --> |
| Nível | **1** |
| Finalidade | A preencher — descreva o problema e o resultado entregue |
| Responsabilidades | A preencher — o que este serviço faz |
| Limites de atuação | A preencher — responsabilidades de outros serviços |
| Equipe responsável | A preencher |
| Contato / suporte | A preencher |
| Consumidores previstos | A preencher |
| Fontes de dados | A preencher |
| Repositório / ambiente | A preencher |
| Situação | Template — personalizar antes do uso |

O nome técnico inicial é `l1-service-template`. O setup preenche o nome acima e
atualiza a identidade do projeto. Os demais campos são preenchidos pela equipe.

## Eventos

**Nenhum evento de domínio é publicado ou consumido pelo template.** O cliente
RabbitMQ está disponível (ative com `RABBITMQ_ENABLED=true`); preencha o catálogo abaixo ao implementar as integrações.

| Direção | Tipo / versão | Quando ocorre | Exchange / routing key | Contrato do payload | Consumidores |
| --- | --- | --- | --- | --- | --- |
| Publica | A preencher | A preencher | A preencher | Link para schema/exemplo | A preencher |
| Consome | A preencher | A preencher | A preencher | Link para schema/exemplo | Este serviço |

| Aspecto da entrega | Definição do serviço |
| --- | --- |
| Identificador e correlação | A preencher — `eventId`, `correlationId` e origem |
| Filas e bindings | A preencher — uma fila por consumidor independente |
| Confirmação e durabilidade | A preencher — quando confirmar e como persistir |
| Retentativas / DLQ | A preencher — limites, intervalos e tratamento de falhas |
| Idempotência e ordenação | A preencher — chave de deduplicação e garantias |
| Retenção / reprocessamento | A preencher — prazo e mecanismo de replay |
| Evolução do contrato | A preencher — compatibilidade e versionamento |

## Interfaces para consumidores

O contrato de exemplo está em [hello.proto](src/main/proto/hello.proto).
Substitua-o pelo domínio do serviço e mantenha esta tabela sincronizada.

| Interface | Operação / endereço | Entrada | Saída | Situação |
| --- | --- | --- | --- | --- |
| gRPC | `inpe.ccs.l1servicetemplate.v1.HelloGrpc/SayHello` | `HelloRequest { name }` | `HelloReply { message }` | Exemplo funcional |
| REST | `GET /hello` | Sem parâmetros | Texto `Hello from Quarkus REST` | Exemplo funcional, removível |
| Prometheus | `GET /q/metrics` | Scrape HTTP | Métricas Prometheus | Implementado |
| Liveness | `GET /q/health/live` | Sem parâmetros | JSON com estado | Implementado |
| Readiness | `GET /q/health/ready` | Sem parâmetros | JSON com estado | Implementado |

| Regra de integração | Definição |
| --- | --- |
| Endereço por ambiente | A preencher; local: gRPC `localhost:9000`, HTTP `localhost:8080` |
| Autenticação / TLS | A definir por ambiente; o template local não exige autenticação nem TLS |
| Filtros / paginação / ordenação | A preencher nos contratos de domínio; ausentes no exemplo |
| Deadline / timeout | A preencher por operação |
| Códigos de erro | A preencher — status gRPC e significado para o consumidor |
| Limites e tamanho de mensagens | A preencher |
| Compatibilidade | A preencher — política de versões e descontinuação |

Exemplo local, sem depender de reflection:

```bash
grpcurl -plaintext -import-path src/main/proto -proto hello.proto \
  -d '{"name":"CCS"}' localhost:9000 \
  inpe.ccs.l1servicetemplate.v1.HelloGrpc/SayHello
```

## Instrumentação pronta

Micrometer exporta as métricas em **`http://localhost:8080/q/metrics`**.
Prometheus consulta esse endpoint; não é necessário um agente dentro do serviço.
As séries recebem as labels fixas `application` (nome configurado) e `level="1"`.
O Prometheus pode acrescentar `job` e `instance` no scrape.

| Área | Exemplos de métricas | Disponibilidade |
| --- | --- | --- |
| JVM | `jvm_memory_used_bytes`, GC, threads | Automática |
| Processo / sistema | CPU, uptime, descritores conforme o sistema | Automática |
| HTTP | `http_server_requests_seconds_*` | Após chamadas HTTP |
| gRPC | `grpc_server_processing_duration_seconds_*`, mensagens recebidas/enviadas | Após chamadas gRPC |
| Pool PostgreSQL | `agroal_*` | Com datasource ativo |
| Negócio / eventos | A preencher — contadores e timers do domínio | Implementar no serviço |

```bash
curl --fail http://localhost:8080/q/metrics
curl --fail http://localhost:8080/q/health/ready
```

Um exemplo de scrape está em [docs/prometheus.yml](docs/prometheus.yml). Ajuste o
host para onde o Prometheus executa; `localhost` dentro de um contêiner identifica
o próprio contêiner. Evite labels com valores ilimitados, como IDs e payloads.
Métricas específicas de filas RabbitMQ não são exportadas por este template.

## Setup inicial

Requisitos: **JDK 25** para setup/build e **Docker** para os testes e Dev Services.
Use uma pasta sem espaços para o projeto: o gerador protobuf/gRPC do Quarkus
3.40.1.1 apresentou falha de resolução de caminhos com espaços nesta validação.
Os dois scripts usam [tools/Setup.java](tools/Setup.java), sem dependências extras,
em modo de execução de fonte Java. Não é preciso compilar um executável nativo.

**Linux / macOS**

```bash
./setup.sh --dry-run telemetry-ingest inpe.ccs 1.0.0-SNAPSHOT
./setup.sh telemetry-ingest inpe.ccs 1.0.0-SNAPSHOT
```

**Windows — CMD ou PowerShell**

```bat
.\setup.bat --dry-run telemetry-ingest inpe.ccs 1.0.0-SNAPSHOT
.\setup.bat telemetry-ingest inpe.ccs 1.0.0-SNAPSHOT
```

Sem argumentos, o setup pergunta o nome. Grupo e versão são opcionais e mantêm os
valores de `gradle.properties`. Hífens viram underscores no pacote Java:
`telemetry-ingest` → `inpe.ccs.telemetry_ingest`.

O setup atualiza nome, grupo, versão, pacotes Java (inclusive testes nativos),
contratos protobuf, exemplos no README e referências de imagens Docker. O nível
permanece **1**. A pasta do repositório, o histórico e o remote Git são preservados.
Use-o antes de publicar os contratos: renomear o pacote protobuf altera o nome
completo do serviço gRPC. Após reconfigurar um projeto já compilado, execute `clean`
para remover classes geradas com a identidade anterior.

## Desenvolvimento e verificação

```bash
./gradlew quarkusDev
./gradlew clean test quarkusBuild
```

No Windows, use `gradlew.bat`. PostgreSQL é iniciado automaticamente pelo Dev
Services. **Dev/test usam `drop-and-create` e precisam de bancos descartáveis.**
O exemplo não abre conexões RabbitMQ nem exige um broker para os testes.

Lombok está configurado em todos os source sets. MapStruct gera beans CDI com
injeção por construtor; `lombok-mapstruct-binding` integra os dois processadores.
[MyEntityMapper](src/main/java/inpe/ccs/l1servicetemplate/MyEntityMapper.java) demonstra
uma conversão entre entidade com Lombok e DTO. O plugin do Quarkus gera mensagens
e stubs gRPC a partir de `src/main/proto`; não adicione outro gerador protobuf.

No Eclipse, instale Lombok na IDE e atualize o projeto Gradle.

### VS Code / VSCodium e classes gRPC geradas

Abra a pasta raiz que contém `build.gradle` e importe o projeto como Gradle.
O plugin `eclipse` expõe ao Buildship/servidor Java a pasta gerada pelo Quarkus
(`build/classes/java/quarkus-generated-sources/grpc`). A sincronização Gradle
executa `quarkusGenerateCode`; não é necessário instalar outro gerador protobuf
nem copiar os arquivos gerados para `src/main/java`.

Se aparecer **“non-project file”**, execute pela paleta (`Ctrl+Shift+P`)
**Java: Import Java Projects into Workspace**. Se o projeto já estiver importado,
mas `HelloGrpc`, `HelloReply` ou outros tipos gerados não forem encontrados:

1. Execute `./gradlew quarkusGenerateCode` na raiz (também após alterar o `.proto`).
2. Execute **Java: Reload Projects** para atualizar o classpath Gradle.
3. Se o índice continuar desatualizado, execute
   **Java: Clean Java Language Server Workspace** e aceite reiniciar o servidor.

No Eclipse, use **Gradle → Refresh Gradle Project**. O ajuste pertence ao modelo
Gradle; `.classpath` e os arquivos em `build/` continuam gerados e ignorados pelo Git.

## Configuração e implantação

| Variável | Uso | Padrão / requisito |
| --- | --- | --- |
| `HTTP_PORT` | REST, métricas e health | `8080` |
| `GRPC_PORT` | Servidor gRPC | `9000` |
| `GRPC_TEST_PORT` | gRPC em testes JVM e testes do executável com perfil `test` | `9001` |
| `DB_JDBC_URL` | JDBC PostgreSQL | Obrigatória em produção |
| `DB_USERNAME` | Usuário PostgreSQL | Obrigatória em produção |
| `DB_PASSWORD` | Senha PostgreSQL | Obrigatória em produção |
| `RABBITMQ_ENABLED` | Ativa o cliente RabbitMQ e seu health check | `false` no template |
| `RABBITMQ_URI` | Conexão / credenciais / vhost RabbitMQ | Configurar ao ativar mensagens |

O schema de produção deve ser provisionado pelo serviço: Hibernate usa `validate`.
O template não inclui migrações de domínio.

```bash
./gradlew quarkusBuild
java -jar build/quarkus-app/quarkus-run.jar
```

Distribua a pasta `build/quarkus-app` inteira. Os Dockerfiles estão em
`src/main/docker/` e expõem as portas 8080 e 9000.

## CI/CD

O [workflow do GitHub Actions](.github/workflows/ci-cd.yml) usa esta política:

| Origem | Verificação | Entrega no GHCR |
| --- | --- | --- |
| Push em qualquer outra branch | Testes JVM com PostgreSQL descartável | Sem publicação |
| Pull request para qualquer branch | Testes JVM sobre o merge de teste | Sem publicação |
| Push em `development` | Testes JVM + build Java 25 | Imagem JVM, executada com `java -jar`; tag `development` |
| Push em `main` | Testes JVM + compilação e testes nativos | Imagem nativa GraalVM/Mandrel; tag `main` |
| Execução manual pelo Actions | Mesmas verificações da branch selecionada | Publica somente em `development` ou `main` |

As imagens usam `ghcr.io/<owner>/<repository>`, derivado automaticamente do
repositório. Cada publicação também recebe uma tag `jvm-<sha>` ou `native-<sha>`;
o digest fica no resumo da execução. Falhas de testes/build bloqueiam a publicação.
O build JVM evita o custo da compilação nativa durante o desenvolvimento.

O pipeline entrega imagens Linux/AMD64. **A implantação em servidor/cluster ainda
não está configurada**: o destino de execução deve ser definido pelo serviço.
Veja [configuração, tags, requisitos e reprodução local](docs/ci-cd.md).

## Referências

- [Quarkus: métricas Micrometer](https://quarkus.io/guides/telemetry-micrometer/)
- [Quarkus: gRPC e métricas](https://quarkus.io/guides/grpc-service-implementation/)
- [MapStruct e Lombok](https://mapstruct.org/faq/#Can-I-use-MapStruct-together-with-Project-Lombok)
- [Origem da marca INPE](https://www.gov.br/inpe/pt-br/eventos/worcap-2026/repositorio-de-imagens/Logo_INPE.png/view)

## Distribuição interna

Este template é destinado a equipes autorizadas e não possui licença aberta.
Consulte [LICENSE](LICENSE). Dependências e materiais de terceiros mantêm suas
próprias condições de uso; a origem da marca está em [docs/assets](docs/assets/README.md).

No GitHub, use **Use this template** para criar o repositório do serviço a partir
de `main`. Crie a branch `development` a partir da `main` do novo repositório e
execute o setup. Configure acesso, permissões do GHCR e regras de branches no
novo repositório conforme a política da equipe.

## Manutenção com agentes de IA

As instruções do repositório estão em [AGENTS.md](AGENTS.md). Mudanças de contrato,
eventos, configuração, métricas ou operação devem atualizar a documentação na
mesma entrega. O [modelo de pull request](.github/pull_request_template.md) inclui
a conferência de documentação e compatibilidade.
