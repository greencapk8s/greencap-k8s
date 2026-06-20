# GreenCap K8s — Sprints & Demandas

> Documento vivo. Atualizar a cada sprint concluída ou nova demanda identificada.

---

## Status Geral

| Sprint | Tema | Status |
|--------|------|--------|
| 91 | Helm: Repositories, Deploy from Helm (wizard), Upgrade e fix de logs em pods Pending | ✅ Concluído |
| 90 | Helm Releases — listagem, detalhes (Notes/Values/Manifest) e uninstall via Helm CLI | ✅ Concluído |
| 89 | PersistentVolumes — operação Delete com guard de Bound e badge de status | ✅ Concluído |
| 88 | Developer Experience: seção no sidebar + Kubernetes Operators (listar, instalar, desinstalar via OLM) | ✅ Concluído |
| 87 | Setup wizard: script de instalação da plataforma GreenCap no minikube | ✅ Concluído |
| 86 | EventsView — seletor de limite de Events exibidos (50/100/200/500/All, padrão 100) no section header | ✅ Concluído |
| 85 | Deploy from Dockerfile — terceiro modo de deploy: wizard 6 passos, build Kaniko inline + provisão de recursos Kubernetes | ✅ Concluído |
| 84 | Bug fixes: Registry remove persistente (rm -rf do diretório após GC), Namespace Terminating bloqueado, seleção de linha ao clicar em View Tags | ✅ Concluído |
| 83 | Import Compose — wizard 3 passos para importar docker-compose.yml de Git Repository público e provisionar recursos Kubernetes | ✅ Concluído |
| 82 | Karibu-Testing: testes de views Vaadin — dialogs destrutivos | ✅ Concluído |

---

## Backlog

> Itens sem sprint definida, organizados por prioridade (Alta, Média, Baixa).

### 🟡 Média Prioridade

#### 🔌 Developer Experience — follow-ups da Sprint 88

- **Custom Resources** — view genérica na seção Developer Experience que lista os tipos de CRD instalados por operators (filtrados por grupo `*.io` de operators gerenciados pelo OLM), exibe instâncias por namespace e permite criar/editar/deletar via YAML reutilizando o mecanismo de Apply existente. Cobre automaticamente qualquer operator instalado (Grafana, Prometheus, cert-manager, KEDA, etc.) sem precisar de painéis específicos por operator. Posicionamento no sidebar: `DEVELOPER EXPERIENCE → Custom Resources`, abaixo de Operators.

#### ⚡ UX — Carregamento assíncrono nas views restantes

- **Aplicar padrão async + banner "cluster inacessível" nas views de workload** — `DeploymentsView` e `PodsView` já implementados como referência (sprint 50). Aplicar o mesmo padrão nas views restantes que fazem chamadas Kubernetes síncronas no `beforeEnter`: `ServicesView`, `ConfigMapsView`, `SecretsView`, `NodesView`, `EventsView`, `HorizontalScalerView`, `IngressView`, `JobsView`, `CronJobsView`, `ReplicaSetView`, `PersistentVolumeClaimsView`, `PersistentVolumesView`, `StorageClassesView`, `MetricsView`, `TopologiaView`. Padrão: criar `loadXxxAsync(UI ui)` com `CompletableFuture` + `UiConstants.VIRTUAL_THREADS`; adicionar `clusterErrorMessage` via `UiConstants.buildClusterUnreachableMessage()`; exibir banner e ocultar grid em caso de `KubernetesOperationException`.

#### 🟣 StatefulSet — follow-ups da Sprint 61

