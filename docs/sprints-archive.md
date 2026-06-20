# GreenCap K8s — Sprints Arquivadas

> Detalhamento das sprints fora da janela das últimas 10. Ordem cronológica crescente. Ver `docs/agents/sprint-archiving.md` para a regra de archiving.

---

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

### Sprint 11 — UI Polish — ícones e navegação

- Ícones de ação (testar conexão e remover) em `ClustersView` aumentados: `LUMO_ICON` + ícone SVG em `28px`
- Seção "OVERVIEW" do menu lateral renomeada para "PROJECT"
- Duração das notificações aumentada de 4s para 6s (`UiConstants.NOTIFICATION_DURATION_MS`)
- Mensagem de teste de conexão corrigida: "Connection to X successful" (era "OK")
- Issue de identidade visual (paleta de cores GreenCap) descartada nesta sprint — requer avaliação da abordagem de theming sem dependência de Node.js/Vite
- Validado manualmente pelo usuário

### Sprint 12 — Observabilidade: Events

- `EventInfo` record DTO com campos: type, reason, involvedObject, message, count, age
- `ObservabilityService.listEvents()`: lista eventos via `client.v1().events()` (core/v1), ordenados por `lastTimestamp` decrescente
- `EventsView` (`/observability/events`): grid com colunas redimensionáveis, badge Normal=verde/Warning=vermelho, Message com word-wrap
- Menu OBSERVABILITY: item "Logs" renomeado e ativado como "Events"
- `CONTEXT.md` atualizado: termo `Event` adicionado ao glossário
- Fix: namespace não resetava para "default" no F5 — `MainLayout` agora preserva o namespace do `ClusterContext` se ainda válido
- Validado manualmente com namespace `greencap-demo`

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

### Sprint 14 — Persistência do Namespace ativo

- Campo `activeNamespace` (String) adicionado à entidade `User`
- Migration `V7__add_active_namespace_to_users.sql`: `ALTER TABLE users ADD COLUMN active_namespace VARCHAR(255)`
- `UserService.updateActiveNamespace()` e `findActiveNamespace()` seguindo padrão de `activeCluster`
- `MainLayout`: ao trocar namespace no ComboBox, persiste via `updateActiveNamespace`
- `MainLayout`: no login, restaura `activeNamespace` do banco antes de carregar a lista de namespaces
- Fallback silencioso: namespace salvo → "default" → primeiro da lista (lógica pré-existente)
- Validado: compilação e testes passando

### Sprint 15 — Visualização de Manifest (YAML)

- `ManifestService`: busca e serializa YAML via Fabric8 `Serialization.asYaml()` para pod, deployment, service, configmap, secret
- `ManifestView`: página `/yaml/{resourceType}/{namespace}/{name}` com YAML em `<pre>` monospace, botão Back e título com tipo/nome do recurso
- Coluna de ação com ícone `CODE` (tamanho `UiConstants.ICON_SIZE`) em todas as 5 views de listagem (Pods, Deployments, Services, ConfigMaps, Secrets)
- Bug fix: trocar namespace com ManifestView ativa dispara `go(PREVIOUS_PAGE)` ao invés de recarregar a view, evitando YAML desatualizado
- `CONTEXT.md`: termo `Manifest` adicionado ao glossário
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

### Sprint 17 — Auto Scaling: HorizontalScaler (HPA)

- Termo canônico `HorizontalScaler` adicionado ao `CONTEXT.md` (evita HPA, AutoScaler, HorizontalPodAutoscaler)
- `HorizontalScalerInfo` record DTO: name, namespace, target, minReplicas, maxReplicas, currentReplicas, metrics, age
- `AutoScalingService.listHorizontalScalers()`: Fabric8 `autoscaling().v2()`, resumo de métricas (ex: `cpu: 45%/80%`)
- `HorizontalScalerView` (`/autoscaling/horizontalscalers`): grid read-only com badge `current/max` colorido + ícone Manifest
- `ManifestService`: case `horizontalscaler` adicionado
- `MainLayout`: item colapsável Auto Scaling > Horizontal Scaler em PROJECT, posicionado após Workloads
- Validado manualmente com aceite do usuário

### Sprint 18 — Workloads: ReplicaSets

- Termo canônico `ReplicaSet` adicionado ao `CONTEXT.md`; `Workload` expandido para incluir ReplicaSet; `_Avoid_: ReplicaSet` removido de Deployment
- `ReplicaSetInfo` record DTO: name, namespace, owner, desired, ready, age
- `WorkloadService.listReplicaSets()`: owner extraído de `ownerReferences[kind=Deployment]`, órfãos exibem `—`
- `ReplicaSetView` (`/workloads/replicasets`): grid read-only com badge `ready/desired` colorido + ícone Manifest
- `ManifestService`: case `replicaset` adicionado
- `MainLayout`: ReplicaSets adicionado em Workloads entre Deployments e Pods
- Validado manualmente: rollout do deployment `frontend` no namespace `greencap-demo` gerou novos ReplicaSets visíveis com histórico e coluna Owner correta

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

### Sprint 22 — UX: Remoção de Namespace redundante + Filtros por coluna

- Coluna Namespace removida das 9 views namespace-scoped: Pods, Deployments, ReplicaSets, Services, ConfigMaps, Secrets, HorizontalScaler, PVC, Metrics
- Filtros por coluna adicionados em 12 views via `ListDataProvider` + `HeaderRow`
- Padrão: `allItems` + `dataProvider.setFilter()` + `dataProvider.refreshAll()` — client-side, sem chamadas extras à API
- Filtros mantidos ativos entre reloads via refresh button
- Validado manualmente com aceite do usuário

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

### Sprint 24 — Topology — Drawer lateral com resumo do recurso

- `TopologyNode` enriquecido com `labels` (metadata do recurso), `readyReplicas`, `desiredReplicas` e `serviceType`
- `TopologyService`: métodos `deploymentNode`, `replicaSetNode`, `serviceNode`, `podGroupNode`, `podNode` populam os novos campos sem custo adicional (dados já disponíveis em memória durante `buildGraph`)
- `topology-graph.ts`: interface `NodeData` atualizada com os novos campos; evento `node-clicked` passa dados completos do nó (id, label, type, status, labels, réplicas, serviceType, manifestUrl); novo evento `canvas-tapped` disparado ao clicar no fundo do Cytoscape
- `TopologyNodeDrawer`: novo componente Vaadin — overlay flutuante (`position: fixed; right: 0; width: 340px`), cabeçalho com badge de status e botão X, corpo por tipo (réplicas para Deployment/ReplicaSet, contagem para grupos de Pod, tipo e selector labels para Service), labels exibidas como chips, botão "Ver YAML" ou "Ver Pods" no rodapé
- `TopologiaView`: clique no nó abre o drawer sem navegar; clicar em outro nó substitui o conteúdo; clicar no canvas fecha; X fecha explicitamente; pan e zoom não fecham o drawer
- Validado manualmente pelo usuário

### Sprint 25 — Regressão de UI — labels do sidebar sem formatação

- Causa raiz: `utility-global.js` do Vaadin Lumo não estava importado no `MainLayout`, tornando ineficazes as classes `LumoUtility.FontSize`, `LumoUtility.FontWeight`, `LumoUtility.Padding` e `LumoUtility.TextColor` usadas nos labels PROJECT, OBSERVABILITY e SETTINGS
- A regressão foi exposta pela reconstrução do bundle Vite na sprint 24 (adição do Cytoscape.js), que parou de resolver o módulo implicitamente em dev mode
- Correção: adicionado `@JsModule("@vaadin/vaadin-lumo-styles/utility-global.js")` ao `MainLayout`, seguindo o mesmo padrão do `badge-global.js` adicionado na sprint 22
- Validado manualmente pelo usuário

### Sprint 26 — Migração para repositório oficial greencapk8s

- Repositório fonte (`joseafilho/greencap-k8s-platform`) migrado para o repositório oficial da organização (`greencapk8s/greencap-k8s`)
- Branch `infra-legacy` criada no destino como backup dos arquivos de infra pré-migração
- Histórico git do destino preservado; commit de migração adicionado em cima
- Aplicação compilada e validada no novo path (`./gradlew compileJava` — BUILD SUCCESSFUL)
- Startup validado com PostgreSQL via Docker: Flyway, Spring Security e Vaadin inicializando sem erros

### Sprint 27 — Topology: PersistentVolumeClaim no grafo

