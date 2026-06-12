# GreenCap K8s — Sprints & Demandas

> Documento vivo. Atualizar a cada sprint concluída ou nova demanda identificada.

---

## Status Geral

| Sprint | Tema | Status |
|--------|------|--------|
| 53 | Versão da plataforma visível no rodapé do drawer | ✅ Concluído |
| 54 | Manutenção — archiving de sprints.md e .scratch | ✅ Concluído |
| 55 | Docker: Quick Start ponta a ponta (Dockerfile + compose + profile prod) | ✅ Concluído |
| 56 | Infrastructure — Cordon/Uncordon de Nodes | ✅ Concluído |
| 57 | UX — barra de seleção e ações na barra de título (14 views) | ✅ Concluído |
| 58 | Polish de listagens — nome do recurso na confirmação de remoção e espaçamento das colunas de ações | ✅ Concluído |
| 59 | YAML do Manifest editável (Edit + Apply) | ✅ Concluído |
| 60 | Fix — scroll horizontal em views de YAML/logs | ✅ Concluído |
| 61 | Workloads — StatefulSets | ✅ Concluído |
| 62 | User Management — treeview de permissões expansível/colapsável | ✅ Concluído |

---

## Candidatos para Próximas Sprints

### 🔵 Gerenciamento ativo — próximas operações de escrita

- **Atualizar imagem do Deployment (`kubectl set image`)** — patch em `spec.template.spec.containers[].image`. Requer UI para escolher o container quando o Pod tem múltiplos (multi-container) — maior complexidade de UX que as demais ações de write já implementadas.

### 🟢 Diferencial — visão de cluster

- **Overview multi-cluster** — tela de entrada com health de todos os clusters registrados (ConnectionStatus, namespace count) antes de entrar em um específico.

### ⚡ UX — Carregamento assíncrono nas views restantes

- **Aplicar padrão async + banner "cluster inacessível" nas views de workload** — `DeploymentsView` e `PodsView` já implementados como referência (sprint 50). Aplicar o mesmo padrão nas views restantes que fazem chamadas Kubernetes síncronas no `beforeEnter`: `ServicesView`, `ConfigMapsView`, `SecretsView`, `NodesView`, `EventsView`, `HorizontalScalerView`, `IngressView`, `JobsView`, `CronJobsView`, `ReplicaSetView`, `PersistentVolumeClaimsView`, `PersistentVolumesView`, `StorageClassesView`, `MetricsView`, `TopologiaView`. Padrão: criar `loadXxxAsync(UI ui)` com `CompletableFuture` + `UiConstants.VIRTUAL_THREADS`; adicionar `clusterErrorMessage` via `UiConstants.buildClusterUnreachableMessage()`; exibir banner e ocultar grid em caso de `KubernetesOperationException`.

### 🔧 Infraestrutura de Demo

- **Validar drivers minikube com suporte estável a multi-node** — driver `virtualbox` falha ao provisionar multi-node no Linux após reboot (DHCP do host-only network não atribui IP às VMs extras). Avaliar `--driver=docker` e `--driver=kvm2` com 3 nós: provisionar, reiniciar host e verificar que o cluster volta healthy automaticamente. Documentar driver recomendado e atualizar `cluster-provision.sh` + README de `samples/greencap-demo/`. Issue: `.scratch/sprint-50/issues/03-minikube-multinode-driver-validation.md`

### 🎓 Diferencial — Onboarding e Aprendizado

> Decorre do posicionamento registrado em `CONTEXT.md` (seção "Purpose & Audience"): GreenCap como plataforma de estudos/dev/teste para PMEs. Ainda sem escopo definido — registrar como exploração futura, não compromisso de sprint.

