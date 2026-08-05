---
title: Permission enum — Jobs/CronJobs write operations + Flyway migration
status: done
---

## O que

Adicionar as quatro novas permissions de escrita para Jobs e CronJobs e criar a migration Flyway que as concede aos perfis corretos.

## Critérios de aceite

- `Permission` enum tem os novos valores na seção Workloads:
  - `WORKLOADS_JOBS_DELETE`
  - `WORKLOADS_CRONJOBS_RUN_NOW`
  - `WORKLOADS_CRONJOBS_SUSPEND`
  - `WORKLOADS_CRONJOBS_DELETE`
- `operatorPermissions()` inclui `WORKLOADS_CRONJOBS_RUN_NOW` e `WORKLOADS_CRONJOBS_SUSPEND`
- `operatorPermissions()` **não** inclui `WORKLOADS_JOBS_DELETE` nem `WORKLOADS_CRONJOBS_DELETE`
- `allPermissions()` cobre tudo automaticamente via `Set.of(values())`
- Migration `V14__add_jobs_cronjobs_write_permissions.sql`:
  - Concede `WORKLOADS_CRONJOBS_RUN_NOW` e `WORKLOADS_CRONJOBS_SUSPEND` a todos os usuários que já possuem `WORKLOADS_CRONJOBS_VIEW`
  - Concede `WORKLOADS_JOBS_DELETE` e `WORKLOADS_CRONJOBS_DELETE` apenas a usuários que já possuem `SETTINGS_USERS_WRITE` (proxy de Admin)