- `TopologyNode` record: campos `capacity` e `accessMode` adicionados; `serviceType` reutilizado para `storageClass` do PVC
- `TopologyService.buildGraph()`: busca PVCs do namespace; cria nós `PersistentVolumeClaim`; detecta arestas `PodGroup→PVC` e `Orphan Pod→PVC` via `spec.volumes[].persistentVolumeClaim.claimName`; PVCs isolados exibidos sem arestas
- `topology-graph.ts`: cor `#F97316` (laranja) para nós PVC; campos `capacity` e `accessMode` na interface `NodeData` e no evento `node-clicked`
- `TopologyNodeDrawer`: case PVC exibe Status, Capacity, Storage Class e Access Mode; badge `Bound`→success, `Lost`→error, `Pending/Terminating`→contrast
- `CONTEXT.md`: termos `Topologia` e `TopologyNode` atualizados para incluir PersistentVolumeClaim
- `topology-graph.ts`: nós do grafo aumentados de `100×52` para `144×76` para melhor legibilidade dos labels; `text-max-width` ajustado de `84px` para `124px` e `font-size` de `10px` para `12px`
- Validado manualmente com aceite do usuário

### Sprint 28 — Dev workflow — skills greencap-run e greencap-stop
- Skills `/greencap-run` e `/greencap-stop` criados em `.claude/skills/`
- `greencap-run`: checa PostgreSQL via Docker, detecta JVM na porta 8080, sobe Spring Boot com profile dev, aguarda "Tomcat started" em `build/boot.log`
- `greencap-stop`: mata o JVM pela porta 8080 (`lsof -ti:8080`), remove `build/boot.pid`, mantém PostgreSQL vivo para restarts rápidos
- Banco de dados preservado entre sessões — só para se explicitamente solicitado
- Elimina reaprendizado do processo de startup a cada nova conversa

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

### Sprint 30 — Auto refresh nas listing views

- Interface `Refreshable` (package-private) com método `refresh()` — contrato para views que suportam atualização automática
- Enum `RefreshInterval` com 5 opções: *No auto refresh*, *5 seconds*, *10 seconds*, *30 seconds*, *1 minute*
- 12 listing views implementam `Refreshable`: Pods, Deployments, ReplicaSets, Services, ConfigMaps, Secrets, Events, Metrics, HorizontalScaler, PersistentVolumeClaims, PersistentVolumes, StorageClasses
- `refresh()` em cada view é silencioso: sem notificação de erro, grid mantém dados anteriores em caso de falha
- `MainLayout`: `ComboBox<RefreshInterval>` adicionado à navbar (direita, antes do logout)
- Timer via `ScheduledExecutorService` (virtual threads) + `ui.access()` — dispara `refresh()` na view ativa se ela implementar `Refreshable`
- Timer reiniciado a cada navegação via `afterNavigation()`, cancelado no `DetachEvent`
- Intervalo selecionado persiste em `localStorage` (chave `greencap-auto-refresh-interval`)

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

### Sprint 33 — Observabilidade — Dashboard de namespace
- `DashboardView` reescrita: cards de cluster removidos, substituídos por visão escopada ao namespace ativo
- 7 cards de contagem de recursos clicáveis: Deployments, Pods, Services, ConfigMaps, Secrets, Volume Claims, HorizontalScalers
- 2 KPI cards de uso total de CPU e Memória via `ObservabilityService.listPodMetrics()`; exibe "N/A" se metrics-server indisponível
- Formatação automática: millicores→cores (≥1000m), MiB→GiB (≥1024 MiB)
- Estado vazio com mensagem de orientação e botão para Clusters quando nenhum cluster está ativo
- "Dashboard" movido da seção PROJECT para OBSERVABILITY no drawer de navegação
- `DashboardView` implementa `Refreshable` (suporte ao auto-refresh do navbar)

### Sprint 34 — UX — Melhorias de navbar, dashboard e ReplicaSets
- Auto-refresh combobox movido para ao lado do seletor de namespace, com label "Auto refresh:"; sempre visível na navbar independente de cluster ativo
- `DashboardView` implementa `BeforeEnterObserver`: dados recarregados ao trocar namespace (equivalente ao comportamento das listing views)
- `WorkloadService.listReplicaSets()`: ativos (`desired > 0`) sempre retornados; inativos filtrados para os criados nas últimas 24h; resultado ordenado por ativos primeiro; lógica na camada de serviço sem mudança no DTO

### Sprint 35 — Platform Settings — tela de configurações globais
- `PlatformSettingsView` criada em `/settings`: tela de configurações da plataforma GreenCap (não de recursos Kubernetes); layout em cards por seção
- Card "Refresh" com `ComboBox<RefreshInterval>`: persiste preferência do usuário no banco (`users.refresh_interval_seconds`) via `UserService`
- Migration `V8__add_refresh_interval_to_users.sql`: coluna `refresh_interval_seconds INTEGER` nullable adicionada à tabela `users`
- Auto-refresh removido da navbar: intervalo lido do banco no `onAttach` do `MainLayout`; `localStorage` eliminado para essa preferência
- Item "Settings" no sidebar habilitado e apontando para `PlatformSettingsView`; "Users" permanece desabilitado
- `PlatformSettings` adicionado ao `CONTEXT.md` como preferências de usuário persistidas no banco
- Candidatos para próxima sprint: largura do drawer (localStorage → banco), tema dark/light (hoje fixo), intervalo de poll do PodLog (hoje hardcoded)

### Sprint 36 — UX — Drawer width no banco, tema dark/light, poll interval do PodLog
- Migration `V9__add_drawer_width_to_users.sql`: coluna `drawer_width_px INTEGER` nullable adicionada à tabela `users`
- Migration `V10__add_theme_to_users.sql`: coluna `theme VARCHAR(10) NOT NULL DEFAULT 'DARK'` adicionada à tabela `users`
- `User.java`: campos `drawerWidthPx` e `theme` adicionados
- `UserService`: métodos `findDrawerWidth/updateDrawerWidth` e `findTheme/updateTheme` adicionados
- `MainLayout`: largura do drawer lida do banco em `onAttach` e passada ao JS; ao soltar o mouse, `@ClientCallable saveDrawerWidth()` persiste no banco (localStorage removido); tema lido do banco via `applyTheme()` no `onAttach`; default DARK quando sem preferência
- `PlatformSettingsView`: card "Appearance" com `RadioButtonGroup` (Dark / Light) — persiste no banco e aplica na UI sem recarregar
- `PodLogsView`: `Select<Integer>` com opções 1s / 3s / 5s / 10s (default 3s) adicionado à toolbar com label "Poll:" — constante hardcoded removida

### Sprint 37 — RBAC — controle de acesso por role e gerenciamento de usuários
- `SecurityUtils`: helper estático `isViewer()` e `isAdmin()` lendo das Spring Security authorities — sem query ao banco
- `ClustersView`: botões "Add Cluster" e "Remove" não renderizados para VIEWER
- `DeploymentsView`: botões Scale e Restart não renderizados para VIEWER; largura da coluna de ações ajustada por role
- `HorizontalScalerView`: botão Edit não renderizado para VIEWER; largura da coluna de ações ajustada por role
- `UserManagementView`: rota `/users` com `@RolesAllowed("ADMIN")` — grid de usuários (username, email, role, status, criado em), dialog "Add User" com validação inline por campo, botão "Deactivate" por linha com proteção contra auto-desativação
- `UserService.deactivateUser()` e `findAll()` adicionados
- `MainLayout`: item "Users" no sidebar renderizado apenas para ADMIN, com link real para `UserManagementView`
- `AccessDeniedView`: handler de `AccessDeniedException` que redireciona para o dashboard — evita `NotFoundException` ao navegar para rota restrita

### Sprint 38 — RBAC granular — permissões por funcionalidade com TreeView
- Detalhamento não registrado em `docs/sprints.md` na época. Ver tabela "Status Geral" em `docs/sprints.md` e `git log` para o histórico de commits desta sprint.

### Sprint 39 — Workloads — Deployment Rollback (Rollout Undo)
- `Permission.WORKLOADS_DEPLOYMENTS_ROLLBACK` adicionado ao enum; incluído em `allPermissions()` e `operatorPermissions()`, ausente em `viewerPermissions()`
- Migration `V12__add_deployment_rollback_permission.sql`: concede `WORKLOADS_DEPLOYMENTS_ROLLBACK` a todos os usuários que já possuem `WORKLOADS_DEPLOYMENTS_RESTART` (ADMIN e OPERATOR)
- `WorkloadService.rolloutUndoDeployment()`: chama `rolling().undo()` via Fabric8 dentro de `try-with-resources`; lança `KubernetesOperationException` em falha
- `DeploymentsView`: botão "Rollout Undo" (ícone `REPLY`) adicionado à coluna de ações, desabilitado sem `WORKLOADS_DEPLOYMENTS_ROLLBACK`; dialog de confirmação antes de executar; notificação de sucesso/erro em `BOTTOM_END`; largura da coluna de ações ajustada de 200px para 240px

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