- **Playground/Sandbox** — marcar um Cluster ou Namespace como "seguro para experimentar", possivelmente com avisos/restrições diferenciados na UI.
- **Sample Manifests** — biblioteca de YAMLs de exemplo que o usuário pode aplicar a partir do Manifest/Apply existente, para aprender padrões comuns de workloads.
- **Onboarding/Tutorial in-app** — guia introdutório dentro da própria UI para usuários iniciantes em Kubernetes.

### 🔌 Diferencial — Integrações futuras

> Ainda sem escopo definido — registrar como exploração futura, não compromisso de sprint. Avaliar caso a caso o impacto no posicionamento "plataforma leve" (`CONTEXT.md`, seção "Purpose & Audience").

- **Helm** — visualizar/gerenciar releases Helm instaladas no cluster (charts, valores, histórico de releases).
- **Operators** — visualizar Operators instalados (CRDs, Custom Resources geridos) e seus recursos gerenciados.

### 🟣 StatefulSet — follow-ups da Sprint 61

- **StatefulSet na Topologia** — `TopologyService`/`TopologyGraphComponent` não cobrem StatefulSet. Adicionar novo tipo de nó `StatefulSet` com edge direto `StatefulSet→Pod` (sem ReplicaSet intermediário) e edge para o headless Service via `spec.serviceName`. Avaliar também os PVCs de `volumeClaimTemplates` como nodes/edges.
- **Coluna Owner em PersistentVolumeClaimsView** — PVCs criados via `volumeClaimTemplates` de um StatefulSet seguem o padrão de nome `<template>-<statefulset>-<ordinal>`; adicionar coluna indicando o StatefulSet de origem (ou "—" para PVCs avulsos), análogo ao Owner de `ReplicaSetView`.
- **Events em StatefulSetsView** — adicionar `SelectionAction.of(VaadinIcon.RECORDS, "Events", sts -> EventsDialog.open(observabilityService, clusterContext, "StatefulSet", sts.name(), sts.namespace()))` na barra de seleção, mesmo padrão de `DeploymentsView` (injetar `ObservabilityService` no construtor).

---

## Sprints Concluídas

> Mostra apenas as últimas 10 sprints. Histórico completo em `docs/sprints-archive.md` (ver `docs/agents/sprint-archiving.md`).

### Sprint 62 ✅ — User Management: treeview de permissões expansível/colapsável

- `UserManagementView.GroupNode` (painel `PermissionTreePanel` do diálogo de Permissões): cada grupo de topo (Workloads, Networking, Parameters, Auto Scaling, Storage, Topology, Observability, Clusters, Infrastructure, Users, Platform Settings) ganhou um chevron (`VaadinIcon.CHEVRON_DOWN`/`CHEVRON_RIGHT`, `LUMO_SMALL/TERTIARY/ICON`) ao lado do `Checkbox` do header, em uma `HorizontalLayout`; novo método `setExpanded(boolean)` alterna a visibilidade de um `itemsContainer` (`VerticalLayout`) com os itens do grupo e atualiza o ícone/tooltip — `SubGroupNode` (Deployments/StatefulSets/Jobs/CronJobs dentro de Workloads) permanece sempre expandido
- `itemsContainer` recebe `margin-left: var(--lumo-size-s)` para compensar a largura do chevron e manter o alinhamento das checkboxes filhas com o checkbox do header (mesmo `margin-left: var(--lumo-space-l)` de `PermissionNode`)
- Estado inicial de cada grupo: expandido se ≥1 das suas permissões já estiver marcada em `initial` (calculado no construtor de `GroupNode`), senão colapsado — uniforme para todos os grupos, inclusive os de 1 item (Topology, Storage, Infrastructure, Platform Settings); no diálogo "New User" (nada marcado) todos os grupos iniciam colapsados
- `Select All` / `Deselect All` (já existentes) continuam alterando apenas os checkboxes, sem afetar o collapse; novos botões `Expand All` / `Collapse All` na `bulkActions` expandem/colapsam todos os grupos de uma vez
- Issue: `.scratch/sprint-62/issues/01-permission-treeview-collapsible.md`

