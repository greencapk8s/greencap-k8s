# 02 — WorkloadService: rolloutUndoDeployment

## Status
closed

## Descrição
Adicionar método `rolloutUndoDeployment(Cluster, String namespace, String name)` ao
`WorkloadService`, usando Fabric8 `rolling().undo()`. Segue o mesmo padrão de
`restartDeployment`.

## Critérios de aceite
- [ ] Método `rolloutUndoDeployment` adicionado ao `WorkloadService`
- [ ] Usa `try-with-resources` com `KubernetesClient`
- [ ] Lança `KubernetesOperationException` em falha
- [ ] Log `info` em sucesso, `error` em falha
