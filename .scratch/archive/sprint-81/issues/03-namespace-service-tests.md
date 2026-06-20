---
title: NamespaceService — testes com KubernetesMockServer (4 cenários)
status: done
sprint: 81
---

## Problem

`NamespaceService` não tem cobertura de teste. O filtro de namespaces em fase `Terminating` (adicionado na Sprint 76) é fácil de quebrar silenciosamente. As operações de escrita (`createNamespace`, `deleteNamespace`) também ficam sem validação.

## Expected

Nova classe `src/test/java/io/greencap/k8s/kubernetes/NamespaceServiceTest.java`.

### Abordagem

Mesma abordagem de `WorkloadServiceTest`: `@EnableKubernetesMockClient(crud = true)` + mocks de `KubernetesClientFactory` e `EncryptionService`.

### Cenários

**1 — `listNamespaceNames` filtra namespaces em fase `Terminating`**
- Criar 3 namespaces no mock client: `active` (sem `deletionTimestamp`), `terminating` (com `deletionTimestamp` preenchido) e `default`
- Verificar que `listNamespaceNames` retorna apenas `active` e `default`, excluindo `terminating`
- Esse cenário protege o comportamento documentado em `CONTEXT.md` para `NamespaceService`

**2 — `listNamespacesWithCounts` retorna contagens corretas**
- Criar 1 namespace com 2 pods, 1 deployment e 1 service no mock client
- Verificar que `NamespaceInfo` retornado tem `podCount=2`, `deploymentCount=1`, `serviceCount=1`

**3 — `createNamespace` cria o namespace no cluster**
- Chamar `createNamespace(cluster, "meu-namespace")`
- Verificar via mock client que o namespace `meu-namespace` existe após a chamada

**4 — Exceção Fabric8 propaga como `KubernetesOperationException`**
- Configurar `mockFactory.buildClient(any())` para lançar `RuntimeException`
- Verificar que `listNamespaceNames` lança `KubernetesOperationException`
