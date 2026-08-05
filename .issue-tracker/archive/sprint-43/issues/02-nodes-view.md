---
title: NodesView — listagem de Nodes do cluster
status: open
---

## O que

Criar a `NodesView` seguindo o padrão das demais listing views de Infrastructure (PersistentVolumesView, StorageClassesView).

## Critérios de aceite

- Rota: `@Route(value = "infrastructure/nodes", layout = MainLayout.class)`
- Protegida por `SecurityUtils.hasPermission(Permission.SETTINGS_INFRASTRUCTURE_VIEW)` no `beforeEnter` — redireciona para `""` se não autorizado
- Implementa `Refreshable`

### Grid — colunas

| Coluna | Largura | Sortable |
|--------|---------|----------|
| Name | flexGrow(2) | sim |
| Status | 110px | sim |
| Role | 130px | sim |
| Version | 130px | não |
| OS | flexGrow(2) | não |
| CPU | 80px | não |
| Memory | 100px | não |
| Age | 80px | não |
| Manifest (ícone CODE) | 60px | não |

- Status: badge `success` para `Ready`, `error` para `NotReady`, `contrast` para `Unknown`
- Manifest: navega para `yaml/node/-/<name>` (nodes são cluster-scoped, sem namespace)

### Filtros

- Filtro por texto no header row em **Name** e **Status**
- `ListDataProvider` com `setFilter` combinando os dois campos

### Comportamento

- Exibe `noClusterMessage` quando nenhum cluster está selecionado
- Sucesso: popula o grid
- Falha (`KubernetesOperationException`): notificação de erro em `BOTTOM_END`
- `refresh()`: silencioso — sem notificação de erro, grid mantém dados anteriores em caso de falha
