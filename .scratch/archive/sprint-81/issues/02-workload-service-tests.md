---
title: WorkloadService — testes com KubernetesMockServer (4 cenários)
status: done
sprint: 81
---

## Problem

`WorkloadService` não tem cobertura de teste. Mudanças no mapeamento de campos de `PodInfo` ou `DeploymentInfo`, ou quebras no tratamento de erro que propagam `KubernetesOperationException`, ficam invisíveis até aparecerem na UI em produção.

## Expected

Nova classe `src/test/java/io/greencap/k8s/kubernetes/WorkloadServiceTest.java`.

### Abordagem

Usar `@EnableKubernetesMockClient(crud = true)` do Fabric8 (já disponível via `kubernetes-server-mock:6.13.1` no `build.gradle.kts`). O mock client é injetado como campo estático. `KubernetesClientFactory` e `EncryptionService` são mockados com Mockito para retornar o mock client.

```java
@EnableKubernetesMockClient(crud = true)
class WorkloadServiceTest {

    static KubernetesClient client;

    WorkloadService workloadService;

    @BeforeEach
    void setup() {
        KubernetesClientFactory mockFactory = mock(KubernetesClientFactory.class);
        EncryptionService mockEncryption = mock(EncryptionService.class);
        when(mockEncryption.decrypt(any())).thenReturn("irrelevant");
        when(mockFactory.buildClient(any())).thenReturn(client);
        workloadService = new WorkloadService(mockFactory, mockEncryption);
    }
}
```

### Cenários

**1 — `listPods` retorna pods com campos mapeados corretamente**
- Criar pod no mock client com `name`, `namespace`, `phase`, `nodeName` e `restartCount` preenchidos
- Verificar que `PodInfo` retornado tem todos os campos corretos

**2 — `listPods` com namespace específico vs. all-namespaces**
- Criar pods em namespaces distintos
- Verificar que `listPods(cluster, "payments")` retorna só os pods de `payments`
- Verificar que `listPods(cluster, "all-namespaces")` retorna todos

**3 — `listDeployments` retorna deployments com campos mapeados corretamente**
- Criar deployment no mock client com `replicas`, `readyReplicas`, `availableReplicas`
- Verificar que `DeploymentInfo` retornado tem os campos corretos

**4 — Exceção Fabric8 propaga como `KubernetesOperationException`**
- Configurar `mockFactory.buildClient(any())` para lançar `RuntimeException`
- Verificar que `listPods` lança `KubernetesOperationException` (não vaza a exceção original)
- Esse cenário garante que o padrão de async loading das views não quebra silenciosamente
