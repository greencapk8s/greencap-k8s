---
title: "IngressView + sidebar + ManifestService"
status: done
sprint: 44
---

## O que
- `IngressView` em `/networking/ingresses`
- Sub-item "Ingresses" no `buildRedeNavItem()` do `MainLayout`
- Tipo "ingress" no `ManifestService`
- "Ingresses" na árvore de permissões do `UserManagementView`

## Colunas
Name · IngressClass · Hosts · TLS (badge) · Age · Manifest

## Badge TLS
- `success` = "TLS" (tem bloco TLS configurado)
- `contrast` = "Plain" (sem TLS)

## Filtros
- Por Name
- Por IngressClass

## Critérios
- Implementa `Refreshable`
- Protegida por `NETWORKING_INGRESS_VIEW`
- Pai "Networking" no sidebar continua apontando para `ServicesView`
