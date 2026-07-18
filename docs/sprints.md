# GreenCap K8s — Sprints & Demandas

> Documento vivo. Atualizar a cada sprint concluída ou nova demanda identificada.

---

## Status Geral

| Sprint | Tema | Status |
|--------|------|--------|
| 105 | Topologia: setas de ServiceDependency (Workload→Service inferido via env/ConfigMap/Secret) + StatefulSet como nó (pré-requisito) | ✅ Concluído |
| 104 | Username no header + Developer Experience como 1ª seção do menu (New Application incorporado) + fix de duplicação nos 4 wizards de deploy | ✅ Concluído |
| 103 | Templates Catalog: ação "Uninstall Template" no card instalado (deleta o Namespace; estado transitório "Uninstalling" com auto-heal) | ✅ Concluído |
| 102 | Templates Catalog: ação "Open Topology" no card de Template instalado (entra na Namespace da solução e abre a Topologia) | ✅ Concluído |
| 101 | Bug fixes do selector de Namespace no header: refresh após Deploy Application/Dockerfile/Compose + seleção preservada no full reload (F5) | ✅ Concluído |
| 100 | Suporte nativo a macOS no setup.sh (Homebrew/Colima) + workflows GitHub Actions validando setup completo em Linux e macOS | ✅ Concluído |
| 99 | Dois novos Templates no catálogo (greencap-templates): CRUD Flask+MongoDB e Cache-aside Flask+PostgreSQL+Redis | ✅ Concluído |
| 98 | Templates Catalog — Developer Experience: catálogo de Templates (repositório greencap-templates) com deploy em um clique | ✅ Concluído |
| 97 | Hotfix: propagação de SecurityContext em polling agendado (AsyncTasks.schedulePolling) | ✅ Concluído |
| 96 | Consolidar execução assíncrona em virtual threads — AsyncTasks como ponto único | ✅ Concluído |

---

## Backlog

> Itens sem sprint definida, organizados por prioridade (Alta, Média, Baixa).

### 🟡 Média Prioridade

#### 🐛 Bug: `UI.navigate(String)` com query string embutida (`?param=`) pode não navegar corretamente

- **Descoberto ao escrever a cobertura de teste da Sprint 105**: o botão "Go to resource"/"Go to `<service>`" do `TopologyNodeDrawer` navega via `ui.navigate(url)` (overload de 1 argumento) para URLs como `networking/services?name=postgres-service` (geradas por `TopologyService.resourceViewUrl`). Esse overload delega para `navigate(path, QueryParameters.empty())`, que constrói o `Location` com `parsePathToSegments(path, false)` — ou seja, não separa a query string do path quando ela já vem embutida na String (diferente do construtor de 1 argumento de `Location`, usado internamente pelo Vaadin em outros fluxos). Em tese o roteador tentaria casar a rota inteira `"networking/services?name=postgres-service"` como um único path, o que não bateria com a rota registrada `networking/services`.
- **Mesmo padrão em outros pontos do código, não exclusivo da Sprint 105**: `CronJobsView.java` (`navigate("workloads/jobs?cronjob=" + nome)`) usa a mesma construção. Já `DeploymentsView`/`StatefulSetsView` usam o overload correto de 2 argumentos (`navigate(path, QueryParameters)`) para o mesmo tipo de link — inconsistência dentro do próprio código.
- **Não confirmado em navegador real** — achado apenas em teste automatizado (`TopologyNodeDrawerTest`), que documenta o comportamento observado sob `-ea` (assertions ligadas, padrão da task `test` do Gradle) sem travar no achado (`assertThatThrownBy(...).isInstanceOfAny(NotFoundException.class, AssertionError.class)`); o efeito exato em produção (assertions desligadas no `bootRun`/JAR final) não foi validado manualmente. Avaliar migrar os call sites afetados para `navigate(path, QueryParameters)` explícito.

#### 🐛 Bug: badge de status de Pod não reflete CrashLoopBackOff/BackOff

- **Status do Pod ignora o estado real do container** — descoberto durante o teste manual do Sample Catalog (Sprint 98): com o Postgres fora do ar, o container `backend` entrou em `CrashLoopBackOff`, mas o badge de status na listagem de Pods continuou mostrando "Running". Provável causa: a derivação do badge usa `pod.status.phase` (que permanece `Running` enquanto o Pod em si não falhou totalmente) em vez de inspecionar `containerStatuses[].state.waiting.reason` (`CrashLoopBackOff`, `ErrImagePull`, etc.) e `restartCount`. Afeta pelo menos `PodsView`; avaliar se `DeploymentsView`/`StatefulSetsView` têm o mesmo problema ao agregar status a partir dos Pods.

#### 🌐 Acesso local via `*.greencap.local` — follow-up dos fluxos de Deploy

- **`/etc/hosts` não suporta curinga** — descoberto ao testar o Ingress do Sample Catalog (Sprint 98): a convenção `<namespace>.greencap.local`, usada também em Deploy Application e Deploy from Dockerfile, exige uma linha manual em `/etc/hosts` por aplicação implantada (`/etc/hosts` faz correspondência exata, sem expansão de glob — uma entrada `*.greencap.local` não resolve nada). Conforme o Sample Catalog cresce com mais Templates, essa fricção tende a aumentar. Solução: documentar (ou automatizar via script) um resolver DNS local com curinga real, ex. `dnsmasq` com `address=/.greencap.local/<ip-do-cluster>`, resolvendo qualquer subdomínio de uma vez — no Linux via NetworkManager/dnsmasq, no macOS via dnsmasq + `/etc/resolver/greencap.local`.

#### 🔗 Registro de Cluster — método alternativo ao kubeconfig

> Token + URL foi entregue na Sprint 93 (`ClustersView` com aba dedicada + `ClusterService.synthesizeKubeconfig()`).

- **In-cluster** — segundo método de registro: quando o GreenCap roda dentro de um cluster Kubernetes, ele pode auto-detectar o service account do pod (`/var/run/secrets/kubernetes.io/serviceaccount/token` + CA bundle) sem nenhuma credencial manual. Útil para quem instala o GreenCap no próprio cluster que quer gerenciar. O Fabric8 suporta via `Config.autoConfigure()` quando rodando in-cluster. No fluxo de registro, seria uma opção "Usar cluster atual" disponível apenas quando a plataforma detectar que está rodando dentro de um pod Kubernetes.

