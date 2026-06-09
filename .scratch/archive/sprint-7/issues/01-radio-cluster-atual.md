# 01 — Radio button para selecionar cluster atual em ClustersView

Status: done

## Contexto

O usuário precisa definir qual cluster é o "atual" da sessão para que todas as telas assumam esse contexto automaticamente. A seleção é feita via radio button na primeira coluna do grid de clusters.

## O que fazer

- Injetar `ClusterContext` em `ClustersView`
- Adicionar coluna de radio button como primeira coluna do grid
- Ao clicar: `clusterContext.setCluster(cluster)` + `clusterContext.setNamespace("default")`
- Ao navegar para `ClustersView`, refletir o cluster ativo no radio (via `BeforeEnterObserver`)
- Ao remover um cluster que é o atual: limpar `ClusterContext` antes de deletar

## Critério de aceite

- Apenas um cluster pode estar ativo por vez
- Selecionar um cluster atualiza o `ClusterContext` imediatamente
- Ao voltar para `ClustersView`, o radio exibe o cluster ativo corretamente
- Remover o cluster ativo limpa o contexto (radio sem seleção)
