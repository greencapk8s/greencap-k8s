# GreenCap K8s — Sprints & Demandas

> Documento vivo. Atualizar a cada sprint concluída ou nova demanda identificada.

---

## Status Geral

| Sprint | Tema | Status |
|--------|------|--------|
| 95 | Bug fix: RBAC fail-closed + propagação de SecurityContext em virtual threads + feedback na tela Users | ✅ Concluído |
| 94 | K8s RBAC substituindo sistema de permissões interno | ✅ Concluído |
| 93 | Métodos alternativos de registro de cluster: Token + URL + remoção de ClusterProvider | ✅ Concluído |
| 92 | Editor de código YAML (CodeMirror 6) + ícone Helm leme + bug fixes de resiliência | ✅ Concluído |
| 91 | Helm: Repositories, Deploy from Helm (wizard), Upgrade e fix de logs em pods Pending | ✅ Concluído |
| 90 | Helm Releases — listagem, detalhes (Notes/Values/Manifest) e uninstall via Helm CLI | ✅ Concluído |
| 89 | PersistentVolumes — operação Delete com guard de Bound e badge de status | ✅ Concluído |
| 88 | Developer Experience: seção no sidebar + Kubernetes Operators (listar, instalar, desinstalar via OLM) | ✅ Concluído |
| 87 | Setup wizard: script de instalação da plataforma GreenCap no minikube | ✅ Concluído |
| 86 | EventsView — seletor de limite de Events exibidos (50/100/200/500/All, padrão 100) no section header | ✅ Concluído |

---

## Backlog

> Itens sem sprint definida, organizados por prioridade (Alta, Média, Baixa).

### 🟡 Média Prioridade

#### 🔗 Registro de Cluster — métodos alternativos ao kubeconfig

- **Token + URL** — segundo método de registro de cluster: o usuário informa apenas o endpoint da API Kubernetes (`https://...`) e um bearer token de service account. Mais acessível para iniciantes que não sabem localizar o kubeconfig, e o fluxo natural para clusters gerenciados (GKE, EKS, AKS) que expõem esses dois valores no console do provedor. Internamente, o Fabric8 constrói o `Config` via `withMasterUrl()` + `withOauthToken()` — sem necessidade de gerar um arquivo kubeconfig. A tela de registro (`ClustersView`) ganha um toggle para escolher o método: _Kubeconfig_ (atual) ou _Token + URL_. Limitação esperada: suporta apenas autenticação por bearer token; certificado de cliente e OIDC não são cobertos por este método.

- **In-cluster** — terceiro método de registro: quando o GreenCap roda dentro de um cluster Kubernetes, ele pode auto-detectar o service account do pod (`/var/run/secrets/kubernetes.io/serviceaccount/token` + CA bundle) sem nenhuma credencial manual. Útil para quem instala o GreenCap no próprio cluster que quer gerenciar. O Fabric8 suporta via `Config.autoConfigure()` quando rodando in-cluster. No fluxo de registro, seria uma opção "Usar cluster atual" disponível apenas quando a plataforma detectar que está rodando dentro de um pod Kubernetes.

#### 🧵 Consolidar execução assíncrona em virtual threads

- **Executor único com propagação de `SecurityContext`** — a correção de fail-closed do `KubernetesClientFactory` (sprint 94, fix de 2026-06-30/07-02) expôs que o `SecurityContextHolder` (baseado em `ThreadLocal`) não é herdado por virtual threads. Antes da correção, isso era mascarado por um fallback silencioso para o kubeconfig admin do Cluster — ou seja, qualquer chamada Kubernetes rodando em background sem contexto propagado recebia acesso admin, independente do usuário. O fix corrigiu o `KubernetesClientFactory`, mas isso só resolveu o sintoma em cada um dos ~16 pontos que disparavam `Thread.ofVirtual().start(...)`/`ScheduledExecutorService` próprios em vez de reusar o `UiConstants.VIRTUAL_THREADS` já existente — cada view reinventou sua própria forma de rodar em background (`DeployApplicationView`, `DeployFromDockerfileView`, `DeployFromHelmView`, `ImportComposeView`, `TopologiaView`, `BuildLogsView`, `PodLogsView`, `MainLayout`, `DashboardView`, todas com o mesmo bug). Causa raiz real: duplicação — ausência de um único ponto de acesso assíncrono no projeto. Follow-up: extrair um helper compartilhado (ex.: `SecurityAwareVirtualThreads` ou consolidar tudo em `UiConstants.VIRTUAL_THREADS`, já corrigido com `DelegatingSecurityContextExecutor`) e migrar os `ScheduledExecutorService` de polling (`BuildLogsView`, `PodLogsView`, `DeployFromDockerfileView`, `ImportComposeView`) para reusar a mesma fábrica, eliminando a necessidade de aplicar `DelegatingSecurityContextRunnable` manualmente em cada `scheduleAtFixedRate`.

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

