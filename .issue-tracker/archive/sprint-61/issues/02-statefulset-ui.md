---
id: "02"
title: "StatefulSet — UI: StatefulSetsView e navegação"
status: done
labels: [feat, frontend]
sprint: 61
depends_on: ["01"]
---

## Contexto

Depende da issue `01` (DTO `StatefulSetInfo`, `WorkloadService`, permissions, `AutoScalingService.findHorizontalScalerForTarget`, `ManifestService`).

Decisões de escopo via `/grill-with-docs`:

- Nova view `StatefulSetsView`, posicionada na navegação **entre Deployments e ReplicaSets**: `Deployments → StatefulSets → ReplicaSets → Pods → Jobs → CronJobs`.
- Colunas: `Name | Replicas (badge ready/desired) | Available | Service | Age`.
- Write operations na barra de ações por linha: **Scale**, **Restart**, **Rollback** (mesmo trio do `DeploymentsView`). **Delete** e **View Manifest** vão na barra de seleção do título (padrão sprint 57).
- Segue o padrão assíncrono de carregamento do `DeploymentsView` (referência da sprint 50) — `loadStatefulSetsAsync(UI)` + `clusterErrorMessage`.
- `replicasBadge(int ready, int desired)` está duplicado (lógica idêntica) em `DeploymentsView` e `ReplicaSetView` — esta é a 3ª ocorrência, então extrair para `UiConstants.replicasBadge(int ready, int desired)` e atualizar as 3 views. (`HorizontalScalerView.replicasBadge(current, max)` tem semântica diferente — current≥max é "error", não "success" — não faz parte desta extração.)

## Entrega

### 1. `UiConstants` — extrair `replicasBadge`

- Novo método estático `static Span replicasBadge(int ready, int desired)`, com a lógica atual de `DeploymentsView`/`ReplicaSetView` (badge `success` se `desired > 0 && ready >= desired`, `error` se `ready == 0`, senão `contrast`).
- `DeploymentsView` e `ReplicaSetView`: remover o método privado `replicasBadge`, chamar `UiConstants.replicasBadge(...)`.

### 2. `StatefulSetsView.java` (novo)

- `@Route(value = "workloads/statefulsets", layout = MainLayout.class)`, `@PageTitle("StatefulSets — GreenCap K8s")`, `@PermitAll`, implementa `BeforeEnterObserver, Refreshable`.
- `HELP_TITLE = "StatefulSets"`; `HELP_TEXT` baseado no termo `StatefulSet` do `CONTEXT.md`, no formato do `HELP_TEXT` de `DeploymentsView` (descrição + parágrafo "On this screen you can: Scale ... Restart ... Rollback ...").
- Construtor recebe `WorkloadService`, `AutoScalingService`, `ObservabilityService`, `ClusterContext`, `GridSelectionMemory` (mesmas dependências de `DeploymentsView` — `ObservabilityService` não é usado aqui pois Events não está no escopo desta view, **não injetar** se não for usado).
- Grid `Grid<StatefulSetInfo>`:
  - Coluna `Name` — `sortable`, `flexGrow(2)`, `resizable`, com `TextField` de filtro (mesmo padrão das demais views).
  - Coluna componente — `UiConstants.replicasBadge(sts.ready(), sts.desired())`, header "Replicas", width 100px.
  - Coluna `Available`, width 110px.
  - Coluna `Service` — `StatefulSetInfo::serviceName`, `flexGrow(1)`, `resizable`.
  - Coluna `Age`, width 80px.
- `UiConstants.configureSingleSelection(grid, selectionMemory, getClass().getSimpleName(), StatefulSetInfo::name)`.
- Barra de seleção (`buildSectionHeader`):
  - `SelectionAction.destructive(VaadinIcon.TRASH, "Delete", canDelete, this::openDeleteDialog)` — `canDelete = SecurityUtils.hasPermission(Permission.WORKLOADS_STATEFULSETS_DELETE)`.
  - `SelectionAction.of(VaadinIcon.CODE, "View Manifest", sts -> UI.getCurrent().navigate("yaml/statefulset/" + sts.namespace() + "/" + sts.name()))`.
