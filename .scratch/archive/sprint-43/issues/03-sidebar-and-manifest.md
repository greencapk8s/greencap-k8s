---
title: Sidebar + ManifestService — integração da NodesView
status: open
---

## O que

Registrar o sub-item Nodes no sidebar de Infrastructure e adicionar suporte ao tipo `"node"` no `ManifestService`.

## Critérios de aceite

### Sidebar (MainLayout)

- Sub-item `"Nodes"` adicionado ao grupo Infrastructure, abaixo de `"StorageClasses"`
- Protegido por `SETTINGS_INFRASTRUCTURE_VIEW` — mesmo guard dos demais itens do grupo
- Ícone: `SERVER` (consistente com o conceito de máquina)

### ManifestService

- Tipo `"node"` adicionado ao switch de `fetchManifest()`:
  - `client.nodes().withName(name).get()` (nodes são cluster-scoped — ignorar o parâmetro namespace)
- Sem nova rota na `ManifestView` — a rota `yaml/{type}/{namespace}/{name}` já funciona; passar `"-"` como namespace (padrão dos recursos cluster-scoped)
