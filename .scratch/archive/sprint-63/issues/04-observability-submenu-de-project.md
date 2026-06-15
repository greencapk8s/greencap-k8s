---
id: "04"
title: "Observability como submenu de PROJECT — drawer e treeview de permissões"
status: done
labels: [feat, frontend]
sprint: 63
depends_on: ["01"]
---

## Contexto

Após a issue 01, o drawer tem 4 seções: PROJECT, OBSERVABILITY, GLOBAL, SETTINGS — com OBSERVABILITY (Dashboard, Events, Metrics) como seção própria, mesmo sendo escopada ao Namespace ativo, igual a PROJECT.

Decisões via `/grill-with-docs`:

- **OBSERVABILITY deixa de ser seção própria** e passa a ser um item expansível "Observability" dentro de **PROJECT**, com os mesmos 3 sub-itens (Dashboard, Events, Metrics) — mesmo padrão estrutural de `Workloads`, `Networking`, `Parameters`, `Auto Scaling`, `Storage` (item pai navegável + `addItem` de sub-itens).
- **Posição**: logo após `Topology`, antes de `Workloads` — ambos são visões de "overview" do Namespace. Ordem final de PROJECT: Topology, Observability, Workloads, Networking, Parameters, Auto Scaling, Storage.
- **Ícone do item pai "Observability"**: `VaadinIcon.EYE` (confirmado existir no enum da versão 24.4.4) — distinto dos ícones já usados pelos filhos (`DASHBOARD`, `RECORDS`, `CHART`). O item pai navega para `DashboardView.class` (primeiro filho), mesmo padrão de `Workloads` → `DeploymentsView.class`.
- **Ícone (i) de contexto**: o cabeçalho da seção OBSERVABILITY (com `NAMESPACE_CONTEXT_TOOLTIP`) deixa de existir. O item "Observability" não recebe (i) próprio — fica coberto pelo (i) do cabeçalho PROJECT, igual aos demais itens de PROJECT.
- **Permissões**: `OBSERVABILITY_DASHBOARD_VIEW`, `OBSERVABILITY_EVENTS_VIEW`, `OBSERVABILITY_METRICS_VIEW` **não são renomeadas** — mesmo padrão de `WORKLOADS_*`/`NETWORKING_*`, que também são sub-itens de PROJECT sem prefixo `PROJECT_`. Sem nova migration.
- `CONTEXT.md` já atualizado nesta sessão: novos termos `Project` (UI section que agrupa Topology, Observability, Workloads, Networking, Parameters, Auto Scaling, Storage) e `Observability` (UI subsection dentro de Project); `Global` ajustado para "Distinct from Project (scoped to the active Namespace, which includes the Observability subsection)".

## Entrega

### `MainLayout.java`

- `buildDrawer()`: remover a linha `navContent.add(buildNavSection("OBSERVABILITY", buildObservabilidadeNav(), NAMESPACE_CONTEXT_TOOLTIP));`. Drawer passa a ter 3 seções: **PROJECT, GLOBAL, SETTINGS**.
- `buildVisaoGeralNav()`: adicionar `SideNavItem observability = buildObservabilidadeNavItem();`, incluir em `addIfEnabled(...)` e em `nav.addItem(...)`, posicionado logo após `topologia` e antes de `workloads`.
- Novo método `buildObservabilidadeNavItem()` (substitui `buildObservabilidadeNav()`):
  - `anyChild = canDashboard || canEvents || canMetrics` (mesmas permissões de hoje).
  - `SideNavItem observability = navItem("Observability", DashboardView.class, VaadinIcon.EYE, anyChild);`
  - `observability.addItem(navItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD, canDashboard));`
  - `observability.addItem(navItem("Events", EventsView.class, VaadinIcon.RECORDS, canEvents));`
  - `observability.addItem(navItem("Metrics", MetricsView.class, VaadinIcon.CHART, canMetrics));`
- Remover o método `buildObservabilidadeNav()` (não usado mais).

### `UserManagementView.java` (`PermissionTreePanel`)

- Remover a linha `add(buildSection("OBSERVABILITY", buildObservabilityGroups(initial)));`.
- `buildProjectGroups(Set<Permission> initial)`: inserir o grupo "Observability" (mesmo conteúdo de `buildObservabilityGroups` — Dashboard/Events/Metrics) logo após o grupo "Topology", antes de "Workloads".
- Remover o método `buildObservabilityGroups(Set<Permission> initial)` (conteúdo absorvido por `buildProjectGroups`).

## Critérios de aceite manual

- Drawer mostra apenas 3 seções: **PROJECT, GLOBAL, SETTINGS** (OBSERVABILITY não existe mais como seção própria).
- Seção PROJECT na ordem: **Topology, Observability, Workloads, Networking, Parameters, Auto Scaling, Storage**.
- "Observability" aparece como item expansível com ícone de "olho" (EYE), contendo Dashboard, Events, Metrics; clicar em "Observability" navega para Dashboard.
- O (i) de contexto aparece apenas nos cabeçalhos PROJECT e GLOBAL — nenhum (i) extra no item "Observability".
- Com o cluster ativo desconectado, "Observability" e seus sub-itens ficam acinzentados/desabilitados, igual a Workloads/Networking/etc (`clusterDependentNavItems` preservado).
- Em "User Management" → "New User" / "Edit Permissions": a seção PROJECT da árvore de permissões mostra o grupo "Observability" logo após "Topology"; não existe mais seção OBSERVABILITY separada.
- Permissões `OBSERVABILITY_DASHBOARD_VIEW`/`OBSERVABILITY_EVENTS_VIEW`/`OBSERVABILITY_METRICS_VIEW` continuam controlando os mesmos itens, sem nova migration.
