---
id: "02"
title: "UX: barra de seleção em Workloads (Deployments, ReplicaSets, Pods, Jobs, CronJobs)"
status: done
labels: [feat, frontend]
sprint: 57
---

## Contexto

Aplica a infraestrutura da issue 01 às 5 views de Workloads, removendo Delete/Manifest/Events da coluna de ações e movendo para a barra de título, operando sobre o item selecionado na grid. Seleção automática do primeiro item ao final do load/refresh/filtro.

## Entrega

Para cada view: `UiConstants.configureSingleSelection(grid)` após montar a grid; `UiConstants.selectFirstOrPreserve(grid, dataProvider, T::name)` ao final de `loadXxx()`/`loadXxxAsync()`/`refresh()` e nos listeners de filtro (incluindo `applyJobFilter` em PodsView); `buildSectionHeader` com a nova sobrecarga (grid + lista de `SelectionAction`).

| View | Coluna de ações (mantém) | Barra de título (nova ordem) |
|---|---|---|
| DeploymentsView | Scale, Restart, Rollout Undo (3) | Delete (canDelete), Manifest, Events |
| ReplicaSetView | — (coluna removida, era Delete+Manifest) | Delete (canDelete), Manifest |
| PodsView | Logs (1) | Delete (canDelete), Manifest, Events |
| JobsView | View Pods (1) | Delete (canDelete), Manifest |
| CronJobsView | Trigger, Suspend/Resume, View Jobs (3) | Delete (canDelete), Manifest |

Manifest URLs, kinds de Events e textos dos diálogos de delete preservados como hoje. `actionsColumnWidth` ajustado para o novo número de botões na coluna (ou coluna removida quando 0).

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros
- Cada listagem abre com o primeiro item selecionado
- Delete/Manifest/Events na barra operam sobre o item selecionado
- Seleção persiste (por nome) após refresh manual/automático e mudança de filtro; cai para o primeiro item se o selecionado sumir da lista

## Comments
