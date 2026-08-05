# 02 — Docker Compose local

Status: done

## Contexto

Não existe ainda um `docker-compose.yml` para desenvolvimento local. O desenvolvedor precisa ter um PostgreSQL rodando para subir o app. O objetivo é que `docker compose up` suba o banco e o app consiga conectar sem configuração manual.

## O que fazer

- Criar `docker-compose.yml` na raiz com serviço PostgreSQL 16
- Configurar `application.properties` (ou `application-local.properties`) para apontar para o banco do compose
- Definir variáveis necessárias: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `GREENCAP_ENCRYPTION_KEY`
- Adicionar `.env.example` com as variáveis necessárias documentadas
- Documentar no README como rodar localmente

## Critério de aceite

- `docker compose up` sobe o PostgreSQL
- `./gradlew bootRun` com as variáveis corretas conecta ao banco e aplica as migrations do Flyway
- App abre no browser em `localhost:8080`
