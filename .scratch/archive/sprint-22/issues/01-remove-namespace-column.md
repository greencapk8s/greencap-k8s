---
id: 01
title: Remover coluna Namespace das views namespace-scoped
status: done
sprint: 22
---

## Objetivo

Remover a coluna Namespace das views que operam no contexto do namespace ativo, pois a informação já está visível no seletor da navbar.

## Views a modificar

- `PodsView`
- `DeploymentsView`
- `ReplicaSetView`
- `ServicesView`
- `ConfigMapsView`
- `SecretsView`
- `HorizontalScalerView`
- `PersistentVolumeClaimsView`
- `MetricsView`
- `EventsView`

## Decisões de design

- Views cluster-scoped (`PersistentVolumesView`, `StorageClassesView`) não são alteradas
- `PersistentVolumesView`: coluna Claim já contém `namespace/nome` — não remover

## Critérios de aceite

- [ ] Nenhuma das 10 views exibe coluna Namespace
- [ ] Compilação sem erros
- [ ] Sem regressão visual nas demais colunas
