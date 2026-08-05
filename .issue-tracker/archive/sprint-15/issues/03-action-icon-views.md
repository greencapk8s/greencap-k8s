# 03 — Ícone de ação nas listagens

Status: done

## O que fazer

- Adicionar coluna "Actions" com ícone `VaadinIcon.CODE` em:
  - `PodsView`, `DeploymentsView`, `ServicesView`, `ConfigMapsView`, `SecretsView`
- Ao clicar, navegar para `/yaml/{resourceType}/{namespace}/{name}`

## Critério de aceite

- Ícone visível em todas as 5 views
- Navegação para ManifestView correta
- Compilação sem erros
