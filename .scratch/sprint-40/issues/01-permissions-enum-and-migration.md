# 01 — Permissions: enum + Flyway migration

## Status
closed

## Descrição
Adicionar `WORKLOADS_JOBS_VIEW` e `WORKLOADS_CRONJOBS_VIEW` ao enum `Permission` e criar
migration Flyway que concede ambas as permissões a todos os perfis (ADMIN, OPERATOR e VIEWER),
pois são operações read-only.

## Critérios de aceite
- [ ] `Permission.WORKLOADS_JOBS_VIEW` existe no enum e está em `allPermissions()`, `operatorPermissions()` e `viewerPermissions()`
- [ ] `Permission.WORKLOADS_CRONJOBS_VIEW` existe no enum e está em `allPermissions()`, `operatorPermissions()` e `viewerPermissions()`
- [ ] Migration `V13__add_jobs_cronjobs_permissions.sql` concede ambas as permissões a todos os usuários que já possuem `WORKLOADS_PODS_VIEW`