### Sprint 61 ✅ — Workloads: StatefulSets

- `CONTEXT.md`: `Workload` atualizado para incluir StatefulSet entre os tipos concretos; novo termo `StatefulSet` — Workload com identidade de rede e storage estáveis, Pods em ordem ordinal (`<name>-0`, `<name>-1`, ...), associado a Service headless via `spec.serviceName`, `volumeClaimTemplates` provisionam PVC dedicado por Pod; suporta Scale/Restart/Rollback/Delete igual ao Deployment; `Manifest` atualizado para 12 tipos namespaced editáveis (StatefulSet incluído)
- `StatefulSetInfo` (novo DTO): `name`, `namespace`, `desired`, `ready`, `available`, `serviceName`, `age`
- `WorkloadService`: `listStatefulSets`, `scaleStatefulSet`, `restartStatefulSet`, `rolloutUndoStatefulSet`, `deleteStatefulSet` — mesmo padrão (`try-with-resources`, `KubernetesOperationException`) dos equivalentes de Deployment; `StatefulSetStatus` expõe `readyReplicas`/`availableReplicas` igual a `DeploymentStatus`
- `Permission.java`: novo grupo `WORKLOADS_STATEFULSETS_{VIEW,SCALE,RESTART,ROLLBACK,DELETE}`, inserido entre os grupos de Deployments e ReplicaSets; `VIEW` em `viewerPermissions()`, todas as 5 em `operatorPermissions()`
- `V20__add_statefulset_permissions.sql`: concede `WORKLOADS_STATEFULSETS_VIEW` a todos os perfis (marker `WORKLOADS_DEPLOYMENTS_VIEW`) e as 4 permissões de escrita a Admin/Operator (marker `SETTINGS_CLUSTERS_WRITE`)
- `AutoScalingService.findHorizontalScalerForDeployment` renomeado para `findHorizontalScalerForTarget(Cluster, String namespace, String targetName)` — já filtrava apenas por `scaleTargetRef.name`, reaproveitado para StatefulSet; `DeploymentsView` atualizado para o novo nome
- `ManifestService`: StatefulSet adicionado a `EDITABLE_RESOURCE_KINDS` e ao `fetchYaml` — `/yaml/statefulset/{namespace}/{name}` com Edit/Apply herdados do `ManifestView`
- `UiConstants.replicasBadge(ready, desired)`: extraído da duplicação em `DeploymentsView`/`ReplicaSetView` (3ª ocorrência), ambas atualizadas para usar o helper compartilhado
- `StatefulSetsView` (nova): rota `workloads/statefulsets`, colunas Name/Replicas (badge)/Available/Service/Age; carregamento assíncrono (`loadStatefulSetsAsync` + `clusterErrorMessage`, padrão sprint 50); barra de seleção com Delete e View Manifest; ações por linha Scale/Restart/Rollback — Scale verifica HPA via `findHorizontalScalerForTarget` antes de abrir o diálogo direto (min 0, max 50)
- `MainLayout.buildWorkloadsNavItem()`: item "StatefulSets" adicionado entre "Deployments" e "ReplicaSets", controlado por `WORKLOADS_STATEFULSETS_VIEW`
- Fix encontrado no aceite manual: `UserManagementView.buildWorkloadsGroup` não incluía o novo grupo de permissões na treeview — adicionado `statefulSetsSubGroup` (Scale/Restart/Rollback), posicionado entre Deployments e Jobs, espelhando a ordem de navegação
- Registrados como candidatos para próximas sprints: StatefulSet na Topologia, coluna Owner em `PersistentVolumeClaimsView` (PVCs de `volumeClaimTemplates`, incluindo PVCs órfãos quando o StatefulSet é removido), Events em `StatefulSetsView`
- Issues: `.scratch/sprint-61/issues/01-statefulset-backend.md`, `02-statefulset-ui.md`

