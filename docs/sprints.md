# GreenCap K8s — Sprints & Demandas

> Documento vivo. Atualizar a cada sprint concluída ou nova demanda identificada.

---

## Status Geral

| Sprint | Tema | Status |
|--------|------|--------|
| 47 | Topologia — agrupamento de nós por labels part-of/component | ✅ Concluído |
| 48 | Topologia — migração para layout fcose (elimina sobreposição de grupos) | ✅ Concluído |
| 49 | Topologia — persistência do TopologyLayout (posições dos nós + toggle) | ✅ Concluído |
| 50 | Demo: cluster-provision + UX async loading | ✅ Concluído |
| 51 | Gerenciamento ativo — Delete em todas as views PROJECT | ✅ Concluído |
| 52 | Fix — Navbar não acompanha o hide do drawer | ✅ Concluído |
| 53 | Versão da plataforma visível no rodapé do drawer | ✅ Concluído |
| 54 | Manutenção — archiving de sprints.md e .scratch | ✅ Concluído |
| 55 | Docker: Quick Start ponta a ponta (Dockerfile + compose + profile prod) | ✅ Concluído |
| 56 | Infrastructure — Cordon/Uncordon de Nodes | ✅ Concluído |

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

---

## Sprints Concluídas

> Mostra apenas as últimas 10 sprints. Histórico completo em `docs/sprints-archive.md` (ver `docs/agents/sprint-archiving.md`).

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
