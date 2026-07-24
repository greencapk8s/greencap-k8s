---
title: CronJobsView — ações de escrita (Trigger, Suspend/Resume, Delete)
status: done
---

## O que

Adicionar três ações de escrita na coluna de ações da `CronJobsView`: Trigger (Run Now), Suspend/Resume e Delete.

## Critérios de aceite

### Botão Trigger (Run Now)
- Ícone `PLAY_CIRCLE` com tooltip `"Trigger Job"`
- Habilitado apenas com `WORKLOADS_CRONJOBS_RUN_NOW`
- Ao clicar: abre dialog simples com mensagem `"Trigger a new Job run from CronJob <nome>?"` e botões "Trigger" + "Cancel"
- Ao confirmar: chama `workloadService.triggerCronJob(cluster, namespace, name)`
  - Sucesso: notificação `"Job <nome-gerado> triggered"` em `BOTTOM_END` + navega para `workloads/jobs?cronjob=<nome-do-cronjob>`
  - Falha: notificação de erro em `BOTTOM_END`

### Botão Suspend/Resume
- Botão único que alterna ícone e tooltip conforme estado atual do CronJob:
  - CronJob ativo (`suspended = false`): ícone `PAUSE`, tooltip `"Suspend"`
  - CronJob suspenso (`suspended = true`): ícone `PLAY`, tooltip `"Resume"`
- Habilitado apenas com `WORKLOADS_CRONJOBS_SUSPEND`
- Sem dialog de confirmação — executa direto (operação reversível)
- Ao clicar: chama `workloadService.suspendCronJob(cluster, namespace, name, !currentSuspendedState)`
  - Sucesso: notificação `"CronJob <nome> suspended"` ou `"CronJob <nome> resumed"` em `BOTTOM_END` + refresh do grid
  - Falha: notificação de erro em `BOTTOM_END`

### Botão Delete
- Ícone `TRASH` com tooltip `"Delete"`
- Habilitado apenas com `WORKLOADS_CRONJOBS_DELETE`
- Ao clicar: verificar `cronJob.activeJobs()` para decidir o nível de aviso:
  - `activeJobs == 0`: mensagem padrão `"Deleting this CronJob will also delete all associated Jobs and Pods. This action cannot be undone."`
  - `activeJobs > 0`: mensagem reforçada `"This CronJob has <N> active Job(s). Deleting it will also delete all associated Jobs and Pods. This action cannot be undone."`
- Botão "Delete" com `LUMO_ERROR` + `LUMO_PRIMARY`; botão "Cancel"
- Ao confirmar: chama `workloadService.deleteCronJob(cluster, namespace, name)`
  - Sucesso: notificação `"CronJob <nome> deleted"` em `BOTTOM_END` + refresh do grid
  - Falha: notificação de erro em `BOTTOM_END`

### Layout
- Ordem dos botões na coluna: `[Trigger] [Suspend/Resume] [Jobs] [Delete] [Manifest]`
- Largura da coluna ajustada para acomodar os botões adicionais
