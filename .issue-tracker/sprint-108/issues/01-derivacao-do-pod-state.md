# 01 — A derivação do PodState

Status: done

Fundação da sprint: a regra única que decide o que o GreenCap diz sobre um Pod. Não muda nada visível sozinha — as issues 02 e 03 a consomem —, mas é onde mora todo o valor da correção, e é a única parte que precisa estar certa antes de qualquer tela mudar.

Hoje o status de um Pod é a fase crua devolvida pela API (`WorkloadService.listPods`), e a fase responde uma pergunta diferente da que o usuário faz. Ela diz se o Pod foi admitido e teve containers criados, não se aquilo está funcionando. Um Pod cuja imagem não pode ser baixada, ou cujo container reinicia em loop, continua reportando fase `Running`. Pior: a fase fica defasada — no cluster de desenvolvimento encontramos Pods com fase `Running` e **zero** containers rodando, porque a fase não regride quando o container cai depois de ter subido.

O `PodState` é o conceito que substitui isso, já definido no `CONTEXT.md`. Ele nasce de uma escada de precedência resolvida em ordem, parando no primeiro degrau que casar.

Primeiro, um Pod com carimbo de deleção é `Terminating`, e isso vence tudo — num Pod que o usuário mandou apagar, container morrendo é esperado, não notícia. Essa derivação a partir do carimbo de deleção já é usada para PersistentVolumeClaim e PersistentVolume no projeto; Pod é o único que ainda não a fazia.

Depois, um init container com problema, cujo rótulo carrega o prefixo `Init:` antes da razão — convenção do próprio `kubectl`, e informação que muda o diagnóstico, porque falha ali significa que os containers principais nunca chegaram a começar.

Depois, um container regular com problema. Havendo mais de um, vence o primeiro na ordem do `spec` — ordem estável, a mesma que o usuário vê no manifesto e no seletor de container da tela de logs, e a mesma que o `kubectl` adota.

Depois, o caso dos containers que rodam mas nunca ficam prontos, que o Kubernetes só reporta pela condition `Ready`: quando ela está falsa com razão `ContainersNotReady`, é esse o rótulo. Falha de probe especificamente não existe como estado de container — aparece apenas como Event. A razão da condition é também o que distingue esse caso de um Pod que simplesmente terminou, que traz `PodCompleted`; por isso a regra se apoia nela e não na fase.

E, por último, quando nada reporta problema, a própria fase.

Um container conta como problemático quando está em espera com qualquer razão, ou terminado com código de saída diferente de zero. O código de saída importa: sem ele, um container que concluiu com sucesso roubaria o rótulo de um irmão legitimamente rodando.

O vocabulário é a razão crua do Kubernetes, sem tradução e sem lista branca — o conjunto é aberto e cresce com o Kubernetes. Traduzir suavizaria o rótulo ao custo de abandonar o usuário no diagnóstico, já que é o termo cru que ele consegue pesquisar e que aparece em toda documentação e resposta de fórum.

Além do rótulo, o `PodState` carrega uma severidade, que é o que dá cor ao badge e ao nó da Topologia sem obrigar nenhuma tela a reconhecer vocabulário de Kubernetes. São quatro: saudável, neutra, degradada e problema. A derivação de Pod nunca produz a degradada — ela pertence aos nós de controlador da Topologia, que comparam réplicas prontas contra desejadas. A neutra cobre o que é passageiro (`Pending`, `ContainerCreating`, `Terminating`) e também o que simplesmente não opina. E o default é problema: uma razão que não conhecemos é mais provavelmente ruim que boa, mesma postura fail-closed que o projeto já adota no controle de acesso.

A regra vive numa classe própria e stateless em `kubernetes/`, consumida pelas duas issues seguintes. Considerou-se colocá-la como fabricação estática no próprio record, o que evitaria uma classe, mas isso traria Fabric8 para dentro de `dto/` — hoje nenhum DTO do projeto importa Fabric8, e essa fronteira vale mais que a classe economizada.

Cobertura de teste: testes unitários puros, sem mock server e sem contexto Spring, já que a regra é função pura sobre um Pod. Uma tabela cobrindo cada degrau da escada, o desempate por ordem do `spec`, terminação com código zero não roubando o rótulo de um irmão rodando, `ContainersNotReady` distinguido de `PodCompleted`, e uma razão inventada caindo em severidade de problema. Um caso é obrigatório: fase `Running` com zero containers rodando — é contraintuitivo o bastante para alguém "consertar" no futuro achando impossível, e é literalmente o estado que encontramos no cluster.

Fora de escopo: qualquer mudança de tela. A fase crua continua existindo no `PodInfo` e nos pontos que dependem dela de verdade — o desvio que evita chamar a API de logs para Pods sem containers iniciados, e a identificação de Pods de Job concluídos.
