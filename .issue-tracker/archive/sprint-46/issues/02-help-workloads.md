---
title: "Help: textos e botão nas views de Workloads"
status: done
sprint: 46
---

## O que
- Adicionar constantes privadas `HELP_TITLE` / `HELP_TEXT` e passar para `buildSectionHeader` em:
  - `DeploymentsView` — o que é um Deployment + operações Scale e Restart
  - `ReplicaSetView` — o que é um ReplicaSet, somente leitura, coluna Owner
  - `PodsView` — o que é um Pod, somente leitura
  - `JobsView` — o que é um Job, status derivado de `.status.conditions`, operação Delete
  - `CronJobsView` — o que é um CronJob, operações Trigger, Suspend/Resume e Delete

## Por quê
- Orientar usuários iniciantes sobre o propósito de cada tela de Workloads e quais ações podem realizar nela

## Critérios
- Cada view exibe o botão de Help no header e abre o `HelpDialog` com o texto correspondente
- Texto baseado nas definições do glossário em `CONTEXT.md`, em linguagem acessível, complementado com as operações disponíveis na tela
