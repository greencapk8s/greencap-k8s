# 03 — JobsView

## Status
closed

## Descrição
Criar `JobsView` na rota `/jobs`, exibindo Jobs do namespace ativo em read-only.
Segue o padrão das demais listing views (Grid, auto-refresh via Refreshable, namespace
scoping via ClusterContext).

## Colunas
| Coluna | Fonte |
|---|---|
| Name | `JobDto.name` |
| Status | `JobDto.status` — badge: `success` (Complete), `error` (Failed), sem variante (Running), `contrast` (Suspended) |
| Completions | `JobDto.completions` — formato `X/Y` |
| Duration | `JobDto.duration` — formato legível (ex: `2m 34s`) |
| Age | `JobDto.age` — formato relativo (ex: `3d`) |
| Owner | `JobDto.owner` — texto simples, `—` quando vazio |
| Actions | Botão "View Manifest" → `yaml/job/{namespace}/{name}` |

## Critérios de aceite
- [ ] Rota `/jobs` acessível com permissão `WORKLOADS_JOBS_VIEW`
- [ ] Grid exibe as colunas descritas acima
- [ ] Badge de status com variante correta por estado
- [ ] Botão "View Manifest" abre o manifest do Job
- [ ] Implementa `Refreshable` para suporte ao auto-refresh
- [ ] Estado vazio exibido quando não há Jobs no namespace
- [ ] Erro de conexão exibido com notificação em `BOTTOM_END`
