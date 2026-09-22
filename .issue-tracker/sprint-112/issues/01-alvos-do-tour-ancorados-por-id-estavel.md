# 01 — Alvos do Tour ancorados por id estável

Status: done

Pré-requisito das issues 03 e 04. Sozinha não muda nada visível.

O Driver.js localiza cada alvo por seletor CSS, e hoje o projeto não oferece nenhum ponto de fixação: não existe uma única chamada a `setId` em todo o pacote `ui/`. Sem isso, cada passo do Tour dependeria de seletor estrutural — a segunda seção do menu, o terceiro item dentro dela — que continua "funcionando" depois que alguém reordena o menu, só que apontando para outra coisa. É a pior classe de regressão para esta sprint, porque não quebra o build nem falha um teste: apenas ensina errado.

Os elementos que o Tour precisa alcançar ganham identificador explícito. São seis alvos, todos construídos pelo `MainLayout`: o bloco de Cluster e Namespace no header, e as quatro seções do menu — Developer Experience, Project, Global e Settings — mais o item de Topology dentro de Project, que tem passo próprio por ser a visão que diferencia o produto.

Os identificadores vivem num único lugar, não espalhados como literais pelas views. A razão é que o mesmo valor é escrito em dois pontos distantes — no Java que marca o elemento e no Java que monta a lista de passos da issue 03 — e um literal duplicado entre eles é um erro de digitação esperando acontecer, que só apareceria como um passo silenciosamente pulado no navegador.

O prefixo dos identificadores precisa deixar óbvio para que servem, de modo que ninguém os remova numa refatoração futura achando que são resto de depuração. Vale o mesmo cuidado ao escolhê-los: são contrato com o Tour, não detalhe interno da view.

Nada de comportamento muda com esta issue. Os elementos passam a carregar um atributo `id` no DOM e nada mais.

Cobertura de teste: um teste Karibu estendendo `KaribuTest` verifica que cada identificador declarado nas constantes é encontrável na árvore de componentes do `MainLayout`. É barato e cobre exatamente a falha que preocupa — alguém renomear ou remover um alvo sem perceber que o Tour dependia dele.

Fora de escopo: identificar elementos que nenhum passo usa. A convenção nasce a serviço do Tour, e generalizá-la para o resto da UI antes de haver uma segunda necessidade real seria abstrair cedo demais.

## Comments

**15/08/2026** — Implementada. `TourTargets` reúne os seis ids e a lista `ALL` que o teste percorre;
o `MainLayout` grava-os na navbar, nas quatro seções do drawer e no item de Topology. O teste Karibu
teve os dentes conferidos (id inexistente na lista faz falhar). Pendente apenas o aceite manual.

O alvo do header ficou na navbar inteira: Cluster e Namespace são dois layouts separados, com o
spacer e o user info entre eles, e nenhum elemento existente envolve só os dois.

**22/09/2026** — Aceite manual concluído. O alvo do passo 1 na navbar inteira foi revisado com o
Tour rodando e mantido: o texto do passo já fala da barra, e nada dentro dela responde a clique
durante o Tour (ver issue 04).
