---
title: NodeInfo DTO + InfrastructureService — listagem de Nodes
status: done
---

## O que

Criar o DTO `NodeInfo` e o método `listNodes()` no serviço responsável por recursos de infraestrutura cluster-scoped.

## Critérios de aceite

- Record `NodeInfo` em `kubernetes/dto/`:
  - `String name`
  - `String status` — derivado de `status.conditions[type=Ready].status`: `"True"` → `"Ready"`, `"False"` → `"NotReady"`, ausente/`"Unknown"` → `"Unknown"`
  - `String role` — `"Control Plane"` se o Node tem label `node-role.kubernetes.io/control-plane` ou `node-role.kubernetes.io/master`; caso contrário `"Worker"`
  - `String version` — `status.nodeInfo.kubeletVersion`
  - `String os` — `status.nodeInfo.osImage`
  - `String cpu` — `status.allocatable["cpu"]` como string (ex: `"4"`, `"500m"`)
  - `String memory` — `status.allocatable["memory"]` convertido para GiB com 1 casa decimal (ex: `"7.8 GiB"`)
  - `String age` — `NamespaceService.age(metadata.creationTimestamp)`

- Novo método `listNodes(Cluster cluster)` em `InfrastructureService` (ou serviço equivalente que já agrupa PVs e StorageClasses):
  - Busca via `client.nodes().list().getItems()`
  - Envolto em `try-with-resources`
  - Lança `KubernetesOperationException` em falha

## Notas

- Memory: `status.allocatable["memory"]` vem em Ki (kibibytes) — dividir por `1024 * 1024` para obter GiB
- Nodes são cluster-scoped: sem filtro de namespace
