# Restart Deployment

Status: done

## Descrição

Adicionar ação de Restart na `DeploymentsView`. Um ícone de ação no grid abre um dialog de confirmação com o nome do Deployment. Ao confirmar, executa rolling restart via patch na annotation `kubectl.kubernetes.io/restartedAt`. Em caso de sucesso, exibe notificação `BOTTOM_END` e recarrega o grid.

## Critérios de aceite

- [ ] Ícone de Restart visível na coluna de ações de cada linha do grid de Deployments
- [ ] Dialog de confirmação exibe o nome do Deployment e botões "Restart" e "Cancel"
- [ ] Ao confirmar: chama `WorkloadService.restartDeployment(cluster, namespace, name)`
- [ ] Sucesso: notificação `BOTTOM_END` + refresh do grid
- [ ] Falha: notificação de erro `BOTTOM_END`

## Implementação sugerida

- `WorkloadService.restartDeployment(Cluster, String namespace, String name)` via patch na annotation `kubectl.kubernetes.io/restartedAt` com timestamp atual
- Fabric8: `client.apps().deployments().inNamespace(ns).withName(name).rolling().restart()`
- Dialog de confirmação inline na `DeploymentsView`, sem nova rota
- Ícone: `VaadinIcon.REFRESH` ou similar
