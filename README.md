# GreenCap K8s

Plataforma web para gerenciamento e monitoramento de clusters Kubernetes externos. Focada em observabilidade — não provisiona clusters, apenas registra credenciais de acesso e exibe workloads em tempo real.

## Stack

- Java 21 + Spring Boot 3.3 + Vaadin Flow 24
- PostgreSQL 16 + Flyway
- Fabric8 Kubernetes Client 6.13
- Gradle 8.8 (Kotlin DSL)

## Pré-requisitos

- Java 21+
- Docker + Docker Compose

## Rodando localmente

**1. Suba o banco de dados:**

```bash
docker compose -f docker-compose.dev.yml up -d
```

**2. Rode o app:**

```bash
./gradlew bootRun
```

O app estará disponível em `http://localhost:8080`.

Login padrão: `admin` / `admin`

## Variáveis de ambiente

Copie `.env.example` para `.env` e ajuste se necessário. Em desenvolvimento, os valores padrão já funcionam sem configuração adicional.

```bash
cp .env.example .env
```

## Usando um cluster Kubernetes

Ao registrar um cluster, o kubeconfig precisa ter os certificados embutidos (não por caminho de arquivo). Gere uma versão portável com:

```bash
kubectl config view --flatten --minify
```

## Ambiente de demonstração

O script `samples/greencap-demo/create.sh` provisiona um namespace completo com Deployments, Services, HPA e Ingress no Minikube — incluindo a ativação automática dos addons `metrics-server` e `ingress`.

```bash
cd samples/greencap-demo
./create.sh
```

## Rodando em produção

```bash
cp .env.example .env
# edite .env com valores seguros
docker compose up -d
```