- **StatefulSet na Topologia** — `TopologyService`/`TopologyGraphComponent` não cobrem StatefulSet. Adicionar novo tipo de nó `StatefulSet` com edge direto `StatefulSet→Pod` (sem ReplicaSet intermediário) e edge para o headless Service via `spec.serviceName`. Avaliar também os PVCs de `volumeClaimTemplates` como nodes/edges.
- **Coluna Owner em PersistentVolumeClaimsView** — PVCs criados via `volumeClaimTemplates` de um StatefulSet seguem o padrão de nome `<template>-<statefulset>-<ordinal>`; adicionar coluna indicando o StatefulSet de origem (ou "—" para PVCs avulsos), análogo ao Owner de `ReplicaSetView`.
- **Events em StatefulSetsView** — adicionar `SelectionAction.of(VaadinIcon.RECORDS, "Events", sts -> EventsDialog.open(observabilityService, clusterContext, "StatefulSet", sts.name(), sts.namespace()))` na barra de seleção, mesmo padrão de `DeploymentsView` (injetar `ObservabilityService` no construtor).

#### 🟢 Diferencial — visão de cluster

- **Overview multi-cluster** — tela de entrada com health de todos os clusters registrados (ConnectionStatus, namespace count) antes de entrar em um específico.

#### 🐳 Deploy from Compose — follow-ups da Sprint 83

- **Ingress no Deploy from Compose** — a Sprint 83 deixou Ingress fora do escopo v1 (decisão registrada no `/grill-with-docs`). Cada serviço com `ports:` expõe apenas um ClusterIP Service. Follow-up: na tela de revisão, adicionar toggle "Expor externamente (Ingress)" por serviço com porta exposta, com campos de host e IngressClass editáveis — mesmo padrão do Deploy from Image. Avaliar também a criação de um único Ingress com múltiplos path rules (um por serviço), o que permite agrupar todos os endpoints sob um único host.

#### 📦 Registry — follow-ups da Sprint 73

- **Build a partir de Git Repository privado** — a Sprint 73 implementou Build via Kaniko apenas para repositórios públicos (sem credenciais). Suporte a repositórios privados exigiria capturar credenciais (token/usuário+senha) na UI e propagá-las ao Job Kaniko (`GIT_TOKEN`/`GIT_USERNAME`/`GIT_PASSWORD`), com cuidado para não persistir as credenciais em texto plano.
- **Histórico de Builds** — a Sprint 73 não persiste histórico: um Build finalizado não deixa rastro em GreenCap (Job efêmero com `ttlSecondsAfterFinished`). Avaliar persistir um registro mínimo (Repositório/Tag, Git Repository/branch, status, timestamps) para permitir consultar Builds anteriores.

#### 🛠️ Infraestrutura de Demo — follow-up da Sprint 71

- **StorageClass com `nodeAffinity` correta no `greencap-demo`** — o `storage-provisioner` (hostpath) do minikube cria PVs sem `nodeAffinity`; em cluster multi-node, se o Pod que monta a PVC for reagendado para outro node, o diretório hostPath local fica vazio (dados "somem" mesmo com a PVC `Bound`). Descoberto na Sprint 71 ao persistir o Registry (contornado com `nodeSelector` fixo no control-plane). Solução geral mais proporcional: substituir a StorageClass default por `local-path-provisioner` (Rancher) — leve (1 pod), mas define `nodeAffinity` corretamente, resolvendo para qualquer PVC sem `nodeSelector` manual por recurso. ODF/Ceph avaliado e descartado — over-engineering para o posicionamento "plataforma leve" do GreenCap (`CONTEXT.md`).

#### 📊 Storage — visualização de uso (sprint 72, cancelada)

- **Gráfico de uso (used/free) por PVC na `PersistentVolumeClaimsView`** — demanda original: coluna com mini gráfico de pizza/donut + diálogo "View Usage" com detalhamento em GiB/%, cores por limiar (70%/90%). Sprint 72 iniciada via `/grill-with-docs` e cancelada na etapa de implementação ao descobrir limitação técnica: a fonte de dados planejada (kubelet `/stats/summary`, endpoint `/api/v1/nodes/{node}/proxy/stats/summary`) **não reporta `pvcRef`/`usedBytes`/`capacityBytes` para volumes `hostPath`** — o `volume.Metrics` não é implementado por esse plugin. Testado no `greencap-demo` (StorageClass `standard` = `k8s.io/minikube-hostpath`): nenhuma PVC (`redis-data`, `registry-storage`) aparece no `/stats/summary`, nem mesmo as montadas por Pods `Running`. Mesma limitação provavelmente afeta `local-path-provisioner` (k3s/kind), candidato do item acima. Caminho alternativo a avaliar quando retomar: `exec df`/`stat -f` no Pod que monta a PVC via Fabric8 (RBAC `pods/exec` em vez de `nodes/proxy`), funciona independente do storage backend desde que o container tenha `df` disponível.

