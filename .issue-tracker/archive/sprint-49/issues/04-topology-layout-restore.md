---
title: "Topologia: restaurar posições e toggle ao entrar na view"
status: done
sprint: 49
---

## Problema

Mesmo com as posições salvas no banco, o `_renderGraph()` sempre executa o fcose do zero, ignorando qualquer layout anterior.

## Solução

Ao carregar o grafo, buscar o TopologyLayout salvo para o contexto atual. Se existir: passar as posições ao componente como propriedade `savedPositions` e aplicar `preset` layout nos nós com posição conhecida, deixando o fcose para os nós novos. Restaurar o estado do toggle antes de renderizar.

## Entregáveis

- [ ] `TopologiaView.beforeEnter` carrega o TopologyLayout salvo junto com o grafo
- [ ] `TopologyGraphComponent` recebe propriedade `savedPositions` (JSON) para uso no render
- [ ] `topology-graph.ts`: ao renderizar, nós com posição salva usam layout `preset`; nós sem posição são posicionados pelo fcose
- [ ] Toggle `groupingEnabled` do checkbox é inicializado com o valor salvo (default `true` se não houver layout)
