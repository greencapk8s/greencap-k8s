---
title: "Docs: README com Quick Start via Docker Compose"
status: done
sprint: 55
---

## O que
- Reestruturar `README.md`:
  - Nova seção "Quick Start" no topo, como caminho principal/recomendado: pré-requisitos (apenas Docker + Docker Compose), `cp .env.example .env`, `docker compose up -d --build`, aguardar `healthy`, acessar `http://localhost:8080`, login `admin`/`admin`
  - Mover o fluxo atual de Gradle (`./gradlew bootRun` + `docker-compose.dev.yml`) para uma seção "Para desenvolvedores", após o Quick Start
  - Manter as seções "Variáveis de ambiente", "Usando um cluster Kubernetes" e "Ambiente de demonstração", ajustando referências cruzadas conforme necessário

## Por quê
- Foco do sprint: usuário final consegue rodar a aplicação só com Docker/Docker Compose, sem instalar IDE, JDK, Node ou Gradle
- Hoje a seção "Rodando em produção" é a última do README e está quebrada (Dockerfile inexistente)

## Critérios
- Seguindo só os passos do Quick Start em um checkout limpo (com Docker instalado), a aplicação sobe e fica acessível em `http://localhost:8080` com login `admin`/`admin`
- Seção "Para desenvolvedores" cobre o fluxo Gradle + `docker-compose.dev.yml` sem duplicar instruções do Quick Start