### ⚪ Baixa Prioridade

#### 🔵 Gerenciamento ativo — próximas operações de escrita

- **Atualizar imagem do Deployment (`kubectl set image`)** — patch em `spec.template.spec.containers[].image`. Requer UI para escolher o container quando o Pod tem múltiplos (multi-container) — maior complexidade de UX que as demais ações de write já implementadas.

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

### Sprint 91 ✅ — Helm: Repositories, Deploy from Helm, Upgrade e fix de logs em pods Pending

- `HelmRepository` (entidade JPA, `HelmRepositoryRepository`, `HelmRepositoryService`): repos persistidos por cluster; `V31__create_helm_repositories.sql`
- `HelmService.install()`: `--create-namespace` para criar namespace se ausente; `ensureRepos()` re-adiciona todos os repos antes de cada operação; `--reuse-values` no `upgrade()`
- `HelmService.upgrade()`: aceita nova versão e values editados; re-adiciona repos antes da execução
- `Permission.PROJECT_HELM_INSTALL/UPGRADE`; `V32__add_helm_install_upgrade_permissions.sql`
- `HelmRepositoriesView` (rota `helm/repositories`): grid Name/URL; botão "Add Repository" no section header; `SelectionAction` Remove com `ConfirmDialog`; sub-item "Repositories" na seção Helm do sidebar
- `DeployFromHelmView` (rota `deploy/helm`): 4º modo em New Application; wizard 3 passos (Chart → Config → Values & Install); Back como ícone puro; footer com spacer para alinhar Next/Install à direita; após install atualiza namespace ativo via `clusterContext.setNamespace` + `userService.updateActiveNamespace` e navega para `HelmReleasesView`
- `HelmReleasesView`: `SelectionAction` Upgrade com dialog pré-preenchido com values atuais e campo de nova versão
- `ObservabilityService.fetchPodLogs()`: guard para pods em fase `Pending` — retorna mensagem informativa imediatamente em vez de bloquear até timeout
- `gradle.properties`: bump `0.7.1` → `0.7.2`
- Issues: `.scratch/sprint-91/issues/` (6 issues, todas `done`)

### Sprint 90 ✅ — Helm Releases: listagem, detalhes e uninstall via Helm CLI

- `HelmService`: executa operações Helm via `ProcessBuilder`; kubeconfig descriptografado gravado em tempfile com permissão `600` e deletado em `finally`; `listReleases` faz parse do `helm list -o json` via Jackson; `getReleaseDetails` executa `helm get notes/values/manifest` e agrega em `HelmReleaseDetails`; `uninstall` executa `helm uninstall`; `HelmOperationException` em falha de processo ou CLI ausente
- DTOs: `HelmReleaseInfo` (name, namespace, chart, appVersion, revision, status, updated), `HelmReleaseDetails` (notes, values, manifest)
- `HelmReleasesView` (rota `helm/releases`): grid com filtro embutido no header da coluna Name; badge de status (`deployed` → success, `failed` → error, demais → contrast); botões "Details" e "Uninstall" como `SelectionAction` no section header; dialog de detalhes com abas Notes/Values/Manifest em `Pre` monoespaçado, carregado async; dialog de uninstall type-to-confirm
- `Dockerfile`: Helm `v4.2.2` baixado no builder stage, binário copiado para `/usr/local/bin/helm` no runtime stage
- `setup.sh`: verifica e instala `helm` automaticamente junto com demais dependências
- `Permission`: `PROJECT_HELM_VIEW` + `PROJECT_HELM_UNINSTALL`; `V30__add_helm_permissions.sql`
- `MainLayout`: seção Helm com sub-item "Releases" abaixo de Storage em Project
- `UserManagementView`: grupo "Helm" na treeview de permissões
- `CONTEXT.md`: novos termos `Helm`, `HelmRelease`, `Uninstall (Helm)`; ADR 0012
- Issues: `.scratch/sprint-90/issues/` (4 issues, todas `done`)