#### 🔌 Developer Experience — follow-ups da Sprint 88

- **Custom Resources** — view genérica na seção Developer Experience que lista os tipos de CRD instalados por operators (filtrados por grupo `*.io` de operators gerenciados pelo OLM), exibe instâncias por namespace e permite criar/editar/deletar via YAML reutilizando o mecanismo de Apply existente. Cobre automaticamente qualquer operator instalado (Grafana, Prometheus, cert-manager, KEDA, etc.) sem precisar de painéis específicos por operator. Posicionamento no sidebar: `DEVELOPER EXPERIENCE → Custom Resources`, abaixo de Operators.

#### 🎯 Templates Catalog — follow-ups da Sprint 103

- **`SampleCatalogView` com construtor de 7 parâmetros — candidata a extração** — descoberto ao revisar `SampleCatalogViewTest` após o ajuste do construtor na Sprint 103 (novo parâmetro `NamespaceService` para o Uninstall). O construtor estoura a convenção de código do projeto ("Métodos: máximo de 3 parâmetros, acima disso criar um objeto de request/DTO") e é sintoma de outra — "Classes: responsabilidade única, uma classe tem um motivo para mudar": a view hoje orquestra quatro preocupações distintas com dependências próprias — listagem/instalação (`SampleCatalogService`), deploy com build (`TemplateDeploymentService` + `RegistryService` + `ObservabilityService`), navegação (`ClusterContext` + `UserService`) e uninstall (`NamespaceService`). Avaliar extrair a lógica de deploy+build (as três dependências de build/logs) para um componente ou controller de suporte próprio, reduzindo a lista de colaboradores injetados diretamente na view.

#### ⚡ UX — Carregamento assíncrono nas views restantes

- **Aplicar padrão async + banner "cluster inacessível" nas views de workload** — `DeploymentsView` e `PodsView` já implementados como referência (sprint 50). Aplicar o mesmo padrão nas views restantes que fazem chamadas Kubernetes síncronas no `beforeEnter`: `ServicesView`, `ConfigMapsView`, `SecretsView`, `NodesView`, `EventsView`, `HorizontalScalerView`, `IngressView`, `JobsView`, `CronJobsView`, `ReplicaSetView`, `PersistentVolumeClaimsView`, `PersistentVolumesView`, `StorageClassesView`, `MetricsView`, `TopologiaView`. Padrão: criar `loadXxxAsync(UI ui)` com `CompletableFuture` + `UiConstants.VIRTUAL_THREADS`; adicionar `clusterErrorMessage` via `UiConstants.buildClusterUnreachableMessage()`; exibir banner e ocultar grid em caso de `KubernetesOperationException`.

#### 🟣 StatefulSet — follow-ups da Sprint 61

> "StatefulSet na Topologia" foi entregue na Sprint 105 (nó `StatefulSet` + edge direto `StatefulSet→PodGroup`, sem ReplicaSet intermediário; edge para o Service headless resolvida pelo mesmo matching de selector já existente, sem tratamento especial; PVCs de `volumeClaimTemplates` já cobertos pela mesma lógica genérica de edge `PodGroup→PersistentVolumeClaim`).

- **Coluna Owner em PersistentVolumeClaimsView** — PVCs criados via `volumeClaimTemplates` de um StatefulSet seguem o padrão de nome `<template>-<statefulset>-<ordinal>`; adicionar coluna indicando o StatefulSet de origem (ou "—" para PVCs avulsos), análogo ao Owner de `ReplicaSetView`.
- **Events em StatefulSetsView** — adicionar `SelectionAction.of(VaadinIcon.RECORDS, "Events", sts -> EventsDialog.open(observabilityService, clusterContext, "StatefulSet", sts.name(), sts.namespace()))` na barra de seleção, mesmo padrão de `DeploymentsView` (injetar `ObservabilityService` no construtor).

#### 🟢 Diferencial — visão de cluster

- **Overview multi-cluster** — tela de entrada com health de todos os clusters registrados (ConnectionStatus, namespace count) antes de entrar em um específico.

#### 🐳 Deploy from Compose — follow-ups da Sprint 83

- **Ingress no Deploy from Compose** — a Sprint 83 deixou Ingress fora do escopo v1 (decisão registrada no `/grill-with-docs`). Cada serviço com `ports:` expõe apenas um ClusterIP Service. Follow-up: na tela de revisão, adicionar toggle "Expor externamente (Ingress)" por serviço com porta exposta, com campos de host e IngressClass editáveis — mesmo padrão do Deploy from Image. Avaliar também a criação de um único Ingress com múltiplos path rules (um por serviço), o que permite agrupar todos os endpoints sob um único host.

#### 📦 Registry — follow-ups da Sprint 73

