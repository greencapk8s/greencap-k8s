# 02 — MetricsView e ativação do menu Metrics

Status: done

## O que fazer

- Criar `MetricsView` em `ui/` com rota `observability/metrics`
- Grid: Pod, CPU (em millicores, ex: "250m"), Memory (em MiB, ex: "128Mi"), Namespace
- Colunas sortáveis e resizáveis
- Mensagem amigável quando metrics-server indisponível
- Menu: substituir `disabledNavItem("Metrics")` por item ativo apontando para `MetricsView`
- Atualizar `CONTEXT.md` com o termo `PodMetric`

## Critério de aceite

- Grid exibe top pods do namespace selecionado, ordenados por CPU desc
- Compilação sem erros
