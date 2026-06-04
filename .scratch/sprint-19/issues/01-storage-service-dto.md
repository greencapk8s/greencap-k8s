---
id: 01
title: StorageService + DTO PersistentVolumeClaimInfo
status: done
sprint: 19
---

## Objetivo

Criar o `StorageService` e o record DTO `PersistentVolumeClaimInfo` para listar PersistentVolumeClaims de um cluster Kubernetes via Fabric8.

## Decisões de design

- Termo canônico: `PersistentVolumeClaim` (sem alias — ver CONTEXT.md)
- Segue o mesmo padrão de `ConfigurationService`
- Suporta namespace específico e all-namespaces (`isAllNamespaces`)
- Capacity extraída de `spec.resources.requests.get("storage")`
- Access mode: primeiro elemento de `spec.accessModes`, ou `—` se ausente
- StorageClass: `spec.storageClassName`, ou `—` se nulo/vazio

## Arquivos a criar/modificar

- `kubernetes/dto/PersistentVolumeClaimInfo.java` — record DTO
- `kubernetes/StorageService.java` — método `listPersistentVolumeClaims(Cluster, String)`

## Campos do DTO

| Campo | Tipo | Fonte |
|-------|------|-------|
| name | String | `metadata.name` |
| namespace | String | `metadata.namespace` |
| status | String | `status.phase` |
| capacity | String | `spec.resources.requests.get("storage")` |
| accessMode | String | `spec.accessModes.get(0)` ou `—` |
| storageClass | String | `spec.storageClassName` ou `—` |
| age | String | `NamespaceService.age(metadata.creationTimestamp)` |

## Critérios de aceite

- [ ] `PersistentVolumeClaimInfo` record compila sem erros
- [ ] `StorageService.listPersistentVolumeClaims()` retorna lista correta para namespace específico
- [ ] Exceções do Fabric8 são relançadas como `KubernetesOperationException`
