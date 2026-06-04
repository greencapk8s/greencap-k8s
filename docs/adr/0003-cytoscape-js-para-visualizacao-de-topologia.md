# Cytoscape.js para visualização de Topologia

A view Topologia usa Cytoscape.js como motor de renderização de grafos, integrado via um Web Component LitElement anotado com `@NpmPackage`. O Vaadin Gradle plugin gerencia o Node.js/npm embutido — nenhuma instalação manual de Node é necessária.

## Por quê

O Vaadin Flow não tem componente nativo de grafo. As alternativas consideradas foram:

- **D3.js** — poderoso, mas exige código imperativo extenso para grafos com layout automático.
- **dagre + cytoscape-dagre** — layout mais elegante, porém adiciona 2 pacotes npm extras sem benefício suficiente para o caso de uso atual.
- **Add-on Vaadin pago** — inexistente para grafos no Vaadin Directory.

O Cytoscape.js oferece layout `breadthfirst` nativo (zero dependências extras), API declarativa de nós/arestas, e pan/zoom embutido — exatamente o que a Topologia precisa.

## Trade-offs

**Introduz JS/TypeScript no projeto**: até aqui o frontend era gerado inteiramente pelo Vaadin. O Web Component LitElement em `src/main/frontend/` é o primeiro arquivo TypeScript escrito manualmente. O build permanece transparente (`./gradlew bootRun`), mas futuros mantenedores precisam entender que esse arquivo existe.

**Acoplamento ao Cytoscape.js**: trocar a biblioteca exige reescrever o Web Component inteiro. Mitigado pelo fato de que a interface Java (`TopologyGraph` DTO) permanece estável — só o rendering muda.
