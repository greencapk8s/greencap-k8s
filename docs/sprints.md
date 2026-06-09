# GreenCap K8s — Sprints & Demandas

> Documento vivo. Atualizar a cada sprint concluída ou nova demanda identificada.

---

## Status Geral

| Sprint | Tema | Status |
|--------|------|--------|
| 1 | Setup + Auth + Login | ✅ Concluído |
| 2 | Conexão de Clusters (kubeconfig) | ✅ Concluído |
| 3 | Visualização de Workloads | ✅ Concluído |
| 4 | Estabilização + Ambiente Local | ✅ Concluído |
| 5 | Redesign de Layout + UX | ✅ Concluído |
| 6 | Login, Logout + UX de autenticação | ✅ Concluído |
| 7 | Cluster Atual por Sessão | ✅ Concluído |
| 8 | Refinamento de Navegação + Workloads | ✅ Concluído |
| 9 | Rede, Configuração e Demo | ✅ Concluído |
| 10 | UI Language Standardization | ✅ Concluído |
| 11 | UI Polish — ícones e navegação | ✅ Concluído |
| 12 | Observabilidade: Events | ✅ Concluído |
| 13 | Observabilidade: Metrics + UX global | ✅ Concluído |
| 14 | Persistência do Namespace ativo | ✅ Concluído |
| 15 | Visualização de Manifest (YAML) | ✅ Concluído |
| 16 | UX pós-login com cluster inacessível | ✅ Concluído |
| 17 | Auto Scaling — HorizontalScaler (HPA) | ✅ Concluído |
| 18 | Workloads — ReplicaSets | ✅ Concluído |
| 19 | Storage — PersistentVolumeClaims | ✅ Concluído |
| 20 | Infrastructure — PersistentVolumes + StorageClasses | ✅ Concluído |
| 21 | UX — Links entre recursos + Sidebar redimensionável | ✅ Concluído |
| 22 | UX — Remoção de Namespace redundante + Filtros por coluna | ✅ Concluído |
| 23 | Topology — visualização gráfica de objetos Kubernetes | ✅ Concluído |
| 24 | Topology — Drawer lateral com resumo do recurso ao clicar no nó | ✅ Concluído |
| 25 | Regressão de UI — labels do sidebar sem formatação | ✅ Concluído |
| 26 | Migração para repositório oficial greencapk8s | ✅ Concluído |
| 27 | Topology — PersistentVolumeClaim no grafo | ✅ Concluído |
| 28 | Dev workflow — skills greencap-run e greencap-stop | ✅ Concluído |
| 29 | Workloads — Scale e Restart de Deployment | ✅ Concluído |
| 30 | Auto refresh nas listing views | ✅ Concluído |
| 31 | Observabilidade — Events scoped por recurso | ✅ Concluído |
| 32 | Troubleshooting — PodLog viewer em página dedicada | ✅ Concluído |
| 33 | Observabilidade — Dashboard de namespace | ✅ Concluído |
| 34 | UX — Melhorias de navbar, dashboard e ReplicaSets | ✅ Concluído |
| 35 | Platform Settings — tela de configurações globais | ✅ Concluído |
| 36 | UX — Drawer width no banco, tema dark/light, poll interval do PodLog | ✅ Concluído |
| 37 | RBAC — controle de acesso por role e gerenciamento de usuários | ✅ Concluído |
| 38 | RBAC granular — permissões por funcionalidade com TreeView | ✅ Concluído |
| 39 | Workloads — Deployment Rollback (Rollout Undo) | ✅ Concluído |
| 40 | Workloads — Jobs e CronJobs (read-only) | ✅ Concluído |
| 41 | Workloads — Jobs/CronJobs: navegação contextual para logs | ✅ Concluído |
| 42 | Workloads — Jobs/CronJobs: operações de escrita | ✅ Concluído |
| 43 | Infrastructure — Nodes | ✅ Concluído |
| 44 | Networking — Ingresses (read-only) | ✅ Concluído |
| 45 | Topologia: ocultar pods de Jobs/CronJobs | ✅ Concluído |
| 46 | UX — botão de Help em todas as views | ✅ Concluído |
| 47 | Topologia — agrupamento de nós por labels part-of/component | ✅ Concluído |
| 48 | Topologia — migração para layout fcose (elimina sobreposição de grupos) | ✅ Concluído |
| 49 | Topologia — persistência do TopologyLayout (posições dos nós + toggle) | ✅ Concluído |

---

## Candidatos para Próximas Sprints

Prioridade recomendada com base na evolução da plataforma (sprint 44):

### 🟢 Diferencial — visão de cluster

- **Overview multi-cluster** — tela de entrada com health de todos os clusters registrados (ConnectionStatus, namespace count) antes de entrar em um específico.

---

## Sprints Concluídas

### Sprint 49 — Topologia: persistência do TopologyLayout
- `CONTEXT.md`: novo termo `TopologyLayout` — snapshot persistido do estado visual da Topologia por User + Cluster + Namespace; armazena posições dos nós e estado do toggle `groupingEnabled`; auto-save após cada drag; nós removidos são descartados na próxima gravação; nós novos são posicionados pelo fcose enquanto os conhecidos ficam fixos
- Migration `V16__add_topology_layouts.sql`: tabela `topology_layouts` com `unique(user_id, cluster_id, namespace)`, coluna `node_positions` (TEXT), `grouping_enabled` (BOOLEAN), `updated_at`
- `TopologyLayout.java` + `TopologyLayoutRepository.java`: entidade JPA e repository com `findByUserIdAndClusterIdAndNamespace`
- `TopologyLayoutService.java`: método `upsertLayout` — cria ou atualiza o layout salvo para o contexto; método `findLayout` para leitura
- `TopologyGraphComponent.java`: injeção de `TopologyLayoutService` e `UserRepository`; método `@ClientCallable saveLayout(String, boolean)` chamado pelo frontend após cada drag; `setSavedPositions()` para passar posições ao frontend; `setContext(clusterId, namespace)` para contextualizar o save
- `TopologiaView.java`: ao entrar na view, carrega o `TopologyLayout` salvo — restaura o toggle antes da renderização e passa `savedPositions` ao componente; injeção de `TopologyLayoutService` e `UserRepository`
- `topology-graph.ts`: nova property `savedPositions`; ao renderizar, constrói `fixedNodeConstraint` com os nós conhecidos (fcose os pina nas posições salvas, nós novos são posicionados livremente); listener `dragfree` dispara `_saveLayout()` com snapshot completo; mudança no toggle também dispara `_saveLayout()`

### Sprint 48 — Topologia: migração para layout fcose
- `TopologyGraphComponent.java`: adicionado `@NpmPackage(value = "cytoscape-fcose", version = "2.2.0")`
- `topology-graph.ts`: importado e registrado `cytoscape-fcose`; layout substituído de `breadthfirst` para `fcose` com parâmetros `nodeSeparation: 80`, `idealEdgeLength: 120`, `nodeRepulsion: 12000`, `padding: 48`; lógica de `rootIds` removida (exclusiva do breadthfirst)
- `cytoscape-fcose.d.ts`: declaração de tipos criada no frontend (pacote não tem tipos oficiais)
- `fcose` suporta compound nodes nativamente — elimina sobreposição de `TopologyGroup` que ocorria com `breadthfirst`; aplicado nos dois modos (com e sem agrupamento)

