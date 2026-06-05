# Scale Deployment

Status: done

## Descrição

Adicionar ação de Scale na `DeploymentsView`. Um ícone de ação na coluna de ações do grid abre um dialog pré-populado com o valor atual de réplicas (`desired`). O usuário altera o valor e confirma. A operação chama a API Kubernetes e, em caso de sucesso, exibe notificação `BOTTOM_END` e recarrega o grid.

## Critérios de aceite

- [ ] Ícone de Scale visível na coluna de ações de cada linha do grid de Deployments
- [ ] Dialog abre com `NumberField` pré-populado com o `desired` atual, mínimo 0, máximo 50
- [ ] Botão "Scale" só habilitado se o valor foi alterado
- [ ] Ao confirmar: chama `WorkloadService.scaleDeployment(cluster, namespace, name, replicas)`
- [ ] Sucesso: notificação `BOTTOM_END` + refresh do grid
- [ ] Falha: notificação de erro `BOTTOM_END`

## Implementação sugerida

- `WorkloadService.scaleDeployment(Cluster, String namespace, String name, int replicas)` via Fabric8 `client.apps().deployments().inNamespace(ns).withName(name).scale(replicas)`
- Dialog inline na `DeploymentsView`, sem nova rota
- Ícone: `VaadinIcon.RESIZE_H` ou similar
