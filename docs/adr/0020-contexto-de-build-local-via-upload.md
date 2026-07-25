# ADR 0020 — Contexto de build local via upload de pasta

**Status:** Accepted
**Extends:** ADR-0007

## Context

A ADR 0007 estabeleceu que Builds rodam como Job Kaniko in-cluster com contexto vindo de um **Repositório Git público**, e rejeitou explicitamente a alternativa "upload de Dockerfile/contexto via UI" por dois motivos:

1. Um Dockerfile isolado exigiria ConfigMap (limite de ~1MiB no etcd).
2. Um contexto completo (múltiplos arquivos alcançados por `COPY`) exigiria PVC, reabrindo o problema de `nodeAffinity` da StorageClass `standard` descoberto na Sprint 71 — PV sem `nodeAffinity`, dados inacessíveis se o Pod fosse reagendado para outro node.

Ambas as premissas mudaram:

- O problema de `nodeAffinity` foi resolvido na **Sprint 98**, com o `local-path-provisioner` instalado e definido como StorageClass default.
- Mais relevante: **nenhum dos dois mecanismos é necessário**. O contexto pode alcançar o Pod pela própria API do Kubernetes, sem passar por objeto persistido no etcd nem por volume persistente.

A consequência registrada na 0007 — "usuários que precisem incluir arquivos locais no contexto de build devem primeiro publicá-los em um repositório Git" — mostrou-se cara para o público-alvo declarado no `CONTEXT.md` (usuários iniciantes em Kubernetes). Exigir que alguém crie conta num host Git, publique um repositório e o torne público apenas para experimentar a plataforma é uma barreira desproporcional ao objetivo de aprendizado.

## Decision

Um Build passa a aceitar duas origens de **Build Context**: o Repositório Git já existente (inalterado, e ainda o padrão) ou uma **Pasta Local** do computador do usuário. A escolha é por Build, feita na primeira etapa dos wizards Deploy from Dockerfile e Import Compose.

**Seleção e empacotamento acontecem no navegador.** O usuário aponta para a pasta pelo seletor nativo do sistema (`webkitdirectory`); um módulo TypeScript próprio — mesmo padrão de `topology-graph.ts` e `code-mirror-editor.ts` — monta um `tar.gz` da árvore selecionada usando `CompressionStream`, nativo do navegador, e o envia como **um único upload**.

Empacotar no cliente é o que torna a estrutura de subpastas preservável: o `vaadin-upload` envia `formData.append(file.formDataName, file, file.name)` (`vaadin-upload-mixin.js:713`), descartando o `webkitRelativePath`. Numa transferência arquivo a arquivo, `static/style.css` chegaria ao servidor como `style.css`, colidindo com qualquer homônimo de outra pasta. Um artefato único também reduz uma pasta de centenas de arquivos a uma requisição, e já sai no formato exato que o Kaniko consome.

**A transferência para dentro do Cluster usa a API de exec, não um objeto persistido.** O Job Kaniko ganha um `initContainer` que compartilha um `emptyDir` com o container do Kaniko e bloqueia esperando um arquivo sentinela. Com o Pod em execução, o GreenCap injeta o `tar.gz` nesse initContainer pela API de exec do Kubernetes (`PodResource.file(...).upload(...)` do Fabric8 — o mesmo mecanismo do `kubectl cp`), grava a sentinela, o initContainer termina e o Kaniko roda com `--context=tar:///workspace/context.tar.gz`.

**Exclusões do contexto** são aplicadas no navegador, antes do empacotamento, por duas regras compostas nesta ordem de precedência:

1. Uma **lista embutida** — `.git`, `node_modules`, `venv`, `__pycache__`, `target`, `dist`, `build` — sempre exclui.
2. Um `.dockerignore` presente na pasta **acrescenta** exclusões.

Uma negação (`!node_modules`) no `.dockerignore` **não** reinclui o que a lista embutida removeu: o `.dockerignore` só consegue tirar mais do contexto, nunca devolver. A interpretação do `.dockerignore` cobre um subconjunto pragmático da sintaxe (comentários `#`, globs `*`/`**`/`?`, barra final de diretório, negação `!` na ordem das linhas), não a implementação exata do Go usada pelo Docker.

O contexto empacotado é limitado a **50 MB comprimido**, recusado com erro explícito acima disso.

## Alternatives considered

**Contexto via ConfigMap**: descartado. O limite de ~1MiB por objeto no etcd é um penhasco rígido, e o modo de falha é opaco — o usuário descobre estourando, sem saber qual arquivo pesou.

**Contexto via PVC**: descartado. Escrever num PVC de fora do Cluster continuaria exigindo um Pod auxiliar, ou seja, a mesma API de exec — só que somando uma StorageClass, um volume persistente a limpar e um ponto de falha, para um artefato descartável que vive segundos. O `emptyDir` cobre o caso sem nada disso.

**Transferência arquivo a arquivo pelo `vaadin-upload` padrão**: descartado. Além das N requisições, dependeria de o Vaadin preservar barras no nome do arquivo — comportamento não documentado, com risco de sanitização silenciosa.

**Exigir um `.zip` preparado pelo usuário**: descartado. Adiciona um passo manual antes de a tela sequer ser útil, atrito indesejado justamente para o público iniciante.

**Digitar um path do sistema de arquivos num campo de texto**: inviável, não descartado por preferência. O servidor resolveria o path dentro do próprio container do GreenCap, e nenhum navegador entrega o conteúdo de um caminho digitado — só de um arquivo escolhido por seletor nativo.

## Consequences

- **Nova exigência de RBAC**: o Build com origem local requer permissão de `create` em `pods/exec` no Namespace `greencap-system`. Contas sem essa permissão recebem o 403 já traduzido por `KubernetesOperationException` ("permission denied — your account does not have the required RBAC permission"), no momento do build. Não há verificação prévia: o projeto não tem esse padrão (as checagens antecipadas foram removidas pela ADR 0013) e introduzi-lo aqui seria abstrair antes da segunda ocorrência.
- **Builds com origem local não são reproduzíveis**: não há URL, commit ou branch que identifique o que foi buildado. Um Build a partir de Git aponta para um estado versionado e recuperável; um a partir de Pasta Local aponta para o disco de alguém num instante. Consistente com "sem histórico de Builds" (ADR 0007), mas vale saber que a origem local é estritamente mais fraca nesse aspecto — Git continua sendo o padrão por isso.
- **O contexto trafega pelo servidor do GreenCap**, ao contrário do contexto Git, que o Pod Kaniko clona sozinho. O upload é bufferizado em disco temporário (não em heap, diferente do `MemoryBuffer` usado para o kubeconfig em `ClustersView`) e descartado ao fim do Build ou ao sair da view.
- **A lista embutida de exclusões pode remover algo legítimo** — um projeto que realmente precise versionar `dist/` no contexto não tem como forçar a inclusão. A tela mostra o que foi excluído e a contagem, então a remoção é visível e não silenciosa.
- **O Build da view de Registry continua aceitando apenas Git Repository**, assim como Deploy Template (repositório curado, onde a origem local não faz sentido). A inconsistência do Registry é deliberada e está registrada no backlog.