- **Build a partir de Git Repository privado** — a Sprint 73 implementou Build via Kaniko apenas para repositórios públicos (sem credenciais). Suporte a repositórios privados exigiria capturar credenciais (token/usuário+senha) na UI e propagá-las ao Job Kaniko (`GIT_TOKEN`/`GIT_USERNAME`/`GIT_PASSWORD`), com cuidado para não persistir as credenciais em texto plano.
- **Histórico de Builds** — a Sprint 73 não persiste histórico: um Build finalizado não deixa rastro em GreenCap (Job efêmero com `ttlSecondsAfterFinished`). Avaliar persistir um registro mínimo (Repositório/Tag, Git Repository/branch, status, timestamps) para permitir consultar Builds anteriores.
- **Storage do Registry interno pode estar subdimensionado com o crescimento do catálogo de Templates** — descoberto ao planejar a Sprint 99 (dois novos Templates buildados via Kaniko, empilhando mais imagens no Registry interno). O tamanho da PVC não é definido em código Java nem em manifest versionado — apenas em dois scripts shell de provisionamento, com valores **divergentes**: `samples/greencap-demo/cluster-setup.sh` usa `4Gi` (decisão original da Sprint 71, ver `.scratch/archive/sprint-71/issues/01-pvc-persistencia-registry.md`), enquanto `setup/setup.sh` (setup "oficial") usa `8Gi`, sem ADR documentando essa evolução. Avaliar: (a) se o drift entre os dois scripts é intencional ou esquecimento, (b) se mesmo 8Gi comporta o catálogo crescendo além dos 3 Templates atuais sem exigir intervenção manual.
- **Causa raiz mais provável do subdimensionamento: o registry (`registry:3.0.0`, addon `registry` do minikube) não faz garbage collection automático** — descoberto ao testar reruns do `setup.sh` (2026-07-17). Cada rebuild da imagem GreenCap no Step 5 (ou de um Template via Kaniko) sobrescreve só o manifest da tag; as layers antigas viram blobs órfãos e ficam ocupando espaço até alguém rodar `registry garbage-collect` manualmente dentro do Pod — não há cron/hook fazendo isso hoje. Em ambientes de dev com reruns frequentes de `setup.sh` (como os dois seguidos rodados nesse teste), o volume enche bem mais rápido do que o número de imagens "vivas" sugeriria, mesmo com poucos Templates instalados. Aumentar a PVC (4Gi/8Gi → algo maior) só adia o problema; a correção de causa raiz é habilitar `REGISTRY_STORAGE_DELETE_ENABLED=true` no addon e agendar `registry garbage-collect` periodicamente (ou disparar após cada build/push do `setup.sh`).

#### 🐳 Imagem GreenCap em registry público — otimizar Step 5 do `setup.sh`

- **Build local é o passo mais pesado da instalação** — descoberto ao testar reruns do `setup.sh` (2026-07-17): o Step 5 (`docker build` a partir de `docker/Dockerfile` + `docker push` para o registry interno do minikube) roda do zero em toda instalação, mesmo quando o código-fonte não mudou desde a última execução. Avaliar publicar a imagem `greencap-platform/platform` em um registry público (Docker Hub, GHCR) por release/tag via CI, e trocar o Step 5 do `setup.sh` para `docker pull` da imagem publicada (com fallback para build local em desenvolvimento, quando a imagem da versão em uso ainda não foi publicada).

#### 📊 Storage — visualização de uso (sprint 72, cancelada)

- **Gráfico de uso (used/free) por PVC na `PersistentVolumeClaimsView`** — demanda original: coluna com mini gráfico de pizza/donut + diálogo "View Usage" com detalhamento em GiB/%, cores por limiar (70%/90%). Sprint 72 iniciada via `/grill-with-docs` e cancelada na etapa de implementação ao descobrir limitação técnica: a fonte de dados planejada (kubelet `/stats/summary`, endpoint `/api/v1/nodes/{node}/proxy/stats/summary`) **não reporta `pvcRef`/`usedBytes`/`capacityBytes` para volumes `hostPath`** — o `volume.Metrics` não é implementado por esse plugin. Testado no `greencap-demo` (StorageClass `standard` = `k8s.io/minikube-hostpath`): nenhuma PVC (`redis-data`, `registry-storage`) aparece no `/stats/summary`, nem mesmo as montadas por Pods `Running`. Mesma limitação provavelmente afeta `local-path-provisioner` — adotado como StorageClass default do `greencap-demo` na Sprint 98 (resolve `nodeAffinity`, não resolve esta limitação de métricas). Caminho alternativo a avaliar quando retomar: `exec df`/`stat -f` no Pod que monta a PVC via Fabric8 (RBAC `pods/exec` em vez de `nodes/proxy`), funciona independente do storage backend desde que o container tenha `df` disponível.

### ⚪ Baixa Prioridade

#### 🔵 Gerenciamento ativo — próximas operações de escrita

- **Atualizar imagem do Deployment (`kubectl set image`)** — patch em `spec.template.spec.containers[].image`. Requer UI para escolher o container quando o Pod tem múltiplos (multi-container) — maior complexidade de UX que as demais ações de write já implementadas.

#### 📄 Documentação — divulgar `setup/setup.sh` no README principal

- **`setup.sh` não é mencionado no `README.md`** — o script provisiona um cluster Minikube completo (profile configurável, addons metrics-server/ingress/registry/olm, registry persistente, build+push da imagem GreenCap e deploy via manifests em `setup/manifests/`), mas hoje só é descoberto por quem navega até `setup/`. Adicionar uma seção no README principal apresentando `./setup/setup.sh` como opção de quickstart alternativa ao `docker compose up` (ex.: "quero rodar num cluster Kubernetes de verdade, não só localmente via Docker"), com o efeito esperado (`http://greencap.local`, login `admin`/`admin`) e o script de teardown correspondente (`setup/teardown.sh`).

#### 🌐 Divulgação — aprofundar a landing page (`greencapk8s.dev`)

- **Landing page está "discreta"** — o site (`greencapk8s.dev`, repo `../greencap-k8s-portal`) hoje tem hero + proposta de valor + 3 screenshots + botões pro GitHub, mas falta: links para a documentação, instruções de instalação (`setup.sh`/Docker Compose), demo/vídeo ao vivo, e um caminho de contribuição. Último item pendente do **Tier 3** da iniciativa de divulgação open source (ver memória `project_oss_first_contact`); Tiers 0-2 e o restante do Tier 3 (README/LICENSE/arquivos de contribuição/About + topics/social preview) já concluídos e publicados na release v0.7.7. É trabalho no repo do portal, não no `greencap-k8s`.

#### 👀 Observação — `storage-provisioner` pode repetir o bug do `kube-registry-proxy`

