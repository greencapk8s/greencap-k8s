---
id: "01"
title: "Infrastructure: Cordon/Uncordon de Nodes"
status: done
labels: [feat, fullstack]
sprint: 56
---

## Contexto

GreenCap é uma plataforma de gerenciamento ativo (`CONTEXT.md`). A `NodesView` (seção Infrastructure) é hoje 100% read-only. Cordon/Uncordon é a ação de manutenção mais comum sobre Nodes: marca o node como `unschedulable`, impedindo que novos Pods sejam agendados nele, sem afetar os Pods já em execução. Drain (que adicionalmente evicta os Pods existentes) fica fora do escopo desta issue.

## Entrega

### 1. Permission

Adicionar ao `Permission.java`:
- `SETTINGS_INFRASTRUCTURE_CORDON`

Incluir em `operatorPermissions()`. Não incluir em `viewerPermissions()`. `allPermissions()` já cobre via `Set.of(values())`.

### 2. Migration Flyway

Criar `V18__add_node_cordon_permission.sql`, seguindo o padrão de `V17__add_delete_permissions.sql`: conceder `SETTINGS_INFRASTRUCTURE_CORDON` a todos os usuários que possuem `SETTINGS_CLUSTERS_WRITE` (sinal de ADMIN/OPERATOR).

### 3. NodeInfo (DTO)

Adicionar campo `boolean schedulingDisabled` ao record `kubernetes/dto/NodeInfo.java`.

### 4. StorageService

- `listNodes()`: popular `schedulingDisabled` a partir de `node.getSpec().getUnschedulable()` (`Boolean.TRUE.equals(...)`).
- Novo método `cordonNode(Cluster cluster, String nodeName, boolean cordon)`: dentro de `try-with-resources`, usar `client.nodes().withName(nodeName).edit(n -> { n.getSpec().setUnschedulable(cordon); return n; })`. Logar com `log.info` ("Cordoned"/"Uncordoned" node {}). Lançar `KubernetesOperationException` em falha.

### 5. NodesView

- Nova coluna "Scheduling" (badge): `Schedulable` (tema `success`) quando `!schedulingDisabled`, `Cordoned` (tema `contrast`) quando `schedulingDisabled` — mesmo padrão de `suspendBadge` em `CronJobsView`.
- Coluna de ações passa a ter 2 botões (`UiConstants.actionsColumnWidth(2)`):
  - Botão toggle Cordon/Uncordon: ícone `VaadinIcon.PAUSE` ("Cordon") quando schedulable, `VaadinIcon.PLAY` ("Uncordon") quando cordoned; `setEnabled` conforme `SETTINGS_INFRASTRUCTURE_CORDON`; chama `storageService.cordonNode(...)` e recarrega o grid — mesmo padrão (toggle sem `ConfirmDialog`, ação instantaneamente reversível) de `toggleSuspend` em `CronJobsView`.
  - Botão "View Manifest" existente (`VaadinIcon.CODE`).
- `HELP_TEXT`: atualizar mencionando Cordon/Uncordon e a diferença para Drain (fora de escopo).

### 6. CONTEXT.md

- Atualizar a definição de **Node**: acrescentar "Supports one write operation: Cordon/Uncordon."
- Adicionar novo termo **Cordon**, no mesmo formato de **Suspend**:
  > A write operation on a Node that marks it as unschedulable (`spec.unschedulable: true`), preventing new Pods from being scheduled onto it without affecting Pods already running. The inverse operation, Uncordon, sets `spec.unschedulable: false`. In GreenCap both are represented by a single toggling button reflecting the current state.
  > _Avoid_: Pause, disable, drain

## Critérios de aceite

- `./gradlew compileJava` sem erros
- `./gradlew test` passando
- `SETTINGS_INFRASTRUCTURE_CORDON` aparece no enum, em `operatorPermissions()` e ausente em `viewerPermissions()`
- Migration aplicada sem erros no startup
- Badge "Scheduling" reflete `spec.unschedulable` corretamente
- Botão toggle desabilitado para Viewer; Cordon/Uncordon funcional para Operator/Admin

## Comments

- Implementação concluída: `Permission.SETTINGS_INFRASTRUCTURE_CORDON` adicionado (enum + `operatorPermissions()`); `V18__add_node_cordon_permission.sql` concede a permission a ADMIN/OPERATOR via sinal `SETTINGS_CLUSTERS_WRITE`; `NodeInfo.schedulingDisabled` populado a partir de `spec.unschedulable`; `StorageService.cordonNode()` faz o patch via `client.nodes().withName(name).edit(...)`; `NodesView` ganhou coluna "Scheduling" (badge `Schedulable`/`Cordoned`) e botão toggle (PAUSE/PLAY) seguindo o padrão de `toggleSuspend` do `CronJobsView`; `CONTEXT.md` atualizado com o novo termo **Cordon** e a write operation do **Node**.
- `UserManagementView` não foi alterado — os 9 `_DELETE` da sprint 51 também não foram cabeados lá (gap pré-existente, fora do escopo).
- `./gradlew compileJava` e `./gradlew test`: BUILD SUCCESSFUL.
- Aceite manual confirmado no browser. Issue fechada.
