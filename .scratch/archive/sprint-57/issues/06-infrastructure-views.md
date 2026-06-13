---
id: "06"
title: "UX: barra de seleção em Infrastructure (Nodes, PersistentVolumes, StorageClasses)"
status: done
labels: [feat, frontend]
sprint: 57
---

## Contexto

Aplica a infraestrutura da issue 01 às 3 views de Infrastructure (recursos cluster-scoped). Nenhuma destas views suporta Delete ou Events — apenas Manifest migra para a barra de título, operando sobre o item selecionado.

## Entrega

Para cada view: `UiConstants.configureSingleSelection(grid)` após montar a grid; `UiConstants.selectFirstOrPreserve(grid, dataProvider, T::name)` ao final de `loadXxx()`/`refresh()` e nos listeners de filtro; `buildSectionHeader` com a nova sobrecarga.

| View | Coluna de ações (mantém) | Barra de título (nova ordem) |
|---|---|---|
| NodesView | Cordon/Uncordon (1) | Manifest |
| PersistentVolumesView | — (coluna removida, era só Manifest) | Manifest |
| StorageClassesView | — (coluna removida, era só Manifest) | Manifest |

Manifest URLs preservadas como hoje (recursos cluster-scoped usam `-` como namespace). Sem Delete, sem Events.

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros
- Cada listagem abre com o primeiro item selecionado
- Manifest na barra opera sobre o item selecionado
- Seleção persiste (por nome) após refresh manual/automático e mudança de filtro; cai para o primeiro item se o selecionado sumir da lista

## Comments