### Sprint 47 — Topologia: agrupamento de nós por labels part-of/component
- `CONTEXT.md`: novo termo `TopologyGroup` — container visual em torno de nós que compartilham `app.kubernetes.io/part-of` e/ou `app.kubernetes.io/component`, agrupamento aninhado (part-of por fora, component por dentro)
- `TopologyNode`: dois novos campos `partOfGroup`/`componentGroup`, derivados das labels de cada recurso (incluindo `PodGroup`, a partir do primeiro Pod do grupo, e `PersistentVolumeClaim`, a partir do próprio metadata)
- `topology-graph.ts`: renderiza os grupos como compound nodes do Cytoscape — caixa externa `part-of: <valor>`, caixa interna aninhada `component: <valor>`; nó com `component` mas sem `part-of` forma seu próprio grupo de nível externo; nó sem nenhuma das labels permanece solto, fora de qualquer caixa
- `TopologiaView`: checkbox "Group by labels" no canto superior direito do grafo, ligado por padrão — ao desligar, o grafo volta ao layout plano; texto de Help atualizado explicando o agrupamento
- Caixas são puramente visuais — sem colapsar/expandir

### Sprint 46 — UX: botão de Help em todas as views
- `HelpDialog`: novo componente estático (mesmo padrão de `EventsDialog`) — `Dialog` modal com título e parágrafos de texto explicativo, botão "Close"
- `UiConstants.buildSectionHeader`: novo parâmetro de conteúdo de ajuda; botão `VaadinIcon.QUESTION_CIRCLE` adicionado à esquerda do botão de refresh — header final: `Título — [Help] — [Refresh]`
- 16 views migradas com constantes próprias `HELP_TITLE`/`HELP_TEXT`, em inglês, explicando o que é o recurso e quais operações a tela permite (Deployments, ReplicaSets, Pods, Jobs, CronJobs, Services, Ingresses, ConfigMaps, Secrets, Horizontal Scalers, Volume Claims, Nodes, Events, Metrics, Persistent Volumes, Storage Classes)
- `TopologiaView`: botão de Help flutuante no canto superior direito do canvas (a view não usa `buildSectionHeader` por ser full-canvas), abrindo o mesmo `HelpDialog`
- Textos focados na definição do recurso e nas operações disponíveis na tela — sem menções a "somente leitura" nem ao papel do GreenCap na exibição dos dados

### Sprint 45 — Topologia: ocultar pods de Jobs/CronJobs
- `CONTEXT.md`: definição de `Topologia` ampliada com nota explicando que pods owned por Job (direto ou via CronJob) são deliberadamente excluídos — representam execuções efêmeras de tarefas finitas, não a topologia de serviço de longa duração que a view mapeia
- `TopologyService.buildGraph()`: lista de pods filtrada logo após o fetch, removendo pods cujo `ownerReferences` contenha `kind == "Job"` — antes de qualquer agrupamento por ReplicaSet
- Novo método privado `isOwnedByJob(Pod pod)`, ao lado de `ownerReplicaSetName()`
- Cobre tanto Jobs disparados manualmente quanto Jobs criados por CronJobs — o Pod sempre referencia o Job diretamente, nunca o CronJob

### Sprint 44 — Networking — Ingresses (read-only)
- Termo `Ingress` adicionado ao `CONTEXT.md`: namespaced, IngressClass opcional (`"—"` se ausente), hosts colapsados, TLS como boolean, Address de `status.loadBalancer.ingress`
- `Permission.NETWORKING_INGRESS_VIEW` adicionado ao enum; incluído em `allPermissions()`, `operatorPermissions()` e `viewerPermissions()`
- Migration `V15__add_ingress_permission.sql`: concede `NETWORKING_INGRESS_VIEW` a todos os usuários que já possuem `NETWORKING_SERVICES_VIEW`
- `IngressInfo` record criado em `kubernetes/dto/` com campos: name, namespace, ingressClass, hosts, tls, address, age
- `NetworkingService.listIngresses()`: lista via `client.network().v1().ingresses()`; IngressClass de `spec.ingressClassName`; hosts colapsados de `spec.rules[].host`; address de `status.loadBalancer.ingress[].ip` (fallback hostname); suporte a all-namespaces
- `IngressView`: rota `/networking/ingresses`; colunas Name · IngressClass · Hosts · TLS · Address · Age · Manifest; badge `success`="TLS" / `contrast`="Plain"; filtros por Name e IngressClass; implementa `Refreshable`; protegida por `NETWORKING_INGRESS_VIEW`
- `MainLayout.buildRedeNavItem()`: sub-item "Ingresses" com ícone `ARROWS_LONG_RIGHT` adicionado abaixo de "Services"; pai "Networking" permanece apontando para `ServicesView`
- `ManifestService`: tipo `"ingress"` adicionado — `client.network().v1().ingresses().inNamespace(ns).withName(name).get()`
- `UserManagementView`: "Ingresses" adicionado à árvore de permissões sob o grupo Networking
- `samples/greencap-demo/manifests/11-ingress.yaml`: Ingress `greencap-demo.local` com paths `/` → frontend e `/api` → backend, `ingressClassName: nginx`
- `samples/greencap-demo/create.sh`: habilita addon `ingress` + aguarda controller pronto antes de aplicar manifests; exibe comando `/etc/hosts` com IP resolvido
- `samples/greencap-demo/add-hosts.sh`: script auxiliar para adicionar entrada no `/etc/hosts`
- `README.md`: seção "Ambiente de demonstração" apontando para `samples/greencap-demo/create.sh`

### Sprint 43 — Infrastructure — Nodes
- Termo `Node` adicionado ao `CONTEXT.md`: status derivado da condição `Ready`, role derivado de labels canônicos (`control-plane`/`master`), allocatable CPU e memory como campos principais
- `NodeInfo` record criado em `kubernetes/dto/` com campos: name, status, role, version, os, cpu, memory, age
- `StorageService.listNodes()`: lista nodes via `client.nodes().list()`; status derivado de `status.conditions[type=Ready]`; role de labels `node-role.kubernetes.io/control-plane` ou `master`; memory convertida de kibibytes para GiB com 1 casa decimal
- `NodesView`: rota `/infrastructure/nodes`; colunas Name · Status · Role · Version · OS · CPU · Memory · Age · Manifest; badge `success` para Ready, `error` para NotReady, `contrast` para Unknown; filtros por Name e Status; implementa `Refreshable`; protegida por `SETTINGS_INFRASTRUCTURE_VIEW` (sem nova permission, sem migration)
- `MainLayout`: sub-item `"Nodes"` com ícone `SERVER` adicionado ao grupo Infrastructure, abaixo de Storage Classes
- `ManifestService`: tipo `"node"` adicionado ao switch — `client.nodes().withName(name).get()` (cluster-scoped, namespace ignorado)

