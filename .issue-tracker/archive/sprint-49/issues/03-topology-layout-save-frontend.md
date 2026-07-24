---
title: "Topologia: auto-save de posições no frontend após drag"
status: done
sprint: 49
---

## Problema

O Cytoscape não persiste automaticamente as posições dos nós. Após cada recarregamento da view, o fcose recalcula o layout do zero.

## Solução

Após cada evento `dragfree` no Cytoscape, serializar as posições de todos os nós visíveis (snapshot completo) e chamar o método `@ClientCallable` do servidor via `$server.saveLayout(...)`.

## Entregáveis

- [ ] Listener `dragfree` em `topology-graph.ts` que coleta `{ id, x, y }` de todos os nós não-grupo
- [ ] Chamada `this.$server.saveLayout(JSON.stringify(positions), this.groupingEnabled)` após cada drag
- [ ] Listener `change` no toggle `groupingEnabled` também dispara o save (para persistir a mudança do checkbox imediatamente)
