---
id: "01"
title: "StatefulSet — backend: WorkloadService, Permissions, AutoScaling e Manifest"
status: done
labels: [feat, backend]
sprint: 61
---

## Contexto

GreenCap ainda não suporta o Workload `StatefulSet`. Decisões de escopo definidas via `/grill-with-docs` (ver `CONTEXT.md` — termos `Workload` e `StatefulSet`):

- StatefulSet usa a mesma interface `RollableScalableResource` do Deployment no Fabric8 — suporta as mesmas 4 write operations: **Scale**, **Restart**, **Rollback**, **Delete**.
- `StatefulSetStatus` tem os mesmos campos relevantes que `DeploymentStatus` (`replicas`, `readyReplicas`, `availableReplicas`), mais `spec.serviceName` (Service headless que dá identidade de rede aos Pods).
- StatefulSet pode ser alvo de `HorizontalPodAutoscaler` (subrecurso `/scale`), igual ao Deployment — o fluxo de Scale precisa checar HPA antes de abrir o diálogo direto.
- StatefulSet entra como 12º tipo editável no Manifest/Apply.
- Fora de escopo desta sprint (registrado em `docs/sprints.md` → "Candidatos para Próximas Sprints"): StatefulSet na Topologia; coluna Owner em PVCs de `volumeClaimTemplates`.

## Entrega

### 1. DTO `StatefulSetInfo`

Novo arquivo `kubernetes/dto/StatefulSetInfo.java`, mesmo shape de `DeploymentInfo` + `serviceName`:

```java
public record StatefulSetInfo(
        String name,
        String namespace,
        int desired,
        int ready,
        int available,
        String serviceName,
        String age
) {}
```

### 2. `WorkloadService`

Novos métodos, seguindo exatamente o padrão dos equivalentes de Deployment (`try-with-resources`, `KubernetesOperationException` em falhas, `log.info`/`log.error`):

- `listStatefulSets(Cluster cluster, String namespace)` — `client.apps().statefulSets()...`, mapeando para `StatefulSetInfo`. `desired` ← `spec.replicas` (default 0), `ready`/`available` ← `status.readyReplicas`/`status.availableReplicas` (default 0), `serviceName` ← `spec.serviceName` (default `"—"`), `age` via `NamespaceService.age(...)`. Suporta `inAnyNamespace()` via `isAllNamespaces()`, igual a `listDeployments`.
- `scaleStatefulSet(Cluster cluster, String namespace, String name, int replicas)` — `client.apps().statefulSets().inNamespace(namespace).withName(name).scale(replicas)`.
- `restartStatefulSet(Cluster cluster, String namespace, String name)` — `.rolling().restart()`.
- `rolloutUndoStatefulSet(Cluster cluster, String namespace, String name)` — `.rolling().undo()`.
- `deleteStatefulSet(Cluster cluster, String namespace, String name)` — `.delete()`.

### 3. `Permission.java` + `V20__add_statefulset_permissions.sql`

Novo grupo, inserido após o grupo `WORKLOADS_DEPLOYMENTS_*` (antes de `WORKLOADS_REPLICASETS_VIEW`, refletindo a posição de StatefulSets na navegação):

```java
WORKLOADS_STATEFULSETS_VIEW,
WORKLOADS_STATEFULSETS_SCALE,
WORKLOADS_STATEFULSETS_RESTART,
WORKLOADS_STATEFULSETS_ROLLBACK,
WORKLOADS_STATEFULSETS_DELETE,
```

- `VIEW` entra em `viewerPermissions()`.
- Todas as 5 entram em `operatorPermissions()`.

Migration `V20__add_statefulset_permissions.sql`:

```sql
-- Grants WORKLOADS_STATEFULSETS_VIEW to all users who already have
-- WORKLOADS_DEPLOYMENTS_VIEW (all profiles: ADMIN, OPERATOR, and VIEWER).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'WORKLOADS_STATEFULSETS_VIEW'
FROM user_permissions
WHERE permissions = 'WORKLOADS_DEPLOYMENTS_VIEW'
ON CONFLICT DO NOTHING;

-- Grants the 4 write permissions to ADMIN and OPERATOR users
-- (identified by SETTINGS_CLUSTERS_WRITE, same pattern as V17/V18).
INSERT INTO user_permissions (user_id, permissions)
SELECT DISTINCT user_id, p.permission_name
FROM user_permissions up
CROSS JOIN (VALUES
    ('WORKLOADS_STATEFULSETS_SCALE'),
    ('WORKLOADS_STATEFULSETS_RESTART'),
    ('WORKLOADS_STATEFULSETS_ROLLBACK'),
    ('WORKLOADS_STATEFULSETS_DELETE')
) AS p(permission_name)
WHERE up.permissions = 'SETTINGS_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;
```

### 4. `AutoScalingService` — generalizar busca de HPA

`findHorizontalScalerForDeployment(Cluster, String, String deploymentName)` filtra por `spec.scaleTargetRef.getName()` e **não** valida `kind` — já funciona para qualquer recurso com subrecurso `/scale`. Renomear para:

```java
public Optional<HorizontalScalerInfo> findHorizontalScalerForTarget(Cluster cluster, String namespace, String targetName)
```

(mesma assinatura, apenas troca o nome do parâmetro `deploymentName` → `targetName` e do método). Atualizar o único call site existente em `DeploymentsView.openScaleDialog`.

### 5. `ManifestService`

- Adicionar `Map.entry("statefulset", "StatefulSet")` em `EDITABLE_RESOURCE_KINDS`.
- Adicionar `case "statefulset" -> client.apps().statefulSets().inNamespace(namespace).withName(name).get();` em `fetchYaml`.

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros.
- `listStatefulSets` retorna dados corretos para um cluster com StatefulSets (testável via `samples/greencap-demo` ou cluster local).
- `scaleStatefulSet`, `restartStatefulSet`, `rolloutUndoStatefulSet`, `deleteStatefulSet` operam corretamente via `kubectl get statefulset -w` em paralelo.
- `findHorizontalScalerForTarget` continua funcionando para Deployments (sem regressão no Scale de `DeploymentsView`).
- `/yaml/statefulset/{namespace}/{name}` retorna o YAML do StatefulSet (Manifest read-only nesta issue — Edit/Apply herdado automaticamente do `ManifestView` existente).

## Comments
