# GreenCap K8s

Plataforma web para gerenciamento e monitoramento de clusters Kubernetes externos.

## Stack

- Java 21 + Spring Boot 3.3 + Vaadin Flow 24
- PostgreSQL 16 + Flyway
- Fabric8 Kubernetes Client 6.13
- Gradle 8.8 (Kotlin DSL)

## Quick Start (Docker)

Pré-requisitos: apenas **Docker** e **Docker Compose**. Não é necessário instalar Java, Gradle ou Node — todo o build acontece dentro do container.

**1. Clone o repositório e configure as variáveis de ambiente:**

```bash
cp .env.example .env
```

Os valores padrão já funcionam para um teste local. Para um deploy em produção real, edite `.env` e troque `ENCRYPTION_KEY`, `DB_USER` e `DB_PASSWORD` por valores próprios e seguros.

**2. Suba a aplicação:**

```bash
docker compose up -d --build
```

O primeiro build pode levar alguns minutos (compila a aplicação e o frontend Vaadin de produção). Acompanhe o progresso com:

```bash
docker compose logs -f greencap
```

**3. Acesse:**

A aplicação estará disponível em `http://localhost:8080` assim que o serviço `greencap` ficar `healthy` (verifique com `docker compose ps`).

Login padrão: `admin` / `admin`

**Para parar:**

```bash
docker compose down
```

Os dados do PostgreSQL ficam no volume `pgdata` e persistem entre execuções. Para apagar tudo, incluindo o banco: `docker compose down -v`.

## Usando um cluster Kubernetes

Ao registrar um cluster, o kubeconfig precisa ter os certificados embutidos (não por caminho de arquivo). Gere uma versão portável com:

```bash
kubectl config view --flatten --minify
```

## Ambiente de demonstração

O script `samples/greencap-demo/create.sh` provisiona um namespace completo com Deployments, Services, HPA e Ingress no Minikube — incluindo a ativação automática dos addons `metrics-server` e `ingress`.

```bash
cd samples/greencap-demo
./create-demo.sh
```

## Para desenvolvedores

Fluxo alternativo para quem vai alterar o código-fonte. Requer **Java 21+** e usa o Gradle Wrapper (não precisa instalar Gradle).

**1. Suba apenas o banco de dados:**

```bash
docker compose -f docker-compose.dev.yml up -d
```

**2. Rode o app:**

```bash
./gradlew bootRun
```

O app estará disponível em `http://localhost:8080`.

Login padrão: `admin` / `admin`

Em desenvolvimento, os valores padrão de `application.yaml` já funcionam sem precisar de `.env`.
