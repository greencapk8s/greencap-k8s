---
id: "27-02"
title: "topology-graph.ts — cor e interface para PVC"
status: done
labels: [feat, topology, frontend]
sprint: 27
---

## Objetivo

Atualizar o Web Component Cytoscape para reconhecer e renderizar nós do tipo `PersistentVolumeClaim`.

## Escopo

### `topology-graph.ts`
- Adicionar `PersistentVolumeClaim: '#F97316'` em `NODE_COLORS`
- Adicionar campos `capacity` e `accessMode` na interface `NodeData`
- Passar `capacity` e `accessMode` no evento `node-clicked`
- PVC como nó raiz no layout `breadthfirst` quando isolado (sem arestas de entrada)
  - Já tratado automaticamente pela lógica existente de `rootIds`

## Critério de aceite

- [ ] Nó PVC renderiza em laranja `#F97316`
- [ ] Evento `node-clicked` inclui `capacity` e `accessMode`
- [ ] PVC isolado posicionado como raiz no layout
