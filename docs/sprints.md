# GreenCap K8s — Sprints & Demandas

> Documento vivo. Atualizar a cada sprint concluída ou nova demanda identificada.

---

## Status Geral

| Sprint | Tema | Status |
|--------|------|--------|
| 74 | Container Registry — Remove Repository e Remove Tags com multi-seleção | ✅ Concluído |
| 75 | Deploy Application — wizard multi-step para criar Namespace + Deployment + Service + PVC + Ingress a partir de imagem | ✅ Concluído |
| 76 | Namespaces View — Global: listagem com contagens de recursos, Create e Delete Namespace | ✅ Concluído |
| 77 | Topologia: nó Ingress + botão "Go to resource" + pré-filtro ?name= nas views | ✅ Concluído |
| 78 | Topologia: correções de layout (randomize), tap em group nodes e botão Reset Positions | ✅ Concluído |
| 79 | UX — Padronização de header: ClustersView e UserManagementView com buildSectionHeader | ✅ Concluído |
| 80 | Add Cluster dialog — provider Minikube (Docker), aviso OpenShift e comando kubectl copiável | ✅ Concluído |
| 81 | Testes automatizados: TestContainers + cobertura de services críticos | ✅ Concluído |
| 82 | Karibu-Testing: testes de views Vaadin — dialogs destrutivos | ✅ Concluído |
| 83 | Import Compose — wizard 3 passos para importar docker-compose.yml de Git Repository público e provisionar recursos Kubernetes | ✅ Concluído |

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

#### 🚀 New Application — terceiro modo de deploy

- **Deploy from Dockerfile** — terceiro modo na `DeployApplicationView` (ao lado de "Deploy from Image" e "Deploy from Compose"). O usuário informa um Git Repository público (URL + branch + context path + Dockerfile path), GreenCap dispara um Build Kaniko (ADR 0007) para construir e empurrar a imagem para o Registry interno, e em seguida provisiona os recursos Kubernetes (Deployment, Service ClusterIP opcional, PVC opcional) exatamente como o "Deploy from Image" — com a diferença de que a imagem vem do Build em vez de ser informada manualmente. A tela de configuração intercala os campos do Git Repository antes dos campos de imagem/porta, e o passo de execução mostra o log do Build antes da criação dos recursos. Reusa a infra de `RegistryService.startBuild` + `ObservabilityService.fetchPodLogs` já disponível.

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

### Sprint 81 ✅ — Testes automatizados: TestContainers + cobertura de services críticos

- `build.gradle.kts`: dependências TestContainers adicionadas (`spring-boot-testcontainers`, `postgresql`, `junit-jupiter`); H2 removido
- `application-test.yaml`: configuração de datasource/H2/Flyway manual removida; mantida apenas a chave de encriptação — o `@ServiceConnection` do Spring Boot 3 auto-configura o datasource a partir do container
- `GreenCapApplicationTests`: reescrito com `@Testcontainers` + `@ServiceConnection` + `PostgreSQLContainer("postgres:16")`; valida implicitamente que todas as migrations Flyway executam sem erro no PostgreSQL real
- `PostgresIntegrationTest` (nova classe base): `@SpringBootTest(webEnvironment = MOCK)` + container estático compartilhado entre subclasses; `MOCK` necessário pois o `SpringBootAutoConfiguration` do Vaadin exige `WebApplicationContext`
- `WorkloadServiceTest` (nova): 4 cenários com `@EnableKubernetesMockClient(crud = true)` — mapeamento de campos de `PodInfo`/`DeploymentInfo`, filtro por namespace vs. `"all"`, propagação de exceção Fabric8 como `KubernetesOperationException`
- `NamespaceServiceTest` (nova): 4 cenários — filtro de namespaces em fase `Terminating`, contagens de recursos por namespace, criação de namespace, propagação de exceção
- `UserServiceTest` (nova): 4 cenários — authorities corretas em `loadUserByUsername`, usuário inativo lança `UsernameNotFoundException`, senha armazenada como hash BCrypt nunca plaintext, usuário inexistente lança `UsernameNotFoundException`
- `ClusterServiceTest` (nova): 3 cenários — kubeconfig encriptado no banco nunca plaintext, `createdBy` populado a partir do `SecurityContext`, `markAsDisconnectedIfConnected` só altera clusters `CONNECTED`
- `CLAUDE.md`: fluxo de sprint atualizado — compilar a cada mudança relevante; `./gradlew test` somente antes do fechamento da sprint
- Issues: `.scratch/sprint-81/issues/` (5 issues)

