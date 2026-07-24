---
title: "Help: textos e botão nas views de Infrastructure, Observability e Settings"
status: done
sprint: 46
---

## O que
- Adicionar constantes privadas `HELP_TITLE` / `HELP_TEXT` e passar para `buildSectionHeader` em:
  - `NodesView` — o que é um Node, papel na infraestrutura do cluster, somente leitura
  - `EventsView` — o que são Events do Kubernetes e como ajudam a diagnosticar problemas
  - `MetricsView` — o que são as métricas exibidas (uso de CPU/memória) e sua origem
  - `PersistentVolumesView` — o que é um Persistent Volume e sua relação com PVCs
  - `StorageClassesView` — o que é uma Storage Class e como ela define o provisionamento de volumes

## Por quê
- Completar a cobertura do botão de Help em todas as 16 views que usam `buildSectionHeader`, incluindo as fora da seção PROJECT

## Critérios
- Cada view exibe o botão de Help no header e abre o `HelpDialog` com o texto correspondente
- Texto em linguagem acessível a iniciantes, cobrindo o que é o recurso e o que a tela permite fazer