- **Contexto**: um usuário reportou, ao rodar `setup.sh` no macOS, falha `manifest for gcr.io/k8s-minikube/kube-registry-proxy:0.0.8 not found` no Step 4 (addon `registry`). Causa: a tag ficou pinada no binário do minikube instalado (não no `setup.sh`) e foi removida do GCR após o addon migrar para `registry.k8s.io/minikube/kube-registry-proxy:v0.0.11` upstream. Corrigido fixando o override `--images`/`--registries` na chamada `minikube addons enable registry` em `setup/setup.sh`, desacoplando o script da versão de minikube instalada localmente.
- **Por que só o addon `registry` foi afetado**: dos addons habilitados pelo `setup.sh` (`metrics-server`, `ingress`, `registry`, `olm`), só o `KubeRegistryProxy` vivia em `gcr.io/k8s-minikube/*` — o namespace próprio do minikube no GCR, que sofreu a migração. `ingress` e `metrics-server` já nascem em `registry.k8s.io`; `olm` está em `quay.io` (projeto de terceiros). Nenhum dos três compartilha essa causa raiz — guardrail não replicado neles (ver `docs/adr/` — sem abstrair antes da segunda ocorrência real).
- **Risco latente não corrigido**: o `storage-provisioner`, habilitado automaticamente pelo `minikube start` (não é um addon explícito no `setup.sh`), ainda está pinado em `gcr.io/k8s-minikube/storage-provisioner` no código atual do minikube — mesmo padrão de risco do `kube-registry-proxy`. Se uma tag antiga for removida do GCR, o `minikube start` quebraria antes mesmo do Step 4 (habilitação de addons). Sem ocorrência real ainda — não corrigido preventivamente. Não dá para mitigar via `--images`/`--registries` do `addons enable` (não é habilitado por addon toggle); exigiria outro mecanismo, ex. `minikube start --extra-config`. Reavaliar se surgir um relato parecido.

#### 🎓 Diferencial — Onboarding e Aprendizado

> Decorre do posicionamento registrado em `CONTEXT.md` (seção "Purpose & Audience"): GreenCap como plataforma de estudos/dev/teste para PMEs. Ainda sem escopo definido — registrar como exploração futura, não compromisso de sprint.

- **Playground/Sandbox** — marcar um Cluster ou Namespace como "seguro para experimentar", possivelmente com avisos/restrições diferenciados na UI.
- **Onboarding/Tutorial in-app** — guia introdutório dentro da própria UI para usuários iniciantes em Kubernetes.

> "Sample Manifests" (biblioteca de YAMLs de exemplo via Manifest/Apply) foi absorvido e superado pela Sprint 98 — ver Templates Catalog.

---

## Sprints Concluídas

> Mostra apenas as últimas 10 sprints. Histórico completo em `docs/sprints-archive.md` (ver `docs/agents/sprint-archiving.md`).

### Sprint 105 ✅ — Topologia: setas de ServiceDependency + StatefulSet como nó

- Escopo fechado via `/grill-with-docs`, decisões registradas na **ADR 0018** (`docs/adr/0018-heuristica-service-dependency-topologia.md`): matching por substring/word-boundary do valor da env var contra nomes de Service do namespace ativo (não match exato — perderia connection strings reais), sem parse de arquivo de config montado e sem ConfigMap/Secret como nós de primeira classe (permanecem invisíveis, só resolvem o valor da env var). Fora de escopo: DaemonSet
- **StatefulSet na Topologia** (pré-requisito): `TopologyService.buildGraph` passa a buscar `StatefulSet` do namespace, criando um nó por recurso e uma aresta estrutural direta `StatefulSet→PodGroup` (sem ReplicaSet intermediário — `podOwnerId` reconhece owner references de `Kind: StatefulSet` além de `ReplicaSet`). Nome-base do grupo, para StatefulSet, é o próprio nome do recurso (sem o `stripLastSegment` usado para remover o hash de ReplicaSet). Nenhum tratamento especial foi necessário para o Service headless (`spec.serviceName`) — a aresta Service→PodGroup já é resolvida por matching de selector contra labels do Pod, independente de o Service ser headless; PVCs de `volumeClaimTemplates` também já ficam cobertos pela mesma aresta genérica `PodGroup→PersistentVolumeClaim` existente
- **Inferência de `ServiceDependency`** (`TopologyService`): `TopologyEdge` ganha campo `type` (`STRUCTURAL` | `SERVICE_DEPENDENCY`). Varre `env[].value`, `env[].valueFrom.configMapKeyRef`/`secretKeyRef` e `envFrom.configMapRef`/`secretRef` de cada `PodSpec` (containers + initContainers), resolvendo o valor final contra ConfigMaps/Secrets do namespace ativo (Secret decodificado de base64). O valor resolvido é comparado por substring/word-boundary contra os nomes dos Services já carregados — cobre hostname puro, `host:porta` e connection string completa (`jdbc:postgresql://postgres-service:5432/db`). FQDN no formato `<service>.<namespace>.svc.cluster.local` só gera aresta se o segmento de namespace bater com o namespace ativo (evita atribuir a um Service local de mesmo nome quando a intenção era outro namespace). Múltiplas env vars do mesmo Workload que casam com o mesmo Service colapsam numa única aresta (dedup por Service)
- **Renderização** (`topology-graph.ts`, Cytoscape): arestas `SERVICE_DEPENDENCY` renderizadas com `line-style: dashed`; ajuste feito durante o aceite manual — a cor inicial (`#38BDF8`, azul-claro) foi trocada para herdar a mesma cor das arestas estruturais (`#64748B`), mantendo só o traço tracejado como diferencial visual
- **Evidência no drawer** (`TopologyNodeDrawer`): ao abrir o drawer de um Workload com uma ou mais `ServiceDependency` saindo dele, uma seção "Depends on" lista cada dependência inferida com a env var e o valor que geraram o match, reaproveitando o botão "Go to" já usado para outros tipos de nó; Workloads sem dependência inferida não mostram a seção — sem alteração visual para o caso comum de hoje
- Testes: `TopologyServiceTest` (novo, `@EnableKubernetesMockClient`) — StatefulSet como nó com status/edge de PodGroup corretos, Service headless com edge idêntica à de um Service regular, `ServiceDependency` para os três formatos de valor (hostname, host:porta, connection string), resolução via `configMapKeyRef`/`secretKeyRef`, dedup de múltiplas env vars para o mesmo Service, e o caso negativo/positivo de FQDN com namespace diferente/igual ao ativo; `TopologyNodeDrawerTest` (novo, Karibu) — seção "Depends on" aparece só quando há dependência, evidência (env var + valor) renderizada corretamente, ausência da seção quando não há dependência
- **Achado durante a escrita dos testes, registrado no backlog** (não corrigido nesta sprint — fora do escopo original, não verificado em navegador real): o botão "Go to resource"/"Go to `<service>`" do drawer usa `ui.navigate(url)` (overload de 1 argumento) para URLs com `?name=` embutido — esse overload não separa a query string do path internamente, padrão que já existia em outros pontos do código (ex. `CronJobsView`) antes desta sprint
- Issues: `.scratch/sprint-105/issues/` (3 issues, todas `done`)

