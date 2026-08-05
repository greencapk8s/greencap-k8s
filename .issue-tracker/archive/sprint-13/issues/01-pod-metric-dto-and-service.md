# 01 — PodMetricInfo DTO e listPodMetrics no ObservabilityService

Status: done

## O que fazer

- Criar `PodMetricInfo` record em `kubernetes/dto/` com campos: name, namespace, cpuMillicores, memoryMiB
- Adicionar `listPodMetrics(Cluster, namespace)` em `ObservabilityService`
- Usar `client.top().pods().metrics(namespace)` — API metrics.k8s.io/v1beta1 via Fabric8
- Agregar CPU e memória de todos os containers do pod (total por pod)
- Ordenar por CPU decrescente
- Lançar KubernetesOperationException em caso de falha (inclui metrics-server ausente)

## Critério de aceite

- Compilação sem erros
- Segue padrão try-with-resources do projeto
