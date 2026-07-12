# GreenCap K8s — Sprints & Demandas

> Documento vivo. Atualizar a cada sprint concluída ou nova demanda identificada.

---

## Status Geral

| Sprint | Tema | Status |
|--------|------|--------|
| 102 | Templates Catalog: ação "Open Topology" no card de Template instalado (entra na Namespace da solução e abre a Topologia) | ✅ Concluído |
| 101 | Bug fixes do selector de Namespace no header: refresh após Deploy Application/Dockerfile/Compose + seleção preservada no full reload (F5) | ✅ Concluído |
| 100 | Suporte nativo a macOS no setup.sh (Homebrew/Colima) + workflows GitHub Actions validando setup completo em Linux e macOS | ✅ Concluído |
| 99 | Dois novos Templates no catálogo (greencap-templates): CRUD Flask+MongoDB e Cache-aside Flask+PostgreSQL+Redis | ✅ Concluído |
| 98 | Templates Catalog — Developer Experience: catálogo de Templates (repositório greencap-templates) com deploy em um clique | ✅ Concluído |
| 97 | Hotfix: propagação de SecurityContext em polling agendado (AsyncTasks.schedulePolling) | ✅ Concluído |
| 96 | Consolidar execução assíncrona em virtual threads — AsyncTasks como ponto único | ✅ Concluído |
| 95 | Bug fix: RBAC fail-closed + propagação de SecurityContext em virtual threads + feedback na tela Users | ✅ Concluído |
| 94 | K8s RBAC substituindo sistema de permissões interno | ✅ Concluído |
| 93 | Métodos alternativos de registro de cluster: Token + URL + remoção de ClusterProvider | ✅ Concluído |

---

## Backlog

> Itens sem sprint definida, organizados por prioridade (Alta, Média, Baixa).

### 🟡 Média Prioridade

#### 🐛 Bug: badge de status de Pod não reflete CrashLoopBackOff/BackOff

- **Status do Pod ignora o estado real do container** — descoberto durante o teste manual do Sample Catalog (Sprint 98): com o Postgres fora do ar, o container `backend` entrou em `CrashLoopBackOff`, mas o badge de status na listagem de Pods continuou mostrando "Running". Provável causa: a derivação do badge usa `pod.status.phase` (que permanece `Running` enquanto o Pod em si não falhou totalmente) em vez de inspecionar `containerStatuses[].state.waiting.reason` (`CrashLoopBackOff`, `ErrImagePull`, etc.) e `restartCount`. Afeta pelo menos `PodsView`; avaliar se `DeploymentsView`/`StatefulSetsView` têm o mesmo problema ao agregar status a partir dos Pods.

#### 🌐 Acesso local via `*.greencap.local` — follow-up dos fluxos de Deploy

- **`/etc/hosts` não suporta curinga** — descoberto ao testar o Ingress do Sample Catalog (Sprint 98): a convenção `<namespace>.greencap.local`, usada também em Deploy Application e Deploy from Dockerfile, exige uma linha manual em `/etc/hosts` por aplicação implantada (`/etc/hosts` faz correspondência exata, sem expansão de glob — uma entrada `*.greencap.local` não resolve nada). Conforme o Sample Catalog cresce com mais Templates, essa fricção tende a aumentar. Solução: documentar (ou automatizar via script) um resolver DNS local com curinga real, ex. `dnsmasq` com `address=/.greencap.local/<ip-do-cluster>`, resolvendo qualquer subdomínio de uma vez — no Linux via NetworkManager/dnsmasq, no macOS via dnsmasq + `/etc/resolver/greencap.local`.

#### 🔗 Registro de Cluster — método alternativo ao kubeconfig

> Token + URL foi entregue na Sprint 93 (`ClustersView` com aba dedicada + `ClusterService.synthesizeKubeconfig()`).

- **In-cluster** — segundo método de registro: quando o GreenCap roda dentro de um cluster Kubernetes, ele pode auto-detectar o service account do pod (`/var/run/secrets/kubernetes.io/serviceaccount/token` + CA bundle) sem nenhuma credencial manual. Útil para quem instala o GreenCap no próprio cluster que quer gerenciar. O Fabric8 suporta via `Config.autoConfigure()` quando rodando in-cluster. No fluxo de registro, seria uma opção "Usar cluster atual" disponível apenas quando a plataforma detectar que está rodando dentro de um pod Kubernetes.

#### 🔌 Developer Experience — follow-ups da Sprint 88

- **Custom Resources** — view genérica na seção Developer Experience que lista os tipos de CRD instalados por operators (filtrados por grupo `*.io` de operators gerenciados pelo OLM), exibe instâncias por namespace e permite criar/editar/deletar via YAML reutilizando o mecanismo de Apply existente. Cobre automaticamente qualquer operator instalado (Grafana, Prometheus, cert-manager, KEDA, etc.) sem precisar de painéis específicos por operator. Posicionamento no sidebar: `DEVELOPER EXPERIENCE → Custom Resources`, abaixo de Operators.

