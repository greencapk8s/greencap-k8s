---
id: "34-02"
title: "Dashboard deve atualizar dados ao trocar namespace"
status: done
labels: [bug, ui]
---

## Contexto

Ao trocar o namespace na navbar, as listagens (Deployments, Pods, etc.) recarregam porque implementam `BeforeEnterObserver`. O `DashboardView` carrega dados apenas no construtor — ao navegar para a mesma rota, os dados não são atualizados.

## Causa raiz

`DashboardView` não implementa `BeforeEnterObserver`. O `MainLayout` chama `UI.navigate(currentPath)` ao trocar namespace, mas sem `beforeEnter()` o dashboard não reage.

## Entrega

- `DashboardView` passa a implementar `BeforeEnterObserver`
- `loadContent()` é chamado em `beforeEnter()` em vez do construtor

## Critério de aceite

- [ ] Trocar namespace com o dashboard aberto atualiza os cards de recursos
- [ ] Auto-refresh continua funcionando (chama `refresh()`)
