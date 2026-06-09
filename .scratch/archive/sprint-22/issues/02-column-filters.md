---
id: 02
title: Filtros por coluna nas views de listagem
status: done
sprint: 22
---

## Objetivo

Adicionar campos de filtro nos cabeçalhos das colunas mais relevantes de cada view de listagem, operando client-side via `ListDataProvider`.

## Decisões de design

- Mecanismo: `ListDataProvider` com `setFilter()` — filtro client-side, sem chamadas extras à API
- Campos de filtro: `TextField` com `placeholder("Filter...")`, `clearButtonVisible(true)`, tema `small`
- Filtro case-insensitive com `contains`
- `dataProvider.refreshAll()` acionado no value change de cada campo
- Padrão de dados: `List<T> allItems` + `ListDataProvider<T> dataProvider` como campos da view
- `loadXxx()` atualiza `allItems` e chama `dataProvider.refreshAll()` (mantém filtros ativos entre reloads)

## Colunas filtráveis por view

| View | Colunas |
|------|---------|
| PodsView | Name, Status |
| DeploymentsView | Name |
| ReplicaSetView | Name, Owner |
| ServicesView | Name, Type |
| ConfigMapsView | Name |
| SecretsView | Name, Type |
| HorizontalScalerView | Name, Target |
| PersistentVolumeClaimsView | Name, Status |
| PersistentVolumesView | Name, Status, Claim |
| StorageClassesView | Name, Provisioner |
| EventsView | Type, Reason, Involved Object |
| MetricsView | Name |

## Critérios de aceite

- [ ] Cada view exibe linha de filtros abaixo do cabeçalho das colunas definidas
- [ ] Filtros são case-insensitive
- [ ] Botão X limpa o filtro
- [ ] Recarregar dados (botão refresh) mantém os filtros ativos
- [ ] Sem regressão nas demais funcionalidades
