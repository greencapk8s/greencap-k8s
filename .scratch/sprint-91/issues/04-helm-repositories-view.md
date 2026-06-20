---
id: "91-04"
title: "HelmRepositoriesView — gestão de repos no sidebar"
status: done
priority: high
sprint: 91
---

`HelmRepositoriesView` na rota `helm/repositories`, protegida por `PROJECT_HELM_VIEW`. Grid com colunas Name e URL. Botão "Add Repository" como extra leading button no section header. Dialog com campos Name e URL — valida que Name não contém espaços (requisito do Helm). SelectionAction "Remove" destrutivo com `ConfirmDialog` simples.

`MainLayout`: sub-item "Repositories" (ícone `VaadinIcon.BOOK`) adicionado abaixo de "Releases" na seção Helm. `UserManagementView`: permissões `PROJECT_HELM_INSTALL` e `PROJECT_HELM_UPGRADE` adicionadas ao grupo Helm na treeview.
