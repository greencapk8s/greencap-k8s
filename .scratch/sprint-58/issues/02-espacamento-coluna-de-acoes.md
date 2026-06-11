---
id: "02"
title: "Fix — espaçamento dos botões de ação nas colunas das listagens"
status: done
labels: [fix, frontend]
sprint: 58
---

## Contexto

8 views ainda têm uma coluna de ações por linha (ícones à direita, última coluna do grid): `NodesView` (Cordon/Uncordon), `DeploymentsView` (Scale/Restart/Rollout Undo), `PodsView` (Logs), `JobsView` (View Pods), `CronJobsView` (Trigger/Suspend/View Jobs), `HorizontalScalerView` (Edit Limits), `ClustersView` (Test connection/Remove) e `UserManagementView` (Edit permissions/Deactivate).

O último ícone dessas colunas fica colado ou estourando a borda direita do grid. Causa raiz: os `HorizontalLayout` que agrupam os botões mantêm o `padding`/`spacing` padrão do Vaadin (`--lumo-space-m`, ~16px de cada lado), que não é considerado em `UiConstants.actionsColumnWidth(buttonCount)` (`buttonCount * 48px`) nem nas larguras fixas `"120px"`/`"140px"` de `ClustersView`/`UserManagementView`. Nas colunas com 2-3 botões o conteúdo fica mais largo que a coluna e o último ícone vaza para fora do grid.

## Entrega

Novo helper em `UiConstants` para montar a coluna de ações de forma consistente:

- `HorizontalLayout` com `setSpacing(false)` e `setPadding(false)`
- Largura da coluna calculada a partir de `actionsColumnWidth(buttonCount)`, somando uma constante nomeada de respiro à direita (ex.: `ACTIONS_COLUMN_RIGHT_PADDING_PX`) para que o último ícone nunca encoste na borda do grid
- Aplicado nas 8 views listadas, substituindo os blocos atuais de `addComponentColumn(...).setHeader("").setWidth(...).setFlexGrow(0)`
- `ClustersView` e `UserManagementView` passam a usar `actionsColumnWidth(2)` (via o novo helper) no lugar das larguras fixas `"120px"`/`"140px"`

## Critérios de aceite

- `./gradlew compileJava` sem erros
- Validação visual no browser nas 8 views: o(s) ícone(s) da coluna de ações ficam totalmente visíveis, com espaçamento consistente até a borda direita do grid, sem overflow

## Comments