### Sprint 41 — Workloads — Jobs/CronJobs: navegação contextual para logs
- `PodInfo` record: campo `jobName` adicionado; populado de `metadata.labels["job-name"]` no `WorkloadService.listPods()` — padrão Kubernetes adicionado automaticamente pelo Job controller
- `PodsView`: lê query param `?job=<name>`; aplica filtro `jobName == param` no `dataProvider`; exibe banner dismissível "Showing pods for Job: `<name>` ×" acima do grid enquanto filtro ativo; clicar em × limpa o filtro e exibe todos os pods
- `JobsView`: botão "Ver Pods" (ícone `LIST`) adicionado à coluna de ações — navega para `workloads/pods?job=<name>`; lê query param `?cronjob=<name>` e pré-popula o campo de filtro Owner
- `CronJobsView`: botão "Ver Jobs" (ícone `PLAY`) adicionado à coluna de ações antes do Manifest — navega para `workloads/jobs?cronjob=<name>`
- Caminho completo: CronJobsView → [Ver Jobs] → JobsView?cronjob → [Ver Pods] → PodsView?job → [Logs] → PodLogsView; sem novas Permissions — reusa `WORKLOADS_JOBS_VIEW`, `WORKLOADS_CRONJOBS_VIEW`, `WORKLOADS_PODS_VIEW`

### Sprint 42 — Workloads — Jobs/CronJobs: operações de escrita
- `Permission`: 4 novos valores adicionados — `WORKLOADS_JOBS_DELETE`, `WORKLOADS_CRONJOBS_RUN_NOW`, `WORKLOADS_CRONJOBS_SUSPEND`, `WORKLOADS_CRONJOBS_DELETE`; `operatorPermissions()` inclui Run Now e Suspend; delete restrito a Admin
- Migration `V14__add_jobs_cronjobs_write_permissions.sql`: concede Run Now e Suspend a usuários com `WORKLOADS_CRONJOBS_VIEW`; concede delete a usuários com `SETTINGS_USERS_WRITE` (proxy de Admin)
- `WorkloadService`: 4 novos métodos — `triggerCronJob()` cria Job a partir do `spec.jobTemplate` com `ownerReference` apontando para o CronJob (mesmo padrão do controller automático); `suspendCronJob()` faz patch em `spec.suspend`; `deleteJob()` e `deleteCronJob()` com cascade padrão Kubernetes
- `JobsView`: botão TRASH adicionado (habilitado apenas com `WORKLOADS_JOBS_DELETE`); dialog de confirmação com aviso de cascade de Pods
- `CronJobsView`: 3 novos botões na coluna de ações — Trigger (ícone `FAST_FORWARD`, dialog de confirmação, navega para `JobsView?cronjob=<nome>` após criação); Suspend/Resume (botão único que alterna ícone PAUSE/PLAY conforme estado atual, sem dialog — operação reversível); Delete (ícone TRASH, aviso reforçado quando `active > 0`)
- `UiConstants.actionsColumnWidth(n)`: nova função que calcula `n × 48px` — elimina números mágicos de largura de coluna de ações; aplicada em todas as 10 views com coluna de ações
- `UserManagementView`: Jobs e CronJobs convertidos de `PermissionNode` para `SubGroupNode`; desmarcar View desmarca automaticamente as ações filhas
- `CONTEXT.md`: termos `Trigger` e `Suspend` adicionados ao glossário; definições de `Job` e `CronJob` atualizadas para mencionar operações de escrita

### Sprint 43 — Infrastructure — Nodes
- Termo `Node` adicionado ao `CONTEXT.md`: status derivado da condição `Ready`, role derivado de labels canônicos (`control-plane`/`master`), allocatable CPU e memory como campos principais
- `NodeInfo` record criado em `kubernetes/dto/` com campos: name, status, role, version, os, cpu, memory, age
- `StorageService.listNodes()`: lista nodes via `client.nodes().list()`; status derivado de `status.conditions[type=Ready]`; role de labels `node-role.kubernetes.io/control-plane` ou `master`; memory convertida de kibibytes para GiB com 1 casa decimal
- `NodesView`: rota `/infrastructure/nodes`; colunas Name · Status · Role · Version · OS · CPU · Memory · Age · Manifest; badge `success` para Ready, `error` para NotReady, `contrast` para Unknown; filtros por Name e Status; implementa `Refreshable`; protegida por `SETTINGS_INFRASTRUCTURE_VIEW` (sem nova permission, sem migration)
- `MainLayout`: sub-item `"Nodes"` com ícone `SERVER` adicionado ao grupo Infrastructure, abaixo de Storage Classes
- `ManifestService`: tipo `"node"` adicionado ao switch — `client.nodes().withName(name).get()` (cluster-scoped, namespace ignorado)

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

### Sprint 45 — Topologia: ocultar pods de Jobs/CronJobs
- `CONTEXT.md`: definição de `Topologia` ampliada com nota explicando que pods owned por Job (direto ou via CronJob) são deliberadamente excluídos — representam execuções efêmeras de tarefas finitas, não a topologia de serviço de longa duração que a view mapeia
- `TopologyService.buildGraph()`: lista de pods filtrada logo após o fetch, removendo pods cujo `ownerReferences` contenha `kind == "Job"` — antes de qualquer agrupamento por ReplicaSet
- Novo método privado `isOwnedByJob(Pod pod)`, ao lado de `ownerReplicaSetName()`
- Cobre tanto Jobs disparados manualmente quanto Jobs criados por CronJobs — o Pod sempre referencia o Job diretamente, nunca o CronJob

### Sprint 46 — UX: botão de Help em todas as views
- `HelpDialog`: novo componente estático (mesmo padrão de `EventsDialog`) — `Dialog` modal com título e parágrafos de texto explicativo, botão "Close"
- `UiConstants.buildSectionHeader`: novo parâmetro de conteúdo de ajuda; botão `VaadinIcon.QUESTION_CIRCLE` adicionado à esquerda do botão de refresh — header final: `Título — [Help] — [Refresh]`
- 16 views migradas com constantes próprias `HELP_TITLE`/`HELP_TEXT`, em inglês, explicando o que é o recurso e quais operações a tela permite (Deployments, ReplicaSets, Pods, Jobs, CronJobs, Services, Ingresses, ConfigMaps, Secrets, Horizontal Scalers, Volume Claims, Nodes, Events, Metrics, Persistent Volumes, Storage Classes)
- `TopologiaView`: botão de Help flutuante no canto superior direito do canvas (a view não usa `buildSectionHeader` por ser full-canvas), abrindo o mesmo `HelpDialog`
- Textos focados na definição do recurso e nas operações disponíveis na tela — sem menções a "somente leitura" nem ao papel do GreenCap na exibição dos dados

### Sprint 47 — Topologia: agrupamento de nós por labels part-of/component
- `CONTEXT.md`: novo termo `TopologyGroup` — container visual em torno de nós que compartilham `app.kubernetes.io/part-of` e/ou `app.kubernetes.io/component`, agrupamento aninhado (part-of por fora, component por dentro)
- `TopologyNode`: dois novos campos `partOfGroup`/`componentGroup`, derivados das labels de cada recurso (incluindo `PodGroup`, a partir do primeiro Pod do grupo, e `PersistentVolumeClaim`, a partir do próprio metadata)
- `topology-graph.ts`: renderiza os grupos como compound nodes do Cytoscape — caixa externa `part-of: <valor>`, caixa interna aninhada `component: <valor>`; nó com `component` mas sem `part-of` forma seu próprio grupo de nível externo; nó sem nenhuma das labels permanece solto, fora de qualquer caixa
- `TopologiaView`: checkbox "Group by labels" no canto superior direito do grafo, ligado por padrão — ao desligar, o grafo volta ao layout plano; texto de Help atualizado explicando o agrupamento
- Caixas são puramente visuais — sem colapsar/expandir

### Sprint 48 — Topologia: migração para layout fcose
- `TopologyGraphComponent.java`: adicionado `@NpmPackage(value = "cytoscape-fcose", version = "2.2.0")`
- `topology-graph.ts`: importado e registrado `cytoscape-fcose`; layout substituído de `breadthfirst` para `fcose` com parâmetros `nodeSeparation: 80`, `idealEdgeLength: 120`, `nodeRepulsion: 12000`, `padding: 48`; lógica de `rootIds` removida (exclusiva do breadthfirst)
- `cytoscape-fcose.d.ts`: declaração de tipos criada no frontend (pacote não tem tipos oficiais)
- `fcose` suporta compound nodes nativamente — elimina sobreposição de `TopologyGroup` que ocorria com `breadthfirst`; aplicado nos dois modos (com e sem agrupamento)

