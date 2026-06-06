---
title: JobsView — botão Ver Pods + filtro por CronJob
status: done
---

## O que

Duas mudanças na `JobsView`:
1. Botão "Ver Pods" por linha → navega para `workloads/pods?job=<name>`
2. Leitura de `?cronjob=<name>` para pré-popular o filtro de Owner

## Critérios de aceite

- Coluna de ações ganha botão com ícone `LIST` e title "View Pods"
- Clique navega para `workloads/pods?job=<job-name>`
- `beforeEnter()` lê `?cronjob=<name>` e pré-popula o campo de filtro Owner
- Com o filtro pré-populado, apenas Jobs daquele CronJob são exibidos
- Sem o query param: comportamento idêntico ao atual
