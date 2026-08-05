# 02 — A listagem de Pods passa a exibir o PodState

Status: done

Primeira entrega visível da sprint, e a que fecha o bug original: a coluna Status da listagem de Pods deixa de mentir. Consome a issue 01.

O badge passa a exibir o rótulo do `PodState` no lugar da fase. Na prática, os Pods que hoje aparecem verdes como "Running" enquanto nunca subiram passam a dizer `ImagePullBackOff`, `CrashLoopBackOff`, `OOMKilled`, `ContainersNotReady` — o que estiver acontecendo de fato.

A cor do badge vem da severidade que o `PodState` carrega, não de um reconhecimento de texto feito na view. Saudável em verde, neutra em cinza, problema em vermelho. Isso importa por dois motivos: a view não pode conter lógica de negócio, e o conjunto de rótulos é aberto — qualquer regra baseada em reconhecer palavras deixaria razões futuras do Kubernetes caírem silenciosamente num badge neutro, que é exatamente o defeito que estamos corrigindo, só que mais difícil de perceber.

O badge ganha tooltip com a mensagem que acompanha a razão, quando houver. É o que dá ao usuário iniciante o caminho do diagnóstico sem sacrificar o termo pesquisável — no cluster de desenvolvimento, essa mensagem traz o texto útil, do tipo que nomeia a imagem que não pôde ser baixada. O padrão já existe no projeto: o badge de operator com falha carrega a mensagem do status em tooltip da mesma forma.

O filtro de texto da coluna passa a casar pelo rótulo exibido, não mais pela fase. É o mínimo de coerência com o que está na tela: hoje, um usuário que digitasse `ImagePullBackOff` teria a busca comparada contra `Running` e não veria nada — e o filtro é justamente como ele isolaria os Pods quebrados numa lista longa. A consequência a aceitar é que quem digitar `Running` deixa de ver os Pods travados em espera, o que é correto pela nova semântica mas muda um comportamento que hoje parece funcionar.

O toggle que esconde Pods de Job concluídos continua se apoiando na fase, não no `PodState`. Ele pergunta se o Pod terminou com sucesso, que é uma pergunta de ciclo de vida — e é por isso que a fase permanece no `PodInfo` em vez de ser substituída.

Dois efeitos colaterais menores, ambos correções: Pod de Job concluído passa a aparecer em verde, já que hoje `Succeeded` não é reconhecido pelo badge e cai em cinza neutro; e some o tratamento de `Active`, que é fase de Namespace e nunca ocorre em Pod — código morto, provável cópia da listagem de Namespaces. O método do badge é renomeado, já que o nome deixa de ser verdade quando o argumento não é mais uma fase.

Cobertura de teste: testes Karibu novos para esta view, que hoje não tem nenhum. Escopo enxuto e focado no que a issue muda — a variante de tema aplicada ao badge para cada severidade, incluindo a razão desconhecida caindo em vermelho; o filtro casando pelo rótulo exibido e não pela fase; a presença do tooltip quando há mensagem e sua ausência quando não há; e o toggle de Pods de Job concluídos continuando a funcionar depois da troca, que é o ponto onde a convivência entre fase e `PodState` pode quebrar sem ninguém notar.

Fora de escopo: a Topologia (issue 03), e qualquer coluna nova na listagem. A coluna de restarts permanece como está — reinícios são um sinal independente, e um container que reiniciou muitas vezes mas está saudável agora conta uma história diferente de um quebrado agora.
