# 03 — Remover combobox de cluster do WorkloadsView

Status: done

## Contexto

Com o cluster atual gerenciado globalmente pelo `ClusterContext`, o combobox de seleção de cluster em `WorkloadsView` é redundante e duplica responsabilidade. `WorkloadsView` deve usar o `ClusterContext` diretamente, exibindo apenas o seletor de namespace.

## O que fazer

- Remover `clusterCombo` e toda lógica associada de `WorkloadsView`
- `beforeEnter`: se `clusterContext.getCluster() == null`, exibir aviso inline ("Nenhum cluster ativo. Vá em Configuração → Clusters e selecione um cluster.")
- Se cluster ativo existe: carregar namespaces e workloads diretamente
- Manter `namespaceCombo` para seleção de namespace

## Critério de aceite

- `WorkloadsView` não exibe combobox de cluster
- Com cluster ativo: carrega namespaces e workloads automaticamente ao entrar na view
- Sem cluster ativo: exibe aviso claro com orientação para o usuário
