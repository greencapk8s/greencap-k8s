# 04 — Remoção do sistema de permissões interno

Status: done

Todo o código relacionado ao sistema de permissões interno do GreenCap deve ser removido. Isso inclui o enum `Permission`, a classe `SecurityUtils`, o `PermissionTreePanel` da `UserManagementView`, os métodos `updatePermissions` e `findPermissions` do `UserService`, e a lógica de carregamento de permissões como `GrantedAuthority` no `loadUserByUsername`.

O `MainLayout` deve ter todas as chamadas a `SecurityUtils.hasPermission()` removidas — todos os itens de menu passam a ser exibidos para qualquer usuário autenticado, sem filtragem. Os `BeforeEnterObserver` nas views que verificam permissão antes de entrar devem ser removidos ou simplificados (o da `UserManagementView` é substituído pela verificação de admin descrita na issue 03).

O resultado esperado é um código de acesso a recursos Kubernetes sem nenhuma camada de autorização interna — o Kubernetes RBAC é o único guardião.
