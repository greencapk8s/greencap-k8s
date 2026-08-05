---
id: "01"
title: "DevOps: pipeline GitHub Actions para validar `docker compose up` (Quick Start)"
status: done
labels: [feat, devops]
sprint: 64
depends_on: []
---

## Contexto

Projeto ainda não tem nenhum workflow (`.github/` não existe). O `docker-compose.yml` (sprint 55) já sobe `greencap` (build via `docker/Dockerfile`) + `db` (`postgres:16-alpine`), com healthcheck em `/actuator/health` para `greencap` e `pg_isready` para `db`, e foi validado manualmente nessa sprint. O README documenta o fluxo de Quick Start: clone → `cp .env.example .env` → `docker compose up -d --build` → acessar `http://localhost:8080`.

Decisões via `/grill-with-docs`:

- **Escopo**: workflow dedicado, focado apenas em validar esse Quick Start via docker-compose — sem job de build/test Gradle (fora de escopo desta sprint).
- **Triggers**: `pull_request` e `push` para `main`, com `paths` filtrando apenas o que afeta o build/runtime: `docker-compose.yml`, `docker/**`, `.env.example`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/**`, `gradlew`, `src/**`, `.github/workflows/*.yml`.
- **`.env`**: `cp .env.example .env` — replica exatamente o passo do Quick Start, sem GitHub Secrets (defaults de `.env.example` já são funcionais).
- **Build**: `docker compose up -d --build --wait --wait-timeout 120` — sem cache de camadas/Gradle (cold build a cada execução; otimização de cache fica para sprint futura se necessário).
- **Critério de sucesso**: `--wait` já cobre os healthchecks internos (`pg_isready` + `curl /actuator/health` dentro do container `greencap`). Adicionalmente, um `curl --fail -L http://localhost:8080/` do próprio runner valida que a porta `8080:8080` está publicada e que o frontend Vaadin de produção é servido — é a mesma rede do host, então não precisa de polling extra após o `--wait` retornar.
- **Diagnóstico em falha**: em caso de falha, dump de `docker compose logs` para facilitar debug.
- **Cleanup**: `docker compose down -v` ao final, sempre (`if: always()`).
- Nenhuma atualização de `CONTEXT.md`/ADR — decisões são de infraestrutura de CI, não de domínio, e nenhuma é "difícil de reverter" o suficiente para justificar ADR.

## Entrega

### `.github/workflows/docker-compose-validate.yml` (novo)

```yaml
name: Docker Compose Validate

on:
  pull_request:
    branches: [main]
    paths: &watched-paths
      - "docker-compose.yml"
      - "docker/**"
      - ".env.example"
      - "build.gradle.kts"
      - "settings.gradle.kts"
      - "gradle/**"
      - "gradlew"
      - "src/**"
      - ".github/workflows/*.yml"
  push:
    branches: [main]
    paths: *watched-paths

jobs:
  validate:
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - uses: actions/checkout@v4

      - name: Create .env from .env.example
        run: cp .env.example .env

      - name: Build and start services
        run: docker compose up -d --build --wait --wait-timeout 120

      - name: Check application is reachable
        run: curl --fail -L http://localhost:8080/

      - name: Dump logs on failure
        if: failure()
        run: docker compose logs

      - name: Tear down
        if: always()
        run: docker compose down -v
```

(estrutura exata dos steps/nomes pode ser ajustada na implementação, mantendo os comandos e a sequência acima)

## Critérios de aceite manual

- Abrir um PR que altere um dos paths monitorados (ex.: `docker-compose.yml` ou `src/**`) e confirmar que o workflow "Docker Compose Validate" dispara e passa.
- Abrir/atualizar um PR que altere apenas um path fora da lista (ex.: `docs/sprints.md`) e confirmar que o workflow **não** dispara.
- Quebrar propositalmente o healthcheck (ex.: branch de teste com `ENCRYPTION_KEY` ausente do `.env.example` ou endpoint `/actuator/health` retornando erro) e confirmar que o job falha, com `docker compose logs` aparecendo no output.
- Confirmar que `docker compose down -v` roda mesmo quando o job falha (visível no log do step "Tear down").
