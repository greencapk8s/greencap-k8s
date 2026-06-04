# 01 — Aplicação demo greencap-demo

Status: done

## Contexto

Para exercitar a integração com novos tipos de recurso Kubernetes no GreenCap, precisamos de um cluster com dados ricos. O `greencap-demo` é um fixture de ambiente — uma app 3-tier simples criada manualmente via script que popula o cluster com Namespace, Deployments, Services, ConfigMap, Secret, PVC e HPA.

## O que fazer

Criar os arquivos em `samples/greencap-demo/`:

### Manifests (`manifests/`)

- `00-namespace.yaml` — namespace `greencap-demo`
- `01-configmap.yaml` — ConfigMap `app-config` com variáveis de configuração fictícias
- `02-secret.yaml` — Secret `app-secrets` com credenciais fictícias (Opaque)
- `03-pvc.yaml` — PersistentVolumeClaim `redis-data` (1Gi, ReadWriteOnce)
- `04-redis-deployment.yaml` — Deployment `redis` (imagem `redis:7-alpine`, 1 réplica, monta o PVC)
- `05-redis-service.yaml` — Service `redis` (ClusterIP, porta 6379)
- `06-backend-deployment.yaml` — Deployment `backend` (imagem `kennethreitz/httpbin`, 2 réplicas, env do ConfigMap e Secret)
- `07-backend-service.yaml` — Service `backend` (ClusterIP, porta 80)
- `08-frontend-deployment.yaml` — Deployment `frontend` (imagem `nginx:alpine`, 2 réplicas)
- `09-frontend-service.yaml` — Service `frontend` (NodePort, porta 80)
- `10-hpa.yaml` — HPA `backend-hpa` para o Deployment `backend` (min 1, max 5, target CPU 70%)

### Scripts

- `create.sh` — habilita `minikube addons enable metrics-server` antes de aplicar os manifests, depois aplica todos em ordem via `kubectl apply -f`. Exibe mensagem de conclusão com namespace criado.
- `delete.sh` — remove o namespace `greencap-demo` inteiro via `kubectl delete namespace greencap-demo`.

## Critério de aceite

- `bash samples/greencap-demo/create.sh` cria todos os recursos no cluster sem erros
- `kubectl get all -n greencap-demo` exibe os recursos criados
- `bash samples/greencap-demo/delete.sh` remove o namespace e todos os recursos filhos