### Sprint 104 ✅ — Username no header + Developer Experience como 1ª seção do menu + fix de duplicação nos wizards de deploy

- `MainLayout`: bloco "User: `<username>`" adicionado à navbar, à esquerda do bloco "Cluster: nome [badge]" (`userInfoLayout`, construído uma vez em `buildUserInfoLayout()`); sempre visível, inclusive sem cluster ativo — usa o username de login já disponível via `SecurityContextHolder`, sem novo campo em `User`
- `MainLayout`: seção "DEVELOPER EXPERIENCE" movida para ser a primeira do drawer (antes de PROJECT/GLOBAL/SETTINGS); "New Application" deixou de ser item avulso sem seção e passou a fazer parte dela, abaixo de "Templates Catalog"; ícone de "Templates Catalog" trocado de `GRID_H` para `SHOP`
- `DeployModeSelector` (novo, `io.greencap.k8s.ui`): extrai a faixa de botões "Deploy from X" antes duplicada nas 4 telas de deploy (`DeployApplicationView`, `DeployFromDockerfileView`, `ImportComposeView`, `DeployFromHelmView`) — bug descoberto durante o aceite: reordenar o botão "Deploy from Image" só na cópia de `DeployApplicationView` fazia a ordem "resetar" ao navegar para qualquer uma das outras três telas. Ordem fixa: Dockerfile, Compose, Image, Helm; botão da view atual em destaque (`LUMO_PRIMARY`, sem navegação), demais em `LUMO_TERTIARY` com navegação
- Decisão tomada durante o aceite: item de menu "New Application" passou a apontar para `DeployFromDockerfileView` por padrão (era `DeployApplicationView`/Deploy from Image), para bater com Dockerfile agora sendo o primeiro botão em destaque da faixa
- Testes: `MainLayoutTest` (novo) — username sempre visível, "DEVELOPER EXPERIENCE" como 1ª seção com "New Application" abaixo de "Templates Catalog"; `DeployModeSelectorTest` (novo) — ordem fixa dos botões e destaque correto por view. Registro de rotas do `MockVaadin` (necessário porque `SideNavItem` resolve a rota no construtor) feito localmente só em `MainLayoutTest` — fazê-lo na classe base `KaribuTest` quebrou 11 testes existentes, já que nenhuma view desta app tem construtor no-arg e o `MockVaadin` falha ao navegar automaticamente para `""` na configuração inicial
- Planejamento via `/grill-with-docs` cobriu as entregas 01 e 02 (username e reorganização do menu); a entrega 03 (fix de duplicação + `DeployModeSelector`) foi causa e solução evidentes, descobertas durante o aceite manual
- Issues: `.scratch/sprint-104/issues/` (3 issues, todas `done`)

### Sprint 103 ✅ — Templates Catalog: ação "Uninstall Template" no card instalado

- `SampleCatalogView`: card de Template instalado ganha um ícone de lixeira discreto no canto superior direito do título (botão terciário só-ícone, cor de erro sutil, tooltip "Uninstall Template"), separado do footer onde fica "Open Topology" — ação destrutiva não compete visualmente com navegação inócua
- Dialog type-to-confirm: reusa o texto de aviso do Delete Namespace; pede o **nome do Namespace** do Template (não o título do card); botão "Uninstall" só habilita quando o texto digitado bate exatamente
- Confirmar chama `NamespaceService.deleteNamespace(cluster, template.namespace())`, fecha o dialog, limpa o Namespace ativo se era o deletado, atualiza o combo de Namespaces do header (`MainLayout.refreshNamespaceSelector`) e mostra notification de sucesso — single-shot sem rollback, consistente com Deploy Template (ADR 0015)
- Escopo da remoção — decisão registrada na **ADR 0017**: Uninstall Template deleta **apenas o Namespace**, cascateando os recursos namespaced; deliberadamente **não** remove as imagens que os Kaniko Builds do Deploy Template empurraram para o Registry interno, nem eventuais recursos cluster-scoped (hipotéticos hoje) — espelha Uninstall Operator (deixa CRDs) e Uninstall Helm (deixa PVCs)
- Estado transitório "Uninstalling…" com auto-heal: como a deleção de Namespace é assíncrona e o `refresh()` da view é um no-op deliberado, o card específico vira um estado desabilitado (opacity reduzida, `pointer-events: none`, badge contrast, sem lixeira/Open Topology/Deploy) e um polling leve (`AsyncTasks.schedulePolling`) checa `isInstalled` até virar `false`, re-renderizando **apenas aquele card** como "Deploy" — suporta múltiplos uninstalls simultâneos, cancelado em `onDetach`. Conteúdo do card extraído para `renderCardContent` para permitir a troca de estado in-place sem recarregar o catálogo inteiro
- `CONTEXT.md`: entrada **Uninstall Template** adicionada ao glossário
- Testes (`SampleCatalogViewTest`): card instalado renderiza a lixeira (localizada pela `Tooltip`, botão só-ícone) e card não-instalado não a renderiza; guard do botão "Uninstall" — desabilitado ao abrir, continua desabilitado com texto errado, habilita só com o Namespace exato; confirmar dispara `deleteNamespace` e marca o card "Uninstalling…"
- Sem gate de permissão (ADR 0013) — Kubernetes API autoriza (ou 403) a deleção via o service account do usuário
- Planejamento via `/grill-with-docs`: `CONTEXT.md`, ADR 0017 e issue em `.scratch/sprint-103/issues/`
- Backlog: registrado follow-up para extrair a lógica de deploy+build da `SampleCatalogView` (construtor cresceu para 7 dependências com a chegada de `NamespaceService`)
- Issues: `.scratch/sprint-103/issues/` (1 issue, `done`)