### Sprint 60 ✅ — Fix: scroll horizontal em views de YAML/logs

- `ManifestView.java`: estilo de `yamlContent` (`Pre`, leitura) — `white-space: pre` → `pre-wrap`, adicionado `overflow-wrap: anywhere`, mantido `overflow: auto` existente e adicionado `overflow-x: hidden`; linhas longas de YAML quebram visualmente em vez de gerar scroll horizontal. `yamlEditor` (TextArea, edição) inalterado — Vaadin já aplica `pre-wrap`/`min-width: 0` internamente
- `PodLogsView.java`: mesmo padrão aplicado a `logContent` (`Pre`, logs do Pod) em `styleLogContent()`
- Issues: `.scratch/sprint-60/issues/01-fix-scroll-manifestview.md`, `.scratch/sprint-60/issues/02-fix-scroll-podlogsview.md`

### Sprint 59 ✅ — YAML do Manifest editável (Edit + Apply)

- `CONTEXT.md`: termo `Manifest` atualizado — deixa de ser somente leitura; documenta os 11 tipos namespaced editáveis (Pod, Deployment, ReplicaSet, Job, CronJob, Service, Ingress, ConfigMap, Secret, HorizontalScaler, PersistentVolumeClaim) vs. os 3 cluster-scoped que permanecem read-only (Node, PersistentVolume, StorageClass); novo termo `Apply` definido como replace completo (PUT)
- `docs/adr/0005-manifest-apply-as-full-replace.md`: nova ADR registrando por que Apply remove `resourceVersion`/`uid`/`creationTimestamp`/`generation`/`managedFields`/`status` antes do `update()` — evita 409 espúrio por churn de `status` em recursos reconciliados continuamente; semântica de replace completo (não merge estilo `kubectl apply`)
- `Permission.java`: novo grupo "Project — Manifest" com `MANIFEST_EDIT`; incluído em `operatorPermissions()`, ausente de `viewerPermissions()`
- `V19__add_manifest_edit_permission.sql`: concede `MANIFEST_EDIT` a usuários com `SETTINGS_CLUSTERS_WRITE` (Admin/Operator)
- `ManifestService`: novo método `applyYaml()` — parseia o YAML editado via `YAMLMapper`, valida `kind`/`metadata.name`/`metadata.namespace` contra os parâmetros da URL (bloqueia divergências sem chamar a API), remove campos gerenciados pelo servidor e o nó `status`, e aplica via `client.resource(yaml).inNamespace(namespace).update()`; novo `isEditable(resourceType)` com o mapa dos 11 tipos editáveis → `kind` esperado
- `ManifestView`: botões **Edit** e **Apply** no header — Edit alterna para Cancelar (descarta alterações e volta ao YAML original), Apply só visível em modo edição e abre `ConfirmDialog` antes de enviar; editor é um `TextArea` monoespaçado que substitui o `Pre` em modo edição e recebe foco automático; sucesso re-busca o YAML e volta ao modo leitura com notificação, falha mantém o texto editado com notificação de erro; Edit visível apenas para os 11 tipos editáveis e desabilitado (com tooltip) sem `MANIFEST_EDIT`
- Issue: `.scratch/sprint-59/issues/01-yaml-manifest-editavel.md`

### Sprint 58 ✅ — Polish de listagens: nome do recurso na confirmação de remoção e espaçamento das colunas de ações

