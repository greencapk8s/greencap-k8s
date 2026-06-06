# 04 — CronJobsView

## Status
closed

## Descrição
Criar `CronJobsView` na rota `/cronjobs`, exibindo CronJobs do namespace ativo em read-only.
Segue o padrão das demais listing views.

## Colunas
| Coluna | Fonte |
|---|---|
| Name | `CronJobDto.name` |
| Schedule | `CronJobDto.schedule` — expressão cron literal (ex: `*/5 * * * *`) |
| Suspend | `CronJobDto.suspended` — badge `contrast` quando `true`, célula vazia quando `false` |
| Active | `CronJobDto.active` — número de Jobs ativos no momento |
| Last Schedule | `CronJobDto.lastScheduleTime` — formato relativo (ex: `5m ago`), `—` se nunca agendado |
| Age | `CronJobDto.age` — formato relativo |
| Actions | Botão "View Manifest" → `yaml/cronjob/{namespace}/{name}` |

## Critérios de aceite
- [ ] Rota `/cronjobs` acessível com permissão `WORKLOADS_CRONJOBS_VIEW`
- [ ] Grid exibe as colunas descritas acima
- [ ] Badge `contrast` exibido apenas quando `suspended = true`
- [ ] Botão "View Manifest" abre o manifest do CronJob
- [ ] Implementa `Refreshable` para suporte ao auto-refresh
- [ ] Estado vazio exibido quando não há CronJobs no namespace
- [ ] Erro de conexão exibido com notificação em `BOTTOM_END`
