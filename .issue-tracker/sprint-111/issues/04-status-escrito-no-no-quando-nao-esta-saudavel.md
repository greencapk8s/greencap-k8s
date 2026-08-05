# 04 — O status passa a ser escrito no nó quando ele não está saudável

Status: done

Consome as issues 01 e 03. É a entrega que corrige o defeito mais fundo da sprint.

O rótulo do nó da Topologia é nome e tipo, e nada mais. O status — `Degraded`, `CrashLoopBackOff`, `ImagePullBackOff` — viaja no nó e só aparece depois que o usuário clica nele. Na prática, toda a precisão construída pela Sprint 108, que fez o `PodState` correto chegar ao grafo, é invisível no grafo: sobra uma borda colorida como único portador do estado. O usuário iniciante vê um contorno avermelhado e não tem como saber o que está errado sem clicar em cada nó suspeito.

O mesmo vale para a proporção de réplicas. O ADR 0021 justificou remover `Degraded` do nó de PodGroup afirmando que "o nó do controlador ao lado mantém a proporção", e o `CONTEXT.md` descreve o par de nós como respondendo perguntas diferentes: quantos estão prontos, e por quê. O controlador tem os números de réplicas prontas e desejadas, mas nunca os desenhou — a promessa do ADR nunca chegou à tela.

O nó ganha uma terceira linha, exibida apenas quando a severidade não é saudável nem neutra. Num nó saudável nada muda em relação a hoje. Num nó de controlador degradado, a linha mostra a proporção de réplicas prontas contra desejadas. Num nó de Pod ou de PodGroup com problema, mostra a razão — a mesma palavra crua que a listagem de Pods exibe, conforme o ADR 0021.

A escolha de mostrar só no caso não-saudável é deliberada. O grafo é um mapa, não uma listagem: vinte nós repetindo `Running` enterrariam o único que diz `CrashLoopBackOff`, que é exatamente o oposto do que a sprint existe para fazer. Quem quiser ver o status de todos os recursos tem a listagem, onde ele já aparece sempre.

O nó de PodGroup não quantifica quantas réplicas estão afetadas. Escrever `1 de 3` ali colocaria na mesma tela dois números vindos de fontes diferentes — a proporção do controlador vem do status de réplicas do recurso, e a contagem de problemas viria do `PodState` de cada Pod — e eles podem discordar entre si. Sinais discordantes foram precisamente o defeito que a Sprint 108 gastou uma sprint inteira eliminando. O par de nós continua com um número só na tela, de uma fonte só.

Isso também encerra a pergunta em aberto que o backlog registrava sobre um anel segmentado, com uma fatia por Pod, para resolver "uma réplica quebrada em três soa tão grave quanto três em três". A geometria nunca foi o problema: a proporção já deveria estar visível, e passa a estar.

Cobertura de teste: nenhuma automatizada, pela mesma razão da issue 03. O aceite manual precisa cobrir um controlador com réplicas parcialmente prontas, um grupo de Pods com uma réplica quebrada e outras sadias, e um namespace inteiramente saudável — onde a terceira linha não pode aparecer em nó nenhum.

Fora de escopo: mudar o vocabulário de status, que é contrato com o usuário desde o ADR 0021; e o painel de detalhe, que continua mostrando o status de todo nó, saudável ou não.

## Comments

Entregue, com a apresentação decidida no aceite manual: em vez de uma terceira linha de texto, o status aparece num **badge na cor da severidade**, abaixo do nome. A palavra é a mesma que o servidor já produzia — `ErrImagePull`, `CrashLoopBackOff`, `0/1 ready` —, preservando o contrato de vocabulário do ADR 0021.

Um nó do Cytoscape tem um único rótulo de texto, então a pílula não podia ser parte dele: o badge é um SVG gerado em tempo de renderização e entregue ao nó como segunda imagem de fundo, com a largura da coluna de texto e a pílula centralizada dentro dele. Detalhe registrado no ADR 0022.
