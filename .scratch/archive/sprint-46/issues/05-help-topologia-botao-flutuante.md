---
title: "Help: botão flutuante na TopologiaView"
status: done
sprint: 46
---

## O que
- `TopologiaView` não usa `buildSectionHeader` (tela full-canvas, sem título/refresh visíveis) — adicionar um botão de Help flutuante (`VaadinIcon.QUESTION_CIRCLE`, posicionado no canto superior do canvas via CSS `position: absolute`) que abre o mesmo `HelpDialog`
- Texto explicando o que é a Topologia, como interpretar os nós e relações exibidas (Deployments, ReplicaSets, Pods, Services etc.) e a exclusão deliberada de pods de Jobs/CronJobs (ver nota em `CONTEXT.md` da sprint 45)

## Por quê
- Garantir que a orientação ao usuário cubra também a tela de Topologia, mesmo com sua estrutura visual diferente das demais views

## Critérios
- Botão de Help visível sobre o canvas do grafo, sem sobrepor a interação com os nós
- Abre o `HelpDialog` com o texto explicativo da Topologia
