---
title: "Remover ClusterProvider — campo sem valor funcional"
status: done
priority: high
sprint: 93
---

Remover o campo `provider` da entidade `Cluster` e todas as suas referências. O campo nunca foi usado para lógica — apenas como coluna informativa no grid, sem influenciar como o cliente Kubernetes é construído ou como qualquer operação funciona.

Migration Flyway dropa a coluna `provider` da tabela `clusters`. O enum `ClusterProvider`, o record `CreateClusterRequest` e a view `ClustersView` perdem o campo correspondente. A coluna "Provider" some do grid de clusters.
