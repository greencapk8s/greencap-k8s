---
id: "02"
title: "Topologia: redimensionamento dinâmico de nós para acomodar nome completo"
status: done
labels: [feat, ui, topology]
sprint: 50
---

## Contexto

Os nós da Topologia têm tamanho fixo (`width: 144, height: 76`). Nomes longos de recursos (ex: `greencap-demo-backend-5f7c`) ultrapassam visualmente as bordas do nó.

## Entrega

Em `topology-graph.ts`, alterar o estilo dos nós para:

- Tamanho dinâmico baseado no label (`width: 'label'`, `height: 'label'`)
- Mínimo: `min-width: 144`, `min-height: 76` (preserva aparência atual para nomes curtos)
- Padding interno adequado para que o texto não toque as bordas
- Remover `text-max-width` fixo — o nó expande para acomodar o conteúdo

## Critérios de aceite

- Nós com nomes curtos mantêm aparência semelhante à atual
- Nós com nomes longos expandem horizontalmente sem cortar o texto
- Bordas não são ultrapassadas pelo label em nenhum caso