### Sprint 95 ✅ — Bug fix: RBAC fail-closed + propagação de SecurityContext em virtual threads + feedback na tela Users

- `KubernetesClientFactory.resolveKubeconfig`: fail-closed — usuário não-admin sem `serviceaccountToken` provisionado lança `KubernetesOperationException` em vez de cair silenciosamente no kubeconfig admin do Cluster (janela de corrida entre `createUser` e `provisionUser`, transações separadas); seam `resolveKubeconfigForUser(Authentication)` extraída para teste sem mockar `SecurityContextHolder`; `KubernetesClientFactoryTest` (4 casos)
- `Permission.java` e `ClusterProvider.java` removidos — enums órfãos sem nenhum uso real (achado do relatório `/improve-codebase-architecture`)
- Causa raiz mais profunda descoberta durante o teste de aceite: `SecurityContextHolder` (baseado em `ThreadLocal`) não propaga para virtual threads — todo carregamento assíncrono (namespaces, dashboard, deploy wizards, topologia, logs) rodava sem usuário autenticado, e o comportamento pré-fix mascarava isso caindo direto no kubeconfig admin para qualquer usuário, não só os não provisionados
- `UiConstants.VIRTUAL_THREADS`: `Executor` envolvido em `DelegatingSecurityContextExecutor` (spring-security-core, já dependência do projeto) — corrige de uma vez as 8 views que já reusavam esse executor compartilhado
- `DashboardView`: executor privado duplicado removido, passou a reusar `UiConstants.VIRTUAL_THREADS`
- `MainLayout`: `loadNamespacesForCluster` migrado de `Thread.ofVirtual().start()` para `UiConstants.VIRTUAL_THREADS.execute()`; timer de auto-refresh (`applyRefreshInterval`) envolvido em `DelegatingSecurityContextRunnable`
- `DeployApplicationView`, `DeployFromDockerfileView`, `DeployFromHelmView`, `ImportComposeView`, `TopologiaView`: `Thread.ofVirtual().start()` migrado para `UiConstants.VIRTUAL_THREADS.execute()` (14 pontos)
- `BuildLogsView`, `PodLogsView`, `DeployFromDockerfileView`, `ImportComposeView`: pollers (`ScheduledExecutorService` próprio) envolvidos em `DelegatingSecurityContextRunnable`
- `UserManagementView.beforeEnter`: acesso negado (não-admin) agora notifica "Access restricted to administrators" antes do `forwardTo("")`; notificação adiada via `UI.access()` — chamar `Notification.show()` e `forwardTo()` na mesma passada do `beforeEnter()` descarta o push ao cliente (armadilha conhecida do Vaadin Flow), confirmado com teste Karibu descartável
- Backlog: item novo "Consolidar execução assíncrona em virtual threads" registrando a causa raiz (duplicação — falta de um único ponto de acesso assíncrono) para follow-up de extração de helper compartilhado
- Sem issues formais em `.scratch/` — fluxo de bug fix pontual (causa e solução evidentes)

### Sprint 94 ✅ — K8s RBAC substituindo sistema de permissões interno

- `user_permissions` table e enum `Permission` removidos; migration `V34__migrate_to_kubernetes_rbac.sql` dropa `user_permissions` e adiciona colunas `serviceaccount_name`, `cluster_role_name`, `serviceaccount_token` em `users`
- `User`: campos `serviceaccountName`, `clusterRoleName`, `serviceaccountToken`; campo `permissions` removido
- `UserService`: `createUser` simplificado (sem permissões); `deactivateUser` substituído por `deleteUser`; `assignKubernetesIdentity` + `clearKubernetesIdentity` para lifecycle do SA
- `SecurityUtils.isAdmin()`: identifica admin pelo username hardcoded `"admin"` (sem verificação de `Permission`)
- `KubernetesClientFactory`: context-aware — admin usa kubeconfig do cluster; usuário regular usa SA token encriptado armazenado no banco; `buildAdminClient` e `buildFromRawKubeconfig` para casos especiais
- `UserProvisioningService`: cria ServiceAccount + ClusterRoleBinding + token no namespace `greencap-system`; constrói kubeconfig sintético com CA extraído do kubeconfig admin (sem double-encoding); `deprovisionUser` deleta SA + CRB; `updateClusterRole` deleta CRB antigo, cria novo e regenera token
- `KubernetesOperationException.from()`: extrai mensagem legível do cause chain (`KubernetesClientException.getCode()` + fallback por texto "Forbidden"/"403"); exibe "permission denied" para 403, "resource not found" para 404, "conflict" para 409
- `UserManagementView`: reescrita completa — grid sem coluna Status; ação "Edit Role" (troca ClusterRole e regenera token); ação "Remove" (substitui "Deactivate" — deleta usuário + deprovisions SA/CRB); dialog "Add User" com ComboBox de Cluster + ClusterRole dinâmico; restrita a admin via `beforeEnter`
- 35 views Vaadin: todos os guards `SecurityUtils.hasPermission()` e `beforeEnter` de permissão removidos — K8s RBAC é o único guard de autorização
- 14 services Fabric8: migrados de `buildClient(String)` para `buildClient(Cluster)` — factory resolve credenciais pelo SecurityContext
- `MainLayout`: cluster switcher oculto para não-admin
- `CONTEXT.md` atualizado; `docs/adr/0004` supersedido; `docs/adr/0013-kubernetes-rbac-replaces-permission-system.md` criado
- Issues: `.scratch/sprint-94/issues/` (4 issues, todas `done`)

