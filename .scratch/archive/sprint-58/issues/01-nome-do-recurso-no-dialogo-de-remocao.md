---
id: "01"
title: "Incluir nome do recurso na mensagem do diálogo de remoção"
status: done
labels: [feat, frontend]
sprint: 58
---

## Contexto

Os diálogos de confirmação de remoção (`ConfirmDialog`, header "Delete X") não citam o nome do recurso que será removido — apenas o tipo. Como a remoção é disparada pela barra de título sobre o item selecionado no grid (sprint 57), é importante que o usuário veja explicitamente qual item será afetado antes de confirmar.

## Entrega

Em todas as views com diálogo de remoção, incluir o nome do recurso na primeira frase do `setText()`, substituindo "this `<Tipo>`" por "`<Tipo>` \"`<nome>`\"":

- `PodsView.openDeleteDialog` — "Deleting Pod \"{name}\" will remove it from the cluster..."
- `DeploymentsView.openDeleteDialog` — "Deleting Deployment \"{name}\" will also remove all its ReplicaSets and Pods..."
- `ReplicaSetView.openDeleteDialog` — "Deleting ReplicaSet \"{name}\" will also remove all its Pods..."
- `JobsView.openDeleteJobDialog` — "Deleting Job \"{name}\" will also remove all its Pods and logs..."
- `CronJobsView.openDeleteDialog` — ambas as variantes (com e sem Jobs ativos) passam a citar `cj.name()`
- `ServicesView.openDeleteDialog` — "Deleting Service \"{name}\" will remove its network endpoint..."
- `IngressView.openDeleteDialog` — "Deleting Ingress \"{name}\" will remove all its routing rules..."
- `ConfigMapsView.openDeleteDialog` — "Deleting ConfigMap \"{name}\" will remove it from the cluster..."
- `SecretsView.openDeleteDialog` — "Deleting Secret \"{name}\" will remove it from the cluster..."
- `HorizontalScalerView.openDeleteDialog` — "Deleting HorizontalPodAutoscaler \"{name}\" will remove automatic scaling for its target..."
- `PersistentVolumeClaimsView.openDeleteDialog` — "Deleting PersistentVolumeClaim \"{name}\" may result in permanent data loss..."

`ClustersView.confirmDelete` já cita o nome ("Are you sure you want to remove \"{name}\"?") — fora de escopo, sem alteração.

## Critérios de aceite

- `./gradlew compileJava` sem erros
- Os 11 diálogos acima exibem o nome do recurso no corpo da mensagem
- Header dos diálogos permanece inalterado ("Delete X")

## Comments
