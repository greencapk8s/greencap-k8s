---
id: "91-02"
title: "Permissões Helm install/upgrade e migration"
status: done
priority: high
sprint: 91
---

`Permission` recebe `PROJECT_HELM_INSTALL` e `PROJECT_HELM_UPGRADE`. Adicionados a `operatorPermissions`; apenas `PROJECT_HELM_VIEW` em `viewerPermissions` (sem install/upgrade para viewer).

`V32__add_helm_install_upgrade_permissions.sql` concede ambas as permissões a usuários com `GLOBAL_CLUSTERS_WRITE`.