- `ConfirmDialog` de remoção atualizado em 11 views para incluir o nome do recurso na mensagem: `PodsView`, `DeploymentsView`, `ReplicaSetView`, `JobsView`, `CronJobsView` (ambas as variantes, com e sem Jobs ativos), `ServicesView`, `IngressView`, `ConfigMapsView`, `SecretsView`, `HorizontalScalerView`, `PersistentVolumeClaimsView`
- Fix: último botão (lado direito) das colunas de ações ficava colado/cortado na borda da grid — causa raiz era o `HorizontalLayout` padrão (padding/spacing) das colunas de componente, não considerado em `actionsColumnWidth`
- `UiConstants.actionsColumnWidth`: agora soma `ACTION_BUTTON_WIDTH_PX` (48px) por botão + `ACTIONS_COLUMN_RIGHT_PADDING_PX` (8px) de respiro
- Novo helper `UiConstants.addActionsColumn(grid, buttonCount, buttonsProvider)`: monta a coluna de ações com `HorizontalLayout` sem padding/spacing padrão e `padding-right` consistente — substitui o padrão duplicado em 8 views: `NodesView`, `DeploymentsView`, `PodsView`, `JobsView`, `CronJobsView`, `HorizontalScalerView`, `ClustersView`, `UserManagementView`
- `CronJobsView`: `buildActionsLayout` refatorado para `buildActionButtons` (retorna `List<Button>`); `ClustersView`/`UserManagementView`: `buildActions` passam a retornar `List<Button>` em vez de `HorizontalLayout`
- Issues: `.scratch/sprint-58/issues/01-nome-do-recurso-no-dialogo-de-remocao.md` e `02-espacamento-coluna-de-acoes.md`

### Sprint 57 ✅ — UX: barra de seleção e ações na barra de título (14 views)

- `UiConstants.buildSectionHeader`: nova ordem `[heading][...selectionActions][Refresh][Help]` (antes `[heading][Help][Refresh]`); EventsView/MetricsView seguem usando a sobrecarga sem botões extras
- `UiConstants.SelectionAction<T>` (record): `icon`, `title`, `enabled`, `destructive`, `handler`; factories `of(icon, title, handler)`, `of(icon, title, enabled, handler)`, `destructive(icon, title, enabled, handler)`
- Nova sobrecarga `buildSectionHeader(title, onRefresh, helpTitle, helpText, grid, List<SelectionAction<T>>)`: monta os botões de ação operando sobre `grid.asSingleSelect()`, habilitados/desabilitados via `ValueChangeListener` conforme há seleção e permissão
- `UiConstants.configureSingleSelection(Grid<T>)`: `SelectionMode.SINGLE` + `setDeselectAllowed(false)` — impede deselecionar clicando na linha já selecionada
- `UiConstants.selectFirstOrPreserve(grid, dataProvider, nameExtractor)`: preserva a seleção por nome após load/refresh manual/automático/filtro; cai para o primeiro item se o selecionado sumir da lista; `deselectAll()` se a lista ficar vazia
- 14 views migradas — Delete/Manifest/Events saem da coluna de ações da grid e vão para a barra de título, operando sobre o item selecionado; coluna de ações reduzida ou removida (`actionsColumnWidth` ajustado); cada listagem abre com o primeiro item selecionado:
  - **Workloads**: `DeploymentsView` (mantém Scale/Restart/Rollout Undo; barra: Delete, Manifest, Events), `ReplicaSetView` (coluna removida; barra: Delete, Manifest), `PodsView` (mantém Logs; barra: Delete, Manifest, Events), `JobsView` (mantém View Pods; barra: Delete, Manifest), `CronJobsView` (mantém Trigger/Suspend-Resume/View Jobs; barra: Delete, Manifest)
  - **Networking**: `ServicesView`, `IngressView` (colunas removidas; barra: Delete, Manifest)
  - **Parameters**: `ConfigMapsView`, `SecretsView` (colunas removidas; barra: Delete, Manifest)
  - **Auto Scaling / Storage**: `HorizontalScalerView` (mantém Edit Limits; barra: Delete, Manifest), `PersistentVolumeClaimsView` (coluna removida; barra: Delete, Manifest)
  - **Infrastructure** (recursos cluster-scoped, sem Delete/Events): `NodesView` (mantém Cordon/Uncordon; barra: Manifest), `PersistentVolumesView`, `StorageClassesView` (colunas removidas; barra: Manifest)
