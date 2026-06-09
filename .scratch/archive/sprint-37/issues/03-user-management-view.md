# 03 — UserManagementView

**Status:** done

## Descrição
Criar `UserManagementView` em `/users` restrita a ADMIN. Permite criar e desativar usuários.

## Critérios de aceite
- Rota `/users` anotada com `@RolesAllowed("ADMIN")`
- Grid com colunas: username, email, role, ativo, criado em
- Botão "Add User" abre dialog com campos: username, email, senha, role (select ADMIN/OPERATOR/VIEWER)
- Botão "Deactivate" por linha — chama `UserService.deactivateUser()`
- ADMIN não pode se auto-desativar (botão desabilitado na própria linha)
- `UserService.deactivateUser()` adicionado (seta `active = false`)