### Sprint 93 ✅ — Métodos alternativos de registro de cluster: Token + URL + remoção de ClusterProvider

- `ClusterProvider` enum removido; migration `V33__remove_cluster_provider.sql` dropa coluna `provider` da tabela `clusters`; `CreateClusterRequest` passa a ter apenas `name` + `kubeconfigContent`
- `ClusterService.synthesizeKubeconfig(url, token, caCert)`: sintetiza kubeconfig mínimo a partir de Token+URL; CA em PEM → base64-encoda para `certificate-authority-data`; sem CA → `insecure-skip-tls-verify: true`
- `ClustersView`: dialog "New Cluster" com `TabSheet` (Token+URL primeiro, Kubeconfig segundo); dialog `560×520px` resizable; aba Token+URL tem campo URL, TextArea de bearer token e `Details` colapsável para CA certificate opcional
- Guards de UX: primeiro cluster registrado é ativado automaticamente; ao deletar cluster ativo, ativa automaticamente o próximo disponível (ou limpa contexto se não há mais)
- `cluster-provision.sh` renomeado para `cluster-setup.sh` via `git mv`; `cluster-teardown.sh` criado com confirmação explícita (`yes`) antes de deletar
- `cluster-setup.sh`: cria service account `greencap-admin` com `cluster-admin` + token de 1 ano; exibe URL e token no final do provisionamento
- `CONTEXT.md`: definição de `Cluster` atualizada para refletir múltiplos métodos de registro (kubeconfig e Token+URL como inputs alternativos)
- Testes: `ClusterServiceTest`, `ClustersViewTest`, `RegistryViewTest`, `NamespacesViewTest` atualizados para remover `ClusterProvider`

### Sprint 92 ✅ — Editor de código YAML (CodeMirror 6) + ícone Helm leme + bug fixes de resiliência

- `CodeMirrorEditor` (componente Vaadin customizado): Lit + `@NpmPackage` (`codemirror`, `@codemirror/lang-yaml`, `@codemirror/theme-one-dark`); syntax highlighting YAML, line numbers, indent 2 espaços, fonte mono; tema sincronizado via `closest('[theme~="dark"]')` + MutationObserver subtree; altura configurável; API: `getValue/setValue/setReadOnly/focus`
- `ManifestView`: substituído `Pre` + `TextArea` por `CodeMirrorEditor` permanente; `readOnly=true` na visualização, `readOnly=false` no edit; campo `lastLoadedYaml` para restaurar no Cancel sem reload de rede
- `DeployFromHelmView`, `HelmReleasesView`: `TextArea` de values substituído por `CodeMirrorEditor`; dialog de Upgrade com `setResizable(true)`, tamanho inicial 720×520px e editor flex-grow
- `helm.svg` em `META-INF/resources/icons/helm.svg`: símbolo ⎈ (leme, 8 raios) com `currentColor`; `MainLayout.buildHelmNavItem()` usa `SvgIcon` em vez de `VaadinIcon.PACKAGE`
- `MainLayout`: ícone ⓘ como `setSuffixComponent` no item "Repositories" com tooltip "cluster-scoped"
- `HelmReleasesView`: coluna Updated truncada para `yyyy-MM-dd HH:mm`; `refresh()` com guard `clusterErrorMessage.isVisible()` — para de disparar quando cluster em erro
- `HelmService`: `USER-SUPPLIED VALUES:` stripped do output de `helm get values`; `LIST_TIMEOUT_SECONDS=8` para `listReleases` (era 30s) — evita acúmulo de virtual threads bloqueados
- `DeployFromDockerfileView`, `ImportComposeView`: botão "Deploy from Helm" adicionado ao mode selector (estava ausente)
- `setup.sh`: guard de `/etc/hosts` verifica IP atual vs. `$MINIKUBE_IP` e atualiza se divergente

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
