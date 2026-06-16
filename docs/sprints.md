# GreenCap K8s — Sprints & Demandas

> Documento vivo. Atualizar a cada sprint concluída ou nova demanda identificada.

---

## Status Geral

| Sprint | Tema | Status |
|--------|------|--------|
| 66 | Workloads — coluna/filtro Nodes em Deployments/ReplicaSets/StatefulSets/Jobs/Pods | ✅ Concluído |
| 67 | PodsView — esconder Pods Succeeded de Jobs por padrão (toggle) | ✅ Concluído |
| 68 | Container Registry — menu Global, listagem de Repositories e Tags | ✅ Concluído |
| 69 | Fix — Container Registry: item ausente na treeview de permissões + View Tags na grid | ✅ Concluído |
| 70 | Platform Settings — auto-refresh: nova opção "3 seconds" e novo default | ✅ Concluído |
| 71 | Infraestrutura de Demo — PVC para persistir o Container Registry interno | ✅ Concluído |
| 73 | Container Registry — Build & push de imagem via Kaniko a partir de Git Repository público | ✅ Concluído |
| 74 | Container Registry — Remove Repository e Remove Tags com multi-seleção | ✅ Concluído |
| 75 | Deploy Application — wizard multi-step para criar Namespace + Deployment + Service + PVC + Ingress a partir de imagem | ✅ Concluído |
| 76 | Namespaces View — Global: listagem com contagens de recursos, Create e Delete Namespace | ✅ Concluído |

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

#### 📦 Registry — follow-ups da Sprint 73

- **Build a partir de Git Repository privado** — a Sprint 73 implementou Build via Kaniko apenas para repositórios públicos (sem credenciais). Suporte a repositórios privados exigiria capturar credenciais (token/usuário+senha) na UI e propagá-las ao Job Kaniko (`GIT_TOKEN`/`GIT_USERNAME`/`GIT_PASSWORD`), com cuidado para não persistir as credenciais em texto plano.
- **Histórico de Builds** — a Sprint 73 não persiste histórico: um Build finalizado não deixa rastro em GreenCap (Job efêmero com `ttlSecondsAfterFinished`). Avaliar persistir um registro mínimo (Repositório/Tag, Git Repository/branch, status, timestamps) para permitir consultar Builds anteriores.

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

### Sprint 76 ✅ — Namespaces View: listagem com contagens de recursos, Create e Delete Namespace

- `NamespaceInfo`: +campos `podCount`, `deploymentCount`, `serviceCount`
- `NamespaceService`: `listNamespacesWithCounts()` (4 chamadas Fabric8 — namespaces, pods, deployments, services — agrupadas por namespace em uma única conexão), `createNamespace()`, `deleteNamespace()`; `listNamespaceNames()` passa a filtrar namespaces em fase `Terminating` para que namespaces deletados não reapareçam no combobox da navbar durante a exclusão em cascata
- `Permission.GLOBAL_NAMESPACES_VIEW/WRITE/DELETE` (novos): VIEW concedido a todos com `GLOBAL_CLUSTERS_VIEW`; WRITE e DELETE concedidos a usuários com `GLOBAL_CLUSTERS_WRITE`; `V26__add_namespace_permissions.sql`
- `NamespacesView` (nova, rota `global/namespaces`): grid com Name/Status/Pods/Deployments/Services/Age; botão "Create Namespace" (visível com WRITE) — dialog com campo de nome validado por regex DNS; ação "Delete" (visível com DELETE) — dialog exige digitar o nome do namespace antes de confirmar, com aviso explícito de destruição cascata; system namespaces (`kube-system`, `kube-public`, `kube-node-lease`, `default`) bloqueados para deleção; após create/delete chama `MainLayout.refreshClusterState()` para recarregar o combobox de namespaces; se o namespace deletado era o namespace ativo, o contexto é zerado antes do refresh (seleciona "default" automaticamente); async load com padrão `CompletableFuture` + `UiConstants.VIRTUAL_THREADS`
- `MainLayout`: item "Namespaces" (ícone `FOLDER_O`) na seção Global, entre Clusters e Infrastructure, adicionado à lista `clusterDependentNavItems`
- `UserManagementView`: grupo "Namespaces" com as 3 permissões na treeview Global
- `CONTEXT.md`: entradas `Namespace`, `Create Namespace`, `Delete Namespace` detalhadas; `Global` atualizado para incluir Namespaces
- Issue: `.scratch/sprint-76/issues/01-namespaces-view.md`

### Sprint 75 ✅ — Deploy Application: wizard multi-step para criar Namespace + Deployment + Service + PVC + Ingress