### Sprint 102 ✅ — Templates Catalog: ação "Open Topology" no card de Template instalado

- Escopo fechado via `/grill-with-docs` — feature pequena e focada; Uninstall de Template avaliado e **deliberadamente adiado** para sprint própria (registrado no backlog com as 4 questões abertas: operação destrutiva, o que "uninstall" remove do Registry interno, simetria com Deploy Template, hierarquia visual do footer)
- `SampleCatalogView`: o card de um Template instalado passa a mostrar o badge "Installed" à esquerda **e** um botão "Open Topology" à direita (footer `JustifyContentMode.BETWEEN`); botão secundário (small/tertiary) com o ícone `CLUSTER` — o mesmo da Topologia no sidebar — para não competir com o "Deploy" primário dos cards não-instalados
- Ação "Open Topology": troca o Namespace ativo para o Namespace do Template (vem do `TemplateSummary`/`catalog.json`, sem fetch extra), persiste via `userService.updateActiveNamespace`, atualiza o combo do header com `MainLayout.refreshNamespaceSelector(UI)` (helper da Sprint 101) e navega para a `TopologiaView` — mesmo contrato de navegação do pós-deploy das views de New Application. Injeta `UserService` na view (antes ausente)
- Estado "Installed" é snapshot: a ação navega **sem** re-checar existência da Namespace no clique — se removida por fora, a `TopologiaView` (async, com tratamento de inacessível desde a Sprint 50) renderiza topologia vazia, sem crash; consistente com o tratamento de snapshot de `ConnectionStatus`
- Fix cosmético incluído: no badge "Installed", ícone de check e texto estavam colados — adicionado `margin-inline-start` no label
- `CONTEXT.md`: entrada **Templates Catalog** atualizada descrevendo a ação "Open Topology" no card instalado, explicitando que é distinta do botão "Go to resource" do painel de detalhe da própria Topologia
- Testes: `SampleCatalogViewTest` (Karibu) estendido — card instalado renderiza "Open Topology" junto ao badge; card não-instalado não o mostra (só "Deploy"); clicar troca o Namespace ativo para o do Template (`clusterContext.setNamespace` + `userService.updateActiveNamespace` verificados, absorvendo o `NotFoundException` de `navigate` no ambiente de teste sem rotas)
- Issues: `.scratch/sprint-102/issues/` (1 issue, `done`)

### Sprint 101 ✅ — Bug fixes do selector de Namespace no header (refresh pós-deploy + seleção no F5)

- Dois bugs do combobox de Namespaces do `MainLayout`, ambos registrados no backlog durante os aceites das Sprints 98/99 — fluxo de bug fix pontual (sem `/grill-with-docs` nem issues formais em `.scratch/`)
- **Selector desatualizado após deploy**: Deploy Application, Deploy from Dockerfile e Import Compose criam uma Namespace nova e navegam direto para a `TopologiaView`, mas não recarregavam a lista do combobox no header (a Namespace nova só aparecia após trocar de Cluster e voltar, pois `updateNamespaceSelector()` só recarrega quando o Cluster ativo muda). Fix: os três fluxos passam a chamar o refresh do `MainLayout` após `setNamespace`, antes do `navigate` — mesmo mecanismo já usado por `NamespacesView`/`SampleCatalogView`
- **Seleção perdida no full reload (F5)**: o valor selecionado sumia após F5 (voltava para "Select...") enquanto a lista continuava correta. Causa: o valor só era aplicado via `@Push` assíncrono — num F5 a `UI` nova abre o canal push apenas após a resposta HTML inicial, então o push do valor podia chegar antes do canal estar pronto e ser descartado pelo cliente (a implementação anterior ainda o piorava usando um *segundo* push disparado de uma virtual thread solta). Fix: `loadNamespacesForCluster()` semeia o combo **sincronamente** com o Namespace da sessão (item único + valor) antes do load assíncrono — como o `ClusterContext` é `@VaadinSessionScope` e sobrevive ao F5, o valor entra no render HTML inicial sem depender de push; o task assíncrono depois substitui pela lista completa de Namespaces
- **Limpeza de duplicação**: o helper que localiza o `MainLayout` a partir da view e chama `refreshClusterState()` estava copiado idêntico em `NamespacesView` e `SampleCatalogView`; com mais três fluxos precisando dele, foi extraído para o estático `MainLayout.refreshNamespaceSelector(UI)` e os cinco call sites apontam para lá
- Sem novos testes automatizados: o núcleo do fix do F5 é timing do canal `@Push` (inerentemente de browser, fora do alcance do Karibu, que roda single-thread sem push real); validado por aceite manual nos dois cenários (F5 e navegação SPA) e nos três fluxos de deploy. Suíte existente rodada como verificação de regressão (verde)

### Sprint 100 ✅ — Suporte nativo a macOS no setup.sh + workflows GitHub Actions

