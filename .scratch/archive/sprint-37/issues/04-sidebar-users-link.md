# 04 — Sidebar: item Users visível só para ADMIN

**Status:** done

## Descrição
Substituir o `disabledNavItem("Users")` no `MainLayout` por um link real para `UserManagementView` visível apenas para ADMIN. Invisível para OPERATOR e VIEWER.

## Critérios de aceite
- Item "Users" no sidebar navega para `/users` quando clicado por ADMIN
- Item "Users" não aparece no sidebar para OPERATOR e VIEWER
