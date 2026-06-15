# GreenCap K8s — Sprints & Demandas

> Documento vivo. Atualizar a cada sprint concluída ou nova demanda identificada.

---

## Status Geral

| Sprint | Tema | Status |
|--------|------|--------|
| 62 | User Management — treeview de permissões expansível/colapsável | ✅ Concluído |
| 63 | UX — seção GLOBAL no drawer, ícone de contexto (i) e Observability como submenu de PROJECT | ✅ Concluído |
| 64 | DevOps — pipeline GitHub Actions para validar docker-compose | ✅ Concluído |
| 65 | Infraestrutura de Demo — migrar greencap-demo para driver docker multi-node | ✅ Concluído |
| 66 | Workloads — coluna/filtro Nodes em Deployments/ReplicaSets/StatefulSets/Jobs/Pods | ✅ Concluído |
| 67 | PodsView — esconder Pods Succeeded de Jobs por padrão (toggle) | ✅ Concluído |
| 68 | Container Registry — menu Global, listagem de Repositories e Tags | ✅ Concluído |
| 69 | Fix — Container Registry: item ausente na treeview de permissões + View Tags na grid | ✅ Concluído |
| 70 | Platform Settings — auto-refresh: nova opção "3 seconds" e novo default | ✅ Concluído |
| 71 | Infraestrutura de Demo — PVC para persistir o Container Registry interno | ✅ Concluído |

---

## Backlog

> Itens sem sprint definida, organizados por prioridade (Alta, Média, Baixa).

### 🔴 Alta Prioridade

#### 🔵 Gerenciamento ativo — próximas operações de escrita

- **Atualizar imagem do Deployment (`kubectl set image`)** — patch em `spec.template.spec.containers[].image`. Requer UI para escolher o container quando o Pod tem múltiplos (multi-container) — maior complexidade de UX que as demais ações de write já implementadas.

### 🟡 Média Prioridade

#### ⚡ UX — Carregamento assíncrono nas views restantes

- **Aplicar padrão async + banner "cluster inacessível" nas views de workload** — `DeploymentsView` e `PodsView` já implementados como referência (sprint 50). Aplicar o mesmo padrão nas views restantes que fazem chamadas Kubernetes síncronas no `beforeEnter`: `ServicesView`, `ConfigMapsView`, `SecretsView`, `NodesView`, `EventsView`, `HorizontalScalerView`, `IngressView`, `JobsView`, `CronJobsView`, `ReplicaSetView`, `PersistentVolumeClaimsView`, `PersistentVolumesView`, `StorageClassesView`, `MetricsView`, `TopologiaView`. Padrão: criar `loadXxxAsync(UI ui)` com `CompletableFuture` + `UiConstants.VIRTUAL_THREADS`; adicionar `clusterErrorMessage` via `UiConstants.buildClusterUnreachableMessage()`; exibir banner e ocultar grid em caso de `KubernetesOperationException`.

#### 🟣 StatefulSet — follow-ups da Sprint 61

- **StatefulSet na Topologia** — `TopologyService`/`TopologyGraphComponent` não cobrem StatefulSet. Adicionar novo tipo de nó `StatefulSet` com edge direto `StatefulSet→Pod` (sem ReplicaSet intermediário) e edge para o headless Service via `spec.serviceName`. Avaliar também os PVCs de `volumeClaimTemplates` como nodes/edges.
- **Coluna Owner em PersistentVolumeClaimsView** — PVCs criados via `volumeClaimTemplates` de um StatefulSet seguem o padrão de nome `<template>-<statefulset>-<ordinal>`; adicionar coluna indicando o StatefulSet de origem (ou "—" para PVCs avulsos), análogo ao Owner de `ReplicaSetView`.
- **Events em StatefulSetsView** — adicionar `SelectionAction.of(VaadinIcon.RECORDS, "Events", sts -> EventsDialog.open(observabilityService, clusterContext, "StatefulSet", sts.name(), sts.namespace()))` na barra de seleção, mesmo padrão de `DeploymentsView` (injetar `ObservabilityService` no construtor).

#### 🟢 Diferencial — visão de cluster

