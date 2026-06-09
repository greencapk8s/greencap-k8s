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
| 50 | Demo: cluster-provision + UX async loading | ✅ Concluído |
| 51 | Gerenciamento ativo — Delete em todas as views PROJECT | ✅ Concluído |
| 52 | Fix — Navbar não acompanha o hide do drawer | ✅ Concluído |
| 53 | Versão da plataforma visível no rodapé do drawer | ✅ Concluído |
| 54 | Manutenção — archiving de sprints.md e .scratch | ✅ Concluído |

---

## Candidatos para Próximas Sprints

Prioridade recomendada com base na evolução da plataforma (sprint 44):

### 🟢 Diferencial — visão de cluster

- **Overview multi-cluster** — tela de entrada com health de todos os clusters registrados (ConnectionStatus, namespace count) antes de entrar em um específico.

### ⚡ UX — Carregamento assíncrono nas views restantes

- **Aplicar padrão async + banner "cluster inacessível" nas views de workload** — `DeploymentsView` e `PodsView` já implementados como referência (sprint 50). Aplicar o mesmo padrão nas views restantes que fazem chamadas Kubernetes síncronas no `beforeEnter`: `ServicesView`, `ConfigMapsView`, `SecretsView`, `NodesView`, `EventsView`, `HorizontalScalerView`, `IngressView`, `JobsView`, `CronJobsView`, `ReplicaSetView`, `PersistentVolumeClaimsView`, `PersistentVolumesView`, `StorageClassesView`, `MetricsView`, `TopologiaView`. Padrão: criar `loadXxxAsync(UI ui)` com `CompletableFuture` + `UiConstants.VIRTUAL_THREADS`; adicionar `clusterErrorMessage` via `UiConstants.buildClusterUnreachableMessage()`; exibir banner e ocultar grid em caso de `KubernetesOperationException`.

### 🔧 Infraestrutura de Demo

- **Validar drivers minikube com suporte estável a multi-node** — driver `virtualbox` falha ao provisionar multi-node no Linux após reboot (DHCP do host-only network não atribui IP às VMs extras). Avaliar `--driver=docker` e `--driver=kvm2` com 3 nós: provisionar, reiniciar host e verificar que o cluster volta healthy automaticamente. Documentar driver recomendado e atualizar `cluster-provision.sh` + README de `samples/greencap-demo/`. Issue: `.scratch/sprint-50/issues/03-minikube-multinode-driver-validation.md`

### 🐳 Infraestrutura de Produção

- **`Dockerfile` + `docker-compose` validados ponta a ponta** — hoje só existe `docker-compose.yml`/`docker-compose.dev.yml` (PostgreSQL); falta um `Dockerfile` da aplicação Spring Boot e validação do stack completo via `docker-compose up`.
- **`GREENCAP_ENCRYPTION_KEY` obrigatória em produção** — `application.yaml` hoje tem fallback `dev-encryption-key-change-me-32x`; adicionar validação no startup que falha se a variável não estiver definida em profile de produção.

---

## Sprints Concluídas

> Mostra apenas as últimas 10 sprints. Histórico completo em `docs/sprints-archive.md` (ver `docs/agents/sprint-archiving.md`).

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

### Sprint 51 ✅ — Gerenciamento ativo: Delete em todas as views PROJECT
- `CONTEXT.md`: GreenCap declarado como plataforma de gerenciamento ativo (não somente leitura); `Pod` e `ReplicaSet` atualizados para incluir Delete como write operation
- `Permission.java`: 9 novos `_DELETE` adicionados (`WORKLOADS_DEPLOYMENTS_DELETE`, `WORKLOADS_REPLICASETS_DELETE`, `WORKLOADS_PODS_DELETE`, `NETWORKING_SERVICES_DELETE`, `NETWORKING_INGRESS_DELETE`, `PARAMETERS_CONFIGMAPS_DELETE`, `PARAMETERS_SECRETS_DELETE`, `AUTOSCALING_HORIZONTALSCALER_DELETE`, `STORAGE_PVC_DELETE`); `operatorPermissions()` atualizado para incluir todos; `viewerPermissions()` inalterado
- `V17__add_delete_permissions.sql`: concede os 9 novos deletes a Admin e Operator (identificados por `SETTINGS_CLUSTERS_WRITE`)
- `WorkloadService`: `deleteDeployment`, `deleteReplicaSet`, `deletePod`
- `NetworkingService`: `deleteService`, `deleteIngress`; `listServices()` enriquecido com `hasReadyEndpoints` — busca todos os Endpoints do namespace em uma chamada e deriva se cada Service tem Pods prontos
- `ConfigurationService`: `deleteConfigMap`, `deleteSecret`
- `AutoScalingService`: `deleteHorizontalScaler`; `listHorizontalScalers()` enriquecido com `targetMissing` — verifica se o Deployment alvo ainda existe
- `StorageService`: `deletePersistentVolumeClaim`
- 9 views receberam botão Delete (`TRASH`, `LUMO_ERROR`, desabilitado para Viewer): `DeploymentsView`, `ReplicaSetView`, `PodsView`, `ServicesView`, `IngressView`, `ConfigMapsView`, `SecretsView`, `HorizontalScalerView`, `PersistentVolumeClaimsView`
- `ConfirmDialog` com texto específico por recurso descrevendo consequências de cascade onde aplicável
- `HorizontalScalerView`: coluna Target exibe badge `"Not Found"` (error) quando o Deployment alvo foi deletado
- `ServicesView`: coluna Type exibe badge `"No Endpoints"` (error) quando o Service não tem Pods prontos para receber tráfego

### Sprint 50 ✅ — Demo: cluster-provision + UX async loading
- `samples/greencap-demo/cluster-provision.sh`: novo script para provisionar o cluster minikube `greencap-demo` (1 nó, 2 CPUs, 4 GiB) via driver `virtualbox`; ajuste de 3 nodes para 1 após instabilidade do driver virtualbox com multi-node (issue backlog: validar docker/kvm2)
- `samples/greencap-demo/create.sh`: corrigido para usar `-p greencap-demo` nos comandos minikube e `kubectl config use-context` automático; evita falha por perfil padrão não encontrado
- `topology-graph.ts`: nós com tamanho dinâmico baseado no label (`width: label`, `height: label`, `min-width: 144`, `min-height: 76`, padding interno); cast `as cytoscape.Css.Node` para resolver erro de tipo TS2353 no Vite build
- `KubernetesClientFactory`: retries desabilitados (`requestRetryBackoffLimit=0`); o default do Fabric8 era 10, causando timeouts de ~50s em vez dos 5–10s configurados — afeta todas as views
- `UiConstants`: novo `buildClusterUnreachableMessage()` — banner de erro com botão "Check Cluster Settings" navegando para `ClustersView`; `VIRTUAL_THREADS` executor compartilhado
- `DashboardView`: refatorado para carregamento paralelo — todos os cards (7 contadores + 2 métricas) renderizam imediatamente com estado de loading `…` e atualizam individualmente via `ui.access()` ao chegar o resultado
- `DeploymentsView` + `PodsView`: carregamento assíncrono no `beforeEnter` via `loadXxxAsync(UI ui)`; banner `clusterErrorMessage` exibido em caso de `KubernetesOperationException` com orientação ao usuário; padrão de referência para views restantes (registrado como candidato a sprint)

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
