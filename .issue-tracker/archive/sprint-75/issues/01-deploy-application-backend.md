---
id: "01"
title: "Deploy Application — Backend: DeployApplicationService, IngressClass listing, Permission"
status: done
labels: [feature]
sprint: 75
---

## Contexto

Sprint 75 introduz o wizard "Deploy Application" (ver ADR 0009 e termo **Deploy Application** no `CONTEXT.md`). Esta issue cobre toda a camada de backend: novo serviço, novos métodos de listagem de recursos do cluster, permissão e migration. A issue 02 cobre a UI (wizard + sidebar + CTA).

Decisões de design:
- O wizard cria: Namespace → Deployment → Service (se porta informada) → PVC (opcional) → Ingress (opcional). Best-effort sem rollback — ver ADR 0009.
- A imagem do Deployment usa campo livre (`repo:tag`); sugestões do Registry interno são prefixadas com `registry.kube-system.svc.cluster.local:80/` (mesma constante já definida em `RegistryService.REGISTRY_INTERNAL_HOST`).
- PVC: StorageClass vem de dropdown com classes disponíveis no cluster (`StorageService.listStorageClasses` já existe). Access mode fixo em `ReadWriteOnce`.
- Ingress: IngressClassName vem de dropdown com classes disponíveis no cluster (novo método). Host sugerido: `<namespace>.greencap.local`. Path `/`, pathType `Prefix`.
- O Deployment e o Service compartilham o mesmo nome do Namespace, com label `app: <namespace>`.

## Entrega

### 1. `domain/user/Permission.java`

Adicionar `PROJECT_DEPLOY_APPLICATION` logo após `TOPOLOGY_VIEW` (primeira permission do bloco PROJECT):

```java
// Project — Deploy Application
PROJECT_DEPLOY_APPLICATION,
```

Incluir em `operatorPermissions()` (write operation, não conceder a `viewerPermissions()`).

### 2. `src/main/resources/db/migration/V25__add_deploy_application_permission.sql` (novo)

```sql
-- Grants PROJECT_DEPLOY_APPLICATION to ADMIN and OPERATOR users
-- (identified by GLOBAL_CLUSTERS_WRITE, same pattern as V23/V24).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'PROJECT_DEPLOY_APPLICATION'
FROM user_permissions
WHERE permissions = 'GLOBAL_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;
```

### 3. `ui/UserManagementView.java`

Em `buildProjectGroups()`, adicionar novo grupo antes de "Topology":

```java
groups.add(buildGroup("Deploy Application", Map.of(
        "Deploy Application", Permission.PROJECT_DEPLOY_APPLICATION
), initial));
```

### 4. `kubernetes/NetworkingService.java`

Adicionar método para listar IngressClasses disponíveis no cluster:

```java
public List<String> listIngressClassNames(Cluster cluster) {
    try (KubernetesClient client = clientFactory.buildClient(
            encryptionService.decrypt(cluster.getKubeconfigContent()))) {
        return client.network().v1().ingressClasses().list().getItems().stream()
                .map(ic -> ic.getMetadata().getName())
                .sorted()
                .toList();
    } catch (Exception e) {
        log.error("Failed to list IngressClasses for cluster {}: {}", cluster.getName(), e.getMessage());
        throw new KubernetesOperationException("Failed to list IngressClasses: " + e.getMessage(), e);
    }
}
```

### 5. `kubernetes/dto/DeployApplicationRequest.java` (novo)

```java
package io.greencap.k8s.kubernetes.dto;

public record DeployApplicationRequest(
        String namespace,
        String image,
        int replicas,
        String cpuRequest,
        String cpuLimit,
        String memoryRequest,
        String memoryLimit,
        Integer containerPort,   // null = no Service, no Ingress
        PvcConfig volume,        // null = no PVC
        IngressConfig ingress    // null = no Ingress (requires containerPort != null)
) {
    public record PvcConfig(String storageClass, int storageGi, String mountPath) {}
    public record IngressConfig(String host, String ingressClassName) {}
}
```