- **Overview multi-cluster** — tela de entrada com health de todos os clusters registrados (ConnectionStatus, namespace count) antes de entrar em um específico.

#### 📦 Registry — follow-up da Sprint 68

- **Build & push de imagens para o Registry interno** — requer decisão de mecanismo de build (Docker socket no container do GreenCap vs Job/Kaniko in-cluster via Fabric8 Client vs BuildKit remoto), origem do código-fonte/Dockerfile (upload via UI, repositório git) e acompanhamento de progresso (logs em tempo real, análogo a `PodLog`). Decisão arquitetural grande — exige sessão `/grill-with-docs` dedicada antes de iniciar.

#### 🛠️ Infraestrutura de Demo — follow-up da Sprint 71

- **StorageClass com `nodeAffinity` correta no `greencap-demo`** — o `storage-provisioner` (hostpath) do minikube cria PVs sem `nodeAffinity`; em cluster multi-node, se o Pod que monta a PVC for reagendado para outro node, o diretório hostPath local fica vazio (dados "somem" mesmo com a PVC `Bound`). Descoberto na Sprint 71 ao persistir o Registry (contornado com `nodeSelector` fixo no control-plane). Solução geral mais proporcional: substituir a StorageClass default por `local-path-provisioner` (Rancher) — leve (1 pod), mas define `nodeAffinity` corretamente, resolvendo para qualquer PVC sem `nodeSelector` manual por recurso. ODF/Ceph avaliado e descartado — over-engineering para o posicionamento "plataforma leve" do GreenCap (`CONTEXT.md`).

#### 📊 Storage — visualização de uso (sprint 72, cancelada)

- **Gráfico de uso (used/free) por PVC na `PersistentVolumeClaimsView`** — demanda original: coluna com mini gráfico de pizza/donut + diálogo "View Usage" com detalhamento em GiB/%, cores por limiar (70%/90%). Sprint 72 iniciada via `/grill-with-docs` e cancelada na etapa de implementação ao descobrir limitação técnica: a fonte de dados planejada (kubelet `/stats/summary`, endpoint `/api/v1/nodes/{node}/proxy/stats/summary`) **não reporta `pvcRef`/`usedBytes`/`capacityBytes` para volumes `hostPath`** — o `volume.Metrics` não é implementado por esse plugin. Testado no `greencap-demo` (StorageClass `standard` = `k8s.io/minikube-hostpath`): nenhuma PVC (`redis-data`, `registry-storage`) aparece no `/stats/summary`, nem mesmo as montadas por Pods `Running`. Mesma limitação provavelmente afeta `local-path-provisioner` (k3s/kind), candidato do item acima. Caminho alternativo a avaliar quando retomar: `exec df`/`stat -f` no Pod que monta a PVC via Fabric8 (RBAC `pods/exec` em vez de `nodes/proxy`), funciona independente do storage backend desde que o container tenha `df` disponível.

### ⚪ Baixa Prioridade

#### 🎓 Diferencial — Onboarding e Aprendizado

> Decorre do posicionamento registrado em `CONTEXT.md` (seção "Purpose & Audience"): GreenCap como plataforma de estudos/dev/teste para PMEs. Ainda sem escopo definido — registrar como exploração futura, não compromisso de sprint.

- **Playground/Sandbox** — marcar um Cluster ou Namespace como "seguro para experimentar", possivelmente com avisos/restrições diferenciados na UI.
- **Sample Manifests** — biblioteca de YAMLs de exemplo que o usuário pode aplicar a partir do Manifest/Apply existente, para aprender padrões comuns de workloads.
- **Onboarding/Tutorial in-app** — guia introdutório dentro da própria UI para usuários iniciantes em Kubernetes.

#### 🔌 Diferencial — Integrações futuras

> Ainda sem escopo definido — registrar como exploração futura, não compromisso de sprint. Avaliar caso a caso o impacto no posicionamento "plataforma leve" (`CONTEXT.md`, seção "Purpose & Audience").

