---
id: "01"
title: "Fix: Container Registry ausente na treeview de permissões + ação View Tags na grid"
status: done
labels: [fix, frontend]
sprint: 69
---

## Contexto

No aceite manual da sprint 68 (Container Registry), identificados dois problemas:

1. A permission `GLOBAL_REGISTRY_VIEW` (introduzida na sprint 68) não foi adicionada à treeview de permissões do `UserManagementView` (`buildGlobalGroups()`) — admins não conseguiam conceder/revogar o acesso ao Container Registry por usuário.
2. A ação "View Tags" da `RegistryView` estava na barra de título (selection action sobre a linha selecionada), enquanto o padrão das demais views com navegação por linha (ex.: `JobsView` → "View Pods") é uma coluna de ações dedicada na própria grid.

## Entrega

### `ui/UserManagementView.java`

- `buildGlobalGroups()`: novo grupo "Container Registry" com `Permission.GLOBAL_REGISTRY_VIEW`, mesmo padrão de grupo único do "Infrastructure" (`GLOBAL_INFRASTRUCTURE_VIEW`).

### `ui/RegistryView.java`

- Removida a `SelectionAction` "View Tags" da barra de título; `buildSectionHeader` passa a usar a sobrecarga sem `selectionActions`.
- Nova coluna de ações via `UiConstants.addActionsColumn(grid, 1, ...)` com botão "View Tags" (`VaadinIcon.LIST`, tema `LUMO_TERTIARY`/`LUMO_ICON`), navegando para `registry/<repository>` — mesmo padrão de `JobsView` ("View Pods").

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros.
- `UserManagementView`: grupo "Container Registry" aparece em GLOBAL na treeview de permissões, com checkbox ligado a `GLOBAL_REGISTRY_VIEW`.
- `RegistryView`: cada linha da grid tem um botão "View Tags" que navega para a view de Tags do repository; barra de título não tem mais botão de ação.

## Comments

- Aceite manual confirmado pelo usuário.
