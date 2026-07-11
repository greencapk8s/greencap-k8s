---
id: "88-01"
title: "KubernetesOperatorService — DTOs e operações OLM"
status: done
priority: high
sprint: 88
---

O GreenCap precisa de uma camada de serviço que encapsule toda a comunicação com o OLM, detectando sua presença e expondo as operações necessárias para listar, instalar e desinstalar Kubernetes Operators.

**Detecção de OLM:**

O serviço verifica se o OLM está instalado no cluster tentando listar `PackageManifest`s. Se a API não estiver disponível (a listagem lançar exceção ou retornar erro 404), o serviço sinaliza ausência do OLM — a view usará isso para exibir o empty state informativo.

**DTOs:**

`OperatorInfo` representa um operator instalado: nome, versão atual, channel, CatalogSource de origem, fase do CSV (`Installing`, `Succeeded`, `Failed`) e mensagem de status (preenchida quando `Failed`, usada pelo tooltip do badge).

`OperatorPackage` representa um package disponível no catálogo: nome, nome de exibição, descrição, provider, CatalogSource e lista de channels disponíveis.

`OperatorChannel` representa um channel de um package: nome e versão do CSV corrente.

**Operações:**

`isOlmInstalled(Cluster)` — retorna `true` se o OLM estiver presente no cluster.

`listInstalled(Cluster)` — lista todos os `ClusterServiceVersion`s presentes no namespace `operators`, mapeados para `OperatorInfo`. Ordena por nome.

`listCatalog(Cluster)` — lista todos os `PackageManifest`s de todos os `CatalogSource`s disponíveis, mapeados para `OperatorPackage`. Ordena por nome.

`install(Cluster, packageName, channel)` — verifica se já existe um `OperatorGroup` no namespace `operators`; se não existir, cria um com `spec.targetNamespaces: []` (AllNamespaces). Em seguida, cria uma `Subscription` no namespace `operators` apontando para o package e channel informados. Lança `KubernetesOperationException` em falhas de API.

`uninstall(Cluster, operatorName)` — remove a `Subscription` e o `ClusterServiceVersion` correspondentes do namespace `operators`. Não remove os CRDs criados pelo operator. Lança `KubernetesOperationException` em falhas de API.

A comunicação com o OLM usa `OpenShiftClient` (via `client.adapt(OpenShiftClient.class)`) para acesso aos tipos tipados do Fabric8 — `KubernetesClientFactory` receberá um novo método `buildOpenShiftClient(String kubeconfigContent)` para essa finalidade.
