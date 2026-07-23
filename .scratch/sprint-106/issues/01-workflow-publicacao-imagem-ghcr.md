# 01 — Workflow de publicação da imagem da plataforma no GHCR

Status: done

Hoje nenhum pipeline publica a imagem da plataforma — ela só existe quando alguém roda `docker build` localmente (no `setup.sh` ou no `docker compose`). Esta entrega cria o lado **produtor**: um workflow do GitHub Actions que constrói a imagem da plataforma e a publica em `ghcr.io/greencapk8s/platform` a cada release, deixando-a pronta para o `setup.sh` puxar (issue 02).

Gatilho do workflow: push de git tag `v*` — a mesma tag que já é a fonte de verdade da versão em `main` (ver `build.gradle.kts` → `lastGitTag`), garantindo uma imagem por release e o versionamento da imagem 1:1 com o do binário. Além disso, `workflow_dispatch` para permitir republicar manualmente uma tag caso a release falhe no meio, sem custo e como rede de segurança.

A imagem é construída a partir de `docker/Dockerfile` para a plataforma **`linux/amd64`** apenas — publicar arm64 exigiria emulação QEMU no runner, encarecendo cada release para beneficiar a minoria macOS/Apple Silicon (decisão registrada na ADR 0019; o `setup.sh` cobre arm64 com build local). O login no GHCR usa o `GITHUB_TOKEN` do próprio Actions com permissão `packages: write` — sem cadastrar secret externo.

Tags aplicadas à imagem publicada, derivadas do nome da git tag (`v0.7.8` → `0.7.8`): a versão exata `X.Y.Z` sempre, e `latest` **apenas** quando a tag é um release estável (`vX.Y.Z`), nunca um pré-release (`vX.Y.Z-rc.N`) — assim um RC tagueado não sequestra o `latest` que os usuários finais puxam. A tag `X.Y.Z` é o ponto imutável e reproduzível; `latest` é o ponteiro móvel que o `setup.sh` consome por padrão.

O pacote no GHCR precisa ficar com **visibilidade pública** (sem `imagePullSecret` no consumo) e vinculado ao repositório. A primeira publicação cria o pacote como privado por padrão — ajustar a visibilidade para público (via configuração do pacote no GitHub ou `gh`) faz parte desta entrega; documentar no corpo da issue/PR que esse passo é manual e único, caso o Actions não consiga defini-lo por si.

Cobertura de teste: não há código Java envolvido — a validação é a própria execução do workflow. Como a publicação só dispara em tag `v*`, o caminho pode ser exercitado antes do primeiro release real via `workflow_dispatch`, confirmando que a imagem aparece pública em `ghcr.io/greencapk8s/platform` com as tags esperadas. O gate de testes Karibu/integração do fluxo de sprint não se aplica a esta entrega.

Fora de escopo: multi-arch (só amd64 nesta sprint) e a troca do `docker-compose.yml` para a imagem publicada (registrada no backlog como follow-up).
