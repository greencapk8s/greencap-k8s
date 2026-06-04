---
id: "27-03"
title: "TopologyNodeDrawer — painel lateral para PVC"
status: done
labels: [feat, topology, ui]
sprint: 27
---

## Objetivo

Exibir informações relevantes do PVC no drawer lateral ao clicar no nó.

## Escopo

### `TopologyNodeDrawer`
- Adicionar case `PersistentVolumeClaim` em `buildBody()`
- Exibir 4 linhas via `buildInfoRow()`:
  - `Status` → valor do campo `status`
  - `Capacity` → campo `capacity`
  - `Storage Class` → campo `serviceType` (reutilizado para storageClass)
  - `Access Mode` → campo `accessMode`
- Botão de ação: `"Ver YAML"` navegando para `manifestUrl`
- `open()` deve ler `capacity` e `accessMode` do `JsonObject detail`
- Badge de status: `Bound` → success, `Pending` → contrast, `Lost` → error, `Terminating` → contrast

## Critério de aceite

- [ ] Drawer abre ao clicar em nó PVC
- [ ] 4 campos exibidos corretamente
- [ ] Badge de status com variante correta
- [ ] Botão "Ver YAML" navega para o manifest do PVC
