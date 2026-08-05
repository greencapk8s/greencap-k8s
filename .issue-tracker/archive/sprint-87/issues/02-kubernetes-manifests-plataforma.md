---
id: "87-02"
title: "Manifests Kubernetes: Postgres e GreenCap no namespace greencap-platform"
status: done
priority: high
sprint: 87
---

O `setup.sh` aplica os manifests em `setup/manifests/` na ordem numérica. Cada manifest é responsável por um recurso específico da plataforma.

**Manifests necessários:**

`00-namespace.yaml` — namespace `greencap-platform`.

`01-secrets.yaml` — Secret `greencap-secrets` com `DB_PASSWORD` e `GREENCAP_ENCRYPTION_KEY`; os valores são placeholders substituídos pelo `setup.sh` via `envsubst` ou `kubectl create secret` imperativo antes do apply dos demais manifests.

`02-postgres-pvc.yaml` — PersistentVolumeClaim `postgres-data`, 2 Gi, ReadWriteOnce, StorageClass `standard`.

`03-postgres-deployment.yaml` — Deployment `postgres` com `postgres:16-alpine`; variáveis `POSTGRES_DB=greencap`, `POSTGRES_USER=greencap`, `POSTGRES_PASSWORD` lidas do Secret; volume montado em `/var/lib/postgresql/data`; readinessProbe via `pg_isready`.

`04-postgres-service.yaml` — Service ClusterIP `greencap-db` na porta 5432; sem exposição externa.

`05-greencap-deployment.yaml` — Deployment `greencap` com a imagem `localhost:5000/greencap-platform/platform:latest` (imagePullPolicy `Always`); variáveis de ambiente: `SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_URL=jdbc:postgresql://greencap-db:5432/greencap`, `SPRING_DATASOURCE_USERNAME=greencap`, `SPRING_DATASOURCE_PASSWORD` e `GREENCAP_ENCRYPTION_KEY` lidas do Secret; readinessProbe via `/actuator/health`; 1 réplica.

`06-greencap-service.yaml` — Service ClusterIP `greencap` na porta 8080.

`07-greencap-ingress.yaml` — Ingress com host `greencap.local` apontando para o Service `greencap` na porta 8080; sem TLS; sem IngressClass explícita (usa o default do addon `ingress` do minikube).
