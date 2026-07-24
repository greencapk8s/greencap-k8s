# 02 — Dividir WorkloadsView em PodsView e DeploymentsView

Status: done

## Contexto

`WorkloadsView` hoje usa um `TabSheet` para alternar entre Pods e Deployments. Com a navegação sendo promovida para o menu lateral como sub-itens, cada tipo de Workload deve ter sua própria view e rota independente.

## O que fazer

- Criar `PodsView` com rota `/workloads/pods` contendo o grid de Pods atual
- Criar `DeploymentsView` com rota `/workloads/deployments` contendo o grid de Deployments atual
- Em ambas as views: carregar workloads em `beforeEnter` usando o namespace do `ClusterContext`
- Em ambas as views: exibir mensagem de estado vazio quando não há cluster ativo (sem botão de refresh — reload ocorre ao navegar ou mudar namespace)
- Remover `WorkloadsView` após a criação das duas novas views
- Mover a lógica de badges (`phaseBadge`, `replicasBadge`) para cada view respectiva

## Critério de aceite

- `/workloads/pods` exibe o grid de Pods filtrado pelo namespace ativo
- `/workloads/deployments` exibe o grid de Deployments filtrado pelo namespace ativo
- Navegar para qualquer uma das views sem cluster ativo exibe mensagem orientando o usuário
- Trocar o namespace na navbar recarrega os dados da view ativa automaticamente
