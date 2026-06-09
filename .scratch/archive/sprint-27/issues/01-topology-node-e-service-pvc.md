---
id: "27-01"
title: "TopologyNode + TopologyService — suporte a PVC"
status: done
labels: [feat, topology, backend]
sprint: 27
---

## Objetivo

Adicionar PersistentVolumeClaim como tipo de nó no grafo de Topology, com arestas conectando PodGroups (e pods órfãos) aos PVCs que montam.

## Escopo

### `TopologyNode` record
- Adicionar campos `capacity` (String) e `accessMode` (String)
- Campo `serviceType` já existente reutilizado para `storageClass`

### `TopologyService.buildGraph()`
- Buscar PVCs do namespace via `client.persistentVolumeClaims().inNamespace(namespace).list()`
- Para cada PVC, criar nó com:
  - `id`: `nodeId("persistentvolumeclaim", pvc.name)`
  - `type`: `"PersistentVolumeClaim"`
  - `status`: derivado de `pvc.status.phase` (Bound/Pending/Lost/Terminating)
  - `serviceType`: storageClass
  - `capacity`: valor de `pvc.status.capacity["storage"]` (ex: `"2Gi"`)
  - `accessMode`: primeiro item de `pvc.status.accessModes` (ex: `"ReadWriteOnce"`)
  - `manifestUrl`: `yaml/persistentvolumeclaim/{namespace}/{name}`
- Detectar arestas PodGroup→PVC via `pod.spec.volumes[].persistentVolumeClaim.claimName`
  - Basta checar qualquer pod do grupo (todos compartilham o mesmo template)
- Detectar arestas Orphan Pod→PVC pelo mesmo mecanismo
- PVCs sem nenhum pod conectado: exibidos como nós isolados (sem arestas)

## Critério de aceite

- [ ] `TopologyNode` compilando com os dois novos campos
- [ ] PVCs aparecem como nós no grafo
- [ ] Arestas PodGroup→PVC criadas corretamente
- [ ] PVCs isolados aparecem sem arestas