### Sprint 42 — Workloads — Jobs/CronJobs: operações de escrita
- `Permission`: 4 novos valores adicionados — `WORKLOADS_JOBS_DELETE`, `WORKLOADS_CRONJOBS_RUN_NOW`, `WORKLOADS_CRONJOBS_SUSPEND`, `WORKLOADS_CRONJOBS_DELETE`; `operatorPermissions()` inclui Run Now e Suspend; delete restrito a Admin
- Migration `V14__add_jobs_cronjobs_write_permissions.sql`: concede Run Now e Suspend a usuários com `WORKLOADS_CRONJOBS_VIEW`; concede delete a usuários com `SETTINGS_USERS_WRITE` (proxy de Admin)
- `WorkloadService`: 4 novos métodos — `triggerCronJob()` cria Job a partir do `spec.jobTemplate` com `ownerReference` apontando para o CronJob (mesmo padrão do controller automático); `suspendCronJob()` faz patch em `spec.suspend`; `deleteJob()` e `deleteCronJob()` com cascade padrão Kubernetes
- `JobsView`: botão TRASH adicionado (habilitado apenas com `WORKLOADS_JOBS_DELETE`); dialog de confirmação com aviso de cascade de Pods
- `CronJobsView`: 3 novos botões na coluna de ações — Trigger (ícone `FAST_FORWARD`, dialog de confirmação, navega para `JobsView?cronjob=<nome>` após criação); Suspend/Resume (botão único que alterna ícone PAUSE/PLAY conforme estado atual, sem dialog — operação reversível); Delete (ícone TRASH, aviso reforçado quando `active > 0`)
- `UiConstants.actionsColumnWidth(n)`: nova função que calcula `n × 48px` — elimina números mágicos de largura de coluna de ações; aplicada em todas as 10 views com coluna de ações
- `UserManagementView`: Jobs e CronJobs convertidos de `PermissionNode` para `SubGroupNode`; desmarcar View desmarca automaticamente as ações filhas
- `CONTEXT.md`: termos `Trigger` e `Suspend` adicionados ao glossário; definições de `Job` e `CronJob` atualizadas para mencionar operações de escrita

### Sprint 41 — Workloads — Jobs/CronJobs: navegação contextual para logs
- `PodInfo` record: campo `jobName` adicionado; populado de `metadata.labels["job-name"]` no `WorkloadService.listPods()` — padrão Kubernetes adicionado automaticamente pelo Job controller
- `PodsView`: lê query param `?job=<name>`; aplica filtro `jobName == param` no `dataProvider`; exibe banner dismissível "Showing pods for Job: `<name>` ×" acima do grid enquanto filtro ativo; clicar em × limpa o filtro e exibe todos os pods
- `JobsView`: botão "Ver Pods" (ícone `LIST`) adicionado à coluna de ações — navega para `workloads/pods?job=<name>`; lê query param `?cronjob=<name>` e pré-popula o campo de filtro Owner
- `CronJobsView`: botão "Ver Jobs" (ícone `PLAY`) adicionado à coluna de ações antes do Manifest — navega para `workloads/jobs?cronjob=<name>`
- Caminho completo: CronJobsView → [Ver Jobs] → JobsView?cronjob → [Ver Pods] → PodsView?job → [Logs] → PodLogsView; sem novas Permissions — reusa `WORKLOADS_JOBS_VIEW`, `WORKLOADS_CRONJOBS_VIEW`, `WORKLOADS_PODS_VIEW`

### Sprint 40 — Workloads — Jobs e CronJobs (read-only)
- `Permission.WORKLOADS_JOBS_VIEW` e `Permission.WORKLOADS_CRONJOBS_VIEW` adicionados ao enum; incluídos em `allPermissions()`, `operatorPermissions()` e `viewerPermissions()` (read-only para todos os perfis)
- Migration `V13__add_jobs_cronjobs_permissions.sql`: concede ambas as permissões a todos os usuários que já possuem `WORKLOADS_PODS_VIEW`
- `JobInfo` e `CronJobInfo` records criados em `kubernetes/dto/`
- `WorkloadService.listJobs()`: lista Jobs via `client.batch().v1().jobs()`; status derivado de `.status.conditions` (Complete/Failed) e `spec.suspend` (Suspended); duration calculada de `startTime`/`completionTime`; owner extraído de `ownerReferences[kind=CronJob]`
- `WorkloadService.listCronJobs()`: lista CronJobs via `client.batch().v1().cronjobs()`; active count de `.status.active`; lastScheduleTime formatado como idade relativa
- `JobsView`: rota `/workloads/jobs`; colunas Name, Status (badge), Completions, Duration, Age, Owner, Manifest; badge `success`/`error`/`contrast` por estado; filtros por Name e Owner
- `CronJobsView`: rota `/workloads/cronjobs`; colunas Name, Schedule, Suspend (badge `contrast`), Active, Last Schedule, Age, Manifest; filtro por Name
- `MainLayout.buildWorkloadsNavItem()`: sub-itens Jobs e CronJobs adicionados após Pods, protegidos por `WORKLOADS_JOBS_VIEW` e `WORKLOADS_CRONJOBS_VIEW`
- `ManifestService`: tipos `"job"` e `"cronjob"` adicionados ao switch
- `UserManagementView.PermissionTreePanel`: Jobs e CronJobs adicionados à TreeView; `SubGroupNode` introduzido para agrupar Scale/Restart/Rollback como filhos de Deployments — desmarcar View desmarca as ações filhas; `GroupNode` refatorado para separar leaves (lógica) de displayItems (renderização)
- `CONTEXT.md`: definição de `Workload` atualizada; termos `Job` e `CronJob` adicionados ao glossário

### Sprint 39 — Workloads — Deployment Rollback (Rollout Undo)
- `Permission.WORKLOADS_DEPLOYMENTS_ROLLBACK` adicionado ao enum; incluído em `allPermissions()` e `operatorPermissions()`, ausente em `viewerPermissions()`
- Migration `V12__add_deployment_rollback_permission.sql`: concede `WORKLOADS_DEPLOYMENTS_ROLLBACK` a todos os usuários que já possuem `WORKLOADS_DEPLOYMENTS_RESTART` (ADMIN e OPERATOR)
- `WorkloadService.rolloutUndoDeployment()`: chama `rolling().undo()` via Fabric8 dentro de `try-with-resources`; lança `KubernetesOperationException` em falha
- `DeploymentsView`: botão "Rollout Undo" (ícone `REPLY`) adicionado à coluna de ações, desabilitado sem `WORKLOADS_DEPLOYMENTS_ROLLBACK`; dialog de confirmação antes de executar; notificação de sucesso/erro em `BOTTOM_END`; largura da coluna de ações ajustada de 200px para 240px

