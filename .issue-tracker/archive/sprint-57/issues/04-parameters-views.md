---
id: "04"
title: "UX: barra de seleção em Parameters (ConfigMaps, Secrets)"
status: done
labels: [feat, frontend]
sprint: 57
---

## Contexto

Aplica a infraestrutura da issue 01 às 2 views de Parameters. Ambas tinham apenas Delete+Manifest na coluna de ações — a coluna é removida e as ações migram para a barra de título, operando sobre o item selecionado.

## Entrega

Para cada view: `UiConstants.configureSingleSelection(grid)` após montar a grid; `UiConstants.selectFirstOrPreserve(grid, dataProvider, T::name)` ao final de `loadXxx()`/`refresh()` e nos listeners de filtro; `buildSectionHeader` com a nova sobrecarga.

| View | Coluna de ações (mantém) | Barra de título (nova ordem) |
|---|---|---|
| ConfigMapsView | — (coluna removida) | Delete (canDelete), Manifest |
| SecretsView | — (coluna removida) | Delete (canDelete), Manifest |

Manifest URLs e textos dos diálogos de delete preservados como hoje. Sem Events (não suportado nestas views).

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros
- Cada listagem abre com o primeiro item selecionado
- Delete/Manifest na barra operam sobre o item selecionado
- Seleção persiste (por nome) após refresh manual/automático e mudança de filtro; cai para o primeiro item se o selecionado sumir da lista

## Comments
