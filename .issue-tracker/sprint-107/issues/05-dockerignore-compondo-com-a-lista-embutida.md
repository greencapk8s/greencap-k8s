# 05 — `.dockerignore` do projeto compondo com a lista embutida

Status: done

Refinamento da issue 01. Com a lista embutida entregue, o contexto já está protegido do caso que quebra a aba do navegador. Falta respeitar o que o próprio projeto declara: um `.dockerignore` na raiz da pasta selecionada existe justamente para dizer o que não pertence à imagem, e ignorá-lo faria o GreenCap subir arquivos que o `docker build` do usuário nunca subiria.

A ordem de precedência foi decidida e é o coração desta entrega: a lista embutida exclui sempre, e o `.dockerignore` **acrescenta** exclusões por cima. Uma negação no `.dockerignore` — uma linha tentando reincluir `node_modules`, por exemplo — não devolve ao contexto o que a lista embutida removeu. O `.dockerignore` só consegue tirar mais, nunca repor. Isso é assimétrico em relação ao Docker de propósito, e a consequência está registrada na ADR 0020: um projeto que legitimamente precise de uma dessas pastas no contexto não tem como forçá-la.

A sintaxe suportada é um subconjunto pragmático, não a implementação exata do Go usada pelo Docker: linhas de comentário iniciadas por `#`, linhas em branco ignoradas, curingas de um nível e de múltiplos níveis, curinga de caractere único, barra final designando diretório, e negação aplicada na ordem em que as linhas aparecem. O que ficar fora do subconjunto não deve falhar silenciosamente nem abortar o empacotamento — um padrão não compreendido é ignorado, e o resumo mostrado ao usuário deve deixar isso visível em vez de esconder.

O resumo pré-envio da issue 01 passa a distinguir as duas procedências ao listar o que foi excluído: o que saiu pela lista embutida e o que saiu pelo `.dockerignore` do projeto. Sem essa distinção, um usuário que veja um arquivo faltando no contexto não tem como saber se a causa foi uma regra do GreenCap ou uma regra que ele mesmo escreveu.

Quando não há `.dockerignore` na pasta, vale apenas a lista embutida e nada muda em relação à issue 01. O arquivo `.dockerignore` em si, como qualquer `.dockerignore`, não precisa entrar no contexto empacotado.

Cobertura de teste: a interpretação vive no TypeScript, fora do Karibu. Os casos que precisam ser exercitados no aceite manual, com o resumo pré-envio como evidência: pasta sem `.dockerignore` (só a lista embutida atua), pasta com `.dockerignore` excluindo um arquivo que a lista embutida não pega (exclusão adicional aparece), e pasta com `.dockerignore` tentando reincluir `node_modules` (a lista embutida vence e a pasta continua fora).

Fora de escopo: paridade completa com a semântica do Docker, e `.dockerignore` em subdiretórios — o Docker só considera o da raiz do contexto, e esta entrega faz o mesmo.