#### 🎯 Templates Catalog — follow-ups da Sprint 102

- **Uninstall Template no card instalado** — adicionar ao card de um Template instalado uma ação de desinstalação, complementando o "Open Topology" entregue na Sprint 102. Mecanicamente é encaixável (estado instalado = Namespace existe; desinstalar = `NamespaceService.deleteNamespace`, cascateando a remoção dos recursos namespaced — mesmo mecanismo da `Delete Namespace`), mas foi deixado para sprint própria por carregar um design tree que não deve ser decidido no impulso: (a) **operação destrutiva** — exige dialog type-to-confirm e cobertura de teste Karibu própria (guard de confirmação); (b) **o que "Uninstall" realmente remove** — deletar a Namespace não remove as imagens buildadas via Kaniko e publicadas no **Registry interno** do Cluster no Deploy Template, nem eventuais recursos **cluster-scoped** que um Template venha a criar (hoje os Templates são namespaced, mas o modelo não garante) — decisão *hard-to-reverse* e *surprising*, candidata a ADR; (c) **simetria com Deploy Template** — Deploy é abort-no-primeiro-conflito sem rollback (ADR 0015); Uninstall precisa de semântica coerente e novo termo no glossário espelhando `Uninstall Operator`/`Delete Namespace`; (d) **layout do card** — footer passaria a ter badge + Open Topology + Uninstall, misturando navegação inócua com ação destrutiva — avaliar hierarquia visual (ícone discreto / overflow menu) em vez de empilhar botões.

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
- **Storage do Registry interno pode estar subdimensionado com o crescimento do catálogo de Templates** — descoberto ao planejar a Sprint 99 (dois novos Templates buildados via Kaniko, empilhando mais imagens no Registry interno). O tamanho da PVC não é definido em código Java nem em manifest versionado — apenas em dois scripts shell de provisionamento, com valores **divergentes**: `samples/greencap-demo/cluster-setup.sh` usa `4Gi` (decisão original da Sprint 71, ver `.scratch/archive/sprint-71/issues/01-pvc-persistencia-registry.md`), enquanto `setup/setup.sh` (setup "oficial") usa `8Gi`, sem ADR documentando essa evolução. Avaliar: (a) se o drift entre os dois scripts é intencional ou esquecimento, (b) se mesmo 8Gi comporta o catálogo crescendo além dos 3 Templates atuais sem exigir intervenção manual.

#### 📊 Storage — visualização de uso (sprint 72, cancelada)

- **Gráfico de uso (used/free) por PVC na `PersistentVolumeClaimsView`** — demanda original: coluna com mini gráfico de pizza/donut + diálogo "View Usage" com detalhamento em GiB/%, cores por limiar (70%/90%). Sprint 72 iniciada via `/grill-with-docs` e cancelada na etapa de implementação ao descobrir limitação técnica: a fonte de dados planejada (kubelet `/stats/summary`, endpoint `/api/v1/nodes/{node}/proxy/stats/summary`) **não reporta `pvcRef`/`usedBytes`/`capacityBytes` para volumes `hostPath`** — o `volume.Metrics` não é implementado por esse plugin. Testado no `greencap-demo` (StorageClass `standard` = `k8s.io/minikube-hostpath`): nenhuma PVC (`redis-data`, `registry-storage`) aparece no `/stats/summary`, nem mesmo as montadas por Pods `Running`. Mesma limitação provavelmente afeta `local-path-provisioner` — adotado como StorageClass default do `greencap-demo` na Sprint 98 (resolve `nodeAffinity`, não resolve esta limitação de métricas). Caminho alternativo a avaliar quando retomar: `exec df`/`stat -f` no Pod que monta a PVC via Fabric8 (RBAC `pods/exec` em vez de `nodes/proxy`), funciona independente do storage backend desde que o container tenha `df` disponível.

### ⚪ Baixa Prioridade

#### 🔵 Gerenciamento ativo — próximas operações de escrita

- **Atualizar imagem do Deployment (`kubectl set image`)** — patch em `spec.template.spec.containers[].image`. Requer UI para escolher o container quando o Pod tem múltiplos (multi-container) — maior complexidade de UX que as demais ações de write já implementadas.

#### 📄 Documentação — divulgar `setup/setup.sh` no README principal

- **`setup.sh` não é mencionado no `README.md`** — o script provisiona um cluster Minikube completo (profile configurável, addons metrics-server/ingress/registry/olm, registry persistente, build+push da imagem GreenCap e deploy via manifests em `setup/manifests/`), mas hoje só é descoberto por quem navega até `setup/`. Adicionar uma seção no README principal apresentando `./setup/setup.sh` como opção de quickstart alternativa ao `docker compose up` (ex.: "quero rodar num cluster Kubernetes de verdade, não só localmente via Docker"), com o efeito esperado (`http://greencap.local`, login `admin`/`admin`) e o script de teardown correspondente (`setup/teardown.sh`).

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
