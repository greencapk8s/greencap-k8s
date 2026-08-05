---
title: "Topologia: migrar layout para fcose eliminando sobreposição de TopologyGroups"
status: done
sprint: 48
---

## Problema

O layout `breadthfirst` não suporta compound nodes (TopologyGroups). Quando o agrupamento está ativo, os containers se sobrepõem porque o algoritmo não calcula o espaço necessário para os grupos pai-filho.

## Solução

Migrar o algoritmo de layout do Cytoscape de `breadthfirst` para `fcose` (force-directed com suporte nativo a compound nodes). Usar `fcose` nos dois modos: com e sem agrupamento.

## Entregáveis

- [ ] Adicionar dependência `cytoscape-fcose` no frontend
- [ ] Registrar e configurar o layout `fcose` em `topology-graph.ts`
- [ ] Remover lógica de `rootIds` (exclusiva do `breadthfirst`)
- [ ] Validar que nós não se sobrepõem com grupos ativados
- [ ] Validar que o grafo sem grupos também renderiza corretamente
