# 03 — Nó da Topologia com corpo neutro, ícone de tipo e cor exclusiva de estado

Status: done

Consome a issue 01. É o coração da sprint e o item de alta prioridade do backlog.

Hoje o corpo do nó é pintado por tipo de recurso e o estado ocupa uma borda fina — um fundo saturado disputando atenção com o sinal que realmente importa. A codificação inteira troca de canal: o tipo passa a ser dito por um ícone, e a cor passa a dizer exclusivamente estado. As decisões e as alternativas descartadas estão no ADR 0022.

A forma retangular permanece. Nomes de recurso do Kubernetes são longos e cheios de hash, e o nó já se dimensiona pelo próprio rótulo; o círculo do modelo de referência obrigaria a empurrar o texto para fora do nó justamente no caso mais comum.

O corpo passa a ser neutro e segue o tema ativo da plataforma. Isso não é detalhe: o tema escuro é o padrão do GreenCap, e um corpo branco fixo — a leitura literal do modelo OpenShift — seria um cartão claro sobre canvas escuro no caso comum. O neutro é lido dos tokens do tema no momento da renderização, e precisa ficar um degrau afastado do fundo do canvas, que já segue o mesmo token de base; se corpo e canvas saírem do mesmo valor, o nó desaparece e sobra a borda. A troca de tema acontece em outra view, e a Topologia é reconstruída ao ser reaberta, então não é necessário observar mudança de tema ao vivo.

Cada nó ganha um ícone à esquerda, dentro do retângulo, com o texto deslocado para a direita. Os ícones vêm do conjunto ExamPro Kubernetes Architecture Icons, sob licença MIT, vendorizados no repositório junto do aviso de copyright — a plataforma precisa funcionar sem rede externa, então nada de CDN. O projeto já tem precedente de SVG servido como recurso estático, e o mesmo caminho serve aqui. A URL usada pelo grafo precisa ser absoluta: o endereço é resolvido relativo à rota da view, não à raiz da aplicação. São oito ícones, um por tipo de nó, incluindo o de Pods no plural para o nó de PodGroup — foi a existência dele que decidiu o conjunto a favor do ExamPro.

Os ícones são tiles azuis sólidos, não desenho de linha monocromático. Isso é deliberado e está justificado no ADR: o azul é o mesmo em todos os tipos, então não codifica nada e não compete com o estado — é o azul *variável* de hoje que destrói a leitura. A referência de ícone é indexada pelo tipo do nó, que a issue 01 tornou confiável.

O estado se manifesta em dois lugares. A borda continua sendo pintada pela severidade decidida no servidor, mantendo o reforço de espessura já existente para problema. E o corpo ganha um banho de baixa saturação quando a severidade é de degradação ou de problema — nós saudáveis e neutros ficam com o corpo limpo. É o banho que faz o nó quebrado ser encontrado de relance num grafo denso; uma borda, por mais grossa, ainda exige varrer a tela nó a nó.

A tabela de cores por tipo desaparece do frontend, junto com a entrada morta que a issue 01 expôs. A tabela de cores por severidade permanece, agora como única fonte de cor da tela.

Cobertura de teste: nenhuma automatizada é possível — o projeto não tem infraestrutura de teste de frontend e o grafo é desenhado em canvas. O aceite é manual e precisa passar pelos dois temas, por um namespace com nó saudável, degradado e quebrado ao mesmo tempo, e por um nome de recurso longo o bastante para exercitar o auto-dimensionamento com o ícone presente.

Fora de escopo: o texto de status dentro do nó, que é da issue 04; e o painel de detalhe que abre ao clicar num nó, que não muda nesta sprint.

## Comments

Entregue com uma divergência decidida no aceite manual: **o banho de baixa saturação no corpo foi descartado**. Ele não sobreviveu à travessia para o canvas — o Cytoscape trata opacidade de fundo como propriedade própria e ignora o alfa da cor, então o banho de 18% virava um cartão vermelho ou laranja sólido, competindo com a borda e com o azul do ícone. O estado passou a ser dito só pela borda, com a espessura reforçada agora também na degradação, não apenas no problema. O ADR 0022 registra o descarte como opção considerada.

Três defeitos apareceram no aceite e foram corrigidos na mesma sprint:

- **Nomes colidindo com o ícone e vazando pela borda.** As propriedades de padding por lado do Cytoscape são apelidos de um único `padding` uniforme, então a coluna reservada ao ícone era descontada dos quatro lados. O nó deixou de se auto-dimensionar pelo rótulo e passou a ter largura e altura medidas em JavaScript.
- **Texto branco sobre nó quase branco no tema escuro.** Os tokens de contraste do Lumo carregam alfa e o grafo desenha sobre canvas vazio, não sobre a página. As cores do tema passaram a ser achatadas contra `--lumo-base-color` antes de irem para o grafo. As caixas de agrupamento, que estavam com cores claras fixas, também passaram a seguir o tema.
- **Ícones achatados e cortados.** O conjunto ExamPro traz só `viewBox`, sem `width`/`height`; sem tamanho intrínseco o Chrome aplica o padrão de 300x150 dos elementos substituídos, e o Cytoscape usa esse tamanho como retângulo de origem ao desenhar. Os arquivos vendorizados ganharam dimensões explícitas, e o tamanho do ícone subiu de 32 para 48 pixels.

Também por pedido no aceite, o quadrado de fundo dos ícones ganhou raio de canto. As duas modificações nos arquivos vendorizados estão registradas junto do aviso de licença MIT.
