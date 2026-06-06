# 02 — Ocultar botões de escrita para VIEWER

**Status:** done

## Descrição
Usar `SecurityUtils.isViewer()` para ocultar botões de escrita em três views.

## Critérios de aceite
- `ClustersView`: "Add Cluster" e "Remove" invisíveis para VIEWER
- `DeploymentsView`: Scale e Restart invisíveis para VIEWER
- `HorizontalScalerView`: Edit invisível para VIEWER
- OPERATOR e ADMIN continuam vendo todos os botões
