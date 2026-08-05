# 02 — Severidade de Service, Ingress e PersistentVolumeClaim passa a refletir a realidade

Status: done

Independente da issue 01. Precisa estar pronta antes do aceite da issue 03, porque é lá que a cor ganha peso.

Três dos oito tipos de nó da Topologia têm severidade fixa no código, decidida na construção do nó e nunca derivada de nada. Service e Ingress são sempre saudáveis. PersistentVolumeClaim é sempre neutro, mesmo tendo o status correto calculado logo acima, na mesma função.

Isso passa despercebido hoje porque a borda é fina e o corpo do nó é saturado — o estado é um detalhe periférico da aparência. A sprint promove a cor a canal principal e único, e aí os três casos deixam de ser imprecisão de fundo e viram mentira em destaque.

Service e Ingress passam a ser neutros. Eles não têm saúde própria a reportar: um Service existe ou não existe, e o fato de ele não selecionar Pod nenhum não muda seu status na API. Pintá-los de verde afirma para o usuário algo que a plataforma não verificou. Pior, gasta o verde: num namespace típico, a maioria dos anéis verdes da tela estaria dizendo apenas "isto é um Service", e o verde de um Deployment realmente saudável se perde no meio deles. Neutro é a resposta honesta — o recurso está lá, e não há juízo de saúde a fazer sobre ele.

PersistentVolumeClaim passa a derivar a severidade do status que já calcula. `Bound` é neutro, pela mesma razão dos anteriores: o volume está ligado, não há processo rodando de que se possa afirmar saúde. `Pending` é degradado, porque é um estado de espera que pode ser transitório e pode ser definitivo — é exatamente a situação em que o usuário precisa olhar sem precisar se assustar. `Lost` é problema, e essa é a correção mais séria da issue: perda de volume é perda de dados, e hoje desenha na tela a mesma cor de um volume perfeitamente ligado. `Terminating` acompanha o tratamento de neutro, sendo o resultado esperado de uma remoção deliberada.

Vale registrar que a Sprint 108 mapeou essas três severidades de propósito para preservar exatamente as cores que já existiam, para que nenhum nó mudasse de aparência sem decisão. Esta issue é essa decisão.

Cobertura de teste: extensão dos testes de Topologia, com um caso por transição — Service e Ingress passando a neutros, e o PersistentVolumeClaim em cada uma das quatro fases produzindo a severidade correspondente. O caso `Lost` é o que documenta a intenção da issue e não deve ser agrupado com os outros.

Fora de escopo: severidade dos nós de controlador e de Pod, que já é derivada e está correta; e a aparência do nó, que é da issue 03.
