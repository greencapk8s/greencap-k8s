---
title: "Docker: configuração de produção (profile prod, healthcheck, .env.example)"
status: done
sprint: 55
---

## O que
- Criar `src/main/resources/application-prod.yaml` com `greencap.encryption.key: ${GREENCAP_ENCRYPTION_KEY}` (sem fallback) — falha no startup se a variável não estiver definida
- `docker-compose.yml`: serviço `greencap` recebe `SPRING_PROFILES_ACTIVE: prod` e `healthcheck` via `/actuator/health` (intervalo curto, com `start_period` para dar tempo ao boot da aplicação)
- Corrigir `.env.example`: descomentar `ENCRYPTION_KEY`, `DB_USER`, `DB_PASSWORD` com valores padrão funcionais para Quick Start, com comentário avisando para trocar em produção real; manter `GREENCAP_ENCRYPTION_KEY` documentado para o fluxo de dev (Gradle)

## Por quê
- Hoje `cp .env.example .env && docker compose up -d` falha porque `ENCRYPTION_KEY` (exigida por `docker-compose.yml`) está comentada no `.env.example`
- `application.yaml` (default) tem fallback `dev-encryption-key-change-me-32x` sempre ativo — em produção isso permite subir com a chave de encriptação default sem aviso
- Healthcheck permite ao usuário final saber quando a aplicação está pronta (`docker compose ps` / `docker compose up` aguardando `healthy`)

## Critérios
- `cp .env.example .env && docker compose up -d --build` sobe `db` e `greencap` sem erro, com `greencap` ficando `healthy` após o boot
- Remover `ENCRYPTION_KEY` do `.env` e subir com profile `prod` ativo faz o container `greencap` falhar no startup com erro claro de propriedade não resolvida
- Profile `dev`/local (Gradle) continua funcionando com o fallback em `application.yaml`, sem exigir `.env`
