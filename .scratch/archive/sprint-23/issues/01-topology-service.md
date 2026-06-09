---
id: 01
title: TopologyService — busca e montagem do grafo de recursos
status: done
sprint: 23
---

## Objetivo

Implementar `TopologyService` e os DTOs `TopologyGraph`, `TopologyNode`, `TopologyEdge` que representam o grafo de recursos de um Namespace.

## Escopo

- DTOs em `kubernetes/dto/`: `TopologyGraph`, `TopologyNode`, `TopologyEdge`
- `TopologyService` em `kubernetes/` que busca Deployments, ReplicaSets, Pods e Services em paralelo e monta o grafo

## Regras de negócio

**Nós:**
- Um `TopologyNode` por recurso com: `id` = `{tipo}/{nome}`, `label` = nome, `type` (Deployment | ReplicaSet | Pod | Service), `status` (para badge), `manifestUrl` = `/yaml/{tipo}/{namespace}/{nome}`

**Arestas:**
- Deployment → ReplicaSet: `ownerReferences` do ReplicaSet aponta para o Deployment
- ReplicaSet → Pod: `ownerReferences` do Pod aponta para o ReplicaSet
- Service → Pod: `selector` do Service é subconjunto dos `labels` do Pod

**Nós isolados:** incluídos — sem aresta não é motivo para omitir

## Critérios de aceite

- [ ] `TopologyService.buildGraph(ClusterContext, namespace)` retorna `TopologyGraph` válido
- [ ] Arestas corretas para ownerReferences e label selector
- [ ] Nós sem conexão incluídos no resultado
- [ ] Operações Fabric8 dentro de `try-with-resources`
- [ ] Compilação sem erros
