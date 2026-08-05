# 01 — SecurityUtils helper

**Status:** done

## Descrição
Criar `SecurityUtils` com métodos estáticos `isViewer()` e `isAdmin()` lendo das Spring Security authorities — sem query ao banco.

## Critérios de aceite
- `SecurityUtils.isViewer()` retorna `true` quando authority contém `ROLE_VIEWER`
- `SecurityUtils.isAdmin()` retorna `true` quando authority contém `ROLE_ADMIN`
- Nenhuma dependência de repositório ou service
