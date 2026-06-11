---
title: "Topologia: endpoint server-side para receber e salvar o snapshot de posições"
status: done
sprint: 49
---

## Problema

O frontend Cytoscape tem as posições dos nós mas não há canal para enviá-las ao servidor após cada drag.

## Solução

Adicionar método `@ClientCallable` no `TopologyGraphComponent` que recebe o snapshot JSON de posições e o estado do toggle, e delega ao `TopologyLayoutService` para persistir.

## Entregáveis

- [ ] Método `saveLayout(String nodePositionsJson, boolean groupingEnabled)` anotado com `@ClientCallable` em `TopologyGraphComponent`
- [ ] `TopologyGraphComponent` recebe `TopologyLayoutService` via construtor e delega o upsert
- [ ] `TopologiaView` passa `clusterId`, `namespace` e `userId` ao componente para contextualizar o save
