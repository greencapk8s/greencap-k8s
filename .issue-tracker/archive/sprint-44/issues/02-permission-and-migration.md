---
title: "Permission NETWORKING_INGRESS_VIEW + migration V15"
status: done
sprint: 44
---

## O que
- `NETWORKING_INGRESS_VIEW` no enum `Permission`
- Incluir em `allPermissions()`, `operatorPermissions()`, `viewerPermissions()`
- `V15__add_ingress_permission.sql`: concede a quem já tem `NETWORKING_SERVICES_VIEW`

## Critérios
- Migration idempotente (INSERT sem duplicar)
