# 05 — MainLayout sidebar + ManifestService

## Status
closed

## Descrição
Registrar Jobs e CronJobs no sidebar (sob Workloads) e adicionar os tipos `"job"` e
`"cronjob"` ao switch do `ManifestService`.

## Critérios de aceite
- [ ] `buildWorkloadsNavItem()` inclui sub-itens "Jobs" (→ `JobsView`) e "CronJobs" (→ `CronJobsView`) após Pods, protegidos por `WORKLOADS_JOBS_VIEW` e `WORKLOADS_CRONJOBS_VIEW` respectivamente
- [ ] `ManifestService.fetchYaml()` trata `case "job"` via `client.batch().v1().jobs()`
- [ ] `ManifestService.fetchYaml()` trata `case "cronjob"` via `client.batch().v1().cronJobs()`
- [ ] Itens do sidebar ficam desabilitados quando o cluster não está ativo (comportamento herdado de `clusterDependentNavItems`)
