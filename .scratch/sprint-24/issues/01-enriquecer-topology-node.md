# 01 — Enriquecer TopologyNode com dados de resumo

Status: done

## Contexto

`TopologyNode` atualmente carrega apenas `id`, `label`, `type`, `status` e `manifestUrl`.
Para o drawer da sprint 24 exibir resumo rico sem uma segunda chamada ao cluster, o record
precisa de campos extras que o `TopologyService` já tem disponíveis durante o `buildGraph`.

## Entregáveis

- Adicionar ao record `TopologyNode`:
  - `Map<String, String> labels` — labels do `metadata` do recurso
  - `int readyReplicas` — réplicas prontas (Deployment e ReplicaSet; 0 nos demais)
  - `int desiredReplicas` — réplicas desejadas (Deployment e ReplicaSet; 0 nos demais)
  - `String serviceType` — tipo do Service (ClusterIP, NodePort, LoadBalancer, ExternalName; `""` nos demais)
- Atualizar `TopologyService` nos métodos `deploymentNode`, `replicaSetNode`, `serviceNode`, `podGroupNode`, `podNode` para popular os novos campos
- `topology-graph.ts`: atualizar a interface `NodeData` com os novos campos e repassá-los ao `data()` de cada nó Cytoscape

## Critério de aceite

`TopologyGraph` serializado em JSON contém os novos campos em todos os nós; compilação sem erros.

## Comments
