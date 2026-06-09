---
id: 02
title: PersistentVolumeClaimsView + menu Storage
status: done
sprint: 19
---

## Objetivo

Criar a `PersistentVolumeClaimsView` e integrá-la ao menu lateral em uma nova seção "Storage", posicionada após Parameters.

## Decisões de design

- Label no sidebar: `"Volume Claims (PVC)"` — mais legível para o usuário iniciante
- Seção nova: `"Storage"` (padrão AWS-inspired, consistente com Networking, Parameters)
- Badges de status: `Bound` → success, `Pending` → contrast, `Lost` → error
- Read-only nesta sprint
- Ícone Manifest incluso (padrão das demais views)
- Posição no menu: após Parameters, antes de Topologia

## Arquivos a criar/modificar

- `ui/PersistentVolumeClaimsView.java` — grid em `/storage/pvcs`
- `ui/MainLayout.java` — seção Storage + `buildStorageNavItem()`
- `kubernetes/ManifestService.java` — case `"persistentvolumeclaim"`

## Campos da listagem

| Coluna | Fonte |
|--------|-------|
| Name | `metadata.name` |
| Status | `status.phase` (badge colorido) |
| Capacity | `spec.resources.requests.get("storage")` |
| Access Mode | `spec.accessModes.get(0)` |
| Storage Class | `spec.storageClassName` |
| Namespace | `metadata.namespace` |
| Age | `metadata.creationTimestamp` |
| Manifest | ícone CODE → `/yaml/persistentvolumeclaim/{namespace}/{name}` |

## Critérios de aceite

- [ ] Menu Storage > Volume Claims (PVC) visível no sidebar
- [ ] Grid lista PVCs do namespace ativo com as colunas definidas
- [ ] Badge de status colorido: Bound=verde, Pending=cinza, Lost=vermelho
- [ ] Ícone Manifest abre o YAML completo do PVC
- [ ] Item de menu desabilitado quando cluster inacessível (clusterDependentNavItems)
- [ ] Sem regressão nas demais views