- **Helm** — visualizar/gerenciar releases Helm instaladas no cluster (charts, valores, histórico de releases).
- **Operators** — visualizar Operators instalados (CRDs, Custom Resources geridos) e seus recursos gerenciados.

---

## Sprints Concluídas

> Mostra apenas as últimas 10 sprints. Histórico completo em `docs/sprints-archive.md` (ver `docs/agents/sprint-archiving.md`).

### Sprint 71 ✅ — Infraestrutura de Demo: PVC para persistir o Container Registry interno

- `samples/greencap-demo/cluster-provision.sh`: após `minikube addons enable registry`, cria `PersistentVolumeClaim` `registry-storage` (4Gi, `kube-system`, StorageClass `standard`) e aplica `kubectl patch` (strategic merge) no `Deployment registry` adicionando `volumes`/`volumeMounts` (`/var/lib/registry`) e `nodeSelector: kubernetes.io/hostname: greencap-demo`; `kubectl rollout status deployment/registry` aguarda o rollout, mesmo padrão do wait do `ingress-nginx-controller`
- Decisão (`/grill-with-docs`): patch in-place em vez de manifest próprio — preserva `Service`/`registry-proxy` do addon; validado que não há `kube-addon-manager` rodando neste cluster (reconcile contínuo não existe em versões recentes do minikube) e que `volumes`/`volumeMounts`/`nodeSelector` sobrevivem a reexecuções de `minikube addons enable registry` (merge de 3 vias do `kubectl apply` não remove campos fora do manifest do addon)
- Achado crítico durante o teste: a StorageClass `standard` (hostpath-provisioner) cria PVs sem `nodeAffinity` — em multi-node, um Pod reagendado para outro node monta um diretório hostPath local vazio, "perdendo" os dados mesmo com a PVC `Bound`. Fix: `nodeSelector` fixa o Pod do registry no control-plane (`greencap-demo`), node estável (= nome do profile)
- `samples/greencap-demo/README.md`: nova seção "Container Registry" documentando a persistência via PVC, o caveat do `nodeSelector` (control-plane sempre existe no demo de 3 nodes) e que os dados só são perdidos com `minikube delete -p greencap-demo`
- Problema geral de `nodeAffinity` da StorageClass (afeta qualquer PVC) registrado no backlog como candidato de substituição por `local-path-provisioner`; ODF/Ceph avaliado e descartado — over-engineering para o posicionamento "plataforma leve" do GreenCap
- Validado ponta a ponta no `greencap-demo`: `cluster-provision.sh` roda do zero e idempotente (PVC `unchanged`, patch `no change`); push de imagens de teste (`greencap-demo/persistence-test:v1`, `greencap-demo/hello:v1/v2/latest`, `greencap-demo/backend:v1`) via port-forward; após `minikube stop`/`start -p greencap-demo` (restart completo do cluster), pod do registry voltou no mesmo node e os 3 repositories continuaram visíveis no catálogo e na UI "Container Registry"
- Issue: `.scratch/sprint-71/issues/01-pvc-persistencia-registry.md`

### Sprint 70 ✅ — Platform Settings: auto-refresh — nova opção "3 seconds" e novo default

- `CONTEXT.md`: entrada `PlatformSettings` atualizada — auto-refresh varia de "no auto-refresh" até 1 minuto; usuário sem preferência salva (conta nova ou que nunca abriu Platform Settings) passa a ter default de 3 segundos, escolhido pela responsividade para o público-alvo de clusters pequenos de dev/teste
- `RefreshInterval`: novo valor `THREE_SECONDS("3 seconds", 3)`, posicionado entre `NONE` e `FIVE_SECONDS`
- `PlatformSettingsView.buildRefreshCard()`: fallback do ComboBox (sem preferência salva) passa de `NONE` para `THREE_SECONDS`
- `MainLayout`: default do field `currentRefreshInterval` e fallback em `onAttach()` passam de `NONE` para `THREE_SECONDS` — auto-refresh a 3s ativo desde o login para quem nunca configurou; usuários que já salvaram explicitamente "No auto refresh" (0) ou outro valor continuam inalterados; aplicado uniformemente a todas as views `Refreshable`, sem migration Flyway (mesmo padrão do fallback de tema `"DARK"`)
- Issue: `.scratch/sprint-70/issues/01-auto-refresh-3-seconds-default.md`