### Sprint 49 — Topologia: persistência do TopologyLayout
- `CONTEXT.md`: novo termo `TopologyLayout` — snapshot persistido do estado visual da Topologia por User + Cluster + Namespace; armazena posições dos nós e estado do toggle `groupingEnabled`; auto-save após cada drag; nós removidos são descartados na próxima gravação; nós novos são posicionados pelo fcose enquanto os conhecidos ficam fixos
- Migration `V16__add_topology_layouts.sql`: tabela `topology_layouts` com `unique(user_id, cluster_id, namespace)`, coluna `node_positions` (TEXT), `grouping_enabled` (BOOLEAN), `updated_at`
- `TopologyLayout.java` + `TopologyLayoutRepository.java`: entidade JPA e repository com `findByUserIdAndClusterIdAndNamespace`
- `TopologyLayoutService.java`: método `upsertLayout` — cria ou atualiza o layout salvo para o contexto; método `findLayout` para leitura
- `TopologyGraphComponent.java`: injeção de `TopologyLayoutService` e `UserRepository`; método `@ClientCallable saveLayout(String, boolean)` chamado pelo frontend após cada drag; `setSavedPositions()` para passar posições ao frontend; `setContext(clusterId, namespace)` para contextualizar o save
- `TopologiaView.java`: ao entrar na view, carrega o `TopologyLayout` salvo — restaura o toggle antes da renderização e passa `savedPositions` ao componente; injeção de `TopologyLayoutService` e `UserRepository`
- `topology-graph.ts`: nova property `savedPositions`; ao renderizar, constrói `fixedNodeConstraint` com os nós conhecidos (fcose os pina nas posições salvas, nós novos são posicionados livremente); listener `dragfree` dispara `_saveLayout()` com snapshot completo; mudança no toggle também dispara `_saveLayout()`

### Sprint 50 — Demo: cluster-provision + UX async loading
- `samples/greencap-demo/cluster-provision.sh`: novo script para provisionar o cluster minikube `greencap-demo` (1 nó, 2 CPUs, 4 GiB) via driver `virtualbox`; ajuste de 3 nodes para 1 após instabilidade do driver virtualbox com multi-node (issue backlog: validar docker/kvm2)
- `samples/greencap-demo/create.sh`: corrigido para usar `-p greencap-demo` nos comandos minikube e `kubectl config use-context` automático; evita falha por perfil padrão não encontrado
- `topology-graph.ts`: nós com tamanho dinâmico baseado no label (`width: label`, `height: label`, `min-width: 144`, `min-height: 76`, padding interno); cast `as cytoscape.Css.Node` para resolver erro de tipo TS2353 no Vite build
- `KubernetesClientFactory`: retries desabilitados (`requestRetryBackoffLimit=0`); o default do Fabric8 era 10, causando timeouts de ~50s em vez dos 5–10s configurados — afeta todas as views
- `UiConstants`: novo `buildClusterUnreachableMessage()` — banner de erro com botão "Check Cluster Settings" navegando para `ClustersView`; `VIRTUAL_THREADS` executor compartilhado
- `DashboardView`: refatorado para carregamento paralelo — todos os cards (7 contadores + 2 métricas) renderizam imediatamente com estado de loading `…` e atualizam individualmente via `ui.access()` ao chegar o resultado
- `DeploymentsView` + `PodsView`: carregamento assíncrono no `beforeEnter` via `loadXxxAsync(UI ui)`; banner `clusterErrorMessage` exibido em caso de `KubernetesOperationException` com orientação ao usuário; padrão de referência para views restantes (registrado como candidato a sprint)

### Sprint 51 — Gerenciamento ativo: Delete em todas as views PROJECT
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

### Sprint 52 — Fix: Navbar não acompanha o hide do drawer

- `MainLayout.initResizableDrawer()`: `applyWidth()` agora verifica `appLayout.hasAttribute('drawer-opened')` antes de aplicar o offset — usa `w + 'px'` se aberto, `'0px'` se fechado
- `MutationObserver` adicionado sobre o atributo `drawer-opened` do AppLayout: sempre que o drawer abre ou fecha, recalcula `navbarPart.style.left` e `contentPart.style.marginInlineStart` conforme o novo estado

### Sprint 53 — Versão da plataforma visível no rodapé do drawer

- `build.gradle.kts`: `springBoot { buildInfo() }` registra o task `bootBuildInfo` — gera `META-INF/build-info.properties` com a versão do projeto; disponível como bean `BuildProperties` em runtime
- Formato de versão adotado: `v{major}.{minor}.{patch}-rc` para release candidates, `v{major}.{minor}.{patch}` para releases finais; controlado manualmente no `build.gradle.kts`; versão inicial: `0.1.53-rc`
- `MainLayout`: `BuildProperties` injetado via construtor; `buildDrawer()` refatorado para separar nav content em `Scroller` + `VerticalLayout` externo com `expand(scroller)` para empurrar o rodapé ao fundo
- `buildVersionFooter()`: `Div` centralizado com `Span` `v{version}` em `FontSize.XXSMALL` + `TextColor.TERTIARY`, fixado no fundo do drawer em todas as páginas

### Sprint 54 — Manutenção: archiving de sprints.md e .scratch

- `docs/agents/sprint-archiving.md` (novo): documenta a regra de archiving — "Sprints Concluídas" mantém só as últimas 10 sprints, restante vai para `docs/sprints-archive.md`; `.scratch/sprint-N/` antigos vão para `.scratch/archive/sprint-N/`; executado na etapa 6 (Fechamento) do fluxo de sprint
- `CLAUDE.md`: referência ao novo doc em "Agent skills"; etapa 6 do fluxo de sprint passa a citar a verificação de archiving
- `docs/agents/issue-tracker.md`: nota sobre `.scratch/archive/`
- `docs/sprints-archive.md` (novo): detalhamento das sprints 1-43 em ordem cronológica crescente, migrado de `docs/sprints.md` (que estava fora de ordem); sprint 38 marcada com nota de detalhamento não registrado na época
- `docs/sprints.md`: "Sprints Concluídas" reduzida de 47 para as últimas 10 entradas; tabela "Status Geral" mantida completa; seção "Backlog" removida (sprints 28-32 já cobertas no archive); itens pendentes de Dockerfile/`GREENCAP_ENCRYPTION_KEY` realocados para "Candidatos para Próximas Sprints" sob novo grupo "🐳 Infraestrutura de Produção"
- `.scratch/`: diretórios `sprint-4` a `sprint-43` movidos para `.scratch/archive/sprint-N/` via `git mv`, preservando histórico

### Sprint 55 ✅ — Docker: Quick Start ponta a ponta (Dockerfile + compose + profile prod)

- `docker/Dockerfile` (novo): build multi-stage — stage `builder` (`eclipse-temurin:21-jdk`) roda `./gradlew bootJar -x jar` (gera o frontend Vaadin de produção via plugin, sem Node instalado no host); stage `runtime` (`eclipse-temurin:21-jre` + `curl`) só com o JAR final
- `.dockerignore` (novo): exclui `build/`, `bin/`, `node_modules/`, `.git/`, `.gradle/`, `.scratch/`, `docs/` do contexto de build
- `docker-compose.yml`: corrigido bug pré-existente em `build.context: ..` (apontava um nível acima do diretório do projeto, fazendo `docker compose up` falhar sempre); adicionado `SPRING_PROFILES_ACTIVE: prod` e `healthcheck` via `/actuator/health` no serviço `greencap`
- `src/main/resources/application-prod.yaml` (novo): `greencap.encryption.key: ${GREENCAP_ENCRYPTION_KEY}` sem fallback — falha rápido no startup se a variável não estiver definida (testado isoladamente: erro claro de placeholder não resolvido)
- `.env.example`: `ENCRYPTION_KEY`, `DB_USER`, `DB_PASSWORD` agora com valores padrão funcionais para Quick Start, com aviso para troca em produção real; `GREENCAP_ENCRYPTION_KEY` documentado separadamente para o fluxo Gradle/dev
- `README.md`: nova seção "Quick Start (Docker)" como caminho principal (clone → `cp .env.example .env` → `docker compose up -d --build` → `http://localhost:8080`, login `admin`/`admin`); fluxo Gradle movido para "Para desenvolvedores"
- Validado ponta a ponta: `docker compose up -d --build` sobe `db` + `greencap`, container `greencap` fica `healthy`, login acessível
- `docs/agents/sprint-archiving.md`: regra ajustada — "Status Geral" agora acompanha a mesma janela de 10 sprints de "Sprints Concluídas" (em vez de manter histórico completo)

