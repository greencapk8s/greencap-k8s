# 01 — EventInfo DTO e ObservabilityService

Status: done

## O que fazer

- Criar `EventInfo` record em `kubernetes/dto/`
- Criar `ObservabilityService` em `kubernetes/` com método `listEvents(Cluster, namespace)`
- Listar via Fabric8 `client.v1().events()`, ordenar por `lastTimestamp` decrescente
- Campos: type, reason, involvedObject (Kind/Name), message, count, age

## Critério de aceite

- Compilação sem erros
- `ObservabilityService` segue o padrão try-with-resources + KubernetesOperationException