- Fix encontrado no aceite manual: ao abrir "View Manifest" e voltar (Back), o Vaadin recria a View (nova `Grid`, novo `ListDataProvider`) e a seleção voltava sempre para o primeiro item. Novo bean `GridSelectionMemory` (`@VaadinSessionScope`, `Map<viewKey, itemName>`) + nova sobrecarga `configureSingleSelection(grid, selectionMemory, viewKey, nameExtractor)` que registra o nome do item selecionado a cada mudança; `selectFirstOrPreserve` consulta essa memória (via `ComponentUtil`) antes de cair no fallback "primeiro item". `viewKey = getClass().getSimpleName()` em todas as 14 views
- Issues: `.scratch/sprint-57/issues/01-shared-selection-toolbar.md` a `07-selection-memory-across-navigation.md`

### Sprint 56 ✅ — Infrastructure: Cordon/Uncordon de Nodes

- `CONTEXT.md`: definição de `Node` atualizada para incluir Cordon/Uncordon como write operation; novo termo `Cordon` adicionado ao glossário, no formato de `Suspend`
- `Permission.SETTINGS_INFRASTRUCTURE_CORDON` adicionado ao enum; incluído em `operatorPermissions()`; ausente em `viewerPermissions()`
- `V18__add_node_cordon_permission.sql`: concede `SETTINGS_INFRASTRUCTURE_CORDON` a Admin e Operator (identificados por `SETTINGS_CLUSTERS_WRITE`, mesmo padrão de `V17`)
- `NodeInfo`: novo campo `schedulingDisabled`
- `StorageService`: `listNodes()` popula `schedulingDisabled` a partir de `spec.unschedulable`; novo método `cordonNode(Cluster, String, boolean)` faz patch via `client.nodes().withName(name).edit(...)`
- `NodesView`: nova coluna "Scheduling" com badge `Schedulable`/`Cordoned`; botão toggle Cordon/Uncordon (`PAUSE`/`PLAY`) na coluna de ações, desabilitado para Viewer, mesmo padrão de Suspend/Resume do `CronJobsView`; `HELP_TEXT` atualizado
- `UserManagementView` não foi alterado — segue o mesmo gap pré-existente dos 9 `_DELETE` da sprint 51 (permission concedida via migration, sem editor por usuário)
- Issue: `.scratch/sprint-56/issues/01-node-cordon-uncordon.md`

### Sprint 55 ✅ — Docker: Quick Start ponta a ponta (Dockerfile + compose + profile prod)

- `docker/Dockerfile` (novo): build multi-stage — stage `builder` (`eclipse-temurin:21-jdk`) roda `./gradlew bootJar -x jar` (gera o frontend Vaadin de produção via plugin, sem Node instalado no host); stage `runtime` (`eclipse-temurin:21-jre` + `curl`) só com o JAR final
- `.dockerignore` (novo): exclui `build/`, `bin/`, `node_modules/`, `.git/`, `.gradle/`, `.scratch/`, `docs/` do contexto de build
- `docker-compose.yml`: corrigido bug pré-existente em `build.context: ..` (apontava um nível acima do diretório do projeto, fazendo `docker compose up` falhar sempre); adicionado `SPRING_PROFILES_ACTIVE: prod` e `healthcheck` via `/actuator/health` no serviço `greencap`
- `src/main/resources/application-prod.yaml` (novo): `greencap.encryption.key: ${GREENCAP_ENCRYPTION_KEY}` sem fallback — falha rápido no startup se a variável não estiver definida (testado isoladamente: erro claro de placeholder não resolvido)
- `.env.example`: `ENCRYPTION_KEY`, `DB_USER`, `DB_PASSWORD` agora com valores padrão funcionais para Quick Start, com aviso para troca em produção real; `GREENCAP_ENCRYPTION_KEY` documentado separadamente para o fluxo Gradle/dev
- `README.md`: nova seção "Quick Start (Docker)" como caminho principal (clone → `cp .env.example .env` → `docker compose up -d --build` → `http://localhost:8080`, login `admin`/`admin`); fluxo Gradle movido para "Para desenvolvedores"
- Validado ponta a ponta: `docker compose up -d --build` sobe `db` + `greencap`, container `greencap` fica `healthy`, login acessível
- `docs/agents/sprint-archiving.md`: regra ajustada — "Status Geral" agora acompanha a mesma janela de 10 sprints de "Sprints Concluídas" (em vez de manter histórico completo)

