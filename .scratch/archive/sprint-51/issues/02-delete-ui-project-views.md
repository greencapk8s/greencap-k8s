---
id: "02"
title: "Frontend: botão Delete + ConfirmDialog nas 9 views PROJECT"
status: done
labels: [feat, frontend]
sprint: 51
---

## Contexto

Com o backend de delete implementado (issue 01), adicionar a UI de delete nas 9 views da seção PROJECT que ainda não a possuem.

## Entrega

Para cada view, adicionar:
1. Botão Delete (ícone `VaadinIcon.TRASH`, `LUMO_TERTIARY + LUMO_ICON + LUMO_ERROR`) na coluna de ações, visível apenas se o usuário tiver a permission `_DELETE` correspondente
2. `ConfirmDialog` com texto específico antes de executar o delete
3. Recarregar o grid após delete bem-sucedido
4. Notificação de erro via `Notification` em `BOTTOM_END` se `KubernetesOperationException` for lançada

### Textos de confirmação por recurso

| View | Texto do ConfirmDialog |
|---|---|
| **Deployments** | "Deleting this Deployment will also remove all its ReplicaSets and Pods. This action cannot be undone." |
| **ReplicaSets** | "Deleting this ReplicaSet will also remove all its Pods. If owned by a Deployment, a new ReplicaSet will be created. This action cannot be undone." |
| **Pods** | "Deleting this Pod will remove it from the cluster. If managed by a controller, it will be recreated automatically. This action cannot be undone." |
| **Services** | "Deleting this Service will remove its network endpoint. Workloads targeting it will lose connectivity. This action cannot be undone." |
| **Ingresses** | "Deleting this Ingress will remove all its routing rules. External traffic to the associated hosts will stop. This action cannot be undone." |
| **ConfigMaps** | "Deleting this ConfigMap will remove it from the cluster. Workloads that depend on it may fail. This action cannot be undone." |
| **Secrets** | "Deleting this Secret will remove it from the cluster. Workloads that depend on it may fail. This action cannot be undone." |
| **HorizontalScaler** | "Deleting this HorizontalPodAutoscaler will remove automatic scaling for its target. This action cannot be undone." |
| **PVCs** | "Deleting this PersistentVolumeClaim may result in permanent data loss depending on the reclaim policy. This action cannot be undone." |

### Views afetadas

- `DeploymentsView` — permission `WORKLOADS_DEPLOYMENTS_DELETE`
- `ReplicaSetView` — permission `WORKLOADS_REPLICASETS_DELETE`
- `PodsView` — permission `WORKLOADS_PODS_DELETE`
- `ServicesView` — permission `NETWORKING_SERVICES_DELETE`
- `IngressView` — permission `NETWORKING_INGRESS_DELETE`
- `ConfigMapsView` — permission `PARAMETERS_CONFIGMAPS_DELETE`
- `SecretsView` — permission `PARAMETERS_SECRETS_DELETE`
- `HorizontalScalerView` — permission `AUTOSCALING_HORIZONTALSCALER_DELETE`
- `PersistentVolumeClaimsView` — permission `STORAGE_PVC_DELETE`

## Critérios de aceite

- Botão Delete visível para usuário com permission, oculto para Viewer
- ConfirmDialog exibe o texto correto para cada recurso
- Após confirmar, o recurso some do grid
- Erro de API exibe notificação em `BOTTOM_END` sem derrubar a view
- `./gradlew compileJava` sem erros