### Sprint 56 ✅ — Infrastructure: Cordon/Uncordon de Nodes

- `CONTEXT.md`: definição de `Node` atualizada para incluir Cordon/Uncordon como write operation; novo termo `Cordon` adicionado ao glossário, no formato de `Suspend`
- `Permission.SETTINGS_INFRASTRUCTURE_CORDON` adicionado ao enum; incluído em `operatorPermissions()`; ausente em `viewerPermissions()`
- `V18__add_node_cordon_permission.sql`: concede `SETTINGS_INFRASTRUCTURE_CORDON` a Admin e Operator (identificados por `SETTINGS_CLUSTERS_WRITE`, mesmo padrão de `V17`)
- `NodeInfo`: novo campo `schedulingDisabled`
- `StorageService`: `listNodes()` popula `schedulingDisabled` a partir de `spec.unschedulable`; novo método `cordonNode(Cluster, String, boolean)` faz patch via `client.nodes().withName(name).edit(...)`
- `NodesView`: nova coluna "Scheduling" com badge `Schedulable`/`Cordoned`; botão toggle Cordon/Uncordon (`PAUSE`/`PLAY`) na coluna de ações, desabilitado para Viewer, mesmo padrão de Suspend/Resume do `CronJobsView`; `HELP_TEXT` atualizado
- `UserManagementView` não foi alterado — segue o mesmo gap pré-existente dos 9 `_DELETE` da sprint 51 (permission concedida via migration, sem editor por usuário)
- Issue: `.scratch/sprint-56/issues/01-node-cordon-uncordon.md`

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

### Sprint 58 — Polish de listagens: nome do recurso na confirmação de remoção e espaçamento das colunas de ações

- `ConfirmDialog` de remoção atualizado em 11 views para incluir o nome do recurso na mensagem: `PodsView`, `DeploymentsView`, `ReplicaSetView`, `JobsView`, `CronJobsView` (ambas as variantes, com e sem Jobs ativos), `ServicesView`, `IngressView`, `ConfigMapsView`, `SecretsView`, `HorizontalScalerView`, `PersistentVolumeClaimsView`
- Fix: último botão (lado direito) das colunas de ações ficava colado/cortado na borda da grid — causa raiz era o `HorizontalLayout` padrão (padding/spacing) das colunas de componente, não considerado em `actionsColumnWidth`
- `UiConstants.actionsColumnWidth`: agora soma `ACTION_BUTTON_WIDTH_PX` (48px) por botão + `ACTIONS_COLUMN_RIGHT_PADDING_PX` (8px) de respiro
- Novo helper `UiConstants.addActionsColumn(grid, buttonCount, buttonsProvider)`: monta a coluna de ações com `HorizontalLayout` sem padding/spacing padrão e `padding-right` consistente — substitui o padrão duplicado em 8 views: `NodesView`, `DeploymentsView`, `PodsView`, `JobsView`, `CronJobsView`, `HorizontalScalerView`, `ClustersView`, `UserManagementView`
- `CronJobsView`: `buildActionsLayout` refatorado para `buildActionButtons` (retorna `List<Button>`); `ClustersView`/`UserManagementView`: `buildActions` passam a retornar `List<Button>` em vez de `HorizontalLayout`
- Issues: `.scratch/sprint-58/issues/01-nome-do-recurso-no-dialogo-de-remocao.md` e `02-espacamento-coluna-de-acoes.md`

### Sprint 59 ✅ — YAML do Manifest editável (Edit + Apply)

- `CONTEXT.md`: termo `Manifest` atualizado — deixa de ser somente leitura; documenta os 11 tipos namespaced editáveis (Pod, Deployment, ReplicaSet, Job, CronJob, Service, Ingress, ConfigMap, Secret, HorizontalScaler, PersistentVolumeClaim) vs. os 3 cluster-scoped que permanecem read-only (Node, PersistentVolume, StorageClass); novo termo `Apply` definido como replace completo (PUT)
- `docs/adr/0005-manifest-apply-as-full-replace.md`: nova ADR registrando por que Apply remove `resourceVersion`/`uid`/`creationTimestamp`/`generation`/`managedFields`/`status` antes do `update()` — evita 409 espúrio por churn de `status` em recursos reconciliados continuamente; semântica de replace completo (não merge estilo `kubectl apply`)
- `Permission.java`: novo grupo "Project — Manifest" com `MANIFEST_EDIT`; incluído em `operatorPermissions()`, ausente de `viewerPermissions()`
- `V19__add_manifest_edit_permission.sql`: concede `MANIFEST_EDIT` a usuários com `SETTINGS_CLUSTERS_WRITE` (Admin/Operator)
- `ManifestService`: novo método `applyYaml()` — parseia o YAML editado via `YAMLMapper`, valida `kind`/`metadata.name`/`metadata.namespace` contra os parâmetros da URL (bloqueia divergências sem chamar a API), remove campos gerenciados pelo servidor e o nó `status`, e aplica via `client.resource(yaml).inNamespace(namespace).update()`; novo `isEditable(resourceType)` com o mapa dos 11 tipos editáveis → `kind` esperado
- `ManifestView`: botões **Edit** e **Apply** no header — Edit alterna para Cancelar (descarta alterações e volta ao YAML original), Apply só visível em modo edição e abre `ConfirmDialog` antes de enviar; editor é um `TextArea` monoespaçado que substitui o `Pre` em modo edição e recebe foco automático; sucesso re-busca o YAML e volta ao modo leitura com notificação, falha mantém o texto editado com notificação de erro; Edit visível apenas para os 11 tipos editáveis e desabilitado (com tooltip) sem `MANIFEST_EDIT`
- Issue: `.scratch/sprint-59/issues/01-yaml-manifest-editavel.md`

### Sprint 60 ✅ — Fix: scroll horizontal em views de YAML/logs

- `ManifestView.java`: estilo de `yamlContent` (`Pre`, leitura) — `white-space: pre` → `pre-wrap`, adicionado `overflow-wrap: anywhere`, mantido `overflow: auto` existente e adicionado `overflow-x: hidden`; linhas longas de YAML quebram visualmente em vez de gerar scroll horizontal. `yamlEditor` (TextArea, edição) inalterado — Vaadin já aplica `pre-wrap`/`min-width: 0` internamente
- `PodLogsView.java`: mesmo padrão aplicado a `logContent` (`Pre`, logs do Pod) em `styleLogContent()`
- Issues: `.scratch/sprint-60/issues/01-fix-scroll-manifestview.md`, `.scratch/sprint-60/issues/02-fix-scroll-podlogsview.md`

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

### Sprint 62 — User Management: treeview de permissões expansível/colapsável
- `UserManagementView.GroupNode` (painel `PermissionTreePanel` do diálogo de Permissões): cada grupo de topo (Workloads, Networking, Parameters, Auto Scaling, Storage, Topology, Observability, Clusters, Infrastructure, Users, Platform Settings) ganhou um chevron (`VaadinIcon.CHEVRON_DOWN`/`CHEVRON_RIGHT`, `LUMO_SMALL/TERTIARY/ICON`) ao lado do `Checkbox` do header, em uma `HorizontalLayout`; novo método `setExpanded(boolean)` alterna a visibilidade de um `itemsContainer` (`VerticalLayout`) com os itens do grupo e atualiza o ícone/tooltip — `SubGroupNode` (Deployments/StatefulSets/Jobs/CronJobs dentro de Workloads) permanece sempre expandido
- `itemsContainer` recebe `margin-left: var(--lumo-size-s)` para compensar a largura do chevron e manter o alinhamento das checkboxes filhas com o checkbox do header (mesmo `margin-left: var(--lumo-space-l)` de `PermissionNode`)
- Estado inicial de cada grupo: expandido se ≥1 das suas permissões já estiver marcada em `initial` (calculado no construtor de `GroupNode`), senão colapsado — uniforme para todos os grupos, inclusive os de 1 item (Topology, Storage, Infrastructure, Platform Settings); no diálogo "New User" (nada marcado) todos os grupos iniciam colapsados
- `Select All` / `Deselect All` (já existentes) continuam alterando apenas os checkboxes, sem afetar o collapse; novos botões `Expand All` / `Collapse All` na `bulkActions` expandem/colapsam todos os grupos de uma vez
- Issue: `.scratch/sprint-62/issues/01-permission-treeview-collapsible.md`

### Sprint 63 ✅ — UX: seção GLOBAL no drawer, ícone de contexto (i) e Observability como submenu de PROJECT

