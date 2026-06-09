# 02 — WorkloadService: listJobs + listCronJobs + DTOs

## Status
closed

## Descrição
Adicionar DTOs e métodos `listJobs(Cluster, namespace)` e `listCronJobs(Cluster, namespace)`
ao `WorkloadService`, seguindo o mesmo padrão dos métodos existentes (try-with-resources,
KubernetesOperationException em falha).

## DTOs

**JobDto**: `name`, `namespace`, `status` (Complete | Failed | Running | Suspended),
`completions` (succeeded / desired), `duration` (calculado de startTime/completionTime),
`age` (de creationTimestamp), `owner` (nome do CronJob pai via ownerReferences, ou vazio)

**CronJobDto**: `name`, `namespace`, `schedule`, `suspended` (boolean),
`active` (contagem de .status.active), `lastScheduleTime`, `age` (de creationTimestamp)

## Critérios de aceite
- [ ] `JobDto` record criado em `kubernetes/dto/`
- [ ] `CronJobDto` record criado em `kubernetes/dto/`
- [ ] `WorkloadService.listJobs()` usa `client.batch().v1().jobs()` dentro de `try-with-resources`
- [ ] `WorkloadService.listCronJobs()` usa `client.batch().v1().cronJobs()` dentro de `try-with-resources`
- [ ] Status do Job derivado corretamente de `.status.conditions` (Complete/Failed) e `.spec.suspend` (Suspended); Running quando nenhuma condition terminal
- [ ] Duration calculado como `completionTime - startTime` se completo, ou `agora - startTime` se ainda em execução
- [ ] Owner extraído de `ownerReferences[kind=CronJob].name`; vazio quando não há owner
- [ ] Lança `KubernetesOperationException` em falha
- [ ] Log `info` em sucesso, `error` em falha
