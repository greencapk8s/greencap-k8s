---
id: "88-04"
title: "Developer Experience — seção no sidebar e item Operators"
status: done
priority: high
sprint: 88
---

O `MainLayout` recebe uma nova seção **Developer Experience** no sidebar, posicionada entre Settings e Global. A seção contém inicialmente um único item: **Kubernetes Operators** (ícone `VaadinIcon.GRID_BIG_O` ou similar), que navega para `/developer-experience/operators` e é visível apenas quando um Cluster ativo está selecionado — segue o padrão dos demais itens dependentes de cluster (`clusterDependentNavItems`).

A proteção de acesso ao item segue o padrão existente: o item aparece apenas para usuários com `DEVELOPER_EXPERIENCE_OPERATORS_VIEW`.

A `UserManagementView` recebe um novo grupo **"Developer Experience"** na treeview de permissões do editor de permissões de usuário, listando as três novas permissões: `DEVELOPER_EXPERIENCE_OPERATORS_VIEW`, `DEVELOPER_EXPERIENCE_OPERATORS_INSTALL` e `DEVELOPER_EXPERIENCE_OPERATORS_UNINSTALL`.