### 6. `kubernetes/dto/DeployApplicationResult.java` (novo)

```java
package io.greencap.k8s.kubernetes.dto;

import java.util.List;

public record DeployApplicationResult(
        List<String> createdResources,
        String failedStep,      // null = all succeeded
        String failureMessage   // null = all succeeded
) {
    public boolean isFullSuccess() {
        return failedStep == null;
    }
}
```

### 7. `kubernetes/DeployApplicationService.java` (novo)

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class DeployApplicationService {

    private static final String REGISTRY_INTERNAL_HOST = "registry.kube-system.svc.cluster.local:80";

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;

    public DeployApplicationResult deploy(Cluster cluster, DeployApplicationRequest request) {
        List<String> created = new ArrayList<>();
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {

            createNamespace(client, request.namespace());
            created.add("Namespace: " + request.namespace());

            createDeployment(client, request);
            created.add("Deployment: " + request.namespace());

            if (request.containerPort() != null) {
                try {
                    createService(client, request);
                    created.add("Service: " + request.namespace());
                } catch (Exception e) {
                    return new DeployApplicationResult(created, "Service", e.getMessage());
                }
            }

            if (request.volume() != null) {
                try {
                    createPvc(client, request);
                    created.add("PersistentVolumeClaim: " + request.namespace() + "-pvc");
                } catch (Exception e) {
                    return new DeployApplicationResult(created, "PersistentVolumeClaim", e.getMessage());
                }
            }

            if (request.ingress() != null && request.containerPort() != null) {
                try {
                    createIngress(client, request);
                    created.add("Ingress: " + request.namespace() + "-ingress");
                } catch (Exception e) {
                    return new DeployApplicationResult(created, "Ingress", e.getMessage());
                }
            }

            return new DeployApplicationResult(created, null, null);

        } catch (Exception e) {
            log.error("Failed to deploy application {} on cluster {}: {}", request.namespace(), cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to deploy application: " + e.getMessage(), e);
        }
    }

    private void createNamespace(KubernetesClient client, String name) {
        client.namespaces().resource(
                new NamespaceBuilder()
                        .withNewMetadata().withName(name).endMetadata()
                        .build()
        ).create();
    }

    private void createDeployment(KubernetesClient client, DeployApplicationRequest req) {
        var resourceRequirements = new ResourceRequirementsBuilder()
                .addToRequests("cpu", new Quantity(req.cpuRequest()))
                .addToRequests("memory", new Quantity(req.memoryRequest()))
                .addToLimits("cpu", new Quantity(req.cpuLimit()))
                .addToLimits("memory", new Quantity(req.memoryLimit()))
                .build();

        var containerBuilder = new ContainerBuilder()
                .withName(req.namespace())
                .withImage(req.image())
                .withResources(resourceRequirements);

        if (req.containerPort() != null) {
            containerBuilder.addNewPort().withContainerPort(req.containerPort()).endPort();
        }

        if (req.volume() != null) {
            containerBuilder.addNewVolumeMount()
                    .withName("data")
                    .withMountPath(req.volume().mountPath())
                    .endVolumeMount();
        }

        var specBuilder = new PodSpecBuilder()
                .addToContainers(containerBuilder.build());

        if (req.volume() != null) {
            specBuilder.addNewVolume()
                    .withName("data")
                    .withNewPersistentVolumeClaim()
                        .withClaimName(req.namespace() + "-pvc")
                    .endPersistentVolumeClaim()
                    .endVolume();
        }

        client.apps().deployments().inNamespace(req.namespace()).resource(
                new DeploymentBuilder()
                        .withNewMetadata()
                            .withName(req.namespace())
                            .withNamespace(req.namespace())
                        .endMetadata()
                        .withNewSpec()
                            .withReplicas(req.replicas())
                            .withNewSelector()
                                .withMatchLabels(Map.of("app", req.namespace()))
                            .endSelector()
                            .withNewTemplate()
                                .withNewMetadata()
                                    .withLabels(Map.of("app", req.namespace()))
                                .endMetadata()
                                .withNewSpecLike(specBuilder.build()).endSpec()
                            .endTemplate()
                        .endSpec()
                        .build()
        ).create();
    }

    private void createService(KubernetesClient client, DeployApplicationRequest req) {
        client.services().inNamespace(req.namespace()).resource(
                new ServiceBuilder()
                        .withNewMetadata()
                            .withName(req.namespace())
                            .withNamespace(req.namespace())
                        .endMetadata()
                        .withNewSpec()
                            .withType("ClusterIP")
                            .withSelector(Map.of("app", req.namespace()))
                            .addNewPort()
                                .withPort(req.containerPort())
                                .withTargetPort(new IntOrStringBuilder().withIntVal(req.containerPort()).build())
                            .endPort()
                        .endSpec()
                        .build()
        ).create();
    }

    private void createPvc(KubernetesClient client, DeployApplicationRequest req) {
        var pvc = req.volume();
        client.persistentVolumeClaims().inNamespace(req.namespace()).resource(
                new PersistentVolumeClaimBuilder()
                        .withNewMetadata()
                            .withName(req.namespace() + "-pvc")
                            .withNamespace(req.namespace())
                        .endMetadata()
                        .withNewSpec()
                            .withStorageClassName(pvc.storageClass())
                            .withAccessModes("ReadWriteOnce")
                            .withNewResources()
                                .addToRequests("storage", new Quantity(pvc.storageGi() + "Gi"))
                            .endResources()
                        .endSpec()
                        .build()
        ).create();
    }

    private void createIngress(KubernetesClient client, DeployApplicationRequest req) {
        var ingress = req.ingress();
        client.network().v1().ingresses().inNamespace(req.namespace()).resource(
                new IngressBuilder()
                        .withNewMetadata()
                            .withName(req.namespace() + "-ingress")
                            .withNamespace(req.namespace())
                        .endMetadata()
                        .withNewSpec()
                            .withIngressClassName(ingress.ingressClassName())
                            .addNewRule()
                                .withHost(ingress.host())
                                .withNewHttp()
                                    .addNewPath()
                                        .withPath("/")
                                        .withPathType("Prefix")
                                        .withNewBackend()
                                            .withNewService()
                                                .withName(req.namespace())
                                                .withNewPort()
                                                    .withNumber(req.containerPort())
                                                .endPort()
                                            .endService()
                                        .endBackend()
                                    .endPath()
                                .endHttp()
                            .endRule()
                        .endSpec()
                        .build()
        ).create();
    }
}
```

Notas de implementação:
- O try-with-resources externo envolve apenas Namespace e Deployment (falhas aí lançam `KubernetesOperationException`); Service, PVC e Ingress têm try/catch interno que retorna `DeployApplicationResult` com falha parcial — dentro do mesmo try-with-resources do client.
- Nome do Deployment = nome do Service = nome do Namespace = label `app`. Simplifica o lookup na Topologia e nas views existentes.
- O PVC é criado antes do Deployment subir os Pods (ordem: namespace → deployment → service → pvc → ingress). O volume mount referencia o PVC por nome (`<namespace>-pvc`); se o PVC ainda não existe quando o Pod é agendado, o Kubernetes aguarda até que esteja `Bound`.
- `REGISTRY_INTERNAL_HOST` é a mesma constante de `RegistryService` — considerar extração para uma constante compartilhada em `RegistryService` ou em um pacote comum se reutilização se tornar frequente.

## Critérios de aceite

- `./gradlew compileJava` passa sem erros.
- `./gradlew test` passa.
- Migration V25 aplica sem erro na inicialização.
- `listIngressClassNames` retorna as classes disponíveis no `greencap-demo` sem erro.
- A permissão `PROJECT_DEPLOY_APPLICATION` aparece na treeview de permissões de UserManagementView sob o grupo "Deploy Application" (seção PROJECT).
- ADMIN e OPERATOR têm a permissão; VIEWER não tem.