- Escopo fechado via `/grill-with-docs`: `setup/setup.sh` recusava auto-install fora do Linux; decisão de dar suporte nativo real a macOS (não só cobertura de CI) via instaladores Homebrew, com Docker provido por **Colima** headless em vez de Docker Desktop (GUI, inviável para o fluxo plug-and-play e para CI) — raciocínio completo na ADR 0016 (`docs/adr/0016-colima-como-provedor-docker-no-macos.md`)
- `install_docker/kubectl/minikube/helm` ganham branch macOS (`brew install colima docker` / `kubectl` / `minikube` / `helm`); `ensure_homebrew()` auto-instala o Homebrew se ausente (`NONINTERACTIVE=1`); mecanismo Linux (curl/apt) inalterado
- Modo não-interativo via variáveis de ambiente (`AUTO_INSTALL`, `PROFILE_CHOICE`, `NODES`/`CPUS`/`MEMORY`, e `CONFIRM` em `teardown.sh`) — estende o padrão já usado por `GREENCAP_ENCRYPTION_KEY`/`DB_PASSWORD`, necessário para automação em CI
- Novo workflow `.github/workflows/setup-script-validate.yml`: matrix `ubuntu-24.04` (fluxo completo: setup → reachability com retry → teardown) e `macos-14` (`INSTALL_ONLY=true` — só valida os instaladores Homebrew, sem provisionar cluster). `docker-compose-validate.yml` também migrado de `ubuntu-latest` para `ubuntu-24.04` — tags `*-latest` são realocadas pelo GitHub sem aviso, arriscando invalidar premissas específicas de Apple Silicon
- Bugs descobertos e corrigidos durante as execuções reais de CI (não visíveis em revisão de código nem build local): `${AUTO_INSTALL,,}` (Bash 4+) quebrando no `/bin/bash` 3.2 do macOS (substituído por `case` portável); `sed -i` sem `-i.bak` quebrando em BSD sed; `curl` de reachability com 503 por corrida do `ingress-nginx` sincronizando a config (retry 10x/5s); `docker/Dockerfile` baixava o Helm CLI fixo em `linux-amd64`, quebrando em runtime num build arm64 (fix via `ARG TARGETARCH`)
- **Achado de plataforma, não de código**: runners `macos-*` hospedados padrão do GitHub Actions não suportam virtualização aninhada — `colima start` nunca teria sucesso ali, independente do `setup.sh`. Confirmado empiricamente (corrigiu premissa errada da ADR 0016 original); job macOS da CI reduzido para `INSTALL_ONLY`. Suporte real a macOS (uso local do usuário, fora do runner sandboxado) permanece completo
- Issues: `.scratch/sprint-100/issues/` (3 issues, todas `done`)

### Sprint 99 ✅ — Dois novos Templates no catálogo: CRUD Flask+MongoDB e Cache-aside Flask+PostgreSQL+Redis

- Trabalho inteiramente no repositório `greencap-templates` (fora desta base) — o mecanismo de Deploy Template já é genérico (ADR 0015), sem nenhuma mudança de código Java em `greencap-k8s`; catálogo de Templates passa de 1 para 3
- **Template `crud-flask-mongodb`**: análogo ao `crud-flask-postgres` seed trocando o datastore relacional pelo documental — mesma entidade `items` (`name`/`description`), mesmas rotas CRUD, mesma UI HTML servida pelo próprio Flask; persistência via `pymongo` direto (sem ODM, espelhando o uso de `psycopg2` no template Postgres) com retry de conexão no boot; MongoDB `mongo:8.0` com autenticação obrigatória via Secret (padrão `postgres-credentials`) e PVC de 1Gi; backend buildado via Kaniko (sentinela `__BUILD__backend`); Ingress fixo `crud-flask-mongodb.greencap.local`. Objetivo didático: comparar diretamente conexão relacional vs. documental sob o mesmo padrão Deployment stateless + storage stateful na Namespace
- **Template `cache-aside-flask-postgres-redis`**: demonstra o padrão cache-aside (decisão do `CONTEXT.md`: Redis pelo seu papel idiomático de cache, não como datastore primário de um CRUD); reaproveita a app do `crud-flask-postgres` adicionando cache na listagem — `GET /` lê a chave `items:all` no Redis antes de consultar o Postgres, populando-a com TTL de 60s no miss; escritas invalidam ativamente a chave além do TTL; Redis `redis:8-alpine` com `requirepass` via Secret e **sem PVC** (cache descartável — perder no restart é esperado, a próxima leitura repopula a partir do Postgres); backend via Kaniko; Ingress fixo `cache-aside-flask-postgres-redis.greencap.local`
- Ambos com entrada em `catalog.json` (title/description/technologies); imagens `mongo:8.0` e `redis:8-alpine` fixadas após validar as versões estáveis mais recentes (as issues previam `7.0`/`7.4-alpine` como piso)
- Issues: `.scratch/sprint-99/issues/` (2 issues, ambas `done`)

### Sprint 98 ✅ — Templates Catalog: catálogo de Templates (greencap-templates) com deploy em um clique

