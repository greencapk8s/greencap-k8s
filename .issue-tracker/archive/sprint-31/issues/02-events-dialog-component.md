---
title: "EventsDialog: componente Vaadin reutilizável"
status: done
labels: [feat, ui]
---

## Descrição

Criar componente `EventsDialog` que encapsula o Dialog + Grid de events scoped por recurso. Reutilizável por `DeploymentsView` e `PodsView`.

## Colunas do Grid

| Coluna  | Largura | Observação              |
|---------|---------|-------------------------|
| Type    | 110px   | Badge success/error     |
| Reason  | 180px   | Texto simples           |
| Message | flex    | Texto com word-wrap     |
| Count   | 80px    | Número                  |
| Age     | 70px    | Texto                   |

## Critérios de aceite

- Título do dialog: `Events — <resourceName>`
- Grid vazio exibe mensagem "No events found"
- Abre via método estático ou builder para não criar acoplamento com as views
- Padrão visual consistente com `EventsView` (mesmo badge de tipo)
