# 01 — StatefulSet como nó na Topologia

Status: in-progress

Hoje `TopologyService.buildGraph` modela Deployment→ReplicaSet→Pod, mas não busca nem representa StatefulSet — apesar de `WorkloadService` já listar StatefulSets para as demais telas do Project. Isso é um gap real: bancos de dados normalmente rodam como StatefulSet (padrão já documentado na entrada `StatefulSet` do `CONTEXT.md`), então hoje esses objetos simplesmente não aparecem na Topologia. Esta entrega é pré-requisito da Sprint — sem ela, um `ServiceDependency` (entregas seguintes) apontaria para um Service sem nenhum Workload visível atrás dele.

Diferente de Deployment, StatefulSet gerencia seus Pods diretamente, sem ReplicaSet intermediário — os `ownerReferences` dos Pods apontam direto para o StatefulSet, com nomeação ordinal (`<name>-0`, `<name>-1`, ...) em vez do sufixo de hash usado por ReplicaSet. O agrupamento de Pods por dono (hoje `podsByOwnerRs`, chaveado só por ReplicaSet) precisa reconhecer também donos do tipo StatefulSet, criando um `TopologyNode` do tipo StatefulSet e uma aresta estrutural StatefulSet→PodGroup (mesmo padrão visual de ReplicaSet→PodGroup, sem o nó intermediário). O nome-base do grupo, para StatefulSet, já é o nome do próprio recurso — sem precisar do `stripLastSegment` usado para remover o hash de ReplicaSet.

Não é necessário nenhum tratamento especial para o Service headless que acompanha um StatefulSet (`spec.serviceName`): a aresta Service→Pod já é resolvida hoje por matching de `selector` contra labels do Pod, independente do Service ser headless ou não — o mesmo caminho de código já cobre esse caso sem alteração.

Fora de escopo: DaemonSet (decisão registrada na sessão de `/grill-with-docs` — não é um alvo comum de dependência entre serviços, motivo da sprint). Também fora de escopo qualquer write operation nova para StatefulSet na Topologia — a view é read-only, mesmo padrão hoje aplicado aos demais tipos de nó.

Cobertura de teste: nenhum teste automatizado de `TopologyService` existe hoje — avaliar, no momento da cobertura de testes da sprint, se cabe um teste de integração (`PostgresIntegrationTest` ou equivalente) validando que um StatefulSet com Pods aparece como nó, com aresta StatefulSet→PodGroup e, quando há um Service headless com selector compatível, a aresta Service→PodGroup também presente.
