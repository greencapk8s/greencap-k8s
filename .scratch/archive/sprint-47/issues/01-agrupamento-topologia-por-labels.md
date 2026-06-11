---
title: "Topologia: agrupamento de nós por app.kubernetes.io/part-of e component"
status: done
sprint: 47
---

## O que
- `TopologyService`: derivar, para cada `TopologyNode`, os valores de `app.kubernetes.io/part-of` e `app.kubernetes.io/component` (já existe `Map<String, String> labels` no DTO; propagar labels também para `PodGroup` — a partir do primeiro Pod do grupo — e para `PersistentVolumeClaim` — a partir do próprio metadata)
- `TopologyGraph`/`TopologyNode`: expor a informação de grupo (ex.: `partOfGroup` e `componentGroup`) necessária para o frontend montar os compound nodes
- `topology-graph.ts`: renderizar `TopologyGroup`s como compound nodes do Cytoscape, aninhados — caixa externa por `part-of`, caixa interna por `component`; nó com `component` mas sem `part-of` vira caixa de nível externo; nó sem nenhuma das labels fica solto
- Rótulo das caixas no formato `chave: valor` (ex.: `part-of: payments`, `component: api`)
- `TopologiaView`: adicionar um controle (toggle) para ligar/desligar o agrupamento, ligado por padrão

## Por quê
- Em namespaces com muitos recursos, o grafo plano fica difícil de interpretar; agrupar visualmente por `part-of`/`component` (convenção padrão de labels recomendadas do Kubernetes) ajuda o usuário iniciante a identificar quais recursos pertencem à mesma aplicação/componente

## Critérios
- Nós que compartilham o mesmo valor de `app.kubernetes.io/part-of` aparecem contidos numa mesma caixa rotulada `part-of: <valor>`; dentro dela, os que também compartilham `app.kubernetes.io/component` ficam em uma sub-caixa `component: <valor>`
- Nó com `component` mas sem `part-of` forma sua própria caixa de nível externo `component: <valor>`
- Nó sem nenhuma das duas labels permanece fora de qualquer caixa, como hoje
- `PodGroup` e `PersistentVolumeClaim` participam do agrupamento (labels derivadas do primeiro Pod do grupo e do próprio metadata, respectivamente)
- Toggle no header da `TopologiaView`, ligado por padrão; ao desligar, o grafo volta ao layout plano atual
- Caixas são puramente visuais — sem colapsar/expandir
- Compila (`./gradlew compileJava`) e os testes existentes de `TopologyService` continuam passando
