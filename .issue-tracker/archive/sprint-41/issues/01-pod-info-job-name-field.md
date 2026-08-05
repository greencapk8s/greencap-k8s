---
title: PodInfo — campo jobName extraído do label job-name
status: done
---

## O que

Adicionar campo `jobName` ao record `PodInfo`, populado de `metadata.labels["job-name"]`.
Padrão Kubernetes: o Job controller adiciona automaticamente esse label em todos os Pods que cria.

## Critérios de aceite

- `PodInfo` tem campo `String jobName` (vazio `""` para pods sem esse label)
- `WorkloadService.listPods()` popula `jobName` de `pod.getMetadata().getLabels().getOrDefault("job-name", "")`
