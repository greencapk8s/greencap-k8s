---
id: 01
title: DTOs PersistentVolumeInfo + StorageClassInfo e expansão do StorageService
status: done
sprint: 20
---

## Objetivo

Criar os records DTO `PersistentVolumeInfo` e `StorageClassInfo`, e expandir o `StorageService` com os métodos de listagem correspondentes.

## Decisões de design

- PV e StorageClass são cluster-scoped — sem filtro de namespace
- Status `Terminating` detectado via `metadata.deletionTimestamp` (mesmo padrão do PVC)
- Claim exibido como `namespace/nome` quando Bound, ou `—` quando ausente
- `allowVolumeExpansion` serializado como `"Yes"` / `"No"` no DTO para simplificar a view
- StorageClass não tem status — sem campo de fase

## Arquivos a criar/modificar

- `kubernetes/dto/PersistentVolumeInfo.java` — record DTO
- `kubernetes/dto/StorageClassInfo.java` — record DTO
- `kubernetes/StorageService.java` — adicionar `listPersistentVolumes()` e `listStorageClasses()`

## Campos dos DTOs

### PersistentVolumeInfo

| Campo | Tipo | Fonte |
|-------|------|-------|
| name | String | `metadata.name` |
| status | String | `metadata.deletionTimestamp != null ? "Terminating" : status.phase` |
| capacity | String | `spec.capacity.get("storage")` |
| accessMode | String | `spec.accessModes.get(0)` ou `—` |
| reclaimPolicy | String | `spec.persistentVolumeReclaimPolicy` ou `—` |
| storageClass | String | `spec.storageClassName` ou `—` |
| claim | String | `spec.claimRef` → `namespace/name` ou `—` |
| age | String | `NamespaceService.age(metadata.creationTimestamp)` |

### StorageClassInfo

| Campo | Tipo | Fonte |
|-------|------|-------|
| name | String | `metadata.name` |
| provisioner | String | `provisioner` |
| reclaimPolicy | String | `reclaimPolicy` ou `—` |
| volumeBindingMode | String | `volumeBindingMode` ou `—` |
| allowVolumeExpansion | String | `allowVolumeExpansion ? "Yes" : "No"` |
| age | String | `NamespaceService.age(metadata.creationTimestamp)` |

## Critérios de aceite

- [ ] Ambos os records compilam sem erros
- [ ] `listPersistentVolumes()` usa `client.persistentVolumes().list()` (sem namespace)
- [ ] `listStorageClasses()` usa `client.storage().v1().storageClasses().list()` (sem namespace)
- [ ] Exceções relançadas como `KubernetesOperationException`
