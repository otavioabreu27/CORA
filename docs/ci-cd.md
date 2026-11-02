# CI/CD do CORA

O workflow [ci-cd.yml](../.github/workflows/ci-cd.yml) executa no GitHub Actions.
A entrega termina na publicação da imagem no GitHub Container Registry (GHCR).
Não há implantação automática em servidor, Kubernetes ou outro ambiente.

## Branches e verificações

| Evento | Testes | Build/publicação |
| --- | --- | --- |
| Push em branches diferentes de `development` e `main` | `./gradlew test` | Nenhum |
| Pull request, inclusive de forks | `./gradlew test` no merge de teste | Nenhum |
| Push em `development` | `./gradlew test` | Fast JAR Java 25 + `Dockerfile.jvm` |
| Push em `main` | `./gradlew test` e `./gradlew testNative` | GraalVM/Mandrel JDK 25 + `Dockerfile.native-micro` |
| Tag Git | Nenhum | Nenhum |
| Execução manual (`workflow_dispatch`) | Mesmas verificações da branch selecionada | JVM em `development`, nativo em `main`; outras branches somente testes |

Para iniciar ou repetir uma entrega sem criar um commit, abra **Actions → CI / CD
→ Run workflow** e selecione a branch. O comando equivalente é
`gh workflow run ci-cd.yml --ref development` (ou `--ref main`).

`development` é o nome exato da branch de desenvolvimento. Uma branch chamada
`develop` recebe apenas testes. Uma branch com PR aberto pode ter uma execução
pelo push e outra pelo PR: a segunda verifica a combinação com a branch de destino.

O job `Tests` é pré-requisito da publicação nas duas branches. Em `main`, os testes
do executável também precisam passar antes do login no registry. Eles cobrem
REST, liveness/readiness e métricas HTTP/JVM. Esses testes acessam o processo
empacotado externamente, sem injeção CDI no teste e sem exigir banco de dados. Os relatórios
JUnit/HTML ficam nos artefatos da execução por 14 dias, inclusive em caso de falha.
Execuções antigas de branches de trabalho/PRs podem ser canceladas; publicações
da mesma branch são serializadas e não são interrompidas por um push mais recente.

## Configuração no GitHub

1. Faça push do repositório para ativar o workflow.
2. Habilite GitHub Actions e permita as actions utilizadas, caso a organização
   tenha uma lista de permissões. As referências estão fixadas por commit SHA.
3. Permita publicação de packages pelo `GITHUB_TOKEN`. O job de publicação pede
   `packages: write`; os outros jobs têm somente `contents: read`. Não é preciso
   cadastrar PAT ou senha do GHCR. Um package preexistente precisa conceder acesso
   de escrita a este repositório; a visibilidade do package é configurada no GitHub.
4. Proteja `development` e `main` com revisão de PR e o check `Tests` obrigatório,
   conforme a política da equipe. O workflow não cria regras de proteção.
5. Para implantar, configure o destino, credenciais de pull e variáveis de runtime
   descritas no README. Use o digest publicado como referência da versão.

O nome da imagem vem de `github.repository` convertido para minúsculas. Criar um
serviço a partir do template não exige editar o workflow nem executar o setup
para ajustar esse nome: ele acompanha o proprietário/nome do repositório GitHub.

## Imagens e versões

| Branch | Tag móvel | Tag por commit | Execução |
| --- | --- | --- | --- |
| `development` | `ghcr.io/<owner>/<repository>:development` | `jvm-<sha-completo>` | JVM com `java -jar /deployments/quarkus-run.jar` via launcher UBI |
| `main` | `ghcr.io/<owner>/<repository>:main` | `native-<sha-completo>` | Executável nativo `/work/application`, sem JVM |

As tags por commit identificam a origem, mas uma reexecução pode republicá-las;
**o digest `sha256:…` é a referência imutável** para implantação e rollback.
O resumo do job contém a referência completa `ghcr.io/<owner>/<repository>@sha256:…`.
Não se publica a tag `latest`. A versão interna continua vindo de `serviceVersion`
em `gradle.properties`; a equipe deve atualizá-la conforme sua política de versões.

## Ambiente de build

- Runner Linux `ubuntu-24.04`, arquitetura AMD64, Java Temurin 25 e Gradle Wrapper.
- Docker disponível para compilação nativa em contêiner; os testes JVM não usam
  banco de dados.
- O build nativo usa Mandrel (distribuição de GraalVM voltada ao Quarkus), JDK 25,
  UBI 9, fixado por digest no workflow. O runtime micro também usa UBI 9.
- O compilador nativo usa heap máximo de 4 GiB, além da memória de Gradle, Docker
  e sistema. Reserve pelo menos 8 GiB para o runner; aumente recursos/limite se o
  serviço crescer. A arquitetura alvo usa `-march=compatibility`.
- `testNative` inicia o artefato com perfil `test`, sem banco de dados. A
  imagem publicada inicia no perfil `prod` e depende apenas do L2
  `orbital-dynamics` em `ORBITAL_DYNAMICS_HOST`/`ORBITAL_DYNAMICS_PORT`.
- Cache Gradle e camadas Docker agilizam builds. PRs/branches de trabalho só leem
  o cache Gradle; publicação exige push em `development` ou `main`.

Fast JAR distribui **toda** a pasta `build/quarkus-app`, não somente o launcher JAR.
O `.dockerignore` inclui explicitamente essas subpastas no contexto Docker.
O build nativo demanda mais tempo e memória; sua vantagem depende da carga e deve
ser medida no serviço. Métricas de JVM não têm necessariamente equivalentes no
executável nativo; valide os painéis de produção com as séries realmente expostas.

## Reprodução local sem publicação

JVM:

```bash
./gradlew --no-daemon test quarkusBuild -Dquarkus.native.enabled=false -Dquarkus.package.jar.type=fast-jar
docker build -f src/main/docker/Dockerfile.jvm -t service:jvm .
```

Nativo (Linux/AMD64, Docker e memória disponível):

```bash
export QUARKUS_NATIVE_BUILDER_IMAGE='quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-25@sha256:50ce63f80dabf588d0e3c43857ae9d1a981d78a3e9ff42579965be23d47254c7'
./gradlew --no-daemon testNative \
  -Dquarkus.native.enabled=true \
  -Dquarkus.package.jar.enabled=false \
  -Dquarkus.native.container-build=true \
  -Dquarkus.native.native-image-xmx=4g \
  -Dquarkus.native.additional-build-args=-march=compatibility \
  -Dquarkus.test.integration-test-profile=test
docker build -f src/main/docker/Dockerfile.native-micro -t service:native .
```

Ao alternar entre builds JVM e nativo no mesmo checkout, execute `./gradlew clean`
antes do próximo build para remover os artefatos do modo anterior. Os jobs do
GitHub usam checkouts novos. Os exemplos constroem imagens locais e não fazem push.

## Referências

- [GitHub: publicação de imagens Docker/GHCR](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images)
- [Quarkus: Gradle e testes nativos](https://quarkus.io/guides/gradle-tooling/#running-native-tests)
- [Quarkus: build nativo e Mandrel](https://quarkus.io/guides/building-native-image/)
