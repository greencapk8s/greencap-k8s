# 01 — Drawer width: localStorage → banco

**Status:** done

## Contexto
A largura do drawer é redimensionável por drag e hoje persiste apenas em `localStorage` (chave `greencap-drawer-width`). O objetivo é migrar para o banco de dados para que a preferência siga o usuário entre dispositivos e sessões, consistente com `refresh_interval_seconds`.

## Entrega
- Migration `V9__add_drawer_width_to_users.sql`: coluna `drawer_width_px INTEGER` na tabela `users`
- Campo `drawerWidthPx` em `User.java`
- Métodos `findDrawerWidth(username)` e `updateDrawerWidth(username, width)` em `UserService`
- `MainLayout`: lê a largura do banco em `onAttach`, passa como parâmetro inicial ao JS
- JS: ao soltar o mouse (mouseup), chama `@ClientCallable saveDrawerWidth(int)` em vez de `localStorage.setItem`
- `localStorage` como fallback removido; localStorage existente ignorado

## Restrições
- Largura mínima: 180px, máxima: 400px, padrão: 240px (sem valor no banco)
