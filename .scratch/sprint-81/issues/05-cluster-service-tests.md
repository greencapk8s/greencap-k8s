---
title: ClusterService — testes com @SpringBootTest + TestContainers + @MockBean (3 cenários)
status: done
sprint: 81
---

## Problem

`ClusterService` não tem cobertura de teste. O risco mais crítico é `createCluster` persistir o kubeconfig em plaintext por bug no `EncryptionService` — dado sensível exposto no banco sem nenhum alarme.

## Expected

Nova classe `src/test/java/io/greencap/k8s/domain/cluster/ClusterServiceTest.java`.

### Abordagem

`@SpringBootTest` + `@Testcontainers` + `@ServiceConnection` — mesmo container da issue 01. `KubernetesClientFactory` é mockado com `@MockBean` (decisão tomada no planejamento da sprint: sem refatoração de código de produção). O mock retorna um `KubernetesClient` que responde `namespaces().list()` com lista vazia → `testWithPlaintext` retorna `CONNECTED`.

### Cenários

**1 — `createCluster` persiste kubeconfig encriptado, nunca plaintext**
- Montar `CreateClusterRequest` com kubeconfig plaintext conhecido (ex: `"plaintext-kubeconfig"`)
- Chamar `createCluster` (com `SecurityContext` populado via `@WithMockUser`)
- Buscar o `Cluster` no banco e verificar que `kubeconfigContent` **não é igual** ao plaintext original
- Verificar que `encryptionService.decrypt(cluster.getKubeconfigContent())` retorna o plaintext original

**2 — `createCluster` preenche `createdBy` com o usuário autenticado**
- Garantir que o usuário autenticado existe no banco antes do teste
- Chamar `createCluster` com `SecurityContext` populado
- Verificar que `cluster.getCreatedBy().getUsername()` bate com o usuário autenticado

**3 — `markAsDisconnectedIfConnected` só altera clusters em status `CONNECTED`**
- Criar cluster com status `CONNECTED` → verificar que vira `DISCONNECTED`
- Criar cluster com status `ERROR` → verificar que **permanece** `ERROR` (não altera)
- Criar cluster com status `UNKNOWN` → verificar que **permanece** `UNKNOWN`