### Sprint 37 — RBAC — controle de acesso por role e gerenciamento de usuários
- `SecurityUtils`: helper estático `isViewer()` e `isAdmin()` lendo das Spring Security authorities — sem query ao banco
- `ClustersView`: botões "Add Cluster" e "Remove" não renderizados para VIEWER
- `DeploymentsView`: botões Scale e Restart não renderizados para VIEWER; largura da coluna de ações ajustada por role
- `HorizontalScalerView`: botão Edit não renderizado para VIEWER; largura da coluna de ações ajustada por role
- `UserManagementView`: rota `/users` com `@RolesAllowed("ADMIN")` — grid de usuários (username, email, role, status, criado em), dialog "Add User" com validação inline por campo, botão "Deactivate" por linha com proteção contra auto-desativação
- `UserService.deactivateUser()` e `findAll()` adicionados
- `MainLayout`: item "Users" no sidebar renderizado apenas para ADMIN, com link real para `UserManagementView`
- `AccessDeniedView`: handler de `AccessDeniedException` que redireciona para o dashboard — evita `NotFoundException` ao navegar para rota restrita

### Sprint 36 — UX — Drawer width no banco, tema dark/light, poll interval do PodLog
- Migration `V9__add_drawer_width_to_users.sql`: coluna `drawer_width_px INTEGER` nullable adicionada à tabela `users`
- Migration `V10__add_theme_to_users.sql`: coluna `theme VARCHAR(10) NOT NULL DEFAULT 'DARK'` adicionada à tabela `users`
- `User.java`: campos `drawerWidthPx` e `theme` adicionados
- `UserService`: métodos `findDrawerWidth/updateDrawerWidth` e `findTheme/updateTheme` adicionados
- `MainLayout`: largura do drawer lida do banco em `onAttach` e passada ao JS; ao soltar o mouse, `@ClientCallable saveDrawerWidth()` persiste no banco (localStorage removido); tema lido do banco via `applyTheme()` no `onAttach`; default DARK quando sem preferência
- `PlatformSettingsView`: card "Appearance" com `RadioButtonGroup` (Dark / Light) — persiste no banco e aplica na UI sem recarregar
- `PodLogsView`: `Select<Integer>` com opções 1s / 3s / 5s / 10s (default 3s) adicionado à toolbar com label "Poll:" — constante hardcoded removida

### Sprint 35 — Platform Settings — tela de configurações globais
- `PlatformSettingsView` criada em `/settings`: tela de configurações da plataforma GreenCap (não de recursos Kubernetes); layout em cards por seção
- Card "Refresh" com `ComboBox<RefreshInterval>`: persiste preferência do usuário no banco (`users.refresh_interval_seconds`) via `UserService`
- Migration `V8__add_refresh_interval_to_users.sql`: coluna `refresh_interval_seconds INTEGER` nullable adicionada à tabela `users`
- Auto-refresh removido da navbar: intervalo lido do banco no `onAttach` do `MainLayout`; `localStorage` eliminado para essa preferência
- Item "Settings" no sidebar habilitado e apontando para `PlatformSettingsView`; "Users" permanece desabilitado
- `PlatformSettings` adicionado ao `CONTEXT.md` como preferências de usuário persistidas no banco
- Candidatos para próxima sprint: largura do drawer (localStorage → banco), tema dark/light (hoje fixo), intervalo de poll do PodLog (hoje hardcoded)

### Sprint 34 — UX — Melhorias de navbar, dashboard e ReplicaSets
- Auto-refresh combobox movido para ao lado do seletor de namespace, com label "Auto refresh:"; sempre visível na navbar independente de cluster ativo
- `DashboardView` implementa `BeforeEnterObserver`: dados recarregados ao trocar namespace (equivalente ao comportamento das listing views)
- `WorkloadService.listReplicaSets()`: ativos (`desired > 0`) sempre retornados; inativos filtrados para os criados nas últimas 24h; resultado ordenado por ativos primeiro; lógica na camada de serviço sem mudança no DTO

### Sprint 33 — Observabilidade — Dashboard de namespace
- `DashboardView` reescrita: cards de cluster removidos, substituídos por visão escopada ao namespace ativo
- 7 cards de contagem de recursos clicáveis: Deployments, Pods, Services, ConfigMaps, Secrets, Volume Claims, HorizontalScalers
- 2 KPI cards de uso total de CPU e Memória via `ObservabilityService.listPodMetrics()`; exibe "N/A" se metrics-server indisponível
- Formatação automática: millicores→cores (≥1000m), MiB→GiB (≥1024 MiB)
- Estado vazio com mensagem de orientação e botão para Clusters quando nenhum cluster está ativo
- "Dashboard" movido da seção PROJECT para OBSERVABILITY no drawer de navegação
- `DashboardView` implementa `Refreshable` (suporte ao auto-refresh do navbar)

### Sprint 1 — Setup + Auth + Login
- Projeto Gradle (Kotlin DSL), Java 21, Spring Boot 3.3, Vaadin Flow 24
- `LoginView` + `SecurityConfig` (extends `VaadinWebSecurity`)
- `UserService` implementando `UserDetailsService`
- `DataInitializer`: cria `admin/admin` no primeiro startup
- Flyway migrations: `users`, `clusters`, `audit_logs`
- `MainLayout` (AppLayout + SideNav + logout)

### Sprint 2 — Conexão de Clusters
- `EncryptionService`: kubeconfig encriptado com AES via Spring Security Crypto
- `ClusterService`: adiciona cluster, testa conexão (Fabric8), persiste status
- `ClustersView`: grid de clusters + dialog de adição (upload `.yaml`/`.yml` ou paste)
- `KubernetesClientFactory`: recebe kubeconfig plaintext (desacoplado da entidade)

### Sprint 3 — Visualização de Workloads
- `ClusterContext` (`@VaadinSessionScope`): cluster e namespace ativos na sessão
- `WorkloadService`: lista pods e deployments via Fabric8
- `NamespaceService`: lista namespaces
- `WorkloadsView`: seletor de cluster + namespace, TabSheet (Pods | Deployments | Namespaces), badges de status

### Sprint 4 — Estabilização + Ambiente Local
- `apiUrl` removido da entidade `Cluster` — kubeconfig é fonte única de verdade (ADR-0001, migration V4)
- `docker-compose.dev.yml` com PostgreSQL para desenvolvimento local
- `DashboardView`: cards com contagem por `ConnectionStatus`, badge colorido, navegação para `ClustersView`
- `KubeconfigValidator`: detecta certs por caminho no YAML e bloqueia o salvar com instrução de correção
- Filtro de extensão removido do `Upload` — aceita arquivo `config` sem extensão

