---
id: "91-06"
title: "HelmReleasesView — SelectionAction Upgrade com dialog de values"
status: done
priority: high
sprint: 91
---

`HelmReleasesView` recebe novo `SelectionAction.of(VaadinIcon.UPLOAD, "Upgrade", canUpgrade, this::openUpgradeDialog)` no section header, entre "Details" e "Uninstall".

`openUpgradeDialog` carrega os values atuais via `HelmService.getReleaseDetails()` e exibe um Dialog com: TextArea pré-preenchida com os values atuais; TextField opcional de nova versão do chart. Botão "Upgrade" confirma; executa `HelmService.upgrade()` e recarrega o grid. Botão "Cancel" cancela. Erros exibidos via Notification.
