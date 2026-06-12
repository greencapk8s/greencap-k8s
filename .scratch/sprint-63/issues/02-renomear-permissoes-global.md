---
id: "02"
title: "Renomear permissões SETTINGS_CLUSTERS_*/SETTINGS_INFRASTRUCTURE_* para GLOBAL_*"
status: done
labels: [refactor, backend]
sprint: 63
depends_on: ["01"]
---

## Contexto

Com `Clusters` e `Infrastructure` migrando para a nova seção **GLOBAL** (issue 01), os identificadores de permissão `SETTINGS_CLUSTERS_VIEW`, `SETTINGS_CLUSTERS_WRITE`, `SETTINGS_INFRASTRUCTURE_VIEW` e `SETTINGS_INFRASTRUCTURE_CORDON` ficam inconsistentes com a seção onde os itens agora vivem.

Decisão via `/grill-with-docs`: renomear para `GLOBAL_CLUSTERS_VIEW`, `GLOBAL_CLUSTERS_WRITE`, `GLOBAL_INFRASTRUCTURE_VIEW`, `GLOBAL_INFRASTRUCTURE_CORDON`, incluindo migration de dados — `Permission` é persistido como `@Enumerated(EnumType.STRING)` em `user_permissions.permissions` (`User.java:33-37`), então o valor renomeado precisa ser atualizado nas linhas existentes.

`CONTEXT.md` já foi atualizado nesta sessão de planejamento para referenciar `GLOBAL_INFRASTRUCTURE_VIEW`/`GLOBAL_INFRASTRUCTURE_CORDON` nos termos `Node` e `Cordon`.

## Entrega

### `Permission.java`

- Renomear os 4 enum constants:
  - `SETTINGS_CLUSTERS_VIEW` → `GLOBAL_CLUSTERS_VIEW`
  - `SETTINGS_CLUSTERS_WRITE` → `GLOBAL_CLUSTERS_WRITE`
  - `SETTINGS_INFRASTRUCTURE_VIEW` → `GLOBAL_INFRASTRUCTURE_VIEW`
  - `SETTINGS_INFRASTRUCTURE_CORDON` → `GLOBAL_INFRASTRUCTURE_CORDON`
- Mover esses 4 constants do grupo `// Settings` para um novo grupo `// Global` (mantendo a ordem relativa entre eles).
- Atualizar `operatorPermissions()` e `viewerPermissions()` para usar os novos nomes.

### Referências a atualizar

- `MainLayout.java`:
  - `buildGlobalNav()` (criado na issue 01) — checagem do item `Clusters` passa a usar `Permission.GLOBAL_CLUSTERS_VIEW`; `buildInfrastructureNavItem()` passa a usar `Permission.GLOBAL_INFRASTRUCTURE_VIEW`.
  - Texto do banner de cluster inacessível (`buildClusterWarningBanner`, ~linha 269): `"Cluster unreachable — check your connection settings in Settings › Clusters"` → `"Cluster unreachable — check your connection settings in Global › Clusters"`.
- `ClustersView.java`: `Permission.GLOBAL_CLUSTERS_VIEW` (linha 66), `Permission.GLOBAL_CLUSTERS_WRITE` (linhas 77 e 156).
- `NodesView.java`: `Permission.GLOBAL_INFRASTRUCTURE_VIEW` (linha 74), `Permission.GLOBAL_INFRASTRUCTURE_CORDON` (linha 87).
- `PersistentVolumesView.java`: `Permission.GLOBAL_INFRASTRUCTURE_VIEW` (linha 75).
- `StorageClassesView.java`: `Permission.GLOBAL_INFRASTRUCTURE_VIEW` (linha 68).
- `UserManagementView.java` (`buildGlobalGroups`, criado na issue 01): chaves do `Map`/`LinkedHashMap` passam a referenciar `Permission.GLOBAL_CLUSTERS_VIEW`, `Permission.GLOBAL_CLUSTERS_WRITE`, `Permission.GLOBAL_INFRASTRUCTURE_VIEW`.

### Migration

Nova migration `V21__rename_global_permissions.sql` (não alterar `V11`/`V18`/`V19` — já aplicadas):

```sql
UPDATE user_permissions SET permissions = 'GLOBAL_CLUSTERS_VIEW' WHERE permissions = 'SETTINGS_CLUSTERS_VIEW';
UPDATE user_permissions SET permissions = 'GLOBAL_CLUSTERS_WRITE' WHERE permissions = 'SETTINGS_CLUSTERS_WRITE';
UPDATE user_permissions SET permissions = 'GLOBAL_INFRASTRUCTURE_VIEW' WHERE permissions = 'SETTINGS_INFRASTRUCTURE_VIEW';
UPDATE user_permissions SET permissions = 'GLOBAL_INFRASTRUCTURE_CORDON' WHERE permissions = 'SETTINGS_INFRASTRUCTURE_CORDON';
```

## Critérios de aceite manual

- Aplicação sobe normalmente (Flyway aplica `V21` sem erro) sobre o banco existente com dados das sprints anteriores.
- Usuários existentes (Admin/Operator/Viewer) mantêm o mesmo acesso efetivo a `Clusters` e `Infrastructure` que tinham antes da renomeação.
- Em "User Management", os grupos "Clusters" e "Infrastructure" (agora dentro de GLOBAL) continuam refletindo corretamente o estado salvo de cada usuário (checkboxes marcados/desmarcados como antes).
- Com o cluster ativo desconectado, o banner mostra "...in Global › Clusters".
