# 03 — Renderização e evidência de ServiceDependency (frontend)

Status: done

Depende da entrega 02 (a inferência precisa existir no backend antes de ter algo para desenhar). Esta entrega cobre a parte visível ao usuário: diferenciar uma aresta `SERVICE_DEPENDENCY` das arestas estruturais existentes, e mostrar a evidência que justificou a inferência.

No `topology-graph.ts` (Cytoscape), arestas com `type: SERVICE_DEPENDENCY` são renderizadas com estilo distinto das estruturais — linha tracejada e cor diferente (ex: âmbar, em vez do cinza usado hoje), mantendo a seta direcional (`target-arrow-shape: triangle`) já existente. As arestas estruturais mantêm exatamente o estilo atual, sem regressão visual.

Como a Topologia hoje só tem interação de clique em nó (`TopologyNodeDrawer`), sem clique em aresta, a evidência da inferência (qual env var e qual valor geraram o match) aparece dentro do drawer do nó de origem — não como uma interação nova de clique-em-aresta. Ao abrir o drawer de um Workload que tem uma ou mais arestas `SERVICE_DEPENDENCY` saindo dele, uma seção nova lista cada dependência inferida (ex: "Depende de: `database-service` (via `DB_HOST=postgres-service:5432` no container `backend`)"), reaproveitando o mesmo botão "Go to resource" já usado para outros tipos de nó — apontando para o Service alvo.

Workloads sem nenhuma `ServiceDependency` não mostram essa seção no drawer — sem alteração visual para o caso comum de hoje.

Fora de escopo: qualquer interação de clique diretamente na aresta (tooltip ao passar o mouse, clique para abrir detalhe da aresta) — decisão já tomada na sessão de `/grill-with-docs`, para não introduzir um novo tipo de interação no Cytoscape só para esse propósito.

Cobertura de teste (Karibu, estendendo o teste existente da Topologia se houver): abrir o drawer de um nó com `ServiceDependency` e verificar que a seção de dependências aparece com a env var/valor esperados; verificar que o botão de navegação leva ao Service correto; verificar que um nó sem `ServiceDependency` não renderiza a seção.
