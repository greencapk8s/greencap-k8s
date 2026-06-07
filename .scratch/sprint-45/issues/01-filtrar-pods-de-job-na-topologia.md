---
title: "Topologia: não renderizar pods de Jobs/CronJobs"
status: done
sprint: 45
---

## O que
- `TopologyService.buildGraph()`: filtrar a lista de Pods logo após o fetch (linha ~43), excluindo pods cujo `ownerReferences` contenha `kind == "Job"` — antes de qualquer agrupamento por ReplicaSet
- Novo método privado `isOwnedByJob(Pod pod)` ao lado de `ownerReplicaSetName()`

## Por quê
- Hoje esses pods caem no balde de "orphan pods" (sem owner ReplicaSet) e poluem a topologia com execuções efêmeras de tarefas finitas
- Pods de CronJob são cobertos pelo mesmo filtro — a cadeia de ownership é `CronJob → Job → Pod`, então o Pod sempre referencia o Job diretamente, nunca o CronJob

## Critérios
- Pods owned por Job (manual ou disparado por CronJob) não aparecem como nós na Topologia
- Pods owned por ReplicaSet/Deployment continuam aparecendo normalmente (sem regressão no agrupamento)
- `CONTEXT.md` já atualizado com a nota explicando a exclusão deliberada (feito durante o `/grill-with-docs`)
