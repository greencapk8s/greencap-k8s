---
id: "01"
title: "Backend: permissions de delete + métodos de delete nos kubernetes services"
status: done
labels: [feat, backend]
sprint: 51
---

## Contexto

GreenCap passa a ser uma plataforma de gerenciamento ativo de recursos Kubernetes. A sprint 51 adiciona a operação Delete nas 9 views da seção PROJECT que ainda não a possuem (Jobs e CronJobs já têm delete).

## Entrega

### 1. Permission enum

Adicionar ao `Permission.java`:
- `WORKLOADS_DEPLOYMENTS_DELETE`
- `WORKLOADS_REPLICASETS_DELETE`
- `WORKLOADS_PODS_DELETE`
- `NETWORKING_SERVICES_DELETE`
- `NETWORKING_INGRESS_DELETE`
- `PARAMETERS_CONFIGMAPS_DELETE`
- `PARAMETERS_SECRETS_DELETE`
- `AUTOSCALING_HORIZONTALSCALER_DELETE`
- `STORAGE_PVC_DELETE`

Atualizar `operatorPermissions()` para incluir todos os 9 novos valores.
`allPermissions()` já inclui automaticamente via `Set.of(values())`.
`viewerPermissions()` não recebe nenhum.

### 2. Migrations Flyway

Criar `V{n}__add_delete_permissions.sql` para conceder os 9 novos permissions a todos os usuários com perfil ADMIN e OPERATOR (seguindo o padrão das migrations existentes).

### 3. Métodos de delete nos kubernetes services

Adicionar métodos `delete*(Cluster, namespace, name)` aos services existentes:

- `WorkloadService`: `deleteDeployment`, `deleteReplicaSet`, `deletePod`
- `NetworkingService`: `deleteService`, `deleteIngress`
- `ConfigurationService`: `deleteConfigMap`, `deleteSecret`
- `AutoScalingService`: `deleteHorizontalScaler`
- `StorageService`: `deletePersistentVolumeClaim`

Cada método deve usar `try-with-resources` com `KubernetesClient`, lançar `KubernetesOperationException` em falha e logar com `log.info` ao sucesso.

## Critérios de aceite

- `./gradlew compileJava` sem erros
- `./gradlew test` passando
- Os 9 novos permissions aparecem no enum e nos presets corretos
- Migration Flyway aplicada sem erros no startup
