---
title: "ObservabilityService: listEventsForResource com field selector"
status: done
labels: [feat, kubernetes]
---

## Descrição

Adicionar método `listEventsForResource(Cluster, String namespace, String kind, String name)` ao `ObservabilityService` que usa Fabric8 field selector para buscar apenas os events do recurso especificado (`involvedObject.name` + `involvedObject.kind`).

## Critérios de aceite

- Método retorna `List<EventInfo>` ordenado por `lastTimestamp` decrescente
- Field selectors aplicados: `involvedObject.name=<name>` e `involvedObject.kind=<kind>`
- Respeita o namespace ativo (sem suporte a all-namespaces para este método — events scoped são sempre namespaced)
- Lança `KubernetesOperationException` em falha de API
