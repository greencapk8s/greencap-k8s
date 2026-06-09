---
id: "35-01"
title: "Persistência de PlatformSettings no banco por usuário"
status: done
labels: [feat, backend]
sprint: 35
---

## O que

Adicionar coluna `refresh_interval_seconds` na tabela `users` e expor get/set no `UserService`.

## Critérios de aceite

- Migration `V8__add_refresh_interval_to_users.sql` aplicada sem erros
- Campo `refreshIntervalSeconds` (Integer, nullable) na entidade `User`
- `UserService.findRefreshInterval(username)` retorna `Optional<Integer>`
- `UserService.updateRefreshInterval(username, seconds)` persiste o valor