### Sprint 5 — Redesign de Layout + UX
- Dark theme fixo via Lumo (`getElement().setAttribute("theme", Lumo.DARK)`)
- Logo `greencap.png` no topo da sidebar
- Cluster ativo exibido abaixo do logo (atualizado por `AfterNavigationObserver`)
- Sidebar com 3 seções: VISÃO GERAL, OBSERVABILIDADE, CONFIGURAÇÃO
- Itens futuros visíveis e acinzentados (desabilitados via `pointer-events: none`)
- `SecurityConfig` liberando `/greencap.png` para acesso público
- Ação de remover cluster com dialog de confirmação (`ClusterService.deleteCluster()`)

### Sprint 6 — Login, Logout + UX de autenticação
- Dark theme aplicado na `LoginView`
- Logo `greencap.png` centralizada acima do formulário (140px)
- Logout via invalidação da sessão HTTP (`WrappedSession.invalidate()`) — sem GET para `/logout`

### Sprint 7 — Cluster Atual por Sessão
- Radio button (coluna "Ativo") no grid de `ClustersView` — seleção imediata define cluster ativo
- `ClusterContext` atualizado com cluster selecionado + namespace resetado para `"default"`
- Remoção do cluster ativo limpa `ClusterContext` automaticamente
- Navbar superior exibe `Cluster: <nome> <badge ConnectionStatus>`; "Nenhum cluster ativo" quando vazio
- Navbar atualiza imediatamente ao selecionar o cluster (sem precisar navegar)
- `WorkloadsView` usa `ClusterContext` diretamente — sem combobox de cluster; aviso inline com botão de navegação quando sem cluster ativo
- Aba "Namespaces" removida de `WorkloadsView` (redundante com o combobox de namespace)
- Cluster ativo persistido por usuário no banco (`active_cluster_id` em `users`) — restaurado automaticamente após login
- `@EqualsAndHashCode(of = "id")` adicionado à entidade `Cluster`
- Foco automático no campo Nome ao abrir dialog de novo cluster
- Hint do textarea de kubeconfig reforça uso de `kubectl config view --flatten --minify`
- Migration `V5`: normaliza valores de `ClusterProvider` para `Kubernetes`/`OpenShift`
- Migration `V6`: adiciona `active_cluster_id` em `users`
- Correção: `decrypt()` movido para dentro do `try-catch` em `NamespaceService` e `WorkloadService`
- `ClusterProvider` enum renomeado para `Kubernetes`/`OpenShift` (sem uppercase)

### Sprint 8 — Refinamento de Navegação + Workloads
- `WorkloadsView` dividida em `PodsView` (`/workloads/pods`) e `DeploymentsView` (`/workloads/deployments`)
- Menu lateral "Workloads" vira item pai colapsável com sub-itens Pods e Deployments
- Item "Namespaces" renomeado para "Topologia" (placeholder desabilitado para sprint futura)
- Item "Deploys" removido do menu (substituído pelo sub-menu de Workloads)
- Namespace selector (`ComboBox`) movido da toolbar de `WorkloadsView` para a navbar global do `MainLayout`
- Namespace selector oculto quando não há cluster ativo; visível e persistente após ativar cluster
- Namespaces recarregados apenas quando o cluster muda — sem chamadas redundantes ao Kubernetes API
- Ao trocar cluster: namespace zera, lista recarrega, seleciona `default` (ou primeiro disponível)
- Trocar namespace na navbar re-navega para a view ativa, disparando `beforeEnter` e recarregando dados

### Sprint 9 — Rede, Configuração e Demo (em andamento)

- `samples/greencap-demo/` — aplicação 3-tier demo (nginx + httpbin + redis) com manifests Kubernetes cobrindo: Namespace, Deployments, Services (ClusterIP e NodePort), ConfigMap, Secret (Opaque), PVC e HPA
- `create.sh` — habilita `metrics-server` via minikube addon e aplica todos os manifests em ordem; aguarda rollout dos Deployments
- `delete.sh` — remove o namespace `greencap-demo` e todos os recursos filhos
- `CONTEXT.md` — novos termos: `Service`, `ConfigMap`, `Secret`, `Rede`, `Configuração`, `Topologia` (futuro — grafo animado de objetos do namespace e suas relações)
- Sidebar: grupos colapsáveis Rede (Services) e Configuração (ConfigMaps, Secrets) adicionados ao `MainLayout`
- `ServicesView` (`/networking/services`): grid com nome, tipo (badge), clusterIP, portas, namespace, idade
- `ConfigMapsView` (`/config/configmaps`): grid com nome, contagem de keys, namespace, idade — valores nunca expostos
- `SecretsView` (`/config/secrets`): grid com nome, tipo (badge), contagem de keys, namespace, idade — valores nunca decodificados
- `H3` de título adicionado em todas as views (Pods, Deployments, Services, ConfigMaps, Secrets)
- Validado manualmente com cluster minikube e namespace `greencap-demo`

### Sprint 10 — UI Language Standardization

- Padronização de todo texto visível ao usuário para inglês: labels, botões, mensagens, notificações e exceções
- `buildNoClusterMessage()` extraído para `UiConstants` — eliminando duplicação em 5 views
- Sidebar renomeado: OVERVIEW / OBSERVABILITY / SETTINGS + menu "Parameters" (era "Configuração")
- `CONTEXT.md` atualizado: `Networking` e `Parameters` como termos canônicos (substituindo `Rede` e `Configuração`)
- Issues: 01 refactor UiConstants · 02 MainLayout · 03 Workloads views · 04 Networking/Parameters views · 05 ClustersView · 06 exception messages
- Fix pós-testes: cards do Dashboard traduzidos + largura da coluna Active em ClustersView ajustada
- Validado manualmente com cluster minikube e namespace greencap-demo

### Sprint 22 — UX: Remoção de Namespace redundante + Filtros por coluna

- Coluna Namespace removida das 9 views namespace-scoped: Pods, Deployments, ReplicaSets, Services, ConfigMaps, Secrets, HorizontalScaler, PVC, Metrics
- Filtros por coluna adicionados em 12 views via `ListDataProvider` + `HeaderRow`
- Padrão: `allItems` + `dataProvider.setFilter()` + `dataProvider.refreshAll()` — client-side, sem chamadas extras à API
- Filtros mantidos ativos entre reloads via refresh button
- Validado manualmente com aceite do usuário

### Sprint 21 — UX: Links entre recursos + Sidebar redimensionável

- `PersistentVolumesView`: coluna Claim clicável — troca namespace ativo + navega para PersistentVolumeClaimsView
- `ReplicaSetView`: coluna Owner clicável — navega para DeploymentsView
- `HorizontalScalerView`: coluna Target clicável — navega para DeploymentsView
- Valor `—` não clicável em todas as três views
- Sidebar redimensionável via alça na borda direita do drawer (drag & drop)
- Largura persistida em `localStorage` com chave `greencap-drawer-width`
- Limites: mínimo 180px, padrão 240px, máximo 400px
- Implementação via shadow DOM direto: `drawerPart.width`, `navbarPart.left`, `contentPart.marginInlineStart`
- Validado manualmente com aceite do usuário

### Sprint 20 — Infrastructure: PersistentVolumes + StorageClasses