### Sprint 89 ✅ — PersistentVolumes: operação Delete com guard de Bound e badge de status

- `StorageService.deletePersistentVolume(Cluster, String)`: remove o PV via Fabric8 `client.persistentVolumes().withName(name).delete()`
- `Permission.GLOBAL_INFRASTRUCTURE_PV_DELETE`: nova permissão concedida a usuários com `GLOBAL_INFRASTRUCTURE_CORDON`; `V29__add_pv_delete_permission.sql`
- `PersistentVolumesView`: botão Delete como `extraLeadingButton` no section header; habilitado quando PV selecionado (qualquer status); ao clicar em PV `Bound` exibe `ConfirmDialog` informativo com nome da claim e instrução para deletar a PVC primeiro; ao clicar em PV não-Bound exibe `ConfirmDialog` de confirmação padrão (`ConfirmButtonTheme error primary`); badge `Bound` alterado para `success` (verde) — consistente com `PersistentVolumeClaimsView`
- `CONTEXT.md`: `PersistentVolume` atualizado para incluir Delete e guard de Bound; novo termo `Delete PersistentVolume`
- `gradle.properties`: bump de `0.7.0` → `0.7.1`
- Issues: `.scratch/sprint-89/issues/` (2 issues, ambas `done`)

### Sprint 88 ✅ — Developer Experience: seção no sidebar + Kubernetes Operators via OLM

- Seção **Developer Experience** adicionada ao sidebar do `MainLayout` (entre Global e Settings), com badge `beta` em fonte menor no item pai
- `KubernetesOperatorService`: detecta presença do OLM (`isOlmInstalled`), lista operators instalados via `Subscription` + `ClusterServiceVersion`, lista catálogo via `PackageManifest` de todos os `CatalogSource`s, instala via `Subscription` + `OperatorGroup` (AllNamespaces), desinstala removendo Subscription + CSV; status derivado do CSV phase com fallback para `Subscription.status.state` quando CSV ainda não existe (`Failed`/`ResolutionFailed` → badge vermelho com tooltip do reason)
- DTOs: `OperatorInfo`, `OperatorPackage`, `OperatorChannel`
- `InstalledOperatorsView` (rota `developer-experience/operators/installed`): grid com filtro embutido no header da coluna Name; botão Uninstall como `SelectionAction` destrutiva no section header; dialog type-to-confirm; OLM missing empty state; badge de fase (`Installing`/`Succeeded`/`Failed`) com tooltip no `Failed`
- `OperatorCatalogView` (rota `developer-experience/operators/catalog`): carrega catálogo uma vez com `ProgressBar` indeterminate; filtro de nome no header da coluna Name; filtro de CatalogSource no header da coluna Catalog; botão Install como `SelectionAction` no section header; `refresh()` é no-op (evita reload a cada tick do auto-refresh); botão Refresh manual força reload; após install navega para InstalledOperatorsView
- `Permission`: 3 novas permissões `DEVELOPER_EXPERIENCE_OPERATORS_VIEW/INSTALL/UNINSTALL`; `V28__add_developer_experience_operator_permissions.sql`
- `DataInitializer`: garante que o admin sempre tenha todas as permissões atuais em qualquer startup (não apenas na criação)
- `setup/setup.sh` e `samples/greencap-demo/cluster-provision.sh`: addon `olm` habilitado com `kubectl rollout status deployment/olm-operator` de espera
- `UserManagementView`: grupo "Kubernetes Operators" na treeview de permissões da seção Developer Experience
- `CONTEXT.md`: novos termos `Developer Experience`, `Kubernetes Operator`, `Install Operator`, `Uninstall Operator`; `Global` atualizado
- `docs/adr/0011-olm-como-framework-de-gerenciamento-de-operators.md`: decisão de usar OLM (openshift-client já presente) em vez de CRD discovery puro
- Issues: `.scratch/sprint-88/issues/` (4 issues, todas `done`)

