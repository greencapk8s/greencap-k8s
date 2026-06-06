# 01 — DEPLOYMENT_ROLLBACK: Permission enum + Flyway migration

## Status
closed

## Descrição
Adicionar `WORKLOADS_DEPLOYMENTS_ROLLBACK` ao enum `Permission` e criar migration Flyway
que concede a permissão para ADMIN e OPERATOR (não para VIEWER).

## Critérios de aceite
- [ ] `Permission.WORKLOADS_DEPLOYMENTS_ROLLBACK` existe no enum
- [ ] `operatorPermissions()` inclui a nova permissão
- [ ] `viewerPermissions()` NÃO inclui a nova permissão
- [ ] Migration `V12__add_deployment_rollback_permission.sql` concede a permissão aos usuários ADMIN e OPERATOR existentes
