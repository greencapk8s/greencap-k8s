---
title: UserManagementView — Jobs e CronJobs viram SubGroupNode na TreeView
status: done
---

## O que

Jobs e CronJobs hoje são `PermissionNode` simples (folhas) na TreeView de permissões. Com as novas permissions de escrita, ambos precisam virar `SubGroupNode` — exatamente como Deployments.

## Critérios de aceite

- `buildWorkloadsGroup()` substitui os `PermissionNode` de Jobs e CronJobs por `SubGroupNode`:
  - `SubGroupNode` para Jobs: label `"Jobs"`, view permission `WORKLOADS_JOBS_VIEW`, ações: `{"Delete" → WORKLOADS_JOBS_DELETE}`
  - `SubGroupNode` para CronJobs: label `"CronJobs"`, view permission `WORKLOADS_CRONJOBS_VIEW`, ações: `{"Run Now" → WORKLOADS_CRONJOBS_RUN_NOW, "Suspend" → WORKLOADS_CRONJOBS_SUSPEND, "Delete" → WORKLOADS_CRONJOBS_DELETE}`
- Desmarcar o checkbox "Jobs" ou "CronJobs" (View) desmarca automaticamente suas ações filhas — comportamento herdado do `SubGroupNode` existente
- `allLeaves` do `GroupNode` Workloads inclui todas as novas permissions
- Estrutura visual resultante:
  ```
  Workloads
  ├── Deployments: View | Scale | Restart | Rollback
  ├── Jobs: View | Delete
  ├── CronJobs: View | Run Now | Suspend | Delete
  ├── ReplicaSets: View
  └── Pods: View
  ```
