---
id: "01"
title: "Namespaces View — Global section: listagem com contagens de recursos, Create e Delete"
status: done
labels: [feature]
sprint: 76
---

## Contexto

Sprint 76 introduz a `NamespacesView` na seção Global do menu, permitindo ao usuário visualizar todos os Namespaces do cluster ativo com contagens de recursos (Pods, Deployments, Services), criar novos namespaces e deletar com confirmação por digitação do nome.

Decisões de design (ver grill-with-docs):
- System namespaces (`kube-system`, `kube-public`, `kube-node-lease`, `default`) bloqueados para deleção na UI
- Create Namespace no escopo — dialog simples com validação DNS
- Colunas: Name, Status, Pods, Deployments, Services, Age
- Permissões separadas: `GLOBAL_NAMESPACES_VIEW`, `GLOBAL_NAMESPACES_WRITE`, `GLOBAL_NAMESPACES_DELETE`
- Delete exige digitar o nome do namespace para confirmar (dado o risco de destruição cascata)
- Posição no menu: após Clusters, antes de Infrastructure

## Entrega

- `NamespaceInfo`: +campos `podCount`, `deploymentCount`, `serviceCount`
- `NamespaceService`: `listNamespacesWithCounts()`, `createNamespace()`, `deleteNamespace()`
- `Permission`: 3 novas permissões + atualização de `operatorPermissions()`/`viewerPermissions()`
- `V26__add_namespace_permissions.sql`
- `NamespacesView` (nova)
- `MainLayout`: item Namespaces na seção Global
- `UserManagementView`: grupo Namespaces na treeview
