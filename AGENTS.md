# Orientações para agentes de IA

Estas instruções valem para todo o repositório, inclusive após executar o setup.

## Contexto

Este é o template de serviços **INPE / CCS — nível 1**. A aplicação usa Java 25,
Quarkus, Gradle Wrapper, gRPC, PostgreSQL/Panache, cliente RabbitMQ, Lombok,
MapStruct, Micrometer/Prometheus e health checks.

Antes de alterar o projeto, leia `README.md`, `build.gradle`, `gradle.properties`,
`src/main/resources/application.properties` e os contratos envolvidos em
`src/main/proto/`. Consulte instruções locais adicionais, quando existirem.
Preserve alterações do usuário que já estiverem no diretório de trabalho.

## Documentação faz parte da implementação

**Toda mudança de comportamento, contrato, configuração ou operação deve atualizar
sua documentação na mesma entrega. Não deixe essa atualização para depois.**

| Mudança | Documentação que deve acompanhar |
| --- | --- |
| Responsabilidade ou identidade do serviço | Tabela de identificação no `README.md` |
| RPC, mensagem protobuf, filtro, paginação ou status de erro | `.proto`, tabela de interfaces e exemplos de consumo no `README.md` |
| Publicação/consumo de eventos | Tabelas de eventos e entrega: tipo/versão, gatilho, payload, exchange, routing key, filas, consumidores, confirmação, retentativas, DLQ e idempotência |
| Variável de ambiente, porta, credencial referenciada ou perfil | `application.properties`, tabela de configuração e comandos de execução |
| Métrica, label, health check ou dependência monitorada | Seção de instrumentação e, quando aplicável, `docs/prometheus.yml` |
| Persistência ou migração | Procedimento de preparação/atualização do banco, retenção e limitações |
| Setup, pacote, nome do projeto ou estrutura | Instruções de setup, exemplos, links e testes de personalização |
| Dependência, versão mínima ou comando de build | Requisitos e instruções de desenvolvimento/implantação |
| Pipeline, branches, registry, tags ou publicação | Seção CI/CD no `README.md` e `docs/ci-cd.md`; valide a sintaxe dos workflows |

Documente o comportamento efetivamente implementado. Diferencie exemplos,
campos a preencher, decisões pendentes e funcionalidades prontas. Não apresente
intenção como garantia de entrega, segurança, compatibilidade ou desempenho.
Atualize links relativos quando mover ou renomear arquivos. Preserve a identidade
visual INPE/CCS, a proporção da marca e a origem registrada em `docs/assets/README.md`.
O nível permanece 1, salvo solicitação explícita de mudança.

Uma mudança interna sem efeito documentável não exige alterar texto artificialmente;
explique brevemente no resumo final por que a documentação continua válida.

## Contratos e instrumentação

- Use o plugin Quarkus existente para gerar protobuf/gRPC. Não edite classes
  geradas em `build/` nem adicione outro gerador sem necessidade comprovada.
- Preserve os números dos campos protobuf. Ao remover campos, reserve os números
  e nomes; documente mudanças incompatíveis e seu impacto nos consumidores.
- Use Lombok e MapStruct com os processadores já configurados. Os mappers são
  beans CDI com injeção por construtor; mantenha `lombok-mapstruct-binding`.
- Mantenha a exportação Prometheus e os health checks operacionais. Não desative
  uma dependência realmente usada apenas para fazer a readiness retornar UP.
- O cliente RabbitMQ começa desabilitado porque o template não implementa eventos.
  Ao implementar mensageria, configure conexão, ciclo de vida, health e contrato.
- Use labels métricas de cardinalidade limitada. Nunca inclua IDs de requisição,
  usuários, nomes arbitrários, segredos ou payloads nas labels.
- Não copie regras de negócio de outro serviço para este template sem solicitação.

## Setup e validação

`setup.sh` e `setup.bat` compartilham `tools/Setup.java`. Mantenha os dois launchers
compatíveis e faça a lógica de personalização nesse arquivo Java. Valide nomes,
pacotes e destinos antes de alterar arquivos. Preserve pasta, histórico e remote Git.

Para mudanças em código, dependências ou configuração, execute os testes relevantes
e empacote quando necessário:

```bash
./gradlew test quarkusBuild
```

No Windows, use `gradlew.bat`. Docker é necessário para PostgreSQL em dev/test;
esses perfis usam bancos descartáveis. Após renomear pacotes/protobuf, execute
`clean` para remover código gerado com a identidade anterior.

Para mudanças no setup, teste `--dry-run`, entradas inválidas, colisões de destino,
reexecução e personalização em cópia temporária. Compile a cópia personalizada se
mudar a lógica de renomeação. Não personalize o próprio template só para testar.

Para alterações apenas documentais, confira links, exemplos, tabelas e renderização
quando o layout mudar; não execute a suíte inteira sem necessidade.

Ao concluir, informe o que mudou, a documentação atualizada, as verificações
realmente executadas e limitações pendentes. Nunca declare testes não executados
como aprovados. Não faça commit, push ou publicação sem solicitação do usuário.
