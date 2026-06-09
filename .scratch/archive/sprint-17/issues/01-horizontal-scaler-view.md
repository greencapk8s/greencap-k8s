---
id: 01
title: Listagem de HorizontalScaler (HPA)
status: done
sprint: 17
---

## Objetivo

Exibir os `HorizontalPodAutoscaler` do namespace ativo em uma nova view read-only, acessível via Auto Scaling > Horizontal Scaler no menu lateral.

## Decisões de design

- Termo canônico: `HorizontalScaler` (ver CONTEXT.md)
- API Fabric8: `client.autoscaling().v2().horizontalPodAutoscalers()`
- Read-only nesta sprint (edição de min/max para sprint futura)
- Ícone Manifest incluso (padrão das demais views)

## Arquivos a criar/modificar

- `kubernetes/dto/HorizontalScalerInfo.java` — record DTO
- `kubernetes/AutoScalingService.java` — listagem via Fabric8
- `ui/HorizontalScalerView.java` — grid em `/autoscaling/horizontalscalers`
- `ui/MainLayout.java` — item Auto Scaling > Horizontal Scaler em PROJECT
- `kubernetes/ManifestService.java` — case `"horizontalscaler"`

## Campos da listagem

| Coluna | Fonte |
|--------|-------|
| Name | `metadata.name` |
| Target | `spec.scaleTargetRef.name` |
| Min | `spec.minReplicas` |
| Current / Max | `status.currentReplicas` / `spec.maxReplicas` |
| Metrics | resumo do primeiro metric (ex: `cpu: 45%/80%`) |
| Age | `metadata.creationTimestamp` |
| Manifest | ícone CODE → `/yaml/horizontalscaler/{namespace}/{name}` |

## Critérios de aceite

- [x] Menu Auto Scaling > Horizontal Scaler visível em PROJECT (abaixo de Workloads)
- [x] Grid lista HPAs do namespace ativo com as colunas definidas
- [x] Ícone Manifest abre o YAML completo do HPA
- [x] Sem regressão nas demais views