- Termos canônicos `PersistentVolume`, `StorageClass` e `Infrastructure` adicionados ao `CONTEXT.md`
- `PersistentVolumeInfo` record DTO: name, status, capacity, accessMode, reclaimPolicy, storageClass, claim, age
- `StorageClassInfo` record DTO: name, provisioner, reclaimPolicy, volumeBindingMode, allowVolumeExpansion, age
- `StorageService`: métodos `listPersistentVolumes()` e `listStorageClasses()` — ambos cluster-scoped, sem filtro de namespace
- `PersistentVolumesView` (`/infrastructure/pvs`): grid read-only com badge de status + coluna Claim (`namespace/name`)
- `StorageClassesView` (`/infrastructure/storageclasses`): grid read-only sem badge
- Badges PV: `Available` → success, `Bound/Released/Terminating` → contrast, `Failed` → error
- `ManifestService`: cases `persistentvolume` e `storageclass` adicionados
- `MainLayout`: item pai "Infrastructure" em SETTINGS com sub-itens "Persistent Volumes (PV)" e "Storage Classes"
- Validado manualmente com aceite do usuário

### Sprint 19 — Storage: PersistentVolumeClaims

- Termos canônicos `PersistentVolumeClaim` e `Storage` adicionados ao `CONTEXT.md`
- `PersistentVolumeClaimInfo` record DTO: name, namespace, status, capacity, accessMode, storageClass, age
- `StorageService.listPersistentVolumeClaims()`: Fabric8 `client.persistentVolumeClaims()`, status `Terminating` derivado de `metadata.deletionTimestamp`
- `PersistentVolumeClaimsView` (`/storage/pvcs`): grid read-only com badge de status colorido + ícone Manifest
- Badges: `Bound` → success, `Pending` → contrast, `Terminating` → contrast, `Lost` → error
- `ManifestService`: case `persistentvolumeclaim` adicionado
- `MainLayout`: seção Storage com sub-item "Volume Claims (PVC)" posicionado após Parameters
- `samples/greencap-demo/manifests/03-pvc.yaml`: capacidade atualizada para `2Gi`
- Validado manualmente com aceite do usuário

### Sprint 18 — Workloads: ReplicaSets

- Termo canônico `ReplicaSet` adicionado ao `CONTEXT.md`; `Workload` expandido para incluir ReplicaSet; `_Avoid_: ReplicaSet` removido de Deployment
- `ReplicaSetInfo` record DTO: name, namespace, owner, desired, ready, age
- `WorkloadService.listReplicaSets()`: owner extraído de `ownerReferences[kind=Deployment]`, órfãos exibem `—`
- `ReplicaSetView` (`/workloads/replicasets`): grid read-only com badge `ready/desired` colorido + ícone Manifest
- `ManifestService`: case `replicaset` adicionado
- `MainLayout`: ReplicaSets adicionado em Workloads entre Deployments e Pods
- Validado manualmente: rollout do deployment `frontend` no namespace `greencap-demo` gerou novos ReplicaSets visíveis com histórico e coluna Owner correta

### Sprint 17 — Auto Scaling: HorizontalScaler (HPA)

- Termo canônico `HorizontalScaler` adicionado ao `CONTEXT.md` (evita HPA, AutoScaler, HorizontalPodAutoscaler)
- `HorizontalScalerInfo` record DTO: name, namespace, target, minReplicas, maxReplicas, currentReplicas, metrics, age
- `AutoScalingService.listHorizontalScalers()`: Fabric8 `autoscaling().v2()`, resumo de métricas (ex: `cpu: 45%/80%`)
- `HorizontalScalerView` (`/autoscaling/horizontalscalers`): grid read-only com badge `current/max` colorido + ícone Manifest
- `ManifestService`: case `horizontalscaler` adicionado
- `MainLayout`: item colapsável Auto Scaling > Horizontal Scaler em PROJECT, posicionado após Workloads
- Validado manualmente com aceite do usuário

### Sprint 16 — UX pós-login com cluster inacessível

- `KubernetesClientFactory`: timeouts hardcoded com constantes legíveis — `CONNECTION_TIMEOUT_MS = 5s`, `REQUEST_TIMEOUT_MS = 10s`
- `ClusterService.markAsDisconnectedIfConnected()`: transita `CONNECTED → DISCONNECTED` ao detectar falha
- `MainLayout.loadNamespacesForCluster()`: executa em virtual thread; captura `KubernetesOperationException`, chama `markAsDisconnectedIfConnected`, esconde namespace selector, exibe notificação de erro no `BOTTOM_END`
- Faixa de aviso (`clusterWarningBanner`) na segunda linha da navbar: visível quando cluster inacessível ou nenhum cluster configurado
- Itens de menu dependentes de cluster (Dashboard, Workloads, Networking, Parameters, Events, Metrics) desabilitados via `opacity: 0.4` + `pointer-events: none` quando cluster inacessível
- Settings › Clusters permanece sempre clicável para permitir correção da conexão
- ADR-0002 documentado: estratégia de timeout curto + falha rápida
- Validado manualmente: resposta em ≤ 10s com minikube parado; fluxo normal sem regressão com minikube rodando

### Sprint 15 — Visualização de Manifest (YAML)

- `ManifestService`: busca e serializa YAML via Fabric8 `Serialization.asYaml()` para pod, deployment, service, configmap, secret
- `ManifestView`: página `/yaml/{resourceType}/{namespace}/{name}` com YAML em `<pre>` monospace, botão Back e título com tipo/nome do recurso
- Coluna de ação com ícone `CODE` (tamanho `UiConstants.ICON_SIZE`) em todas as 5 views de listagem (Pods, Deployments, Services, ConfigMaps, Secrets)
- Bug fix: trocar namespace com ManifestView ativa dispara `go(PREVIOUS_PAGE)` ao invés de recarregar a view, evitando YAML desatualizado
- `CONTEXT.md`: termo `Manifest` adicionado ao glossário
- Validado manualmente com aceite do usuário

### Sprint 14 — Persistência do Namespace ativo

- Campo `activeNamespace` (String) adicionado à entidade `User`
- Migration `V7__add_active_namespace_to_users.sql`: `ALTER TABLE users ADD COLUMN active_namespace VARCHAR(255)`
- `UserService.updateActiveNamespace()` e `findActiveNamespace()` seguindo padrão de `activeCluster`
- `MainLayout`: ao trocar namespace no ComboBox, persiste via `updateActiveNamespace`
- `MainLayout`: no login, restaura `activeNamespace` do banco antes de carregar a lista de namespaces
- Fallback silencioso: namespace salvo → "default" → primeiro da lista (lógica pré-existente)
- Validado: compilação e testes passando

### Sprint 13 — Observabilidade: Metrics + UX global

