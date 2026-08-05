---
id: "05"
title: "UX: barra de seleção em Auto Scaling e Storage (HorizontalScalers, Volume Claims)"
status: done
labels: [feat, frontend]
sprint: 57
---

## Contexto

Aplica a infraestrutura da issue 01 à HorizontalScalerView e à PersistentVolumeClaimsView, removendo Delete/Manifest da coluna de ações e movendo para a barra de título, operando sobre o item selecionado.

## Entrega

Para cada view: `UiConstants.configureSingleSelection(grid)` após montar a grid; `UiConstants.selectFirstOrPreserve(grid, dataProvider, T::name)` ao final de `loadXxx()`/`refresh()` e nos listeners de filtro; `buildSectionHeader` com a nova sobrecarga.

| View | Coluna de ações (mantém) | Barra de título (nova ordem) |
|---|---|---|
| HorizontalScalerView | Edit Limits (1) | Delete (canDelete), Manifest |
| PersistentVolumeClaimsView | — (coluna removida) | Delete (canDelete), Manifest |

Manifest URLs e textos dos diálogos de delete preservados como hoje. Sem Events (não suportado nestas views). `actionsColumnWidth` ajustado para o novo número de botões na coluna (HorizontalScalerView passa de 3 para 1).

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros
- Cada listagem abre com o primeiro item selecionado
- Delete/Manifest na barra operam sobre o item selecionado
- Seleção persiste (por nome) após refresh manual/automático e mudança de filtro; cai para o primeiro item se o selecionado sumir da lista

## Comments
