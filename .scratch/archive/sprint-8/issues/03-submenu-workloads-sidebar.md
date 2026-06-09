# 03 — Sub-menu de Workloads no menu lateral

Status: done

## Contexto

O item "Workloads" no menu lateral deve se tornar um pai colapsável com dois filhos: Pods e Deployments. Clicar em "Workloads" expande o sub-menu e navega para `/workloads/pods` por padrão. O item "Namespaces" desabilitado deve ser renomeado para "Topologia".

## O que fazer

- Substituir o `SideNavItem("Workloads", WorkloadsView.class, ...)` por um item pai com rota `PodsView.class`
- Adicionar filhos ao item pai: `SideNavItem("Pods", PodsView.class, ...)` e `SideNavItem("Deployments", DeploymentsView.class, ...)`
- Renomear o item desabilitado "Namespaces" para "Topologia" (mantém desabilitado)
- Remover o item desabilitado "Deploys" (substituído pelos sub-itens de Workloads)

## Critério de aceite

- Menu lateral exibe "Workloads" com seta de expansão e dois filhos: Pods e Deployments
- Clicar em "Workloads" expande o sub-menu e navega para `/workloads/pods`
- Clicar em "Pods" ou "Deployments" navega para a rota correta
- O item ativo no sub-menu fica destacado conforme a rota atual
- Item "Topologia" aparece desabilitado no lugar de "Namespaces"
