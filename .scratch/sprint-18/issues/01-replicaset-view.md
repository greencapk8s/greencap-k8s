---
id: 01
title: Listagem de ReplicaSets
status: done
sprint: 18
---

## Objetivo

Exibir os ReplicaSets do namespace ativo em uma nova view read-only, acessível via Workloads > ReplicaSets no menu lateral. A coluna Owner identifica o Deployment pai ou indica órfão.

## Decisões de design

- Termo canônico: `ReplicaSet` (ver CONTEXT.md)
- Listagem independente (não contextual a um Deployment específico)
- Owner via `ownerReferences` filtrado por kind "Deployment"
- Read-only nesta sprint
- Ícone Manifest incluso (padrão das demais views)
- Posição no menu: Workloads > Deployments > ReplicaSets > Pods

## Arquivos a criar/modificar

- `kubernetes/dto/ReplicaSetInfo.java` — record DTO
- `kubernetes/WorkloadService.java` — adicionar `listReplicaSets()`
- `ui/ReplicaSetView.java` — grid em `/workloads/replicasets`
- `ui/MainLayout.java` — submenu ReplicaSets entre Deployments e Pods
- `kubernetes/ManifestService.java` — case `"replicaset"`

## Campos da listagem

| Coluna | Fonte |
|--------|-------|
| Name | `metadata.name` |
| Namespace | `metadata.namespace` |
| Owner | `ownerReferences[kind=Deployment].name` ou `—` |
| Desired | `spec.replicas` |
| Ready | `status.readyReplicas` / `spec.replicas` (badge colorido) |
| Age | `metadata.creationTimestamp` |
| Manifest | ícone CODE → `/yaml/replicaset/{namespace}/{name}` |

## Critérios de aceite

- [x] Menu Workloads > ReplicaSets visível (entre Deployments e Pods)
- [x] Grid lista ReplicaSets do namespace ativo com as colunas definidas
- [x] Coluna Owner mostra nome do Deployment pai ou "—" para órfãos
- [x] Badge Ready/Desired colorido (verde/amarelo/vermelho)
- [x] Ícone Manifest abre o YAML completo
- [x] Sem regressão nas demais views
