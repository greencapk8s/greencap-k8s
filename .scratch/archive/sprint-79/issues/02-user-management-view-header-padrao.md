---
title: UserManagementView — padronizar header com buildSectionHeader
status: done
sprint: 79
---

## Problem
`UserManagementView` usa um `buildToolbar()` próprio com `H2` + botão "Add User" com `LUMO_PRIMARY` sem `LUMO_SMALL`. Não tem botão Refresh nem Help. As ações de Deactivate e Edit Permissions ficam como colunas inline no grid.

## Expected
- Substituir `buildToolbar()` por `UiConstants.buildSectionHeader(...)` com H3
- "Add User" como `extraLeadingButton` com `LUMO_PRIMARY + LUMO_SMALL`
- Deactivate como `SelectionAction` destrutivo na barra
- Edit Permissions como `SelectionAction` na barra
- Refresh e Help adicionados automaticamente pelo header
- Coluna de ações inline removida do grid
