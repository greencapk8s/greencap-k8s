---
id: 02
title: Web Component LitElement com Cytoscape.js
status: done
sprint: 23
---

## Objetivo

Criar o Web Component `topology-graph` em TypeScript (LitElement + Cytoscape.js) que recebe um `TopologyGraph` serializado como JSON e renderiza o grafo interativo.

## Escopo

- Arquivo `src/main/frontend/topology-graph.ts`
- Anotação `@NpmPackage(value = "cytoscape", version = "3.30.2")` no Java correspondente

## Comportamento esperado

- Recebe propriedade `graphData` (JSON string com `nodes` e `edges`)
- Renderiza com layout `breadthfirst` (raízes = Deployments e Services sem owner)
- Pan e zoom habilitados
- Emite evento `node-clicked` com `{ manifestUrl }` ao clicar em um nó
- Exibe label e tipo de cada nó (estilo visual diferente por tipo)
- Nós isolados visíveis mas sem aresta

## Critérios de aceite

- [ ] Componente renderiza grafo com nós e arestas corretos
- [ ] Clique em nó emite `node-clicked` com `manifestUrl`
- [ ] Pan e zoom funcionam
- [ ] Build `./gradlew bootRun` passa sem erros de compilação frontend
