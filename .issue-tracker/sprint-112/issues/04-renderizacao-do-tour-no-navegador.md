# 04 — Renderização do Tour no navegador

Status: done

Consome a issue 03.

O módulo TypeScript que acompanha o `TourComponent` não decide nada. Ele recebe a lista de passos já montada pelo servidor, repassa ao Driver.js, e avisa o servidor quando o Tour termina. Toda a inteligência — quais passos, em que ordem, com que texto, para quem — ficou do outro lado, e essa pobreza deliberada é o que mantém a sprint fora da lacuna de cobertura de frontend do projeto.

A biblioteca é o Driver.js, sob licença MIT, compatível com a Apache 2.0 do projeto e declarada com versão fixa, como todas as dependências npm daqui. Ela entrega exatamente o padrão desejado — escurecer a tela, recortar o foco em volta do elemento e posicionar um balão — mais quatro comportamentos que pareceriam triviais e não são: reposicionar no redimensionamento da janela, rolar o alvo até a viewport, prender o foco do teclado e fechar no ESC. Reimplementar isso à mão foi considerado e descartado no ADR 0024.

O shadow DOM do Vaadin não é obstáculo. Os identificadores da issue 01 são escritos no elemento host, que vive no DOM claro e é alcançável por seletor comum; nenhum passo previsto mira algo dentro do shadow root de um componente. Ainda assim, é o ponto que o aceite manual precisa confirmar em navegador real antes de qualquer outra coisa.

O recorte precisa acompanhar o tema ativo da plataforma. O GreenCap tem tema claro e escuro, e o estilo padrão da biblioteca não conhece os tokens do Lumo — o balão sairia como um elemento estrangeiro no meio da interface. Ele deve ler as mesmas variáveis de tema que o resto da UI usa, do mesmo jeito que os outros módulos TypeScript do projeto já fazem.

Qualquer forma de encerrar leva ao mesmo lugar: concluir o último passo, pular, apertar ESC ou fechar no X chamam de volta o servidor, que grava a preferência. Uma saída que não avise deixaria o usuário preso recebendo o mesmo Tour todo dia.

O parsing da lista recebida precisa ser defensivo. É o mesmo cuidado que `topology-graph.ts` já toma com os dados que recebe do servidor: uma carga inesperada não pode derrubar a página inteira — no limite, o Tour não aparece, e a plataforma segue utilizável.

Cobertura de teste: nenhuma automatizada. O projeto não tem runner de teste de frontend, e essa lacuna é item próprio do backlog, não desta sprint. A garantia é o aceite manual, que precisa passar pelos dois temas, por uma janela estreita o bastante para o drawer recolher, e por um encerramento de cada tipo — concluído, pulado e ESC — confirmando que nenhum deles traz o Tour de volta no login seguinte.

Fora de escopo: extrair funções puras para um módulo testável. Não há suíte para recebê-las, e criar a estrutura de teste de frontend é trabalho maior que esta sprint inteira.

## Comments

**15/08/2026** — Implementada, **mas nunca renderizou em navegador**. A extensão do Chrome não
estava conectada e não havia cluster local, então o aceite manual ficou inteiro em aberto. Este é o
bloqueio principal da sprint.

Driver.js 1.8.0 (MIT), parsing defensivo, `skipMissingElement` por passo e `onDestroyed` cobrindo
todas as saídas. O ponto de maior risco é o tema: a plataforma pinta o tema escuro no elemento do
`AppLayout` e o popover nasce no `body`, fora dessa subárvore — os tokens Lumo são lidos do host,
que está dentro do layout, e republicados na raiz do documento. Se o balão sair claro no tema
escuro, é aqui.

Verificado só o que não precisa de navegador: o bundle de produção compila, o `driver.css` é
extraído em chunk pareado, e o Vite reportou `[TypeScript] Found 0 errors`.

**22/09/2026** — O clique no overlay escurecido **não fecha mais o Tour**. Por padrão o Driver.js
fecha nesse clique, e como toda saída grava a preferência, um clique acidental consumia o primeiro
acesso do usuário. As saídas ficam só as deliberadas: concluir, pular, ESC e o X.
`overlayClickBehavior` recebe um hook vazio, que no Driver.js 1.8.0 é o jeito de anular o clique
sem desligar o `allowClose`, do qual o ESC e o X dependem. Bundle de produção compilando.

**22/09/2026** — Bug achado no aceite: clicar no botão do menu durante o passo do header recolhia o
drawer, e os passos seguintes apontavam para o menu fechado. O Driver.js deixa o elemento destacado
clicável por padrão, e o destaque do passo 1 é a navbar inteira, com o botão dentro. Os passos do
menu tinham o mesmo furo: seus links navegavam no meio do Tour, contrariando a premissa da issue 03
de que o overlay bloqueia interação. `disableActiveInteraction: true` tira os cliques do elemento
destacado em todos os passos.

**22/09/2026** — Aceite manual concluído nos dois temas, em janela estreita e com uma saída de
cada tipo.
