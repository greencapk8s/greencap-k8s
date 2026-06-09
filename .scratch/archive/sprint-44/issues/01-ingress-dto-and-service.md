---
title: "IngressInfo DTO + NetworkingService.listIngresses()"
status: done
sprint: 44
---

## O que
- `IngressInfo` record em `kubernetes/dto/`
- `NetworkingService.listIngresses()` via Fabric8

## Campos do DTO
- `name`, `namespace`, `ingressClass` ("—" se ausente), `hosts` (colapsado), `tls` (boolean), `age`

## Critérios
- `try-with-resources` para o KubernetesClient
- `KubernetesOperationException` em falha
- Suporte a all-namespaces (mesmo padrão de `listServices`)