### Sprint 69 ✅ — Fix: Container Registry — item ausente na treeview de permissões + ação View Tags na grid

- `UserManagementView.buildGlobalGroups()`: novo grupo "Container Registry" (`GLOBAL_REGISTRY_VIEW`) — permission introduzida na sprint 68 que não havia sido exposta na treeview de permissões (GLOBAL), mesmo padrão de grupo único do "Infrastructure"
- `RegistryView`: ação "View Tags" sai da barra de título (selection action) e passa para uma coluna de ações na própria grid (`UiConstants.addActionsColumn`, botão por linha), mesmo padrão de `JobsView` ("View Pods")
- Issue: `.scratch/sprint-69/issues/01-fix-registry-permission-treeview-view-tags.md`

### Sprint 68 ✅ — Container Registry: menu Global, listagem de Repositories e Tags

- `CONTEXT.md`: novos termos `Registry` (capacidade derivada do `Cluster`, alcançada via port-forward da API do Kubernetes para o `Service` `registry` no Namespace `kube-system` — sem entidade persistida, sem credenciais novas), `Repository` (coleção nomeada de versões de imagem) e `Tag` (referência nomeada a uma versão específica, com digest/size/created); `docs/adr/0006-registry-via-port-forward.md` documenta a decisão de alcançar o Registry via port-forward em vez de uma entidade/configuração própria
- `RepositoryInfo`/`TagInfo` (novos DTOs): `RepositoryInfo(name, tagCount)`, `TagInfo(name, digest, size, createdAt)`
- `RegistryService` (novo, `io.greencap.k8s.kubernetes`): `listRepositories(Cluster)` — port-forward para o `Service` `registry`/`kube-system` (porta `5000`, porta do container — Fabric8 `ServiceResource#portForward` encaminha direto para a porta do Pod, não resolve `targetPort`), `GET /v2/_catalog` + `/v2/<repo>/tags/list` via `java.net.http.HttpClient`; `listTags(Cluster, repository)` — para cada tag, `GET /v2/<repo>/manifests/<tag>` (digest via header `Docker-Content-Digest`, size = `config.size` + soma de `layers[].size`) e `GET /v2/<repo>/blobs/<configDigest>` (campo `created`, formatado via `NamespaceService.age(...)`); qualquer exceção (Service ausente, port-forward falha, catálogo vazio) → `log.warn` + `List.of()`, sem `KubernetesOperationException` — ausência do Registry é estado esperado, não falha de cluster
- `Permission.GLOBAL_REGISTRY_VIEW` (novo, grupo Global): incluído em `operatorPermissions()`/`viewerPermissions()`; `V22__add_registry_permission.sql` concede a todos os usuários com `GLOBAL_INFRASTRUCTURE_VIEW`
- `RegistryView` (nova): rota `registry`, item "Container Registry" no drawer GLOBAL (`MainLayout.buildRegistryNavItem()`, ícone `VaadinIcon.ARCHIVE`); grid de Repositories (Repository/Tags, filtro por nome), ação "View Tags" navega para `registry/<repository>`; estado vazio único ("No repositories found. Make sure the Service \"registry\" in the \"kube-system\" namespace is available on this Cluster.") sem distinguir Service ausente/port-forward falho/catálogo vazio
- `RegistryTagsView` (nova): rota `registry/:repository*` (wildcard para repositories com `/` no nome, ex. `greencap-demo/backend`); cabeçalho com nome do repository + botão Back para `RegistryView`; grid de Tags (Tag/Digest/Size/Created) — coluna Digest com `overflow:hidden`/`text-overflow:ellipsis`/`title` (tooltip) em vez de truncamento fixo
- `samples/greencap-demo/cluster-provision.sh`: addon `registry` habilitado junto de `metrics-server`/`ingress`; `create-demo.sh` refatorado — addons (antes espalhados entre os dois scripts) agora centralizados em `cluster-provision.sh`, `create-demo.sh` passa a só aplicar os manifests do demo; `README.md` atualizado
- Validado ponta a ponta no `greencap-demo`: addon `registry` habilitado, imagens de teste (`greencap-demo/hello` com tags `v1`/`v2`/`latest`, `greencap-demo/backend` com tag `v1`) buildadas e enviadas via port-forward + `docker push`; menu "Container Registry" lista os repositories com contagem de tags e "View Tags" exibe nome/digest/size/created corretamente
- Issues: `.scratch/sprint-68/issues/01-registry-menu-and-repository-listing.md`, `02-repository-tags-view.md`