- `PodMetricInfo` record DTO com campos: name, namespace, cpuMillicores, memoryMiB
- `ObservabilityService.listPodMetrics()`: usa `client.top().pods().metrics(namespace)`, agrega containers por pod, ordena por CPU desc
- `MetricsView` (`/observability/metrics`): grid com CPU (ex: "250m") e Memory (ex: "128Mi"), colunas sortáveis e redimensionáveis
- Menu OBSERVABILITY: item "Metrics" ativado
- `CONTEXT.md`: termo `PodMetric` adicionado ao glossário
- UX global: colunas redimensionáveis em todas as views (Pods, Deployments, Services, ConfigMaps, Secrets, Clusters, Events, Metrics)
- Botão de refresh no canto superior direito de todas as listagens via `UiConstants.buildSectionHeader()`
- Notificação "Data updated" apenas em refresh bem-sucedido (`BooleanSupplier`)
- `UiConstants.ICON_SIZE = "28px"` — constante centralizada usada em todos os ícones de ação
- Vaadin Copilot desabilitado em dev via JVM system property no `bootRun`
- Validado manualmente com namespace `greencap-demo`

### Sprint 12 — Observabilidade: Events

- `EventInfo` record DTO com campos: type, reason, involvedObject, message, count, age
- `ObservabilityService.listEvents()`: lista eventos via `client.v1().events()` (core/v1), ordenados por `lastTimestamp` decrescente
- `EventsView` (`/observability/events`): grid com colunas redimensionáveis, badge Normal=verde/Warning=vermelho, Message com word-wrap
- Menu OBSERVABILITY: item "Logs" renomeado e ativado como "Events"
- `CONTEXT.md` atualizado: termo `Event` adicionado ao glossário
- Fix: namespace não resetava para "default" no F5 — `MainLayout` agora preserva o namespace do `ClusterContext` se ainda válido
- Validado manualmente com namespace `greencap-demo`

### Sprint 11 — UI Polish — ícones e navegação

- Ícones de ação (testar conexão e remover) em `ClustersView` aumentados: `LUMO_ICON` + ícone SVG em `28px`
- Seção "OVERVIEW" do menu lateral renomeada para "PROJECT"
- Duração das notificações aumentada de 4s para 6s (`UiConstants.NOTIFICATION_DURATION_MS`)
- Mensagem de teste de conexão corrigida: "Connection to X successful" (era "OK")
- Issue de identidade visual (paleta de cores GreenCap) descartada nesta sprint — requer avaliação da abordagem de theming sem dependência de Node.js/Vite
- Validado manualmente pelo usuário

### Sprint 23 — Topology — visualização gráfica de objetos Kubernetes

- `TopologyGraph`, `TopologyNode`, `TopologyEdge` records DTO em `kubernetes/dto/`
- `TopologyService.buildGraph()`: busca Deployments, ReplicaSets (apenas ativos, `replicas > 0`), Pods e Services; monta nós e arestas via `ownerReferences` e label selectors
- Pods agrupados por ReplicaSet dono: 1 nó por grupo com contagem (`1 Pod` / `N Pods`) e nome base sem hash aleatório; pods órfãos exibidos individualmente
- `topology-graph.ts` (LitElement + Cytoscape.js): Web Component com layout `breadthfirst`, pan/zoom, cores por tipo de nó (Deployment=azul, ReplicaSet=roxo, Pod=verde, Service=amarelo), borda colorida por status, label com nome + tipo em duas linhas, evento `node-clicked`
- `TopologyGraphComponent.java`: wrapper server-side com `@NpmPackage(cytoscape 3.30.2)` + `@NpmPackage(@types/cytoscape 3.21.7)`
- `TopologiaView`: spinner assíncrono via virtual thread, estado vazio, erro com notificação BOTTOM_END, clique em nó navega para Manifest (grupos de Pods navegam para PodsView)
- `MainLayout`: item "Topology" ativo no sidebar (era placeholder desabilitado); `@JsModule(badge-global.js)` adicionado para garantir estilos de badge após rebuild do bundle Vite
- `CONTEXT.md`: termos `Topologia`, `TopologyGraph`, `TopologyNode`, `TopologyEdge` refinados
- `docs/adr/0003`: Cytoscape.js como motor de renderização — decisão registrada
- Validado manualmente com aceite do usuário

### Sprint 25 — Regressão de UI — labels do sidebar sem formatação

- Causa raiz: `utility-global.js` do Vaadin Lumo não estava importado no `MainLayout`, tornando ineficazes as classes `LumoUtility.FontSize`, `LumoUtility.FontWeight`, `LumoUtility.Padding` e `LumoUtility.TextColor` usadas nos labels PROJECT, OBSERVABILITY e SETTINGS
- A regressão foi exposta pela reconstrução do bundle Vite na sprint 24 (adição do Cytoscape.js), que parou de resolver o módulo implicitamente em dev mode
- Correção: adicionado `@JsModule("@vaadin/vaadin-lumo-styles/utility-global.js")` ao `MainLayout`, seguindo o mesmo padrão do `badge-global.js` adicionado na sprint 22
- Validado manualmente pelo usuário

### Sprint 24 — Topology — Drawer lateral com resumo do recurso

- `TopologyNode` enriquecido com `labels` (metadata do recurso), `readyReplicas`, `desiredReplicas` e `serviceType`
- `TopologyService`: métodos `deploymentNode`, `replicaSetNode`, `serviceNode`, `podGroupNode`, `podNode` populam os novos campos sem custo adicional (dados já disponíveis em memória durante `buildGraph`)
- `topology-graph.ts`: interface `NodeData` atualizada com os novos campos; evento `node-clicked` passa dados completos do nó (id, label, type, status, labels, réplicas, serviceType, manifestUrl); novo evento `canvas-tapped` disparado ao clicar no fundo do Cytoscape
- `TopologyNodeDrawer`: novo componente Vaadin — overlay flutuante (`position: fixed; right: 0; width: 340px`), cabeçalho com badge de status e botão X, corpo por tipo (réplicas para Deployment/ReplicaSet, contagem para grupos de Pod, tipo e selector labels para Service), labels exibidas como chips, botão "Ver YAML" ou "Ver Pods" no rodapé
- `TopologiaView`: clique no nó abre o drawer sem navegar; clicar em outro nó substitui o conteúdo; clicar no canvas fecha; X fecha explicitamente; pan e zoom não fecham o drawer
- Validado manualmente pelo usuário

### Sprint 27 — Topology: PersistentVolumeClaim no grafo

- `TopologyNode` record: campos `capacity` e `accessMode` adicionados; `serviceType` reutilizado para `storageClass` do PVC
- `TopologyService.buildGraph()`: busca PVCs do namespace; cria nós `PersistentVolumeClaim`; detecta arestas `PodGroup→PVC` e `Orphan Pod→PVC` via `spec.volumes[].persistentVolumeClaim.claimName`; PVCs isolados exibidos sem arestas
- `topology-graph.ts`: cor `#F97316` (laranja) para nós PVC; campos `capacity` e `accessMode` na interface `NodeData` e no evento `node-clicked`
- `TopologyNodeDrawer`: case PVC exibe Status, Capacity, Storage Class e Access Mode; badge `Bound`→success, `Lost`→error, `Pending/Terminating`→contrast
- `CONTEXT.md`: termos `Topologia` e `TopologyNode` atualizados para incluir PersistentVolumeClaim
- `topology-graph.ts`: nós do grafo aumentados de `100×52` para `144×76` para melhor legibilidade dos labels; `text-max-width` ajustado de `84px` para `124px` e `font-size` de `10px` para `12px`
- Validado manualmente com aceite do usuário

