# 01 — `type` do `TopologyNode` vira o kind do recurso e a contagem migra para `subtitle`

Status: done

Abre a sprint. É pré-requisito das issues 03 e 04, e sozinha não muda nada na tela.

O campo `type` do `TopologyNode` hoje acumula dois papéis. Para a maioria dos nós ele carrega o kind do recurso — `Deployment`, `Service`, `Ingress` — e é isso que a documentação afirma que ele é. Mas o nó de Pod carrega `1 Pod` e o nó sintético de PodGroup carrega a contagem de réplicas, `3 Pods`. O campo virou, para esses dois casos, um subtítulo de exibição disfarçado de tipo.

A consequência já está no código, silenciosa: a tabela de cores por tipo do grafo é indexada por esse campo e tem uma entrada para `Pod` que nunca casa com nada. Todo nó de Pod cai no cinza de fallback desde sempre, e a entrada verde que existe para ele é código morto. O defeito é invisível porque falha para um valor plausível em vez de quebrar.

Isso importa agora porque o mapa de ícones da issue 03 seria indexado exatamente pelo mesmo campo e cairia exatamente na mesma armadilha — com o agravante de que um ícone ausente é bem mais notável do que uma cor de fallback.

O campo `type` passa a carregar sempre o kind, incluindo `Pod` e o kind sintético `PodGroup`, que não existe na taxonomia do Kubernetes mas existe no vocabulário desta view. Um campo novo carrega o subtítulo — a palavra desenhada sob o nome do recurso — que para a maioria dos nós é o próprio kind e para nós de Pod e PodGroup é a contagem de réplicas que eles já mostravam.

Na tela nada muda: a segunda linha do nó continua exibindo `Deployment` num Deployment e `3 Pods` num grupo de três réplicas. O que muda é que o tipo passa a ser consultável de forma confiável, e a entrada morta na tabela de cores volta a fazer sentido — ou é removida junto com a tabela, na issue 03.

Cobertura de teste: extensão dos testes de Topologia existentes, verificando que um nó de Pod passa a expor `Pod` como tipo e a contagem como subtítulo, que um nó de PodGroup expõe `PodGroup` e a contagem de réplicas do grupo, e que os demais tipos de nó continuam expondo o mesmo valor nos dois campos. O caso do grupo com uma única réplica merece asserção própria, porque é onde o subtítulo muda de plural para singular.

Fora de escopo: qualquer mudança visual, o mapa de ícones e a tabela de cores do frontend.