### Sprint 67 ✅ — PodsView: esconder Pods Succeeded de Jobs por padrão (toggle)

- `CONTEXT.md`: termo `Pod` atualizado — a listagem de Pods esconde por padrão Pods de Job já concluídos (`Succeeded`), via toggle ativo por padrão; Pods filtrados por um Job específico (`?job=`) sempre aparecem, independente da fase
- `PodsView`: novo `Checkbox` "Hide completed Job pods" (marcado por padrão); novo predicado `isCompletedJobPod` (`jobName` não vazio + `phase == "Succeeded"`) combinado ao filtro existente do `ListDataProvider`, junto com Name/Status/Node e o filtro de Job — Pods `Failed` de Jobs permanecem sempre visíveis, independente do toggle
- Ao abrir via `?job=<nome>` (botão "View Pods" de `JobsView`/`CronJobsView`), o checkbox inicia desmarcado — evita grid vazia ao ver os pods de um Job já `Complete`; volta a marcado ao limpar o filtro de Job pelo `jobFilterBanner`
- Issue: `.scratch/sprint-67/issues/01-hide-completed-job-pods.md`

### Sprint 66 ✅ — Workloads: coluna/filtro Nodes em Deployments/ReplicaSets/StatefulSets/Jobs/Pods

- `CONTEXT.md`: glossário de `Deployment`, `ReplicaSet`, `StatefulSet` e `Job` atualizado documentando a nova coluna "Nodes" (Nodes distintos que executam os Pods do recurso, ou "—")
- `PodNodeResolver` (novo, `io.greencap.k8s.kubernetes`): utilitário stateless que resolve os Nodes distintos cujos Pods casam com `spec.selector.matchLabels` de um Deployment/ReplicaSet/StatefulSet/Job — extraído para não inflar `WorkloadService` (já com ~455 linhas)
- `DeploymentInfo`, `ReplicaSetInfo`, `StatefulSetInfo`, `JobInfo`: novo campo `nodes` (String, comma-separated ou "—")
- `WorkloadService`: `listDeployments`/`listReplicaSets`/`listStatefulSets`/`listJobs` passam a buscar os Pods do namespace (ou de todos, se `isAllNamespaces`) e preenchem `nodes` via `PodNodeResolver.resolveNodes(...)`
- `DeploymentsView`, `ReplicaSetView`, `StatefulSetsView`, `JobsView`: nova coluna "Nodes" (sempre antes de "Age") com filtro de texto, mesmo padrão de Name/Owner/Status
- `PodsView`: coluna "Node" existente ganhou filtro de texto (mesmo padrão das demais)
- Validado ponta a ponta no `greencap-demo` (3 Nodes, driver docker, sprint 65): coluna Nodes/Node preenchida corretamente e filtro funcionando nas 5 views
- CronJob de exemplo `node-spread-test` (novo, `samples/greencap-demo/manifests/13-node-spread-cronjob.yaml`): roda a cada minuto no namespace `greencap-demo`, útil para observar a distribuição de Pods entre Nodes na `JobsView`/`PodsView`
- Issues: `.scratch/sprint-66/issues/01-nodes-backend.md`, `.scratch/sprint-66/issues/02-nodes-ui.md`

### Sprint 65 ✅ — Infraestrutura de Demo: migrar greencap-demo para driver docker multi-node

