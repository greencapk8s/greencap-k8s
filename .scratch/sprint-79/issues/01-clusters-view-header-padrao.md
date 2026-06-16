---
title: ClustersView — padronizar header com buildSectionHeader
status: done
sprint: 79
---

## Problem
`ClustersView` usa um `buildToolbar()` próprio com `H2` + botão "Add Cluster" com `LUMO_PRIMARY` sem `LUMO_SMALL`. Não tem botão Refresh nem Help. As ações de Test Connection e Delete ficam como colunas inline no grid, fora do padrão de `SelectionAction` das demais views.

## Expected
- Substituir `buildToolbar()` por `UiConstants.buildSectionHeader(...)` com H3
- "Add Cluster" como `extraLeadingButton` com `LUMO_PRIMARY + LUMO_SMALL`
- Test Connection e Delete como `SelectionAction` na barra (habilitados por seleção)
- Refresh (recarrega a grid) e Help adicionados automaticamente pelo header
- Coluna de ações inline removida do grid
