# 03 — Drawer overlay lateral na TopologiaView

Status: done

## Contexto

Ao clicar num nó, um painel lateral flutuante deve surgir sobre o grafo (sem redimensioná-lo)
mostrando o resumo do recurso. O painel é gerenciado server-side pelo Vaadin.

## Design

- `VerticalLayout` com `position: fixed; top: 0; right: 0; height: 100%; width: 340px`
  via `getStyle()`, elevado com `box-shadow` e `z-index` adequados
- Cabeçalho: ícone de tipo + nome do recurso + badge de status + botão X (fecha)
- Corpo por tipo:
  - **Deployment**: réplicas (ready/desired), labels
  - **ReplicaSet**: réplicas (ready/desired), labels, owner Deployment
  - **Pod group**: contagem de pods, status agregado
  - **Service**: tipo (ClusterIP/NodePort/etc), selector labels
- Rodapé: botão "Ver YAML" (`RouterLink` para `manifestUrl`) — para pod groups vira "Ver Pods"
- Clicar em outro nó substitui o conteúdo do drawer (sem fechar e reabrir)
- Clicar no canvas vazio fecha o drawer (via evento `canvas-tapped` da issue 02)
- Botão X fecha o drawer

## Entregáveis

- `TopologyNodeDrawer.java` — componente Vaadin encapsulando o drawer (construído e adicionado à view)
- `TopologiaView` atualizado: instancia o drawer, registra os listeners de `node-clicked` e `canvas-tapped`

## Critério de aceite

- Drawer abre ao clicar num nó, mostra dados corretos por tipo
- Substituição ao clicar em outro nó funciona sem piscar
- Fecha via X e via clique no canvas
- Pan e zoom no grafo não fecham o drawer

## Comments