- `cluster-provision.sh`: `DRIVER` `virtualbox` → `docker` (auto-detectado pelo minikube no Linux com Docker instalado; elimina o bug do DHCP da rede host-only que impedia clusters multi-node de voltarem saudáveis após reboot); `NODES` `1` → `3` (control-plane + 2 workers); `CPUS=2`/`MEMORY=2048` por node (~6GB total); mensagem final corrigida de `create.sh` para `create-demo.sh`
- `add-hosts.sh`: `minikube ip` → `minikube ip -p greencap-demo`; endurecido com `set -euo pipefail` e validação de que a saída é um IPv4 antes de gravar em `/etc/hosts` — fix encontrado no aceite manual: uma corrida com `create-demo.sh` ainda em andamento gravava uma mensagem de erro literal em `/etc/hosts`
- `samples/greencap-demo/README.md` (novo): quick start, tabela de trade-offs de drivers (docker/virtualbox/kvm2), troubleshooting do bug do virtualbox e do reboot com driver docker, requisitos
- Validado ponta a ponta: provisionamento com 3 nodes via driver `docker` OK, `create-demo.sh` (rollout + addon ingress) OK, acesso a `http://greencap-demo.local` OK
- Aceite manual (reboot do host): cluster com 3 nodes volta `Running`/`OK` sem reprovisionar; com driver `docker` os containers dos nodes não religam automaticamente no boot — é necessário rodar `minikube start -p greencap-demo` manualmente, documentado no README; `http://greencap-demo.local` volta a responder em seguida sem passos adicionais
- Issue: `.scratch/sprint-65/issues/01-migrar-driver-docker-multinode.md`

### Sprint 64 ✅ — DevOps: pipeline GitHub Actions para validar docker-compose

