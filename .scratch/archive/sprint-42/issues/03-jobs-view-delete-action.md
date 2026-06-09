---
title: JobsView — ação de delete
status: done
---

## O que

Adicionar botão de Delete na coluna de ações da `JobsView`, com dialog de confirmação e aviso de cascade.

## Critérios de aceite

- Botão com ícone `TRASH` adicionado à coluna de ações após o botão de Manifest
- Botão habilitado apenas quando `SecurityUtils.hasPermission(Permission.WORKLOADS_JOBS_DELETE)`
- Ao clicar: abre dialog de confirmação contendo:
  - Título: `"Delete Job"`
  - Mensagem: `"Deleting this Job will also remove all its Pods and logs. This action cannot be undone."`
  - Botão "Delete" com `LUMO_ERROR` + `LUMO_PRIMARY`; botão "Cancel"
- Ao confirmar: chama `workloadService.deleteJob(cluster, namespace, name)`
  - Sucesso: notificação `"Job <nome> deleted"` em `BOTTOM_END` + refresh do grid
  - Falha: notificação de erro em `BOTTOM_END`
- Largura da coluna de ações ajustada para acomodar o botão adicional