- Escopo fechado via `/grill-with-docs`: item de backlog "Diferencial — Onboarding e Aprendizado" desdobrado em dois conceitos novos no `CONTEXT.md` — **Templates Catalog** (view em Developer Experience, lista de cards) e **Template** (unidade: app de estudo completa, multi-recurso), mais a operação **Deploy Template**. Raciocínio completo registrado na ADR 0015 (`docs/adr/0015-sample-catalog-templates-via-indice-raw-http.md`): índice `catalog.json` + manifest `template.yaml` por Template, buscados via HTTP raw (sem cliente Git); componentes sem imagem pública são buildados via Kaniko (reaproveitando o mecanismo de Deploy from Dockerfile/Import Compose), publicando no Registry interno do Cluster; "Installed" é por Cluster (Namespace de nome fixo no índice), não por usuário; deploy aborta no primeiro conflito, sem rollback; sem gate de permissão (ADR 0013 já eliminou o sistema de permissões interno — RBAC do Kubernetes autoriza)
- Novo repositório público `greencapk8s/greencap-templates` (em inglês — primeiro repositório do ecossistema a adotar esse padrão): `catalog.json`, e o Template seed `crud-flask-postgres` (Flask + PostgreSQL, sem frontend separado, com Ingress fixo `crud-flask-postgres.greencap.local`, labels `app.kubernetes.io/part-of`/`component` em todos os recursos para agrupamento correto na Topologia, e páginas HTML com CSS próprio via `static/style.css`)
- `SampleCatalogService`: fetch e parsing do índice/manifest via HTTP simples, sem cache; `isInstalled` verifica existência da Namespace declarada no índice
- `TemplateDeploymentService`: aplica o arquivo de recurso da Namespace primeiro; roda Kaniko por entrada em `builds` (`dockerfilePath` resolvido relativo ao `contextPath`, não à raiz do repo — mesma convenção de Deploy from Dockerfile); substitui o valor-sentinela `__BUILD__<name>` pela imagem publicada; aplica os demais recursos via client genérico do Fabric8 (`resourceList`/`NamespaceableResource`, tipos não conhecidos de antemão)
- `SampleCatalogView` (rota `developer-experience/sample-catalog`, menu "Templates Catalog"): lista de cards com CSS próprio (grid responsivo, sombra com hover, chips de tecnologia, badge Installed/botão Deploy), preview somente-leitura antes de confirmar deploy, log de build inline durante o Kaniko; após deploy bem-sucedido, força o recarregamento do combobox de Namespaces do `MainLayout` (`refreshClusterState()`, mesmo mecanismo já usado por `NamespacesView`)
- Menu **Operators** ocultado do sidebar (`OPERATORS_MENU_VISIBLE = false` em `MainLayout`) — ainda beta, rotas continuam funcionando
- Infraestrutura: `local-path-provisioner` instalado no `greencap-demo` (vendorizado em `samples/greencap-demo/local-path-storage.yaml`, aplicado por `cluster-setup.sh`) e definido como StorageClass default, resolvendo a limitação de `nodeAffinity` do hostpath-provisioner já registrada no backlog (Sprint 71) — descoberta reativada ao ver o Postgres do Template em `CreateContainerConfigError` num node diferente do node com os dados
- Testes: `SampleCatalogServiceTest` e `TemplateDeploymentServiceTest` (parsing de fixtures, `isInstalled` via `@EnableKubernetesMockClient`, substituição de sentinela, abort-sem-rollback em conflito) em `kubernetes/`; `SampleCatalogViewTest` (badge Installed oculta/mostra o botão Deploy, preview abre somente-leitura sem disparar deploy antes da confirmação) em `ui/` — `forceReload()` da view tornado síncrono (mesmo padrão de `NamespacesView.loadNamespaces()`) para permitir dirigir o teste sem correr atrás de uma thread virtual
- Dois bugs pré-existentes encontrados durante o aceite manual e registrados no backlog (não corrigidos nesta sprint): badge de status de Pod não reflete `CrashLoopBackOff`; combobox de Namespaces não atualiza após Deploy Application/Deploy from Dockerfile/Import Compose (mesma causa corrigida aqui para Deploy Template)
- Issues: `.scratch/sprint-98/issues/` (5 issues, todas `done`)

### Sprint 97 ✅ — Hotfix: propagação de SecurityContext em polling agendado (AsyncTasks.schedulePolling)

- Encontrado durante validação manual pós-Sprint 96: Deploy from Dockerfile mostrava "Build failed. Check the logs above." mesmo com o Job Kaniko completando com sucesso e a imagem sendo pushada ao registry
- `AsyncTasks.schedulePolling`: `DelegatingSecurityContextExecutor` captura o `SecurityContext` da thread que chama `.execute()` — para o tick recorrente, essa chamada acontecia na thread do `CLOCK`, que nunca tem usuário autenticado (WARN "Unable to resolve Kubernetes credentials: no authenticated user"); fix: captura o contexto da thread chamadora (UI) no momento de `schedulePolling()` e envolve `command` com `DelegatingSecurityContextRunnable` antes de despachar para `VIRTUAL_THREADS` — corrige os 5 call sites (`BuildLogsView`, `DeployFromDockerfileView`, `ImportComposeView`, `MainLayout`, `PodLogsView`) sem exigir mudança neles
- `DeployFromDockerfileView.waitForBuild`: `fetchPodLogs` isolado em `fetchAndDisplayBuildLogs()` com try/catch próprio — falha transitória ao ler logs do pod Kaniko (container de vida curta terminando) não deve abortar a checagem de status do Job, única fonte de verdade sobre sucesso/falha do build
- Sem issues formais em `.scratch/` — fluxo de bug fix pontual (causa e solução evidentes)

### Sprint 96 ✅ — Consolidar execução assíncrona em virtual threads

- `AsyncTasks` (novo, `io.greencap.k8s.ui`): ponto único de execução assíncrona do projeto — `execute(Runnable)` para disparo único e `schedulePolling(Runnable, Duration, Duration)` para polling recorrente, ambos com propagação de `SecurityContext` via `DelegatingSecurityContextExecutor`; internamente, o polling usa um único "clock" de thread de plataforma compartilhado que apenas dispara o tick, despachando o trabalho real para o mesmo executor do disparo único — elimina tanto a duplicação de código quanto o custo de criar/destruir um `ScheduledExecutorService` por view visitada
- `docs/adr/0014-async-tasks-clock-compartilhado.md`: decisão do clock compartilhado em vez de um scheduler por chamador
- `UiConstants.VIRTUAL_THREADS` removido — a classe volta a conter apenas helpers de construção de UI; 12 views (`DashboardView`, `DeployApplicationView`, `DeployFromHelmView`, `DeploymentsView`, `HelmReleasesView`, `InstalledOperatorsView`, `MainLayout`, `NamespacesView`, `OperatorCatalogView`, `PodsView`, `StatefulSetsView`, `TopologiaView`) migradas para `AsyncTasks.execute(...)`; pontos que usavam `CompletableFuture.runAsync(runnable, VIRTUAL_THREADS)` como fire-and-forget simplificados para chamada direta a `AsyncTasks.execute(...)`
- `BuildLogsView`, `DeployFromDockerfileView`, `ImportComposeView`, `MainLayout`, `PodLogsView`: `ScheduledExecutorService` próprio e `shutdown()` no detach removidos — passam a usar `AsyncTasks.schedulePolling(...)`, sem wrapping manual de `DelegatingSecurityContextRunnable`; comportamento observável (intervalo, pause/resume, cancelamento ao sair da view) inalterado
- Fora do escopo, por decisão explícita: o `Thread.ofVirtual().start(...)` cru em `MainLayout` usado para forçar um ciclo de push separado do Vaadin — não faz chamada Kubernetes, não tem o bug de propagação de contexto que motivou a sprint
- `AsyncTasksTest`: cobertura JUnit pura (sem Spring), verificando propagação de `SecurityContext` no disparo único, disparo repetido do polling e efeito do cancelamento
- Issues: `.scratch/sprint-96/issues/` (3 issues, todas `done`)

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