### Sprint 87 ✅ — Setup wizard: script de instalação da plataforma GreenCap no minikube

- `setup/setup.sh`: script idempotente em 7 etapas — verifica/instala ferramentas ausentes (docker, kubectl, minikube) com `sg docker` para ativar grupo sem logout; menu de perfil (Minimal/Recommended/Custom); inicia minikube com driver docker; habilita addons metrics-server, ingress e registry (PVC 8 Gi + nodeSelector para persistência); build + push da imagem via registry-proxy; cria Secret `greencap-secrets` com `DB_PASSWORD`, `GREENCAP_ENCRYPTION_KEY` e `GREENCAP_SELF_CLUSTER_KUBECONFIG`; aplica manifests e aguarda rollout; adiciona entrada `greencap.local` no `/etc/hosts` automaticamente se ausente
- `setup/teardown.sh`: exige confirmação `yes` antes de deletar o profile minikube `greencap-platform`
- `setup/manifests/`: 7 manifests Kubernetes — namespace, PVC Postgres (2 Gi), Deployment Postgres 16, Service ClusterIP `greencap-db`, Deployment GreenCap (imagePullPolicy Always), Service ClusterIP `greencap`, Ingress `greencap.local`
- `DataInitializer`: auto-registra o cluster `greencap-platform` na primeira inicialização quando `GREENCAP_SELF_CLUSTER_KUBECONFIG` está definido; define como cluster e namespace ativos do admin
- `ClusterRepository`: método `existsByName(String)` para idempotência do auto-registro
- `MainLayout`: namespace combobox alargado de 180 px para 220 px; fix de seleção do namespace inicial via dois ciclos de push separados (itens primeiro, valor depois)
- Issues: `.scratch/sprint-87/issues/` (2 issues, ambas `done`)

### Sprint 86 ✅ — EventsView: seletor de limite de Events exibidos

- `ObservabilityService.listEvents()`: novo parâmetro `int limit` (0 = All); stream truncado após ordenação por `lastTimestamp` desc — garante sempre os N mais recentes
- `EventsView`: `Select<String>` com opções 50/100/200/500/All (padrão 100) inserido no section header entre o título e o botão refresh; mudança de valor recarrega imediatamente; auto-refresh respeita o limite selecionado
- `EventsDialog` (events por recurso específico) não alterado — continua sem limite
- Issue: `.scratch/sprint-86/issues/01-events-view-limit-selector.md`

### Sprint 83 ✅ — Import Compose: wizard 3 passos para importar docker-compose.yml de Git Repository público

