# 01 — Persistir último Namespace ativo por usuário

Status: done

## O que fazer

- Adicionar campo `activeNamespace` (String) na entidade `User`
- Criar migration `V7__add_active_namespace_to_users.sql`
- Adicionar `updateActiveNamespace(username, namespace)` e `findActiveNamespace(username)` no `UserService`
- Em `MainLayout`: ao trocar namespace no ComboBox, persistir via `updateActiveNamespace`
- Em `MainLayout`: ao restaurar o cluster no login, carregar `activeNamespace` do banco e usá-lo como `preferred` em `loadNamespacesForCluster`
- Fallback: namespace salvo → "default" → primeiro da lista (lógica já existente, apenas alimentar com valor persistido)

## Critério de aceite

- Selecionar namespace X, fazer logout e login: namespace X restaurado automaticamente
- Se namespace salvo não existe mais no cluster: fallback silencioso para "default"
- Compilação sem erros