- `DeployApplicationService` (novo): cria Namespace → Deployment → Service → PVC → Ingress em sequência best-effort; falha parcial retorna `DeployApplicationResult` com recurso falho sem rollback (ver ADR 0009)
- `DeployApplicationRequest` / `DeployApplicationResult` (novos DTOs em `kubernetes/dto/`)
- `Permission.PROJECT_DEPLOY_APPLICATION` (novo, ADMIN/OPERATOR); `V25__add_deploy_application_permission.sql` concede a usuários com `GLOBAL_CLUSTERS_WRITE`
- `NetworkingService.listIngressClassNames`: lista IngressClasses disponíveis no cluster via Fabric8
- `DeployApplicationView`: wizard 6 passos (Name → Image & Port → Resources → Volume → External Access → Review); sugestões do Registry interno no `ComboBox` de imagem; StorageClass pré-selecionada com a default do cluster; host sugerido `<namespace>.greencap.local`; após sucesso navega para `TopologiaView` do novo Namespace
- `DashboardView`: CTA "New Application" quando namespace não tem Deployments (visível apenas com permissão `PROJECT_DEPLOY_APPLICATION`)
- `MainLayout`: item **"New Application"** com ícone `PLUS_CIRCLE` adicionado acima da seção PROJECT (renomeado de "Deploy Application", extraído para `SideNav` próprio, ícone diferenciado do Workloads/Deployments)
- Fix: `PodLogsView` — polling para automaticamente ao receber `KubernetesOperationException` (ex: `ImagePullBackOff`, container em espera); exibe `[Polling stopped] <mensagem>` no corpo dos logs em vez de toast infinito a cada intervalo
- Fix: `DeployApplicationView.REGISTRY_INTERNAL_HOST` corrigido de `registry.kube-system.svc.cluster.local:80` para `localhost:5000` — DNS de cluster não é resolvível no nível do kubelet/Docker daemon do nó; `registry-proxy` (DaemonSet) expõe o registry em `localhost:5000` via `hostPort` em cada nó
- `docs/adr/0009-deploy-application-sem-rastreamento.md` (novo)
- Issues: `.scratch/sprint-75/issues/01-deploy-application-backend.md`, `02-deploy-application-wizard-ui.md`

### Sprint 74 ✅ — Container Registry: Remove Repository e Remove Tags com multi-seleção

- `docs/adr/0008-registry-remove-via-manifest-delete-and-gc.md` (novo): remoção de manifests por digest via `DELETE /v2/<repo>/manifests/<digest>` (idempotente — 404 = sucesso) + `registry garbage-collect /etc/distribution/config.yml --delete-untagged` no Pod do registry via Fabric8 `pods/exec` (primeiro uso de `pods/exec` no GreenCap; `REGISTRY_STORAGE_DELETE_ENABLED=true` já habilitado na imagem `registry:3.0.0`); "Remove Tags" não roda GC
- `CONTEXT.md`: termos `Registry`, `Repository` e `Tag` atualizados mencionando as novas operações destrutivas e sua irreversibilidade
- `Permission.GLOBAL_REGISTRY_DELETE` (novo, ADMIN/OPERATOR): `V24__add_registry_delete_permission.sql` concede a usuários com `GLOBAL_CLUSTERS_WRITE`; `UserManagementView` adiciona "Container Registry (Delete)" à treeview
- `RegistryMaintenanceService` (novo, `io.greencap.k8s.kubernetes`): `deleteRepository(Cluster, repository)` — lista tags, deleta todos os digests únicos via `DELETE /v2/<repo>/manifests/<digest>`, roda GC via Fabric8 `pods/exec` aguardando exit code (timeout 60s, label `actual-registry=true` em `kube-system`, binário `/bin/registry`, config `/etc/distribution/config.yml`); `deleteTags(Cluster, repository, List<TagInfo>)` — deleta digests únicos sem GC; helper privado `RegistryConnection` (record `AutoCloseable`) encapsula `KubernetesClient` + `LocalPortForward` + `HttpClient` compartilhado entre os dois métodos
- `UiConstants.buildSectionHeader` (7 args): novo overload que combina `extraLeadingButtons` (ordem: primeiro) + `buildSelectionButtons` — permite que `RegistryView` exiba "Build Image" antes de "Remove Repository" sem duplicar o layout do cabeçalho; overload de 6 args delega para o de 7 com `List.of()`
- `RegistryView`: injeta `RegistryMaintenanceService` e `GridSelectionMemory`; `SelectionAction.destructive(VaadinIcon.TRASH, "Remove Repository", canDelete, ...)` no header; `openDeleteRepositoryDialog` com `ConfirmDialog` (texto com nome + contagem de tags + aviso de GC irrevogável); após confirmação: remoção otimista de `allItems` + registro em `deletedRepositoryNames` (filtra nos refreshes automáticos seguintes para evitar que o repositório volte durante o lag do GC); `configureSingleSelection` com `GridSelectionMemory` para restaurar a seleção ao voltar da tela de Tags; HELP_TEXT atualizado
- `RegistryTagsView`: injeta `RegistryMaintenanceService`; `Grid.SelectionMode.MULTI` + botão "Remove Tags" (TRASH, LUMO_ERROR) visíveis apenas com `GLOBAL_REGISTRY_DELETE`; listener de seleção habilita/desabilita o botão; `openDeleteTagsDialog` com `ConfirmDialog` listando os nomes das tags selecionadas; HELP_TEXT atualizado com nota sobre storage liberado apenas no próximo GC
- Validado ponta a ponta no `greencap-demo`: Remove Repository elimina repositório da listagem após confirmação (remoção otimista imediata, sem reaparecer nos refreshes seguintes); Remove Tags remove as tags selecionadas da grid; usuário VIEWER não vê nem botão Remove Repository nem checkboxes de multi-seleção
- Issues: `.scratch/sprint-74/issues/01-remove-repository.md`, `02-remove-tags-multi-selection.md`

