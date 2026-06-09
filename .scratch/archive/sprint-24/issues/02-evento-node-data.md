# 02 — Alterar evento node-clicked para passar dados completos do nó

Status: done

## Contexto

Hoje `topology-graph.ts` dispara `node-clicked` com apenas `{ manifestUrl }`.
Para o drawer server-side funcionar, o servidor precisa receber todos os dados do nó
clicado (id, label, type, status, labels, replicas, serviceType, manifestUrl).

Além disso, o tap no canvas vazio do Cytoscape deve disparar um evento de fechamento
do drawer.

## Entregáveis

- Em `topology-graph.ts`:
  - Evento `node-clicked`: incluir no `detail` todos os campos de `NodeData` (id, label, type, status, labels, readyReplicas, desiredReplicas, serviceType, manifestUrl)
  - Novo evento `canvas-tapped` disparado ao fazer `tap` no background do Cytoscape (sem nó alvo)
- Em `TopologyGraphComponent.java`:
  - Novo método `addNodeClickListener(ComponentEventListener)` ou expor `getElement()` diretamente para o listener de `canvas-tapped`
- Em `TopologiaView`:
  - Listener de `node-clicked` para de navegar; delega ao drawer (issue 03)
  - Listener de `canvas-tapped` fecha o drawer

## Critério de aceite

Clicar num nó não navega mais; clicar no fundo do grafo não causa erro.

## Comments
