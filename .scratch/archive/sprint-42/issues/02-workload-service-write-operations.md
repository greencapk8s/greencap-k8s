---
title: WorkloadService — operações de escrita para Jobs e CronJobs
status: done
---

## O que

Implementar os quatro métodos de escrita no `WorkloadService` que serão chamados pelas views.

## Critérios de aceite

- `triggerCronJob(Cluster cluster, String namespace, String cronJobName)`:
  - Busca o CronJob via `client.batch().v1().cronjobs().inNamespace(namespace).withName(cronJobName).get()`
  - Cria um novo Job a partir de `cronJob.getSpec().getJobTemplate()`
  - Nome gerado: `<cronJobName>-manual-<System.currentTimeMillis() / 1000>`
  - Persiste via `client.batch().v1().jobs().inNamespace(namespace).resource(job).create()`
  - Retorna o nome do Job criado (`String`) para que a view possa navegar para ele
  - Lança `KubernetesOperationException` em falha
  - Envolto em `try-with-resources`

- `suspendCronJob(Cluster cluster, String namespace, String name, boolean suspend)`:
  - Faz patch de `spec.suspend` via `client.batch().v1().cronjobs().inNamespace(namespace).withName(name).edit(cj -> { cj.getSpec().setSuspend(suspend); return cj; })`
  - Lança `KubernetesOperationException` em falha
  - Envolto em `try-with-resources`

- `deleteJob(Cluster cluster, String namespace, String name)`:
  - Deleta via `client.batch().v1().jobs().inNamespace(namespace).withName(name).delete()`
  - Cascade padrão do Kubernetes: Pods do Job são deletados junto
  - Lança `KubernetesOperationException` em falha
  - Envolto em `try-with-resources`

- `deleteCronJob(Cluster cluster, String namespace, String name)`:
  - Deleta via `client.batch().v1().cronjobs().inNamespace(namespace).withName(name).delete()`
  - Cascade padrão: todos os Jobs filhos e seus Pods são deletados junto
  - Lança `KubernetesOperationException` em falha
  - Envolto em `try-with-resources`
