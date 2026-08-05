# Execução inline Build + Deploy — fase de progresso do wizard

Status: done

Ao clicar "Build & Deploy" no Step 6 (Review) da `DeployFromDockerfileView`, a view entra em modo de execução inline — sem navegação, tudo na mesma página.

A execução segue duas fases em sequência:

**Fase 1 — Build Kaniko**: constrói o `BuildRequest` com `gitRepositoryUrl`, `branch`, `contextPath`, `dockerfilePath`, `repository` (parte `<namespace>/<namespace>` da tag), e `tag`. Chama `registryService.startBuild(cluster, buildRequest)` para criar o Job Kaniko. Exibe logs em tempo real via polling a cada 3 segundos (mesmo mecanismo de `ImportComposeView` — `ObservabilityService` + `ScheduledExecutorService`). O log fica num `Pre` com rolagem automática para o fim. Se o build falhar, exibe erro inline e interrompe a execução.

**Fase 2 — Deploy**: se o build concluir com sucesso, chama `deployApplicationService.deploy(cluster, request)` onde `request.image()` é `localhost:5000/<namespace>/<namespace>:<tag>`. O deploy é síncrono (mesma thread virtual de `DeployApplicationView`). Se tiver sucesso completo, aplica `clusterContext.setNamespace(namespace)`, persiste o namespace ativo via `userService.updateActiveNamespace` e navega para `TopologiaView`. Se falhar parcialmente, exibe mensagem de erro inline com os recursos já criados.

O `ScheduledExecutorService` de polling é cancelado em `DetachEvent`, igual ao padrão de `ImportComposeView`.
