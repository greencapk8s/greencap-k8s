---
id: "01"
title: "User Management — treeview de permissões expansível/colapsável"
status: done
labels: [feat, frontend]
sprint: 62
depends_on: []
---

## Contexto

`UserManagementView.PermissionTreePanel` (`src/main/java/io/greencap/k8s/ui/UserManagementView.java`, ~linhas 260-541) é uma árvore de checkboxes construída com `VerticalLayout`/`Checkbox` (sem `TreeGrid`). Estrutura: 3 seções (PROJECT, OBSERVABILITY, SETTINGS) > `GroupNode` (ex: "Workloads", "Networking") > opcionalmente `SubGroupNode` (ex: "Deployments" com Scale/Restart/Rollback aninhados) > `PermissionNode` (checkbox folha). Hoje tudo é sempre renderizado expandido — o grupo "Workloads" tem ~15 checkboxes (4 sub-grupos + ReplicaSets/Pods), tornando o diálogo de permissões muito longo.

Decisões de escopo via `/grill-with-docs`:

- Apenas `GroupNode` (os ~11 grupos de topo) recebem collapse/expand. `SubGroupNode` permanece sempre expandido dentro de Workloads. As 3 seções (PROJECT/OBSERVABILITY/SETTINGS) não são afetadas.
- Mecanismo: ícone chevron (`VaadinIcon.CHEVRON_DOWN` expandido / `VaadinIcon.CHEVRON_RIGHT` colapsado) em `Button` com `LUMO_SMALL, LUMO_TERTIARY, LUMO_ICON` e `UiConstants.ICON_SIZE`, posicionado ao lado do `Checkbox` do header em uma `HorizontalLayout`. O chevron alterna a visibilidade de um container com os `displayItems` do grupo. O `Checkbox` continua controlando apenas select-all/none do grupo — sem afetar collapse.
- Estado inicial de cada grupo: **expandido se ≥1 dos seus leaves estiver marcado** em `initial` (passado ao `GroupNode`), senão **colapsado**. Aplica-se uniformemente, inclusive a grupos de 1 item (Topology, Storage, Infrastructure, Platform Settings). No diálogo "New User" (`initial = Set.of()`), todos os grupos iniciam colapsados.
- `Select All` / `Deselect All` (já existentes em `bulkActions`): alteram apenas os checkboxes/estado dos headers — não alteram collapse de nenhum grupo.
- Novos botões `Expand All` / `Collapse All` em `bulkActions` (mesma `HorizontalLayout`, mesmo estilo `LUMO_SMALL, LUMO_TERTIARY`): expandem/colapsam todos os `GroupNode` de uma vez.
- Sem impacto em `CONTEXT.md` (termo `Permission` é sobre o conceito de domínio, não sobre esta árvore de UI) e sem ADR (mudança de UI contida e reversível).

## Entrega

### `UserManagementView.GroupNode`

- Construtor recebe os mesmos parâmetros (`label`, `leaves`, `displayItems`).
- Reestruturar o layout interno:
  - Header passa a ser uma `HorizontalLayout` (`setSpacing(false)`, `setAlignItems(Alignment.CENTER)`) contendo: `Button` chevron + `Checkbox` (label, estilo `font-weight: 500` mantido).
  - `displayItems` passam a ser adicionados a um `VerticalLayout itemsContainer` (mantendo `setPadding(false)`/`setSpacing(false)`), adicionado após a header row.
- Novo método `setExpanded(boolean expanded)`:
  - `itemsContainer.setVisible(expanded)`.
  - Atualiza o ícone do chevron: `CHEVRON_DOWN` quando `expanded`, `CHEVRON_RIGHT` quando colapsado.
- Clique no chevron: `setExpanded(!itemsContainer.isVisible())`.
- No construtor, calcular `boolean hasAnyChecked = leaves.stream().anyMatch(n -> n.getCheckbox().getValue())` e chamar `setExpanded(hasAnyChecked)` como estado inicial (após `syncState()`).
- `syncState()` permanece inalterado — continua atualizando o estado `checked`/`indeterminate`/`unchecked` do `Checkbox` do header independentemente do collapse.

### `UserManagementView.PermissionTreePanel`

- Adicionar dois botões `Expand All` e `Collapse All` (`LUMO_SMALL, LUMO_TERTIARY`) na `HorizontalLayout bulkActions`, ao lado de `Select All`/`Deselect All`.
- `Expand All` → `groupNodes.forEach(g -> g.setExpanded(true))`.
- `Collapse All` → `groupNodes.forEach(g -> g.setExpanded(false))`.

## Critérios de aceite manual

- Abrir "New User" → todos os ~11 grupos colapsados (nada marcado); chevron aponta para a direita.
- Abrir "Edit Permissions" de um usuário com permissões variadas → grupos com ≥1 permissão marcada abrem expandidos, os demais colapsados.
- Clicar no chevron de um grupo → expande/colapsa só aquele grupo, sem afetar os demais nem o estado do checkbox do header.
- Clicar no checkbox do header de um grupo (select-all/none) → marca/desmarca todos os filhos, sem alterar o collapse atual do grupo.
- "Select All" / "Deselect All" → afetam todos os checkboxes da árvore, sem alterar collapse de nenhum grupo.
- "Expand All" / "Collapse All" → expandem/colapsam todos os ~11 grupos de uma vez.
- Estado `indeterminate`/checked/unchecked do header continua correto e visível independentemente do grupo estar colapsado ou expandido.
- Grupo "Workloads" (com `SubGroupNode`s Deployments/StatefulSets/Jobs/CronJobs) — ao expandir, todos os sub-grupos aparecem já expandidos (sem segundo nível de collapse).
