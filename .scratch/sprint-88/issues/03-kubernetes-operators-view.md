---
id: "88-03"
title: "KubernetesOperatorsView — abas Installed e Catalog"
status: done
priority: high
sprint: 88
---

A `KubernetesOperatorsView` é a view principal da seção Developer Experience, acessível na rota `/developer-experience/operators` e protegida por `DEVELOPER_EXPERIENCE_OPERATORS_VIEW`. Segue o padrão visual das demais views do GreenCap: section header com título e botão de refresh, carregamento assíncrono via `CompletableFuture`, banner de cluster inacessível em caso de `KubernetesOperationException`.

**Empty state — OLM ausente:**

Quando `KubernetesOperatorService.isOlmInstalled()` retornar `false`, a view exibe um empty state informativo em vez das abas — mensagem explicando que o OLM não está instalado no cluster ativo, com instrução para habilitá-lo via `minikube addons enable olm` ou apontando para a documentação oficial.

**Aba Installed:**

Grid com colunas: Name, Version, Channel, CatalogSource e Status. O Status é exibido como badge temático: `Installing` (variante primary), `Succeeded` (success), `Failed` (error). O badge `Failed` carrega um tooltip com o `status.message` do CSV, permitindo diagnóstico rápido sem sair da tela.

A ação de desinstalar aparece na barra de seleção quando um operator está selecionado, visível apenas para usuários com `DEVELOPER_EXPERIENCE_OPERATORS_UNINSTALL`. O dialog de confirmação exige que o usuário digite o nome exato do operator antes de habilitar o botão de confirmação — mesmo padrão de Delete Namespace. Após a confirmação, o operator desaparece do grid no próximo ciclo de auto-refresh.

**Aba Catalog:**

Grid com colunas: Name, Description, Provider e CatalogSource. Um filtro de CatalogSource na toolbar permite narrowar a listagem — exibe todos os catálogos disponíveis no cluster, sem hardcodar nenhum.

O botão "Install" na barra de seleção abre um dialog com o nome do operator como título, uma breve descrição, e um dropdown de seleção de channel populado com os channels do `PackageManifest`. O install mode é sempre AllNamespaces — não exposto ao usuário. Ao confirmar, `KubernetesOperatorService.install()` é chamado e a aba Installed é exibida, onde o novo operator aparecerá com badge `Installing` e evoluirá para `Succeeded` via auto-refresh. O botão "Install" é visível apenas para usuários com `DEVELOPER_EXPERIENCE_OPERATORS_INSTALL`.
