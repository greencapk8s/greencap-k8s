---
id: "34-03"
title: "ReplicaSets: exibir apenas os do último dia, ativos primeiro"
status: done
labels: [enhancement, ux]
---

## Contexto

Kubernetes acumula ReplicaSets a cada rollout (padrão: 10 históricos por Deployment). A maioria fica com `desired=0` indefinidamente, poluindo a listagem com entradas irrelevantes.

## Entrega

- Em `WorkloadService.listReplicaSets()`: filtrar ReplicaSets criados há **mais de 1 dia** (usando `creationTimestamp` do Fabric8)
- Ordenar o resultado: `desired > 0` primeiro, depois `desired == 0`
- Nenhuma mudança no DTO `ReplicaSetInfo`

## Critério de aceite

- [ ] Listagem exibe apenas ReplicaSets criados nas últimas 24h
- [ ] ReplicaSets com `desired > 0` aparecem antes dos inativos
- [ ] ReplicaSets mais antigos que 1 dia não aparecem
