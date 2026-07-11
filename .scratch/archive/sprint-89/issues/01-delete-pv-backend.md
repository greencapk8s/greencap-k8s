---
id: "89-01"
title: "StorageService — deletePersistentVolume + permissão + migration"
status: done
priority: high
sprint: 89
---

`StorageService` recebe o método `deletePersistentVolume(Cluster cluster, String name)` que remove o PV via Fabric8 (`client.persistentVolumes().withName(name).delete()`). Lança `KubernetesOperationException` em falha de API.

`Permission` recebe o novo valor `GLOBAL_INFRASTRUCTURE_PV_DELETE`, adicionado aos presets `operatorPermissions` (mas não em `viewerPermissions`).

`V29__add_pv_delete_permission.sql` concede `GLOBAL_INFRASTRUCTURE_PV_DELETE` a todos os usuários com `GLOBAL_INFRASTRUCTURE_CORDON` (mesmo critério — ação destrutiva de infraestrutura restrita a admins e operators).