- `CONTEXT.md`: novos termos `Project` (UI section que agrupa Topology, Observability, Workloads, Networking, Parameters, Auto Scaling e Storage, escopada ao Namespace ativo) e `Global` (UI section que agrupa Clusters e Infrastructure, escopada ao Cluster); novo termo `Observability` (UI subsection dentro de Project — Dashboard, Events, Metrics); `Infrastructure`, `PersistentVolume`, `StorageClass`, `Node` e `Cordon` atualizados para "within Global" (era "within Settings")
- `MainLayout.buildDrawer()`: drawer reorganizado de 3 seções (PROJECT, OBSERVABILITY, SETTINGS) para 3 seções (**PROJECT, GLOBAL, SETTINGS**) — nova seção `buildGlobalNav()` agrupa `Clusters` e `Infrastructure` (movidos de SETTINGS); `buildConfiguracaoNav()` (SETTINGS) passa a conter apenas `Users` e `Settings`
- `buildNavSection(label, nav, contextTooltip)`: novo ícone `VaadinIcon.INFO_CIRCLE_O` com tooltip nativo (`title`) ao lado do cabeçalho — `NAMESPACE_CONTEXT_TOOLTIP` em PROJECT, `CLUSTER_CONTEXT_TOOLTIP` em GLOBAL, nenhum em SETTINGS
- `Permission.java`: `SETTINGS_CLUSTERS_VIEW`/`SETTINGS_CLUSTERS_WRITE`/`SETTINGS_INFRASTRUCTURE_VIEW`/`SETTINGS_INFRASTRUCTURE_CORDON` renomeados para `GLOBAL_CLUSTERS_VIEW`/`GLOBAL_CLUSTERS_WRITE`/`GLOBAL_INFRASTRUCTURE_VIEW`/`GLOBAL_INFRASTRUCTURE_CORDON`; `operatorPermissions()`/`viewerPermissions()` atualizados; `V21__rename_global_permissions.sql` migra os valores persistidos em `user_permissions`
- `ClustersView`, `NodesView`, `PersistentVolumesView`, `StorageClassesView`: referências às permissões renomeadas atualizadas
- `Observability` (Dashboard, Events, Metrics) deixou de ser seção própria do drawer e passou a ser item expansível dentro de **PROJECT**, logo após `Topology`, com ícone `VaadinIcon.EYE` e navegação padrão para `DashboardView` (mesmo padrão de `Workloads`/`Networking`); permissões `OBSERVABILITY_*` mantidas sem renomear
- `UserManagementView.PermissionTreePanel`: árvore de permissões espelha a nova estrutura — seção `GLOBAL` (grupos Clusters/Infrastructure) entre PROJECT e SETTINGS; grupo `Observability` movido para dentro da seção PROJECT, logo após `Topology`
- Issues: `.scratch/sprint-63/issues/01-secao-global-no-drawer.md`, `02-renomear-permissoes-global.md`, `03-icone-contexto-namespace-cluster.md`, `04-observability-submenu-de-project.md`

### Sprint 64 ✅ — DevOps: pipeline GitHub Actions para validar docker-compose

