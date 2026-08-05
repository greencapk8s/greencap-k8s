# 05 — A ajuda da Topologia passa a explicar o ícone e o código de cores

Status: done

Fecha a sprint. Consome as issues 02, 03 e 04.

O diálogo de ajuda da Topologia descreve os tipos de nó, as relações entre eles e o agrupamento por labels, e não menciona cor em momento algum. Isso era tolerável enquanto a cor codificava tipo, informação redundante com a palavra já escrita no nó. Depois desta sprint a cor é o canal de estado da view, e a documentação da própria tela ficaria omissa quanto ao único código cromático que ela usa.

O texto de ajuda ganha uma explicação de que o ícone à esquerda identifica o tipo do recurso e de que a cor da borda indica o estado, com o significado das quatro severidades: verde para o que está rodando, cinza para recursos sem estado próprio a reportar, âmbar para degradação e vermelho para problema.

A explicação do cinza é a que mais importa, e é a que menos se adivinha. Depois da issue 02, Service, Ingress e volumes ligados deixam de ser verdes, e um usuário que via verde neles antes vai querer saber o que mudou. A resposta é que a plataforma não afirma saúde sobre recursos que não executam nada, e isso precisa estar escrito.

Não entra legenda fixa no canvas. Ela gastaria área de grafo permanentemente com uma informação que se aprende em segundos, concorreria com os controles flutuantes que a view já tem, e é menos necessária agora que o problema aparece escrito por extenso dentro do nó — a cor virou reforço de um texto, não o portador único do significado.

Cobertura de teste: o texto de ajuda é conteúdo estático de um diálogo já coberto pelo padrão de views da plataforma; não justifica teste próprio. A verificação é a leitura do texto durante o aceite manual, junto com o restante da sprint.

Fora de escopo: legenda no canvas, tooltip nos nós, e qualquer mudança no restante do texto de ajuda que já existe.