- `ComposeParser` (novo, `kubernetes/compose/`): busca o `docker-compose.yml` via HTTP raw do GitHub/GitLab, faz parse com SnakeYAML `SafeConstructor`; classifica variáveis de ambiente com heurística de chave sensível (`PASSWORD`, `SECRET`, `TOKEN`, `KEY`, `CREDENTIAL` → Secret; restante → ConfigMap); volumes named → PVC, bind-mounts → aviso; `depends_on:` e diretivas não suportadas → lista de avisos consolidada
- `ComposeDocument` (novo): modelo intermediário do Compose parseado — `ParsedService`, `BuildSpec`, `EnvEntry`, `VolumeEntry`
- `ImportComposeService` (novo, `kubernetes/compose/`): provisiona Namespace, ConfigMap (`<service>-config`), Secret (`<service>-secret`), PVC (`<service>-pvc`), Deployment e Service ClusterIP para cada serviço; labels `app.kubernetes.io/part-of: <namespace>` + `app.kubernetes.io/component: <service-name>` em todos os recursos para agrupamento em Topologia; tenta todos os serviços antes de reportar falhas (sem rollback)
- `ImportComposeView` (nova, rota `deploy/compose`): view independente com wizard de 3 passos — (1) Git URL + branch + path + namespace; (2) revisão por serviço com campos editáveis (imagem para `build:` pré-preenchida como `localhost:5000/<namespace>/<service>:latest`, PVC StorageClass/size, warnings de bind-mounts/`depends_on:`/diretivas ignoradas); (3) execução — Builds Kaniko sequenciais com log live + grid de status em 2 colunas + provisionamento K8s + resultado; em sucesso total navega para Topologia do novo Namespace
- `DeployApplicationView`: seletor de modo "Deploy from Image" / "Deploy from Compose" no topo; botão "Deploy from Compose" navega para `ImportComposeView` (views independentes, sem aninhamento)
- Fixes encontrados no aceite: imagem para `build:` precisava do prefixo `localhost:5000/` (registry-proxy hostPort); contexto de build (`--context-sub-path`) resolvia relativo à raiz do repo em vez de relativo ao diretório do `docker-compose.yml`; nome do repositório inclui namespace como prefixo (`<namespace>/<service>:tag`)
- Padronização de UX: `LUMO_SMALL` aplicado em todos os botões de header e dialog de todas as views (NamespacesView, ClustersView, UserManagementView, DeploymentsView, StatefulSetsView, HorizontalScalerView, ManifestView, RegistryView, HelpDialog, EventsDialog)
- `samples/greencap-demo/`: docker-compose.yml de demo com 5 serviços (postgres, redis, api com `build:`, worker com `build:`, nginx), Dockerfiles e stubs Node.js funcionais para teste do Import Compose end-to-end
- `CONTEXT.md`: novo termo `Import Compose`; `Deploy Application` atualizado com referência ao segundo modo
- Issues: `.scratch/sprint-83/issues/` (4 issues, todas `done`)

### Sprint 82 ✅ — Karibu-Testing: testes de views Vaadin — dialogs destrutivos

- `build.gradle.kts`: dependência `karibu-testing-v24:2.1.2` adicionada
- `KaribuTest` (nova classe base): `MockVaadin.setup/tearDown()` + helper `loginAs(String... authorities)` para configurar o `SecurityContextHolder` sem Spring; testes de view rodam sem TestContainers, sem banco, em ~1s
- `NamespacesViewTest` (novo, 4 cenários): guard de system namespace (`kube-system` exibe notificação de erro sem abrir dialog), estado inicial do dialog (botão Delete desabilitado), type-to-confirm com nome errado (permanece desabilitado) e com nome correto (habilita)
- `ClustersViewTest` (novo, 1 cenário): confirmação de remoção de cluster chama `clusterService.deleteCluster()` com o cluster selecionado
- `docs/adr/0010-karibu-para-testes-de-views-vaadin.md` (novo): decisão de usar Karibu (in-memory, sem browser) em vez de Selenium/Playwright para cobrir lógica de orquestração de views
- `CLAUDE.md`: fluxo de sprint atualizado — novo passo 6 (Testes) após o aceite manual, cobrindo as duas frentes: views Karibu e integração com `PostgresIntegrationTest`
- Issue: `.scratch/sprint-82/issues/01-karibu-destructive-dialog-tests.md`

### Sprint 77 ✅ — Topologia: nó Ingress + botão "Go to resource" + pré-filtro ?name= nas views

