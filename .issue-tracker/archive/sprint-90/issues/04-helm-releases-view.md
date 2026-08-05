---
id: "90-04"
title: "HelmReleasesView — grid, drawer de detalhes e uninstall"
status: done
priority: high
sprint: 90
---

`HelmReleasesView` na rota `helm/releases`, protegida por `PROJECT_HELM_VIEW`. Grid com colunas Name, Chart, App Version, Revision, Status (badge), Updated. Filtro de nome no header da coluna Name. Carregamento assíncrono com `CompletableFuture` + banner de cluster inacessível.

Selecionar uma release abre um drawer (Dialog lateral) com três abas — Notes, Values, Manifest — carregadas via `HelmService.getReleaseDetails()`. Conteúdo de cada aba exibido em um componente `Pre` com fonte monoespaçada e scroll.

Botão "Uninstall" como `SelectionAction.destructive` no section header. Dialog type-to-confirm: usuário digita o nome da release; após confirmação chama `HelmService.uninstall()` e recarrega o grid.

`MainLayout`: nova seção Helm dentro de Project com item "Releases" (ícone `VaadinIcon.PACKAGE`), posicionado abaixo de Storage. Grupo "Helm" adicionado à treeview de permissões da `UserManagementView`.

Badge de status: `deployed` → success, `failed` → error, `pending-*` → contrast, demais → contrast.
