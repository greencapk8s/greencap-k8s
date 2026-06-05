# KPI cards de CPU e memória no namespace

Status: done

## Descrição

Adicionar 2 KPI cards na dashboard exibindo o consumo total de CPU e memória de todos os pods no namespace ativo, usando ObservabilityService.listPodMetrics().

## Especificação

- **CPU**: soma de cpuMillicores de todos os PodMetricInfo do namespace. Exibir em millicores se < 1000, em cores (1 casa decimal) se >= 1000.
- **Memória**: soma de memoryMiB de todos os PodMetricInfo do namespace. Exibir em MiB se < 1024, em GiB (1 casa decimal) se >= 1024.

## Comportamento de erro

- Se listPodMetrics() lançar KubernetesOperationException: exibir "N/A" com tooltip "metrics-server não disponível"
- Cards de métricas ficam visivelmente distintos dos cards de contagem (ex: ícone de gráfico)

## Critérios de aceite

- CPU e memória corretos para o namespace selecionado
- "N/A" exibido quando metrics-server indisponível
- Valores formatados corretamente (cores vs millicores, GiB vs MiB)
