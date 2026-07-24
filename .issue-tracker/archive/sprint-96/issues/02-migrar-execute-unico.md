# 02 — Migrar disparos únicos de UiConstants.VIRTUAL_THREADS para AsyncTasks

Status: done

Depende da issue 01. Todos os pontos do projeto que hoje chamam `UiConstants.VIRTUAL_THREADS.execute(...)` passam a chamar `AsyncTasks.execute(...)`: `DashboardView`, `DeployApplicationView`, `DeployFromHelmView`, `DeploymentsView`, `HelmReleasesView`, `InstalledOperatorsView`, `MainLayout`, `NamespacesView`, `OperatorCatalogView`, `PodsView`, `StatefulSetsView` e `TopologiaView`.

Após a migração, o campo `VIRTUAL_THREADS` é removido de `UiConstants` — a classe deixa de ter qualquer responsabilidade de concorrência, ficando apenas com os helpers de construção de UI que já mantinha antes desta sprint.

Comportamento observável não muda em nenhuma dessas views — é uma substituição direta do executor usado, sem alteração de lógica de negócio ou de UI.
