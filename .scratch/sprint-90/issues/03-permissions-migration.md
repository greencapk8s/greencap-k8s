---
id: "90-03"
title: "Permissões Helm e migration Flyway"
status: done
priority: high
sprint: 90
---

`Permission` recebe `PROJECT_HELM_VIEW` e `PROJECT_HELM_UNINSTALL`. Adicionados a `operatorPermissions`; apenas `PROJECT_HELM_VIEW` em `viewerPermissions`.

`V30__add_helm_permissions.sql` concede `PROJECT_HELM_VIEW` a usuários com `GLOBAL_CLUSTERS_VIEW` e `PROJECT_HELM_UNINSTALL` a usuários com `GLOBAL_CLUSTERS_WRITE`.
