# Uninstall Template remove apenas o Namespace, deixando imagem no Registry interno e recursos cluster-scoped intocados

**Status:** Accepted

Uninstall Template (Templates Catalog) remove um Template instalado deletando o **Namespace** do Template, cascateando a remoção de todos os recursos *namespaced* que ele criou (Deployments, Services, PersistentVolumeClaims, etc.) — o mesmo mecanismo de Delete Namespace (`NamespaceService.deleteNamespace`). A operação é **deliberadamente escopada ao Namespace**: não remove as imagens que os Kaniko Builds do Deploy Template empurraram para o **Registry interno** do Cluster (`localhost:5000/<image>:latest`), nem eventuais recursos **cluster-scoped** que um Template venha a criar.

## Why

O GreenCap já tem uma postura estabelecida para operações de "uninstall": elas removem o corpo principal do que foi instalado, mas **não fazem faxina agressiva de subprodutos** que podem ser compartilhados ou conter dados. Uninstall Operator deleta `Subscription`/`CSV` mas deixa os CRDs ("leaving CRD cleanup to the user avoids accidental data loss"); Uninstall (Helm) remove os recursos do release mas deixa os PVCs. Uninstall Template segue exatamente essa linha.

Remover a imagem buildada exigiria `RegistryMaintenanceService.deleteRepository` seguido de garbage-collect (`registry garbage-collect` via `pods/exec` no pod do registry — ADR 0008). É uma operação **hard-to-reverse** sobre um recurso **compartilhado** do Cluster: o GC roda no registry inteiro, e a tag `:latest` pode estar referenciada por outra coisa (outra reinstalação, outro fluxo de Deploy). Deletar o Namespace, ao contrário, é reversível-por-reinstalação — basta Deploy Template de novo —, então uma limpeza incompleta tem custo baixo.

Recursos cluster-scoped de Templates são **hipotéticos hoje**: todos os Templates do catálogo são 100% namespaced (ADR 0015). Projetar enumeração/limpeza de cluster-scoped para algo que ainda não existe seria over-engineering (o guia proíbe abstrair antes da segunda ocorrência real).

## Considered Options

- **Limpeza completa incluindo GC da imagem no Registry interno** — rejeitado: operação hard-to-reverse sobre um recurso compartilhado do Cluster (a tag `:latest` é compartilhável, o GC afeta o registry inteiro via `pods/exec`), e inconsistente com a postura de Uninstall Operator/Uninstall Helm de não remover subprodutos.
- **Enumerar e deletar recursos cluster-scoped criados pelo Template** — rejeitado: nenhum Template do catálogo cria recursos cluster-scoped hoje; seria especulativo, sem segunda ocorrência real que justifique.
- **Deletar apenas o Namespace** (aceito) — simetria com os uninstalls existentes, evita o hard-to-reverse no Registry e mantém a operação single-shot como o Deploy Template (ADR 0015).

## Consequences

- Após um Uninstall, a imagem buildada **permanece no Registry interno** e continua visível na view Registry; um novo Deploy Template reaproveita/sobrescreve a tag `:latest`. O usuário que quiser removê-la faz isso manualmente via Registry → Remove Repository.
- Se um Template futuro introduzir recursos **cluster-scoped**, esta decisão precisa ser revisitada — a fronteira "só Namespace" passaria a vazar (recursos órfãos após o Uninstall). Autores de Template devem estar cientes de que o Uninstall não os removerá.
- Deletar Namespace é **assíncrono**: o Namespace entra em `Terminating` antes de sumir. Durante essa janela nenhuma ação do card é válida (Open Topology abriria uma topologia sumindo; um novo Deploy falharia no Namespace `Terminating`), então o card entra num estado transitório desabilitado "Uninstalling" e se autocorrige para o estado "Deploy" via polling quando o Namespace some — o auto-refresh da view é um no-op deliberado (re-buscaria o catálogo HTTP inteiro a cada tick), então nada mais o faria. Continua "no rollback", consistente com o Deploy Template (ADR 0015).