- `.github/workflows/docker-compose-validate.yml` (novo): workflow dedicado, sem job de build/test Gradle (fora de escopo); triggers `pull_request` e `push` para `main` com `paths` filtrados (`docker-compose.yml`, `docker/**`, `.env.example`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/**`, `gradlew`, `src/**`, `.github/workflows/*.yml`)
- Steps: `actions/checkout@v4` → `cp .env.example .env` (replica o Quick Start, sem GitHub Secrets) → `docker compose up -d --build --wait --wait-timeout 120` (sem cache de build) → `curl --fail -L http://localhost:8080/` (valida porta publicada e frontend Vaadin de produção servido) → dump de `docker compose logs` em caso de falha → `docker compose down -v` sempre (`if: always()`)
- Fix encontrado na primeira execução no GitHub Actions: `.gitignore` ignorava `gradle/wrapper/gradle-wrapper.jar` — a regra `!gradle/wrapper/gradle-wrapper.jar` (seção Gradle) era sobrescrita pela regra `*.jar` declarada mais abaixo (seção Spring Boot), então o jar nunca foi commitado; checkout limpo (CI) ficava sem o jar e `./gradlew` falhava com `ClassNotFoundException: GradleWrapperMain`. Corrigido movendo a negação para depois de `*.jar` e commitando `gradle/wrapper/gradle-wrapper.jar`
- Validado ponta a ponta: push para `main` disparou o workflow, build + healthcheck (`db`/`greencap` `healthy`) + `curl http://localhost:8080/` (200, página de login Vaadin) passaram — pipeline verde
- Issue: `.scratch/sprint-64/issues/01-pipeline-validacao-docker-compose.md`

### Sprint 65 ✅ — Infraestrutura de Demo: migrar greencap-demo para driver docker multi-node

- `cluster-provision.sh`: `DRIVER` `virtualbox` → `docker` (auto-detectado pelo minikube no Linux com Docker instalado; elimina o bug do DHCP da rede host-only que impedia clusters multi-node de voltarem saudáveis após reboot); `NODES` `1` → `3` (control-plane + 2 workers); `CPUS=2`/`MEMORY=2048` por node (~6GB total); mensagem final corrigida de `create.sh` para `create-demo.sh`
- `add-hosts.sh`: `minikube ip` → `minikube ip -p greencap-demo`; endurecido com `set -euo pipefail` e validação de que a saída é um IPv4 antes de gravar em `/etc/hosts` — fix encontrado no aceite manual: uma corrida com `create-demo.sh` ainda em andamento gravava uma mensagem de erro literal em `/etc/hosts`
- `samples/greencap-demo/README.md` (novo): quick start, tabela de trade-offs de drivers (docker/virtualbox/kvm2), troubleshooting do bug do virtualbox e do reboot com driver docker, requisitos
- Validado ponta a ponta: provisionamento com 3 nodes via driver `docker` OK, `create-demo.sh` (rollout + addon ingress) OK, acesso a `http://greencap-demo.local` OK
- Aceite manual (reboot do host): cluster com 3 nodes volta `Running`/`OK` sem reprovisionar; com driver `docker` os containers dos nodes não religam automaticamente no boot — é necessário rodar `minikube start -p greencap-demo` manualmente, documentado no README; `http://greencap-demo.local` volta a responder em seguida sem passos adicionais
- Issue: `.scratch/archive/sprint-65/issues/01-migrar-driver-docker-multinode.md`

### Sprint 66 ✅ — Workloads: coluna/filtro Nodes em Deployments/ReplicaSets/StatefulSets/Jobs/Pods

- `CONTEXT.md`: glossário de `Deployment`, `ReplicaSet`, `StatefulSet` e `Job` atualizado documentando a nova coluna "Nodes" (Nodes distintos que executam os Pods do recurso, ou "—")
- `PodNodeResolver` (novo, `io.greencap.k8s.kubernetes`): utilitário stateless que resolve os Nodes distintos cujos Pods casam com `spec.selector.matchLabels` de um Deployment/ReplicaSet/StatefulSet/Job — extraído para não inflar `WorkloadService` (já com ~455 linhas)
- `DeploymentInfo`, `ReplicaSetInfo`, `StatefulSetInfo`, `JobInfo`: novo campo `nodes` (String, comma-separated ou "—")
- `WorkloadService`: `listDeployments`/`listReplicaSets`/`listStatefulSets`/`listJobs` passam a buscar os Pods do namespace (ou de todos, se `isAllNamespaces`) e preenchem `nodes` via `PodNodeResolver.resolveNodes(...)`
- `DeploymentsView`, `ReplicaSetView`, `StatefulSetsView`, `JobsView`: nova coluna "Nodes" (sempre antes de "Age") com filtro de texto, mesmo padrão de Name/Owner/Status
- `PodsView`: coluna "Node" existente ganhou filtro de texto (mesmo padrão das demais)
- Validado ponta a ponta no `greencap-demo` (3 Nodes, driver docker, sprint 65): coluna Nodes/Node preenchida corretamente e filtro funcionando nas 5 views
- CronJob de exemplo `node-spread-test` (novo, `samples/greencap-demo/manifests/13-node-spread-cronjob.yaml`): roda a cada minuto no namespace `greencap-demo`, útil para observar a distribuição de Pods entre Nodes na `JobsView`/`PodsView`
- Issues: `.scratch/archive/sprint-66/issues/01-nodes-backend.md`, `.scratch/archive/sprint-66/issues/02-nodes-ui.md`

### Sprint 67 ✅ — PodsView: esconder Pods Succeeded de Jobs por padrão (toggle)

- `CONTEXT.md`: termo `Pod` atualizado — a listagem de Pods esconde por padrão Pods de Job já concluídos (`Succeeded`), via toggle ativo por padrão; Pods filtrados por um Job específico (`?job=`) sempre aparecem, independente da fase
- `PodsView`: novo `Checkbox` "Hide completed Job pods" (marcado por padrão); novo predicado `isCompletedJobPod` (`jobName` não vazio + `phase == "Succeeded"`) combinado ao filtro existente do `ListDataProvider`, junto com Name/Status/Node e o filtro de Job — Pods `Failed` de Jobs permanecem sempre visíveis, independente do toggle
- Ao abrir via `?job=<nome>` (botão "View Pods" de `JobsView`/`CronJobsView`), o checkbox inicia desmarcado — evita grid vazia ao ver os pods de um Job já `Complete`; volta a marcado ao limpar o filtro de Job pelo `jobFilterBanner`
- Issue: `.scratch/sprint-67/issues/01-hide-completed-job-pods.md`

### Sprint 68 ✅ — Container Registry: menu Global, listagem de Repositories e Tags

- `CONTEXT.md`: novos termos `Registry` (capacidade derivada do `Cluster`, alcançada via port-forward da API do Kubernetes para o `Service` `registry` no Namespace `kube-system` — sem entidade persistida, sem credenciais novas), `Repository` (coleção nomeada de versões de imagem) e `Tag` (referência nomeada a uma versão específica, com digest/size/created); `docs/adr/0006-registry-via-port-forward.md` documenta a decisão de alcançar o Registry via port-forward em vez de uma entidade/configuração própria
- `RepositoryInfo`/`TagInfo` (novos DTOs): `RepositoryInfo(name, tagCount)`, `TagInfo(name, digest, size, createdAt)`
- `RegistryService` (novo, `io.greencap.k8s.kubernetes`): `listRepositories(Cluster)` — port-forward para o `Service` `registry`/`kube-system` (porta `5000`, porta do container — Fabric8 `ServiceResource#portForward` encaminha direto para a porta do Pod, não resolve `targetPort`), `GET /v2/_catalog` + `/v2/<repo>/tags/list` via `java.net.http.HttpClient`; `listTags(Cluster, repository)` — para cada tag, `GET /v2/<repo>/manifests/<tag>` (digest via header `Docker-Content-Digest`, size = `config.size` + soma de `layers[].size`) e `GET /v2/<repo>/blobs/<configDigest>` (campo `created`, formatado via `NamespaceService.age(...)`); qualquer exceção (Service ausente, port-forward falha, catálogo vazio) → `log.warn` + `List.of()`, sem `KubernetesOperationException` — ausência do Registry é estado esperado, não falha de cluster
- `Permission.GLOBAL_REGISTRY_VIEW` (novo, grupo Global): incluído em `operatorPermissions()`/`viewerPermissions()`; `V22__add_registry_permission.sql` concede a todos os usuários com `GLOBAL_INFRASTRUCTURE_VIEW`
- `RegistryView` (nova): rota `registry`, item "Container Registry" no drawer GLOBAL (`MainLayout.buildRegistryNavItem()`, ícone `VaadinIcon.ARCHIVE`); grid de Repositories (Repository/Tags, filtro por nome), ação "View Tags" navega para `registry/<repository>`; estado vazio único ("No repositories found. Make sure the Service \"registry\" in the \"kube-system\" namespace is available on this Cluster.") sem distinguir Service ausente/port-forward falho/catálogo vazio
- `RegistryTagsView` (nova): rota `registry/:repository*` (wildcard para repositories com `/` no nome, ex. `greencap-demo/backend`); cabeçalho com nome do repository + botão Back para `RegistryView`; grid de Tags (Tag/Digest/Size/Created) — coluna Digest com `overflow:hidden`/`text-overflow:ellipsis`/`title` (tooltip) em vez de truncamento fixo
- `samples/greencap-demo/cluster-provision.sh`: addon `registry` habilitado junto de `metrics-server`/`ingress`; `create-demo.sh` refatorado — addons (antes espalhados entre os dois scripts) agora centralizados em `cluster-provision.sh`, `create-demo.sh` passa a só aplicar os manifests do demo; `README.md` atualizado
- Validado ponta a ponta no `greencap-demo`: addon `registry` habilitado, imagens de teste (`greencap-demo/hello` com tags `v1`/`v2`/`latest`, `greencap-demo/backend` com tag `v1`) buildadas e enviadas via port-forward + `docker push`; menu "Container Registry" lista os repositories com contagem de tags e "View Tags" exibe nome/digest/size/created corretamente
- Issues: `.scratch/archive/sprint-68/issues/01-registry-menu-and-repository-listing.md`, `02-repository-tags-view.md`

### Sprint 69 ✅ — Fix: Container Registry — item ausente na treeview de permissões + ação View Tags na grid

- `UserManagementView.buildGlobalGroups()`: novo grupo "Container Registry" (`GLOBAL_REGISTRY_VIEW`) — permission introduzida na sprint 68 que não havia sido exposta na treeview de permissões (GLOBAL), mesmo padrão de grupo único do "Infrastructure"
- `RegistryView`: ação "View Tags" sai da barra de título (selection action) e passa para uma coluna de ações na própria grid (`UiConstants.addActionsColumn`, botão por linha), mesmo padrão de `JobsView` ("View Pods")
- Issue: `.scratch/archive/sprint-69/issues/01-fix-registry-permission-treeview-view-tags.md`

### Sprint 70 ✅ — Platform Settings: auto-refresh — nova opção "3 seconds" e novo default

- `CONTEXT.md`: entrada `PlatformSettings` atualizada — auto-refresh varia de "no auto-refresh" até 1 minuto; usuário sem preferência salva (conta nova ou que nunca abriu Platform Settings) passa a ter default de 3 segundos, escolhido pela responsividade para o público-alvo de clusters pequenos de dev/teste
- `RefreshInterval`: novo valor `THREE_SECONDS("3 seconds", 3)`, posicionado entre `NONE` e `FIVE_SECONDS`
- `PlatformSettingsView.buildRefreshCard()`: fallback do ComboBox (sem preferência salva) passa de `NONE` para `THREE_SECONDS`
- `MainLayout`: default do field `currentRefreshInterval` e fallback em `onAttach()` passam de `NONE` para `THREE_SECONDS` — auto-refresh a 3s ativo desde o login para quem nunca configurou; usuários que já salvaram explicitamente "No auto refresh" (0) ou outro valor continuam inalterados; aplicado uniformemente a todas as views `Refreshable`, sem migration Flyway (mesmo padrão do fallback de tema `"DARK"`)
- Issue: `.scratch/archive/sprint-70/issues/01-auto-refresh-3-seconds-default.md`

### Sprint 71 ✅ — Infraestrutura de Demo: PVC para persistir o Container Registry interno

- `samples/greencap-demo/cluster-provision.sh`: após `minikube addons enable registry`, cria `PersistentVolumeClaim` `registry-storage` (4Gi, `kube-system`, StorageClass `standard`) e aplica `kubectl patch` (strategic merge) no `Deployment registry` adicionando `volumes`/`volumeMounts` (`/var/lib/registry`) e `nodeSelector: kubernetes.io/hostname: greencap-demo`; `kubectl rollout status deployment/registry` aguarda o rollout, mesmo padrão do wait do `ingress-nginx-controller`
- Decisão (`/grill-with-docs`): patch in-place em vez de manifest próprio — preserva `Service`/`registry-proxy` do addon; validado que não há `kube-addon-manager` rodando neste cluster (reconcile contínuo não existe em versões recentes do minikube) e que `volumes`/`volumeMounts`/`nodeSelector` sobrevivem a reexecuções de `minikube addons enable registry` (merge de 3 vias do `kubectl apply` não remove campos fora do manifest do addon)
- Achado crítico durante o teste: a StorageClass `standard` (hostpath-provisioner) cria PVs sem `nodeAffinity` — em multi-node, um Pod reagendado para outro node monta um diretório hostPath local vazio, "perdendo" os dados mesmo com a PVC `Bound`. Fix: `nodeSelector` fixa o Pod do registry no control-plane (`greencap-demo`), node estável (= nome do profile)
- `samples/greencap-demo/README.md`: nova seção "Container Registry" documentando a persistência via PVC, o caveat do `nodeSelector` (control-plane sempre existe no demo de 3 nodes) e que os dados só são perdidos com `minikube delete -p greencap-demo`
- Problema geral de `nodeAffinity` da StorageClass (afeta qualquer PVC) registrado no backlog como candidato de substituição por `local-path-provisioner`; ODF/Ceph avaliado e descartado — over-engineering para o posicionamento "plataforma leve" do GreenCap
- Validado ponta a ponta no `greencap-demo`: `cluster-provision.sh` roda do zero e idempotente (PVC `unchanged`, patch `no change`); push de imagens de teste via port-forward; após `minikube stop`/`start -p greencap-demo`, pod do registry voltou no mesmo node e os 3 repositories continuaram visíveis
- Issue: `.scratch/archive/sprint-71/issues/01-pvc-persistencia-registry.md`

### Sprint 73 ✅ — Container Registry: Build & push de imagem via Kaniko a partir de Git Repository público

- `docs/adr/0007-build-via-kaniko-job-git-context.md` (novo): Build executado como `Job` Kaniko (`gcr.io/kaniko-project/executor`) criado pelo GreenCap via Fabric8 no Namespace `greencap-system` (criado sob demanda), mesmo padrão de `WorkloadService.triggerCronJob`; contexto de build via suporte nativo do Kaniko a Git (`--context=git://<host>/<owner>/<repo>.git#refs/heads/<branch>`, sem upload/ConfigMap/PVC); push para o Registry interno via DNS do cluster (`registry.kube-system.svc.cluster.local:80`, porta do Service que mapeia para a porta 5000 do container — corrigido durante o aceite, estava `:5000`); Job efêmero (`ttlSecondsAfterFinished=600`), sem histórico persistido; apenas repositórios públicos
- `CONTEXT.md`: termo `Registry` atualizado; novos termos `Build` e `Git Repository`
- `kubernetes/dto/BuildRequest`/`BuildProgress` (novos)
- `RegistryService`: `startBuild` + `getBuildProgress` + helpers cobertos por `RegistryServiceTest`
- `Permission.GLOBAL_REGISTRY_BUILD` (novo, ADMIN/OPERATOR): `V23__add_registry_build_permission.sql`
- `RegistryView`: botão "Build Image" abre diálogo com Git Repository URL, Branch, Context path, Dockerfile path, Repository e Tag
- `BuildLogsView` (nova, rota `registry/build/:jobName`): log ao vivo com polling de 3s, pausar/retomar
- Fix no aceite: prefixo `git://` no `--context`; `fetchTagInfo` aceita manifesto OCI além de Docker v2
- Validado ponta a ponta no `greencap-demo`: Build de `https://github.com/joseafilho/uni-flask-app`
- Issue: `.scratch/archive/sprint-73/issues/01-build-push-imagem-registry-kaniko-git.md`

### Sprint 74 ✅ — Container Registry: Remove Repository e Remove Tags com multi-seleção

- `docs/adr/0008-registry-remove-via-manifest-delete-and-gc.md` (novo): remoção de repositório via DELETE de manifests por digest + garbage-collect (`registry garbage-collect`) executado via `pods/exec` do Fabric8 no Pod do registry (`actual-registry=true`, `kube-system`); primeiro uso de `pods/exec` no GreenCap; "Remove Tags" não roda GC — apenas DELETE por digest, idempotente (404 tratado como sucesso)
- `CONTEXT.md`: termos `Remove Repository` e `Remove Tags` adicionados
- `RegistryMaintenanceService` (novo): `deleteRepository` + `deleteTags`
- `RegistryTagsView`: ações "Remove Tags" (multi-seleção, dialog de confirmação type-to-confirm) e confirmação de "Remove Repository" propagada para o serviço de manutenção
- Issues: `.scratch/archive/sprint-74/issues/`

### Sprint 75 ✅ — Deploy Application: wizard multi-step a partir de imagem

- `docs/adr/0009-deploy-application-sem-rastreamento.md` (novo): recursos criados pelo wizard são objetos Kubernetes padrão, sem rastreamento no GreenCap; sincronismo banco↔cluster não justificado neste momento
- `CONTEXT.md`: termo `Deploy Application` adicionado
- `DeployApplicationService` (novo): cria Namespace + Deployment + Service ClusterIP (quando porta informada) + PVC (opcional) + Ingress (opcional); `DeployApplicationRequest`/`DeployApplicationResult` DTOs
- `DeployApplicationView` (nova, rota `/deploy`): wizard 6 passos (Name, Image & Port, Resources, Volume, External Access, Review); sugestões de StorageClass e IngressClass carregadas do cluster; sugestão de host `<namespace>.greencap.local`; execução assíncrona em thread virtual; em sucesso navega para Topologia
- `Permission.PROJECT_DEPLOY_APPLICATION` (novo): `V25__add_deploy_application_permission.sql`
- Issues: `.scratch/archive/sprint-75/issues/`


### Sprint 76 ✅ — Namespaces View: listagem com contagens de recursos, Create e Delete Namespace

- `NamespaceService`: `listNamespacesWithCounts()`, `createNamespace()`, `deleteNamespace()`; filtra namespaces em fase `Terminating` no combobox da navbar
- `NamespacesView` (nova, rota `global/namespaces`): grid Name/Status/Pods/Deployments/Services/Age; Create com validação DNS; Delete type-to-confirm; system namespaces bloqueados; chama `MainLayout.refreshClusterState()` após operações
- `Permission.GLOBAL_NAMESPACES_VIEW/WRITE/DELETE`; `V26__add_namespace_permissions.sql`
- Issue: `.scratch/archive/sprint-76/issues/`

### Sprint 77 ✅ — Topologia: nó Ingress + botão "Go to resource" + pré-filtro ?name= nas views

- `TopologyService`: listagem de Ingresses via Fabric8; `ingressNode()` com ingressClass/hosts/TLS; `extractBackendServiceNames()`; `resourceViewUrl()` mapeia cada tipo para rota da view com `?name=`
- `topology-graph.ts`: cor Ingress `#06B6D4`; cor de arestas `#64748B`; fcose separado da inicialização; `fixedNodeConstraint` desabilitado quando compound nodes presentes
- `TopologyNodeDrawer`: bloco `isIngress` com Hosts, badge TLS, IngressClass; botão "Go to resource" substitui "Ver YAML"
- `DeploymentsView`, `ReplicaSetView`, `ServicesView`, `PersistentVolumeClaimsView`, `IngressView`: `nameFilter` instância; `beforeEnter` lê `?name=`
- Issues: `.scratch/archive/sprint-77/issues/`

### Sprint 78 ✅ — Topologia: correções de layout (randomize), tap em group nodes e botão Reset Positions

- `topology-graph.ts`: `randomize` dinâmico — `true` quando posições salvas ausentes (fix para nós empilhados na primeira renderização), `false` quando presentes (mantém layout salvo); guard `if (node.data('isGroup')) return` no tap handler (fix para painel lateral não abrir ao clicar em group nodes)
- `TopologyLayoutRepository`: `deleteByUserIdAndClusterIdAndNamespace` (método derivado Spring Data)
- `TopologyLayoutService`: `deleteLayout()` deleta o registro de posições salvas para user + cluster + namespace
- `TopologiaView`: botão "Reset positions" (ícone refresh, estilo LUMO_TERTIARY + LUMO_ICON + LUMO_CONTRAST, ao lado do botão Help) — deleta o layout salvo e navega para a mesma rota, forçando nova renderização com `randomize: true`
- Issues: `.scratch/archive/sprint-78/issues/`

### Sprint 79 ✅ — UX: padronização de header em ClustersView e UserManagementView

- `ClustersView`: substituído por `UiConstants.buildSectionHeader`; ações "Test Connection" e "Remove" como `SelectionAction`; coluna de ações inline removida; `GridSelectionMemory` com `configureSingleSelection`
- `UserManagementView`: mesmo padrão; ações "Edit Permissions" e "Deactivate" como `SelectionAction`; proteções via early-exit
- Issues: `.scratch/archive/sprint-79/issues/`

### Sprint 80 ✅ — Add Cluster dialog: provider Minikube (Docker), aviso OpenShift e comando kubectl copiável

- `ClusterProvider`: enum renomeado de `Kubernetes` → `MinikubeDocker`; `displayName()` retorna "Minikube (Docker)" / "OpenShift"
- `ClustersView` dialog: aviso inline para OpenShift (não suportado), code block com `kubectl config view --flatten --minify` e botão de cópia
- `CONTEXT.md`: `ClusterProvider` atualizado com valores reais
- Issue: `.scratch/archive/sprint-80/issues/`

### Sprint 81 ✅ — Testes automatizados: TestContainers + cobertura de services críticos

- `build.gradle.kts`: dependências TestContainers; H2 removido; `@ServiceConnection` auto-configura datasource
- `PostgresIntegrationTest`: classe base estática compartilhada; `WorkloadServiceTest`, `NamespaceServiceTest`, `UserServiceTest`, `ClusterServiceTest`
- Issues: `.scratch/archive/sprint-81/issues/`
