---
id: "01"
title: "Nova seção GLOBAL no drawer — move Clusters e Infrastructure para fora de SETTINGS"
status: done
labels: [feat, frontend]
sprint: 63
depends_on: []
---

## Contexto

Hoje `MainLayout.buildDrawer()` monta 3 seções no drawer, todas via `buildNavSection(label, SideNav)`:

- **PROJECT** (`buildVisaoGeralNav`): Topology, Workloads, Auto Scaling, Networking, Parameters, Storage — todos escopados ao Namespace ativo.
- **OBSERVABILITY** (`buildObservabilidadeNav`): Dashboard, Events, Metrics — também escopados ao Namespace ativo.
- **SETTINGS** (`buildConfiguracaoNav`): mistura níveis diferentes — `Clusters` (registro de todos os Clusters, independente de qualquer cluster ativo), `Infrastructure` (Nodes/PersistentVolumes/StorageClasses do cluster ativo — cluster-scoped), `Users` e `Settings` (Platform Settings — preferências de plataforma/usuário).

Decisões de escopo via `/grill-with-docs`:

- Modelo de três níveis adotado: **Namespace** (PROJECT, OBSERVABILITY) / **Cluster** (nova seção GLOBAL) / **Platform** (SETTINGS, sem mudança de nível).
- Nova seção **GLOBAL** agrupa `Clusters` e `Infrastructure`, ambos atualmente em SETTINGS. `Users` e `Settings` (Platform Settings) permanecem em SETTINGS.
- Ordem final do drawer: **PROJECT, OBSERVABILITY, GLOBAL, SETTINGS** — mantém no topo o que é usado no dia a dia (Namespace), com escopo crescente conforme desce.
- `UserManagementView.PermissionTreePanel` espelha essas mesmas 3→4 seções (PROJECT/OBSERVABILITY/SETTINGS hoje) e precisa da mesma reorganização.
- `CONTEXT.md` já atualizado nesta sessão de planejamento: termo `Infrastructure` agora diz "UI section within Global" (era "within Settings"); novo termo `Global` adicionado ao glossário, definindo a seção como "views related to Clusters as a whole rather than to resources inside a Namespace — Clusters (registry) + Infrastructure".
- Renomeação das permissões `SETTINGS_CLUSTERS_*`/`SETTINGS_INFRASTRUCTURE_*` → `GLOBAL_*` é tratada na issue 02 (sequenciada depois desta, para não conflitar nas mesmas linhas).
- Ícone de contexto (i) com tooltip é tratado na issue 03.

## Entrega

### `MainLayout.java`

- `buildDrawer()`: reordenar para `buildNavSection("PROJECT", buildVisaoGeralNav())`, `buildNavSection("OBSERVABILITY", buildObservabilidadeNav())`, `buildNavSection("GLOBAL", buildGlobalNav())`, `buildNavSection("SETTINGS", buildConfiguracaoNav())`.
- Novo método `buildGlobalNav()`: retorna um `SideNav` contendo o item `Clusters` (mesma construção de hoje, via `navItem("Clusters", ClustersView.class, VaadinIcon.SERVER, ...)`) e `buildInfrastructureNavItem()` (inalterado, incluindo o registro em `clusterDependentNavItems`).
- `buildConfiguracaoNav()`: remover o item `Clusters` e a chamada a `buildInfrastructureNavItem()`; manter apenas `usersItem` e `settingsItem`.
- `buildInfrastructureNavItem()`: sem alterações de comportamento, apenas passa a ser chamado por `buildGlobalNav()` em vez de `buildConfiguracaoNav()`.

### `UserManagementView.java` (`PermissionTreePanel`)

- Inserir `add(buildSection("GLOBAL", buildGlobalGroups(initial)));` entre as chamadas de `"OBSERVABILITY"` e `"SETTINGS"` (mesma ordem do drawer).
- Novo método `buildGlobalGroups(Set<Permission> initial)`: contém os grupos `"Clusters"` e `"Infrastructure"`, movidos de `buildSettingsGroups`.
- `buildSettingsGroups(Set<Permission> initial)`: remover os grupos `"Clusters"` e `"Infrastructure"`; manter `"Users"` e `"Platform Settings"`.

## Critérios de aceite manual

- Login como Admin: o drawer mostra a ordem **PROJECT, OBSERVABILITY, GLOBAL, SETTINGS**.
- Seção **GLOBAL** contém `Clusters` e `Infrastructure` (com os 3 subitens: Persistent Volumes (PV), Storage Classes, Nodes).
- Seção **SETTINGS** contém apenas `Users` e `Settings`.
- Navegação para cada item continua funcionando normalmente (URLs/rotas inalteradas).
- Com o cluster ativo desconectado, `Infrastructure` continua acinzentado/desabilitado (comportamento de `clusterDependentNavItems` preservado); `Clusters` continua sempre habilitado.
- Em "User Management" → "New User" / "Edit Permissions", a árvore de permissões mostra a nova seção **GLOBAL** entre OBSERVABILITY e SETTINGS, com os grupos "Clusters" e "Infrastructure"; SETTINGS mostra apenas "Users" e "Platform Settings".