### Sprint 26 — Migração para repositório oficial greencapk8s

- Repositório fonte (`joseafilho/greencap-k8s-platform`) migrado para o repositório oficial da organização (`greencapk8s/greencap-k8s`)
- Branch `infra-legacy` criada no destino como backup dos arquivos de infra pré-migração
- Histórico git do destino preservado; commit de migração adicionado em cima
- Aplicação compilada e validada no novo path (`./gradlew compileJava` — BUILD SUCCESSFUL)
- Startup validado com PostgreSQL via Docker: Flyway, Spring Security e Vaadin inicializando sem erros

## Backlog

### RBAC + Polimento + Docker Final
- [ ] Controle de acesso por role (`ADMIN`, `OPERATOR`, `VIEWER`) com `@Secured` nas views
- [ ] `UserManagementView` (apenas ADMIN): criar/desativar usuários
- [ ] Página de erro customizada no Vaadin
- [ ] `Dockerfile` + `docker-compose` validados ponta a ponta
- [ ] Variável `GREENCAP_ENCRYPTION_KEY` obrigatória em produção (validação no startup)

### Sprint 30 — Auto refresh nas listing views

- Interface `Refreshable` (package-private) com método `refresh()` — contrato para views que suportam atualização automática
- Enum `RefreshInterval` com 5 opções: *No auto refresh*, *5 seconds*, *10 seconds*, *30 seconds*, *1 minute*
- 12 listing views implementam `Refreshable`: Pods, Deployments, ReplicaSets, Services, ConfigMaps, Secrets, Events, Metrics, HorizontalScaler, PersistentVolumeClaims, PersistentVolumes, StorageClasses
- `refresh()` em cada view é silencioso: sem notificação de erro, grid mantém dados anteriores em caso de falha
- `MainLayout`: `ComboBox<RefreshInterval>` adicionado à navbar (direita, antes do logout)
- Timer via `ScheduledExecutorService` (virtual threads) + `ui.access()` — dispara `refresh()` na view ativa se ela implementar `Refreshable`
- Timer reiniciado a cada navegação via `afterNavigation()`, cancelado no `DetachEvent`
- Intervalo selecionado persiste em `localStorage` (chave `greencap-auto-refresh-interval`)

### Sprint 29 — Workloads: Scale e Restart de Deployment

- Termos canônicos `Scale` e `Restart` adicionados ao `CONTEXT.md` sob `Deployment`
- `WorkloadService.scaleDeployment()`: Fabric8 `client.apps().deployments().withName(name).scale(replicas)`
- `WorkloadService.restartDeployment()`: Fabric8 `client.apps().deployments().withName(name).rolling().restart()`
- `DeploymentsView`: coluna de ações expandida com botões Scale (EXPAND) e Restart (ROTATE_RIGHT) por linha
- Scale: dialog com `IntegerField` pré-populado com `desired` atual, mín 0, máx 50, botão habilitado só se valor mudou
- Restart: dialog de confirmação com nome do Deployment, botão destrutivo em vermelho
- Sucesso: notificação `BOTTOM_END` + refresh automático do grid
- Falha: notificação de erro `BOTTOM_END`
- Scale HPA-aware: se o Deployment tem HPA associado, Scale navega para HorizontalScalerView com `?edit=<hpa-name>` e abre o dialog automaticamente
- `AutoScalingService.findHorizontalScalerForDeployment()`: localiza HPA pelo `scaleTargetRef.name`
- `AutoScalingService.updateHorizontalScaler()`: patch de min/max réplicas via Fabric8 edit
- HorizontalScalerView: botão Edit por linha + dialog com `IntegerField` min/max + leitura do query param `edit` para auto-abertura
- RBAC ignorado nesta sprint — qualquer usuário autenticado pode executar as operações

### Sprint 31 — Observabilidade: Events scoped por recurso

- `ObservabilityService.listEventsForResource()`: novo método com Fabric8 field selector (`involvedObject.name` + `involvedObject.kind`) — retorna apenas events do recurso específico, sem trazer o namespace inteiro
- `EventsDialog`: componente package-private reutilizável — Dialog com Grid (Type · Reason · Message · Count · Age), carregamento inicial + botão de refresh manual no cabeçalho (ícone `REFRESH`)
- `DeploymentsView`: botão Events (ícone `RECORDS`) adicionado na coluna de ações — abre `EventsDialog` scoped para o Deployment da linha
- `PodsView`: botão Events (ícone `RECORDS`) adicionado na coluna de ações — abre `EventsDialog` scoped para o Pod da linha
- Auto-refresh não se aplica ao dialog por design — conteúdo modal não deve mudar enquanto o usuário lê; refresh manual disponível

### Sprint 32 — Troubleshooting: PodLog viewer em página dedicada

- Termo canônico `PodLog` adicionado ao `CONTEXT.md`
- `ObservabilityService.listContainersForPod()`: lista containers de um Pod via Fabric8
- `ObservabilityService.fetchPodLogs()`: busca snapshot de log com suporte a `container`, `tailLines` e flag `previous` — retorna `Optional.empty()` quando não há log anterior (sem lançar exceção)
- `PodLogsView`: página dedicada em `logs/pod/:namespace/:name` com toolbar (container select condicional, tail select 100/500/1000, label "Lines:" ao lado, toggle "Previous container", botão Pause/Resume) e área de log `Pre` com auto-scroll via JS
- Auto-poll a cada 3s via `ScheduledExecutorService` + `ui.access()` — mesmo padrão da sprint 30; poll cancelado no `DetachEvent`
- Container select visível apenas quando o Pod tem mais de um container
- Quando `previous=true` e não há log anterior: exibe mensagem informativa em vez de erro
- `PodsView`: botão Logs (ícone `TERMINAL`) adicionado na coluna de ações — navega para a página de logs do Pod

### Sprint 28 — Dev workflow — skills greencap-run e greencap-stop
- Skills `/greencap-run` e `/greencap-stop` criados em `.claude/skills/`
- `greencap-run`: checa PostgreSQL via Docker, detecta JVM na porta 8080, sobe Spring Boot com profile dev, aguarda "Tomcat started" em `build/boot.log`
- `greencap-stop`: mata o JVM pela porta 8080 (`lsof -ti:8080`), remove `build/boot.pid`, mantém PostgreSQL vivo para restarts rápidos
- Banco de dados preservado entre sessões — só para se explicitamente solicitado
- Elimina reaprendizado do processo de startup a cada nova conversa

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
