# Sample Catalog: Templates buscados via índice raw HTTP, sem cliente Git

**Status:** Accepted

O Sample Catalog (Developer Experience) lista Templates — aplicações de estudo completas (múltiplos recursos Kubernetes, podendo incluir um componente que precisa ser buildado) — sourced de um repositório Git externo curado pelo próprio time GreenCap (`greencapk8s/greencap-templates`, público). GreenCap não usa nenhuma biblioteca de cliente Git (JGit, git CLI) nem a API de conteúdo do GitHub para descobrir a estrutura do repositório: busca um índice explícito na raiz (`catalog.json`, metadados de todos os Templates) e, por Template, um segundo manifest (`template.yaml`, lista de arquivos de recurso e builds) — ambos via HTTP simples (`raw.githubusercontent.com`), reaproveitando o padrão de URL raw já existente em `DockerfileParser`/`ComposeParser`.

Componentes de um Template que precisam de imagem própria (não uma imagem pública pronta) são construídos via o mesmo pipeline Kaniko já usado em Deploy from Dockerfile/Import Compose, com o próprio `greencap-templates` como Git Repository de contexto, publicando no Registry interno do Cluster (não em um registro externo tipo GHCR) — o `template.yaml` declara esses componentes em uma seção `builds`, e o manifest do recurso correspondente usa um valor-sentinela (`image: __BUILD__<name>`) substituído pela referência real após o build.

Cada Template declara um `namespace` fixo no índice — o Namespace em si é criado a partir de um dos arquivos de recurso do próprio Template (não a Namespace ativa da sessão). "Installed" é portanto uma condição por Cluster (checagem de existência desse Namespace), não por usuário: dois usuários não podem ter instâncias independentes do mesmo Template no mesmo Cluster. Conflito (Namespace ou qualquer outro recurso já existente) aborta a aplicação no primeiro erro, sem rollback do que já foi aplicado — mesmo padrão de "no rollback on failure" já usado em Import Compose e Deploy from Dockerfile.

## Why

O repositório de templates é curado pelo próprio time GreenCap, não um repositório arbitrário de usuário (diferente do Git Repository do Deploy from Dockerfile/Import Compose) — manter um índice explícito tem custo baixo para quem publica o catálogo e evita depender da API de listagem de diretórios do GitHub (rate limit de 60 req/h sem autenticação) ou de uma dependência nova de cliente Git (clone raso + cache em disco + invalidação), na contramão do posicionamento de "plataforma leve" do GreenCap (`CONTEXT.md`). Reaproveitar Kaniko + Registry interno evita depender de infraestrutura de publicação de imagem externa (conta em registro público, pipeline de CI próprio) só para viabilizar os Templates seed.

## Considered Options

- **API de conteúdo do GitHub** para listar diretórios dinamicamente — rejeitado: amarra a implementação a um provedor específico e sofre rate limit sem token.
- **Clone raso (JGit) do repositório inteiro**, cacheado em disco — rejeitado: dependência nova, gestão de cache e invalidação, mais pesado que o necessário para um repositório pequeno e curado.
- **Publicar a imagem do componente buildado em GHCR via GitHub Actions** — rejeitado: adiciona uma segunda infraestrutura de build/publish (Actions + registro externo) quando o Kaniko + Registry interno do Cluster já resolvem exatamente esse problema para os outros fluxos de deploy do GreenCap.
- **Isolamento por usuário** (Namespace com sufixo automático, permitindo múltiplas instâncias do mesmo Template) — rejeitado a pedido: "Installed" é deliberadamente por Cluster.
- **Rollback automático em caso de conflito** — rejeitado: mantém consistência com o padrão já estabelecido de "no rollback on failure" no restante do GreenCap; o usuário limpa manualmente se necessário.

## Consequences

- O `catalog.json` e o `template.yaml` são um contrato público implícito — qualquer mudança de schema exige compatibilidade com Templates já publicados no repositório.
- Deploy de um Template requer builds públicos: o repositório `greencap-templates` precisa ser público (fetch sem autenticação via raw content).
- Sem `SecurityUtils.hasPermission()`/permissão dedicada gateando a view ou o Deploy (ADR 0013) — a view é visível a todo usuário autenticado, e o Kubernetes API server é quem efetivamente autoriza (ou rejeita com 403) a criação dos recursos via o `UserServiceAccount` do usuário.
