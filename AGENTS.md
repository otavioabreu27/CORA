# Orientações para agentes de IA — CORA

## Contexto

CORA é o serviço **L3** de assistência orbital do INPE/CCS. Ele é uma fachada
REST/JSON que consome o L2 `orbital-dynamics` por gRPC. A aplicação usa Java 25,
Quarkus, Gradle Wrapper, gRPC client, REST/Jackson, Micrometer/Prometheus e health
checks.

CORA é **stateless**: não usa PostgreSQL, Panache, Hibernate ou Flyway nesta
versão. Não adicione banco sem um requisito concreto de sessão/auditoria.

## Documentação faz parte da implementação

Toda mudança de contrato REST, configuração, porta ou comportamento deve atualizar
`README.md` e os exemplos. Preserve os números de campo do protobuf do L2 copiado
em `src/main/proto/orbital_dynamics.proto`; ele é um contrato vendorizado.

## Limites

- Não reimplemente SGP4, passagens, footprints ou catálogo no CORA. Use o L2.
- Não colete TLE diretamente do CelesTrak; isso é responsabilidade do L1.
- Não reintroduza RabbitMQ sem eventos de domínio definidos.
- Não exponha `plaintext`/sem autenticação fora do ambiente local.
- O LLM é opcional (`CORA_AI_ENABLED=false` por padrão). Não habilite modelos
  locais/pagos sem definir provedor, orçamento e política de dados.
- Ferramentas do agente devem chamar o L2; não reimplemente órbita no CORA.

## Validação

```bash
./gradlew clean test quarkusBuild
```

Para o teste integrado com o stack:

```bash
docker compose up -d --no-build --wait cora
curl -s http://localhost:8090/api/info
curl -s 'http://localhost:8090/api/satellites?name=CBERS&pageSize=2'
```

O L2 precisa estar rodando em `orbital-dynamics:9090` no compose ou
`localhost:9090` no desenvolvimento local.