### Sprint 54 ✅ — Manutenção: archiving de sprints.md e .scratch

- `docs/agents/sprint-archiving.md` (novo): documenta a regra de archiving — "Sprints Concluídas" mantém só as últimas 10 sprints, restante vai para `docs/sprints-archive.md`; `.scratch/sprint-N/` antigos vão para `.scratch/archive/sprint-N/`; executado na etapa 6 (Fechamento) do fluxo de sprint
- `CLAUDE.md`: referência ao novo doc em "Agent skills"; etapa 6 do fluxo de sprint passa a citar a verificação de archiving
- `docs/agents/issue-tracker.md`: nota sobre `.scratch/archive/`
- `docs/sprints-archive.md` (novo): detalhamento das sprints 1-43 em ordem cronológica crescente, migrado de `docs/sprints.md` (que estava fora de ordem); sprint 38 marcada com nota de detalhamento não registrado na época
- `docs/sprints.md`: "Sprints Concluídas" reduzida de 47 para as últimas 10 entradas; tabela "Status Geral" mantida completa; seção "Backlog" removida (sprints 28-32 já cobertas no archive); itens pendentes de Dockerfile/`GREENCAP_ENCRYPTION_KEY` realocados para "Candidatos para Próximas Sprints" sob novo grupo "🐳 Infraestrutura de Produção"
- `.scratch/`: diretórios `sprint-4` a `sprint-43` movidos para `.scratch/archive/sprint-N/` via `git mv`, preservando histórico

### Sprint 53 ✅ — Versão da plataforma visível no rodapé do drawer

- `build.gradle.kts`: `springBoot { buildInfo() }` registra o task `bootBuildInfo` — gera `META-INF/build-info.properties` com a versão do projeto; disponível como bean `BuildProperties` em runtime
- Formato de versão adotado: `v{major}.{minor}.{patch}-rc` para release candidates, `v{major}.{minor}.{patch}` para releases finais; controlado manualmente no `build.gradle.kts`; versão inicial: `0.1.53-rc`
- `MainLayout`: `BuildProperties` injetado via construtor; `buildDrawer()` refatorado para separar nav content em `Scroller` + `VerticalLayout` externo com `expand(scroller)` para empurrar o rodapé ao fundo
- `buildVersionFooter()`: `Div` centralizado com `Span` `v{version}` em `FontSize.XXSMALL` + `TextColor.TERTIARY`, fixado no fundo do drawer em todas as páginas

### Sprint 52 ✅ — Fix: Navbar não acompanha o hide do drawer

- `MainLayout.initResizableDrawer()`: `applyWidth()` agora verifica `appLayout.hasAttribute('drawer-opened')` antes de aplicar o offset — usa `w + 'px'` se aberto, `'0px'` se fechado
- `MutationObserver` adicionado sobre o atributo `drawer-opened` do AppLayout: sempre que o drawer abre ou fecha, recalcula `navbarPart.style.left` e `contentPart.style.marginInlineStart` conforme o novo estado

---

## Legenda

| Ícone | Significado |
|-------|-------------|
| ✅ | Concluído |
| ⏸ | Pausado |
| 🔲 | Pendente |
| 📝 | Documentado |
| 🐛 | Bug |
| 💡 | Melhoria |