- `CONTEXT.md`: `Topologia` atualizado — adicionado Ingress aos tipos de nó e ao fluxo de edges (Ingress→Service via `spec.rules[].http.paths[].backend.service.name`, apenas para Services existentes no namespace); comportamento de clique alterado de "navega para o Manifest" para "Go to resource" (navega para a view do recurso pré-filtrada por nome); `TopologyNode` atualizado com tipo Ingress e URL de resource view em vez de link para Manifest
- `TopologyService`: listagem de Ingresses via Fabric8 `client.network().v1().ingresses()`; `ingressNode()` com ingressClass/hosts/TLS; `extractBackendServiceNames()` extrai backends de `spec.rules[].http.paths[].backend.service` e `spec.defaultBackend.service`; método `manifestUrl()` renomeado para `resourceViewUrl()` mapeando cada tipo para a rota da view com `?name=`; parâmetro `namespace` removido dos métodos `*Node()` que não o utilizavam mais
- `topology-graph.ts`: cor Ingress `#06B6D4` (ciano) em `NODE_COLORS`; cor das arestas de `#CBD5E1` para `#64748B` (contraste contra fundo dos grupos); fcose separado da inicialização do Cytoscape (`layout: preset` no construtor + `cy.layout(fcose).run()` após); `fixedNodeConstraint` desabilitado quando compound nodes presentes (fcose não suporta o mix); posições salvas aplicadas manualmente após o layout quando grouping ativo
- `TopologyNodeDrawer`: bloco `isIngress` exibe Hosts, badge TLS (`buildTlsBadgeRow()`), IngressClass; botão "Go to resource" substitui "Ver YAML"; pod groups usam "Go to Pods"
- `DeploymentsView`, `ReplicaSetView`, `ServicesView`, `PersistentVolumeClaimsView`, `IngressView`: `nameFilter` promovido a instância; `beforeEnter` lê `?name=` e aplica `nameFilter.setValue()` seguindo o padrão `?job=` do `PodsView`
- `samples/greencap-demo/manifests/`: label `app.kubernetes.io/part-of: greencap-demo` adicionada em todos os 13 manifests (Deployments e CronJob também no pod template); recursos aplicados e Deployments reiniciados para propagação das labels
- Issues: `.scratch/sprint-77/issues/01-ingress-topology-node.md`, `02-goto-resource-button.md`, `03-prefilter-name-query-param.md`

### Sprint 76 ✅ — Namespaces View: listagem com contagens de recursos, Create e Delete Namespace

- `NamespaceInfo`: +campos `podCount`, `deploymentCount`, `serviceCount`
- `NamespaceService`: `listNamespacesWithCounts()` (4 chamadas Fabric8 — namespaces, pods, deployments, services — agrupadas por namespace em uma única conexão), `createNamespace()`, `deleteNamespace()`; `listNamespaceNames()` passa a filtrar namespaces em fase `Terminating` para que namespaces deletados não reapareçam no combobox da navbar durante a exclusão em cascata
- `Permission.GLOBAL_NAMESPACES_VIEW/WRITE/DELETE` (novos): VIEW concedido a todos com `GLOBAL_CLUSTERS_VIEW`; WRITE e DELETE concedidos a usuários com `GLOBAL_CLUSTERS_WRITE`; `V26__add_namespace_permissions.sql`
- `NamespacesView` (nova, rota `global/namespaces`): grid com Name/Status/Pods/Deployments/Services/Age; botão "Create Namespace" (visível com WRITE) — dialog com campo de nome validado por regex DNS; ação "Delete" (visível com DELETE) — dialog exige digitar o nome do namespace antes de confirmar, com aviso explícito de destruição cascata; system namespaces (`kube-system`, `kube-public`, `kube-node-lease`, `default`) bloqueados para deleção; após create/delete chama `MainLayout.refreshClusterState()` para recarregar o combobox de namespaces; se o namespace deletado era o namespace ativo, o contexto é zerado antes do refresh (seleciona "default" automaticamente); async load com padrão `CompletableFuture` + `UiConstants.VIRTUAL_THREADS`
- `MainLayout`: item "Namespaces" (ícone `FOLDER_O`) na seção Global, entre Clusters e Infrastructure, adicionado à lista `clusterDependentNavItems`
- `UserManagementView`: grupo "Namespaces" com as 3 permissões na treeview Global
- `CONTEXT.md`: entradas `Namespace`, `Create Namespace`, `Delete Namespace` detalhadas; `Global` atualizado para incluir Namespaces
- Issue: `.scratch/sprint-76/issues/01-namespaces-view.md`

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