### Sprint 80 ✅ — Add Cluster dialog: provider Minikube (Docker), aviso OpenShift e comando kubectl copiável

- `ClusterProvider`: enum renomeado de `Kubernetes` → `MinikubeDocker`; método `displayName()` retorna `"Minikube (Docker)"` / `"OpenShift"`
- `V27__rename_provider_kubernetes_to_minikube_docker.sql`: DROP/re-ADD CHECK constraint; UPDATE `'Kubernetes'` → `'MinikubeDocker'` em linhas existentes
- `ClustersView` grid: coluna Provider usa `displayName()` em vez de `.name()`
- `ClustersView` dialog: Select usa `ItemLabelGenerator` com `displayName()`; default alterado para `MinikubeDocker`; ao selecionar OpenShift, exibe aviso inline `"OpenShift support is coming in a future release."` e desabilita o botão Save; code block escuro com `kubectl config view --flatten --minify` e botão de cópia via `navigator.clipboard.writeText`
- `CONTEXT.md`: entrada `ClusterProvider` atualizada com os valores reais (`MinikubeDocker`, `OpenShift`)
- Issue: `.scratch/sprint-80/issues/01-add-cluster-dialog-ux.md`

### Sprint 79 ✅ — UX: padronização de header em ClustersView e UserManagementView

- `ClustersView`: `buildToolbar()` com `H2` removido; substituído por `UiConstants.buildSectionHeader` com H3 + botão "Add Cluster" (`LUMO_PRIMARY + LUMO_SMALL`) como extra leading button; ações "Test Connection" (`VaadinIcon.CONNECT`) e "Remove" (`VaadinIcon.TRASH`, destrutivo) movidas para `SelectionAction` no header (habilitadas pela seleção de linha); coluna de ações inline removida do grid; `GridSelectionMemory` injetado com `configureSingleSelection`; `refreshGrid()` retorna `boolean` para uso como `BooleanSupplier`; imports `H2` e `HorizontalLayout` removidos
- `UserManagementView`: mesmo padrão — botão "Add User" (`LUMO_PRIMARY + LUMO_SMALL`); ações "Edit Permissions" (`VaadinIcon.EDIT`) e "Deactivate" (`VaadinIcon.BAN`, destrutivo) como `SelectionAction`; proteções antes feitas via botão desabilitado por linha passaram para early-exit com toast em `openEditPermissionsDialog` (admin padrão bloqueado) e `confirmDeactivate` (auto-desativação e usuário já inativo bloqueados); imports `H2` e `H4` removidos
- Issues: `.scratch/sprint-79/issues/01-clusters-view-header-padrao.md`, `02-user-management-view-header-padrao.md`

### Sprint 78 ✅ — Topologia: correções de layout (randomize), tap em group nodes e botão Reset Positions

- `topology-graph.ts`: `randomize` dinâmico — `true` quando posições salvas ausentes (fix para nós empilhados na primeira renderização), `false` quando presentes (mantém layout salvo); guard `if (node.data('isGroup')) return` no tap handler (fix para painel lateral não abrir ao clicar em group nodes)
- `TopologyLayoutRepository`: `deleteByUserIdAndClusterIdAndNamespace` (método derivado Spring Data)
- `TopologyLayoutService`: `deleteLayout()` deleta o registro de posições salvas para user + cluster + namespace
- `TopologiaView`: botão "Reset positions" (ícone refresh, estilo LUMO_TERTIARY + LUMO_ICON + LUMO_CONTRAST, ao lado do botão Help) — deleta o layout salvo e navega para a mesma rota, forçando nova renderização com `randomize: true`
- Issues: `.scratch/sprint-78/issues/01-fix-randomize-layout.md`, `02-fix-group-node-tap.md`, `03-reset-positions-button.md`

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
