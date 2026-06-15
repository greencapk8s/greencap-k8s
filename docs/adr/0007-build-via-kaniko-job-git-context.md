# ADR 0007 — Build de imagens via Job Kaniko in-cluster com contexto Git

**Status:** Accepted

## Context

A demanda original (backlog, follow-up da Sprint 68) era "build & push de imagens para o Registry interno", levantando três pontos de decisão: mecanismo de build, origem do código/Dockerfile, e acompanhamento de progresso.

Três mecanismos de build foram avaliados:

- **Docker socket no container do GreenCap** (Docker-in-Docker ou bind mount de `/var/run/docker.sock`): exigiria privilégios elevados e acesso ao daemon Docker do host onde o GreenCap roda, misturando a infraestrutura do GreenCap com a do Cluster gerenciado. Quebra o deploy "plug and play" via `docker-compose up`.
- **BuildKit remoto**: exige implantar e manter um daemon BuildKit separado — mais uma peça de infraestrutura para o usuário operar.
- **Kaniko como Job in-cluster**: builda e faz push a partir de um Pod comum, sem privilégios especiais nem daemon Docker.

Para a origem do código, a demanda inicial falava em "upload de Dockerfile via UI". Transferir um Dockerfile (ou um contexto de build completo, com múltiplos arquivos) do GreenCap para dentro do Cluster exigiria ConfigMap (limite ~1MiB, inviável para contexto completo) ou PVC — reabrindo o problema de `nodeAffinity` da StorageClass `standard` descoberto na Sprint 71 (PV sem `nodeAffinity`, dados "somem" se o Pod for reagendado para outro node).

## Decision

Builds são executados como um `Job` Kaniko (`gcr.io/kaniko-project/executor`) criado pelo GreenCap via Fabric8 no Namespace `greencap-system` (criado sob demanda se não existir), mesmo padrão de criação de `Job` já usado em `WorkloadService.triggerCronJob`.

O contexto de build vem de um **Repositório Git público**, usando o suporte nativo do Kaniko a contexto Git (`--context=git://<host>/<owner>/<repo>.git#refs/heads/<branch>`, `--dockerfile=<path>`). O próprio Pod do Kaniko clona o repositório — GreenCap nunca transfere arquivos, não há ConfigMap, PVC ou upload envolvidos.

O destino do push é sempre o Registry interno do Cluster ativo (`registry.kube-system.svc.cluster.local:80`, porta do Service que mapeia para a porta 5000 do container — ADR 0006). Como o Job roda dentro do Cluster, o push usa o DNS interno do cluster diretamente — sem o port-forward que o caminho de leitura (`RegistryService`) precisa para alcançar o Service a partir de fora do cluster. O registry não possui TLS, então o Job usa a flag `--insecure` do Kaniko.

O `Job` é efêmero: define `ttlSecondsAfterFinished` para que o Kubernetes remova `Job` e `Pod` automaticamente após terminar (sucesso ou falha), sem exigir limpeza manual.

## Alternatives considered

**Docker socket no container do GreenCap**: descartado — quebra o deploy "plug and play" e exige privilégios elevados no host do GreenCap, fora do escopo de "management layer sobre Clusters externos" (`CONTEXT.md`).

**BuildKit remoto**: descartado — infraestrutura extra a implantar e manter, contrário ao posicionamento "plataforma leve".

**Upload de Dockerfile/contexto via UI**: descartado — mesmo restrito a um único Dockerfile autocontido, exigiria ConfigMap; para contexto completo (múltiplos arquivos via `COPY`), exigiria PVC e reabriria o problema de `nodeAffinity` da Sprint 71. O contexto Git do Kaniko resolve "contexto completo" sem nenhuma transferência de arquivo.

## Consequences

- Build limitado a repositórios Git **públicos** no v1 — repositórios privados exigiriam credenciais (token), um novo conceito de credencial não coberto por esta decisão. Registrado no backlog como follow-up.
- Usuários que precisem incluir arquivos locais no contexto de build devem primeiro publicá-los em um repositório Git.
- Sem histórico de builds anteriores — consistente com o "sem entidade persistida" do Registry (ADR 0006). Se necessário no futuro, exigiria listar `Job`s por label ou persistência própria.
- Se o usuário navegar para fora da view de acompanhamento antes do build terminar, perde o acompanhamento em tempo real (mesma limitação do `PodLog` hoje) — o build continua até concluir ou até o `ttlSecondsAfterFinished` expirar.
