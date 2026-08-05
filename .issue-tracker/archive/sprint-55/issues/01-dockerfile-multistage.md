---
title: "Docker: Dockerfile multi-stage para a aplicação Spring Boot/Vaadin"
status: done
sprint: 55
---

## O que
- Criar `docker/Dockerfile` com build multi-stage:
  - **Stage `builder`**: imagem `eclipse-temurin:21-jdk`, copia o projeto, executa `./gradlew bootJar` (gera o JAR de produção, incluindo o build de frontend do Vaadin via plugin — Node/npm são baixados automaticamente pelo plugin, sem necessidade de instalação prévia)
  - **Stage `runtime`**: imagem `eclipse-temurin:21-jre`, copia apenas o JAR final do stage anterior, expõe a porta `8080`, `ENTRYPOINT java -jar app.jar`
- Criar `.dockerignore` na raiz (excluir `build/`, `bin/`, `node_modules/`, `.git/`, `.gradle/`, `.scratch/`) para reduzir o contexto de build

## Por quê
- `docker-compose.yml` já referencia `docker/Dockerfile`, que não existe — hoje `docker compose up` falha
- Usuário final não deve precisar instalar JDK, Gradle ou Node — todo o build acontece dentro do container

## Critérios
- `docker build -f docker/Dockerfile .` completa com sucesso a partir de um checkout limpo
- Imagem final não contém JDK completo nem código-fonte/dependências de build — apenas o JAR e JRE
- `VAADIN_PRODUCTION=true` resulta em frontend minificado embutido no JAR (`vaadinBuildFrontend` roda no stage builder)
