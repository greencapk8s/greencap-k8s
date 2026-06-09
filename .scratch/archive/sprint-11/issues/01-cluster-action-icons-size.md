# 01 — Aumentar ícones de ação na listagem de clusters

Status: done

## Contexto

Os botões de "testar conexão" e "remover cluster" na `ClustersView` usam `ButtonVariant.LUMO_SMALL`, que reduz o tamanho do botão e do ícone. O ícone fica pequeno demais para uma ação destrutiva (remover) e uma ação frequente (testar conexão).

## O que fazer

- Remover `ButtonVariant.LUMO_SMALL` de `testBtn` e `deleteBtn` em `ClustersView.buildActions()`
- Definir o tamanho do ícone SVG para `22px` em ambos os botões via `icon.setSize("22px")`

## Critério de aceite

- Ícones visivelmente maiores na coluna Actions da grid de clusters
- Layout da coluna não quebra (largura 140px comporta dois botões de tamanho padrão)
- Compilação sem erros