- Coluna de ações por linha (`UiConstants.addActionsColumn(grid, 3, ...)`):
  - **Scale** (`VaadinIcon.EXPAND`) — habilitado conforme `WORKLOADS_STATEFULSETS_SCALE`.
  - **Restart** (`VaadinIcon.ROTATE_RIGHT`) — habilitado conforme `WORKLOADS_STATEFULSETS_RESTART`.
  - **Rollback** (`VaadinIcon.REPLY`) — habilitado conforme `WORKLOADS_STATEFULSETS_ROLLBACK`.
- Carregamento: `loadStatefulSets()` (síncrono, para uso pós-ação) + `loadStatefulSetsAsync(UI ui)` (chamado em `beforeEnter`), seguindo exatamente o padrão de `DeploymentsView` (`clusterErrorMessage` via `UiConstants.buildClusterUnreachableMessage()`, `noClusterMessage`, `dataProvider.refreshAll()`, `selectFirstOrPreserve`).
- `beforeEnter`: `if (!SecurityUtils.hasPermission(Permission.WORKLOADS_STATEFULSETS_VIEW)) event.forwardTo("")`.

#### Diálogos

- **Scale** (`openScaleDialog`): chama `autoScalingService.findHorizontalScalerForTarget(cluster, sts.namespace(), sts.name())`.
  - Se presente → `UI.getCurrent().navigate("autoscaling/horizontalscalers", new QueryParameters(Map.of("edit", List.of(hpa.name()))))` (igual ao Deployment).
  - Se ausente → `openDirectScaleDialog`: `IntegerField` (min 0, max 50, valor atual = `sts.desired()`), botão "Scale" habilitado só se valor mudou, chama `workloadService.scaleStatefulSet(...)`.
- **Restart** (`openRestartDialog`): `Dialog` com mensagem "Restart " + name + "? Pods will be replaced one by one, in reverse ordinal order." Botão "Restart" (`LUMO_PRIMARY`, `LUMO_ERROR`) chama `workloadService.restartStatefulSet(...)`.
- **Rollback** (`openRollbackDialog`): `Dialog` com mensagem "Roll back " + name + " to the previous revision?" Botão "Rollback" chama `workloadService.rolloutUndoStatefulSet(...)`.
- **Delete** (`openDeleteDialog`): `ConfirmDialog` com texto: *"Deleting StatefulSet \"{name}\" will also remove all its Pods, in reverse ordinal order. PersistentVolumeClaims created from volumeClaimTemplates are retained and not deleted automatically. This action cannot be undone."* Confirm chama `workloadService.deleteStatefulSet(...)`.
- Todas as ações: após sucesso, `loadStatefulSets()` + `Notification` (`LUMO_SUCCESS`, `BOTTOM_END`); em `KubernetesOperationException`, `Notification` (`LUMO_ERROR`, `BOTTOM_END`).

### 3. `MainLayout.buildWorkloadsNavItem()`

- `boolean canStatefulSet = SecurityUtils.hasPermission(Permission.WORKLOADS_STATEFULSETS_VIEW);` incluído em `anyChild`.
- `workloads.addItem(navItem("StatefulSets", StatefulSetsView.class, VaadinIcon.DATABASE, canStatefulSet));` — inserido entre o item "Deployments" e o item "ReplicaSets".

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros.
- Item "StatefulSets" aparece no menu Workloads (entre Deployments e ReplicaSets), oculto sem `WORKLOADS_STATEFULSETS_VIEW`.
- Listagem exibe StatefulSets do namespace ativo com as 5 colunas corretas; cluster inacessível mostra o banner padrão (sem grid).
- Scale: sem HPA associado abre diálogo direto e aplica `kubectl scale` equivalente; com HPA associado redireciona para `autoscaling/horizontalscalers` com o HPA em edição.
- Restart e Rollback disparam as operações correspondentes e o cluster reflete a mudança (`kubectl rollout status statefulset/<nome>`).
- Delete remove o StatefulSet e seus Pods (em ordem reversa), texto do diálogo conforme especificado; PVCs de `volumeClaimTemplates` (se houver) permanecem no cluster.
- Botões de Scale/Restart/Rollback/Delete desabilitados para Viewer.
- "View Manifest" abre `/yaml/statefulset/{namespace}/{name}` com YAML correto, Edit/Apply funcionais para usuários com `MANIFEST_EDIT` (herdado da issue 01).
- `DeploymentsView` e `ReplicaSetView` continuam funcionando após a extração de `UiConstants.replicasBadge` (sem regressão visual).

## Comments
