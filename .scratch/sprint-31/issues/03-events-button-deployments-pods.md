---
title: "DeploymentsView e PodsView: botão Events na coluna de ações"
status: done
labels: [feat, ui]
---

## Descrição

Adicionar botão de ícone `VaadinIcon.RECORDS` com tooltip "Events" na coluna de ações de `DeploymentsView` e `PodsView`. Ao clicar, abre o `EventsDialog` com os events do recurso selecionado.

## DeploymentsView

- Botão inserido após "View Manifest" na `HorizontalLayout` de ações
- kind = `"Deployment"`, name = `deployment.name()`, namespace = `deployment.namespace()`

## PodsView

- Botão inserido após "View Manifest"
- kind = `"Pod"`, name = `pod.name()`, namespace = `pod.namespace()`

## Critérios de aceite

- Botão visível em todas as linhas do grid
- Dialog abre com events filtrados pelo recurso da linha clicada
- Erro de API exibe notificação `LUMO_ERROR` em `BOTTOM_END`
