# 01 — Namespace selector na navbar

Status: done

## Contexto

O namespace é um sub-contexto do cluster ativo — faz sentido ele estar na navbar global e não dentro de uma view específica. Hoje o `namespaceCombo` vive em `WorkloadsView` e só aparece naquela tela.

## O que fazer

- Injetar `NamespaceService` no `MainLayout`
- Adicionar `ComboBox<String> namespaceCombo` ao `MainLayout`
- Exibir o combobox na navbar apenas quando há cluster ativo (oculto caso contrário)
- Carregar os namespaces somente quando o cluster muda (comparar com cluster anterior armazenado em campo local)
- Ao detectar mudança de cluster: zerar namespace, recarregar lista, selecionar `default` (ou primeiro da lista se `default` não existir)
- Ao selecionar namespace: chamar `clusterContext.setNamespace(value)`
- Remover o `namespaceCombo` e a toolbar de namespace de `WorkloadsView`

## Critério de aceite

- Navbar exibe o combobox de namespace somente quando há cluster ativo
- Trocar o cluster na tela de Clusters faz o combobox recarregar a lista e selecionar `default`
- Selecionar um namespace no combobox atualiza o `ClusterContext` imediatamente
- O combobox permanece visível ao navegar entre telas (Dashboard, Workloads, Clusters)