### Sprint 73 ✅ — Container Registry: Build & push de imagem via Kaniko a partir de Git Repository público

- `docs/adr/0007-build-via-kaniko-job-git-context.md` (novo): Build executado como `Job` Kaniko (`gcr.io/kaniko-project/executor`) criado pelo GreenCap via Fabric8 no Namespace `greencap-system` (criado sob demanda), mesmo padrão de `WorkloadService.triggerCronJob`; contexto de build via suporte nativo do Kaniko a Git (`--context=git://<host>/<owner>/<repo>.git#refs/heads/<branch>`, sem upload/ConfigMap/PVC); push para o Registry interno via DNS do cluster (`registry.kube-system.svc.cluster.local:80`, porta do Service que mapeia para a porta 5000 do container — corrigido durante o aceite, estava `:5000`); Job efêmero (`ttlSecondsAfterFinished=600`), sem histórico persistido; apenas repositórios públicos
- `CONTEXT.md`: termo `Registry` atualizado (suporta a operação de escrita Build); novos termos `Build` (operação de escrita que builda e faz push de uma imagem a partir de um Git Repository, progresso em log ao vivo, sem histórico persistido) e `Git Repository` (origem pública identificada por URL/branch, Context path opcional e path do Dockerfile dentro do Context)
- `kubernetes/dto/BuildRequest`/`BuildProgress` (novos): `BuildRequest(gitRepositoryUrl, branch, contextPath, dockerfilePath, repository, tag)`, `BuildProgress(podName, status)`
- `RegistryService`: `startBuild(Cluster, BuildRequest)` cria o Namespace `greencap-system` se ausente e o `Job` Kaniko (args `--dockerfile`, `--context`, `--destination`, `--insecure`, `--context-sub-path` quando `contextPath` informado); `getBuildProgress(Cluster, jobName)` retorna status do Job (`Running`/`Complete`/`Failed`) e o nome do Pod (label `job-name`); helpers `buildGitContext`/`buildDestination`/`resolveDockerfilePath`/`resolveContextSubPath` cobertos por `RegistryServiceTest`
- `Permission.GLOBAL_REGISTRY_BUILD` (novo, ADMIN/OPERATOR): `V23__add_registry_build_permission.sql` concede a usuários com `GLOBAL_CLUSTERS_WRITE`; `UserManagementView` separa "Container Registry (View)" e "Container Registry (Build)" na treeview
- `RegistryView`: botão "Build Image" (visível só com `GLOBAL_REGISTRY_BUILD`) abre diálogo com Git Repository URL, Branch, Context path, Dockerfile path, Repository e Tag (validação por regex); ao confirmar, navega para `registry/build/<jobName>`; `UiConstants.buildSectionHeader` ganhou overload com botões extras no cabeçalho
- `BuildLogsView` (nova, rota `registry/build/:jobName`): badge de status, logs do Pod Kaniko (container `kaniko`) via `ObservabilityService.fetchPodLogs` com polling de 3s, pausar/retomar, botão voltar
- Fix encontrado no aceite manual: Kaniko v1.23.2 só reconhece o prefixo `git://` no `--context` (não `git+https://`); `fetchTagInfo` (leitura de Tags) só aceitava `Accept: application/vnd.docker.distribution.manifest.v2+json`, mas o Kaniko publica manifesto OCI (`application/vnd.oci.image.manifest.v1+json`) — registry respondia 404 e `listTags` retornava vazio mesmo com a contagem de tags correta em `listRepositories`; corrigido aceitando ambos os media types (records `ManifestResponse`/`ConfigRef`/`ConfigBlob` já compatíveis com o schema OCI)
- Validado ponta a ponta no `greencap-demo`: Build de `https://github.com/joseafilho/uni-flask-app` (`unifametro/flask-app:v1.1`) — push concluído e Tags (`latest`, `v1.1`) visíveis com digest/size/created
- Registrados como follow-ups no backlog: Build a partir de Git Repository privado, histórico de Builds
- Issue: `.scratch/sprint-73/issues/01-build-push-imagem-registry-kaniko-git.md`

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
