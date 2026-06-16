---
id: "02"
title: "Workloads — UI: coluna e filtro 'Nodes' em Deployments/ReplicaSets/StatefulSets/Jobs, filtro 'Node' em Pods"
status: done
labels: [feat, frontend]
sprint: 66
---

## Contexto

Depende da issue `01-nodes-backend.md` (novo campo `nodes` em `DeploymentInfo`/`ReplicaSetInfo`/`StatefulSetInfo`/`JobInfo`, formato comma-separated de nomes de Node distintos ou `"—"`).

Decisões definidas via `/grill-with-docs`:

- Nova coluna **"Nodes"** (plural) em `DeploymentsView`, `ReplicaSetView`, `StatefulSetsView`, `JobsView`, sempre posicionada imediatamente antes da coluna "Age" (mesma posição do campo `nodes` no DTO).
- `PodsView` já tem a coluna "Node" (singular, `PodInfo::node`) — não muda de posição/dados, mas ganha filtro.
- Todas as 5 views ganham um campo de filtro de texto na nova/coluna existente "Node(s)", seguindo exatamente o padrão já usado para Name/Owner/Status (`buildFilterField()`, `TextField` na `HeaderRow`, `matches(...)` no predicado do `ListDataProvider`).
- `CronJobsView` fora de escopo (CronJob não tem Pods próprios).

## Entrega

### 1. `DeploymentsView`

Colunas atuais: `Name | Replicas | Available | Age`. Nova ordem: `Name | Replicas | Available | Nodes | Age`.

- `var nodesCol = deployGrid.addColumn(DeploymentInfo::nodes).setHeader("Nodes").setFlexGrow(1).setResizable(true);` — inserida entre a coluna "Available" e a coluna "Age".
- Novo `TextField nodesFilter = buildFilterField();`.
- `dataProvider.setFilter(...)`: adicionar `&& matches(item.nodes(), nodesFilter.getValue())`.
- `nodesFilter.addValueChangeListener(...)` — mesmo corpo de `nameFilter` (refresh + `selectFirstOrPreserve`).
- `filterRow.getCell(nodesCol).setComponent(nodesFilter);`.

### 2. `ReplicaSetView`

Colunas atuais: `Name | Owner | Ready/Desired | Age`. Nova ordem: `Name | Owner | Ready/Desired | Nodes | Age`.

- `var nodesCol = grid.addColumn(ReplicaSetInfo::nodes).setHeader("Nodes").setFlexGrow(1).setResizable(true);` — entre "Ready / Desired" e "Age".
- Novo `TextField nodesFilter = buildFilterField();`.
- `dataProvider.setFilter(...)`: adicionar `&& matches(item.nodes(), nodesFilter.getValue())`.
- `nodesFilter.addValueChangeListener(...)` — mesmo corpo de `nameFilter`/`ownerFilter`.
- `filterRow.getCell(nodesCol).setComponent(nodesFilter);`.

### 3. `StatefulSetsView`

Colunas atuais: `Name | Replicas | Available | Service | Age`. Nova ordem: `Name | Replicas | Available | Service | Nodes | Age`.

- `var nodesCol = grid.addColumn(StatefulSetInfo::nodes).setHeader("Nodes").setFlexGrow(1).setResizable(true);` — entre "Service" e "Age".
- View hoje só tem `nameFilter` na `HeaderRow`. Novo `TextField nodesFilter = buildFilterField();`.
- `dataProvider.setFilter(...)`: hoje `item -> matches(item.name(), nameFilter.getValue())` → adicionar `&& matches(item.nodes(), nodesFilter.getValue())`.
- `nodesFilter.addValueChangeListener(...)` — mesmo corpo de `nameFilter`.
- `filterRow.getCell(nodesCol).setComponent(nodesFilter);`.

### 4. `JobsView`

Colunas atuais: `Name | Status | Completions | Duration | Age | Owner`. Nova ordem: `Name | Status | Completions | Duration | Nodes | Age | Owner`.

- `var nodesCol = grid.addColumn(JobInfo::nodes).setHeader("Nodes").setFlexGrow(1).setResizable(true);` — entre "Duration" e "Age".
- View hoje tem `nameFilterField` e `ownerFilterField`. Novo `TextField nodesFilterField = buildFilterField();`.
- `dataProvider.setFilter(...)`: adicionar `&& matches(item.nodes(), nodesFilterField.getValue())`.
- `nodesFilterField.addValueChangeListener(...)` — mesmo corpo de `nameFilterField`/`ownerFilterField`.
- `filterRow.getCell(nodesCol).setComponent(nodesFilterField);`.

### 5. `PodsView`

Coluna "Node" já existe (`podGrid.addColumn(PodInfo::node).setHeader("Node")...`), entre "Status" e "Restarts". Não muda de posição — só ganha filtro:

- Capturar a coluna em uma variável: `var nodeCol = podGrid.addColumn(PodInfo::node).setHeader("Node").setFlexGrow(1).setResizable(true);`.
- Novo `TextField nodeFilter = buildFilterField();` (declarado junto a `nameFilter`/`statusFilter`).
- `dataProvider.setFilter(...)`: adicionar `&& matches(item.node(), nodeFilter.getValue())`.
- `nodeFilter.addValueChangeListener(...)` — mesmo corpo de `statusFilter` (refresh + `selectFirstOrPreserve`).
- `filterRow.getCell(nodeCol).setComponent(nodeFilter);`.

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros.
- Em `samples/greencap-demo` (3 Nodes), cada uma das 5 views mostra a coluna Node(s) corretamente preenchida e filtra ao digitar um nome de Node parcial (ex: "m02").
- Workloads sem Pods correspondentes mostram "—" na coluna Nodes, e o filtro por "—" funciona (mesmo comportamento de `matches` já usado para "Owner"/"Service").
- Larguras/alinhamento consistentes com as demais colunas (`setFlexGrow(1).setResizable(true)`); sem quebra de layout em telas estreitas.
- Sem regressão nos filtros existentes (Name, Owner, Status) — todos continuam funcionando em conjunto com o novo filtro de Node(s).

## Comments