- `.github/workflows/docker-compose-validate.yml` (novo): workflow dedicado, sem job de build/test Gradle (fora de escopo); triggers `pull_request` e `push` para `main` com `paths` filtrados (`docker-compose.yml`, `docker/**`, `.env.example`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/**`, `gradlew`, `src/**`, `.github/workflows/*.yml`)
- Steps: `actions/checkout@v4` → `cp .env.example .env` (replica o Quick Start, sem GitHub Secrets) → `docker compose up -d --build --wait --wait-timeout 120` (sem cache de build) → `curl --fail -L http://localhost:8080/` (valida porta publicada e frontend Vaadin de produção servido) → dump de `docker compose logs` em caso de falha → `docker compose down -v` sempre (`if: always()`)
- Fix encontrado na primeira execução no GitHub Actions: `.gitignore` ignorava `gradle/wrapper/gradle-wrapper.jar` — a regra `!gradle/wrapper/gradle-wrapper.jar` (seção Gradle) era sobrescrita pela regra `*.jar` declarada mais abaixo (seção Spring Boot), então o jar nunca foi commitado; checkout limpo (CI) ficava sem o jar e `./gradlew` falhava com `ClassNotFoundException: GradleWrapperMain`. Corrigido movendo a negação para depois de `*.jar` e commitando `gradle/wrapper/gradle-wrapper.jar`
- Validado ponta a ponta: push para `main` disparou o workflow, build + healthcheck (`db`/`greencap` `healthy`) + `curl http://localhost:8080/` (200, página de login Vaadin) passaram — pipeline verde
- Issue: `.scratch/sprint-64/issues/01-pipeline-validacao-docker-compose.md`

### Sprint 63 ✅ — UX: seção GLOBAL no drawer, ícone de contexto (i) e Observability como submenu de PROJECT

- `CONTEXT.md`: novos termos `Project` (UI section que agrupa Topology, Observability, Workloads, Networking, Parameters, Auto Scaling e Storage, escopada ao Namespace ativo) e `Global` (UI section que agrupa Clusters e Infrastructure, escopada ao Cluster); novo termo `Observability` (UI subsection dentro de Project — Dashboard, Events, Metrics); `Infrastructure`, `PersistentVolume`, `StorageClass`, `Node` e `Cordon` atualizados para "within Global" (era "within Settings")
- `MainLayout.buildDrawer()`: drawer reorganizado de 3 seções (PROJECT, OBSERVABILITY, SETTINGS) para 3 seções (**PROJECT, GLOBAL, SETTINGS**) — nova seção `buildGlobalNav()` agrupa `Clusters` e `Infrastructure` (movidos de SETTINGS); `buildConfiguracaoNav()` (SETTINGS) passa a conter apenas `Users` e `Settings`
- `buildNavSection(label, nav, contextTooltip)`: novo ícone `VaadinIcon.INFO_CIRCLE_O` com tooltip nativo (`title`) ao lado do cabeçalho — `NAMESPACE_CONTEXT_TOOLTIP` em PROJECT, `CLUSTER_CONTEXT_TOOLTIP` em GLOBAL, nenhum em SETTINGS
- `Permission.java`: `SETTINGS_CLUSTERS_VIEW`/`SETTINGS_CLUSTERS_WRITE`/`SETTINGS_INFRASTRUCTURE_VIEW`/`SETTINGS_INFRASTRUCTURE_CORDON` renomeados para `GLOBAL_CLUSTERS_VIEW`/`GLOBAL_CLUSTERS_WRITE`/`GLOBAL_INFRASTRUCTURE_VIEW`/`GLOBAL_INFRASTRUCTURE_CORDON`; `operatorPermissions()`/`viewerPermissions()` atualizados; `V21__rename_global_permissions.sql` migra os valores persistidos em `user_permissions`
- `ClustersView`, `NodesView`, `PersistentVolumesView`, `StorageClassesView`: referências às permissões renomeadas atualizadas
- `Observability` (Dashboard, Events, Metrics) deixou de ser seção própria do drawer e passou a ser item expansível dentro de **PROJECT**, logo após `Topology`, com ícone `VaadinIcon.EYE` e navegação padrão para `DashboardView` (mesmo padrão de `Workloads`/`Networking`); permissões `OBSERVABILITY_*` mantidas sem renomear
- `UserManagementView.PermissionTreePanel`: árvore de permissões espelha a nova estrutura — seção `GLOBAL` (grupos Clusters/Infrastructure) entre PROJECT e SETTINGS; grupo `Observability` movido para dentro da seção PROJECT, logo após `Topology`
- Issues: `.scratch/sprint-63/issues/01-secao-global-no-drawer.md`, `02-renomear-permissoes-global.md`, `03-icone-contexto-namespace-cluster.md`, `04-observability-submenu-de-project.md`

### Sprint 62 ✅ — User Management: treeview de permissões expansível/colapsável

- `UserManagementView.GroupNode` (painel `PermissionTreePanel` do diálogo de Permissões): cada grupo de topo (Workloads, Networking, Parameters, Auto Scaling, Storage, Topology, Observability, Clusters, Infrastructure, Users, Platform Settings) ganhou um chevron (`VaadinIcon.CHEVRON_DOWN`/`CHEVRON_RIGHT`, `LUMO_SMALL/TERTIARY/ICON`) ao lado do `Checkbox` do header, em uma `HorizontalLayout`; novo método `setExpanded(boolean)` alterna a visibilidade de um `itemsContainer` (`VerticalLayout`) com os itens do grupo e atualiza o ícone/tooltip — `SubGroupNode` (Deployments/StatefulSets/Jobs/CronJobs dentro de Workloads) permanece sempre expandido
- `itemsContainer` recebe `margin-left: var(--lumo-size-s)` para compensar a largura do chevron e manter o alinhamento das checkboxes filhas com o checkbox do header (mesmo `margin-left: var(--lumo-space-l)` de `PermissionNode`)
- Estado inicial de cada grupo: expandido se ≥1 das suas permissões já estiver marcada em `initial` (calculado no construtor de `GroupNode`), senão colapsado — uniforme para todos os grupos, inclusive os de 1 item (Topology, Storage, Infrastructure, Platform Settings); no diálogo "New User" (nada marcado) todos os grupos iniciam colapsados
- `Select All` / `Deselect All` (já existentes) continuam alterando apenas os checkboxes, sem afetar o collapse; novos botões `Expand All` / `Collapse All` na `bulkActions` expandem/colapsam todos os grupos de uma vez
- Issue: `.scratch/sprint-62/issues/01-permission-treeview-collapsible.md`

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
