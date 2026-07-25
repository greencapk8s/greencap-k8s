# 01 — Seleção e empacotamento do Build Context no navegador

Status: done

Esta é a fundação da sprint: o componente que transforma uma pasta do computador do usuário num artefato único, pronto para virar contexto de build. Nada mais da sprint funciona sem ele, e ele não depende de nenhuma outra entrega.

Hoje não existe caminho nenhum entre um arquivo local e um Build — as duas telas de deploy só aceitam URL de repositório Git. Esta entrega cria um componente de UI reutilizável, no mesmo padrão de módulo TypeScript já usado por `topology-graph.ts` e `code-mirror-editor.ts`, exposto ao lado Java como um componente Vaadin.

O comportamento visível: um botão que abre o seletor nativo de pastas do sistema operacional. O usuário navega até a pasta do projeto e confirma; nenhum caminho é digitado, porque navegador nenhum entrega o conteúdo de um path escrito à mão. Depois da seleção, e **antes** de qualquer envio, o componente mostra um resumo do que vai subir: quantos arquivos entraram, o tamanho total já comprimido, e a lista do que foi excluído com a contagem por entrada. O usuário confirma o que está prestando antes de comprometer-se com o build.

As exclusões desta entrega são a lista embutida — `.git`, `node_modules`, `venv`, `__pycache__`, `target`, `dist` e `build` — aplicadas sempre, antes do empacotamento. São as mesmas pastas que qualquer `.dockerignore` bem escrito exclui, e sem elas um projeto Node.js comum tentaria empacotar centenas de megabytes e travar a aba. A camada do `.dockerignore` do próprio projeto vem na issue 05 e compõe-se com esta; o desenho da lista embutida deve prever essa composição desde já.

O empacotamento produz um `tar.gz` montado no próprio navegador, usando a API de compressão nativa — sem biblioteca nova no `package.json`. Isso é deliberado e não é detalhe de implementação: é o que preserva a estrutura de subpastas. O `vaadin-upload` envia apenas o nome do arquivo e descarta o caminho relativo, então uma transferência arquivo a arquivo faria `static/style.css` chegar ao servidor como `style.css`, colidindo com homônimos de outras pastas. Empacotado, o contexto sobe numa requisição só e já no formato que o Kaniko consome. O raciocínio completo está na ADR 0020.

O contexto é limitado a 50 MB depois de comprimido e de aplicadas as exclusões. Acima disso, o componente recusa com uma mensagem que diz o tamanho encontrado, o limite, e aponta as maiores pastas incluídas — o usuário precisa saber o que pesou, não só que estourou.

No lado servidor, o upload é bufferizado em disco temporário, não em memória. O `ClustersView` usa buffer em memória para kubeconfig, que é um arquivo de poucos KB; segurar dezenas de MB em heap por upload simultâneo é escolha diferente e errada aqui. O arquivo temporário é descartado ao sair da view.

Cobertura de teste: o empacotamento e as regras de exclusão vivem no TypeScript, fora do alcance do Karibu, e serão validados no aceite manual (pasta com `node_modules`, pasta com subpastas aninhadas, pasta acima do teto). O que é testável em Karibu e deve ser coberto: o componente recusar o prosseguimento enquanto nenhuma pasta foi selecionada, e a validação de tamanho refletir-se em estado inválido na view que o hospeda.

Fora de escopo: interpretação do `.dockerignore` (issue 05), e qualquer uso do artefato produzido — as issues 02 a 04 consomem o que esta entrega gera.
