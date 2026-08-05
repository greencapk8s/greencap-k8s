---
id: 02
title: PersistentVolumesView + StorageClassesView + menu Infrastructure
status: done
sprint: 20
---

## Objetivo

Criar as views `PersistentVolumesView` e `StorageClassesView` e integrá-las ao menu lateral em um novo item "Infrastructure" dentro da seção SETTINGS.

## Decisões de design

- Views cluster-scoped — sem dependência de namespace ativo
- `noClusterMessage` exibido quando não há cluster ativo (padrão das demais views)
- Infrastructure agrupado em SETTINGS após Clusters
- StorageClass sem badge de status (não tem fase)
- Manifest incluso em ambas as views (padrão)

## Arquivos a criar/modificar

- `ui/PersistentVolumesView.java` — grid em `/infrastructure/pvs`
- `ui/StorageClassesView.java` — grid em `/infrastructure/storageclasses`
- `ui/MainLayout.java` — item pai "Infrastructure" em `buildConfiguracaoNav()`
- `kubernetes/ManifestService.java` — cases `persistentvolume` e `storageclass`

## Campos das listagens

### PersistentVolumesView

| Coluna | Fonte |
|--------|-------|
| Name | `metadata.name` |
| Status | badge colorido |
| Capacity | `spec.capacity.get("storage")` |
| Access Mode | `spec.accessModes.get(0)` |
| Reclaim Policy | `spec.persistentVolumeReclaimPolicy` |
| Storage Class | `spec.storageClassName` |
| Claim | `spec.claimRef` → `namespace/name` ou `—` |
| Age | `metadata.creationTimestamp` |
| Manifest | ícone CODE → `/yaml/persistentvolume/-/{name}` |

### Badges PV

| Status | Variante |
|--------|----------|
| Available | success |
| Bound | contrast |
| Released | contrast |
| Terminating | contrast |
| Failed | error |

### StorageClassesView

| Coluna | Fonte |
|--------|-------|
| Name | `metadata.name` |
| Provisioner | `provisioner` |
| Reclaim Policy | `reclaimPolicy` |
| Volume Binding Mode | `volumeBindingMode` |
| Allow Expansion | `allowVolumeExpansion` → Yes/No |
| Age | `metadata.creationTimestamp` |
| Manifest | ícone CODE → `/yaml/storageclass/-/{name}` |

## Menu SETTINGS

```
SETTINGS
  Clusters
  Infrastructure  ← novo item pai
    PersistentVolumes
    Storage Classes
  Users (disabled)
  Settings (disabled)
```

## Critérios de aceite

- [ ] Menu Settings > Infrastructure > PersistentVolumes visível
- [ ] Menu Settings > Infrastructure > Storage Classes visível
- [ ] Grid de PVs lista todos os PVs do cluster ativo com colunas definidas
- [ ] Badge de status colorido nos PVs
- [ ] Grid de StorageClasses lista todas as StorageClasses com colunas definidas
- [ ] Ícone Manifest abre YAML correto em ambas as views
- [ ] `noClusterMessage` exibido quando não há cluster ativo
- [ ] Sem regressão nas demais views
