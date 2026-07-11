---
id: "88-02"
title: "Permissões Developer Experience e migration Flyway"
status: done
priority: high
sprint: 88
---

A seção Developer Experience introduz três novas permissões que seguem o padrão de nomenclatura existente: `DEVELOPER_EXPERIENCE_OPERATORS_VIEW` (acessa a view com as abas Installed e Catalog), `DEVELOPER_EXPERIENCE_OPERATORS_INSTALL` (instala operators via Subscription) e `DEVELOPER_EXPERIENCE_OPERATORS_UNINSTALL` (desinstala operators).

A migration Flyway adiciona as três permissões ao enum `permission_type` e as concede aos usuários existentes seguindo o mesmo critério das permissões análogas: `DEVELOPER_EXPERIENCE_OPERATORS_VIEW` é concedida a todos os usuários com `GLOBAL_CLUSTERS_VIEW`; `DEVELOPER_EXPERIENCE_OPERATORS_INSTALL` e `DEVELOPER_EXPERIENCE_OPERATORS_UNINSTALL` são concedidas apenas a usuários com `GLOBAL_CLUSTERS_WRITE`.
