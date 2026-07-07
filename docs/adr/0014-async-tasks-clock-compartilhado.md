# ADR 0014 — AsyncTasks: clock compartilhado para execução assíncrona em virtual threads

**Status:** Accepted

Toda execução assíncrona do GreenCap (disparo único e polling recorrente) passa a ter um único ponto de acesso: `AsyncTasks`, na camada `ui`. `AsyncTasks.execute(Runnable)` substitui `UiConstants.VIRTUAL_THREADS.execute(...)`. `AsyncTasks.schedulePolling(Runnable, Duration, Duration)` substitui os `ScheduledExecutorService` que `BuildLogsView`, `DeployFromDockerfileView`, `ImportComposeView`, `MainLayout` e `PodLogsView` criavam individualmente. Internamente, `schedulePolling` não cria um `ScheduledExecutorService` por chamador — usa uma única thread de clock compartilhada (plataforma, não virtual) só para disparar o tick; o trabalho real de cada disparo é despachado para o mesmo executor de `execute(...)`, que já propaga `SecurityContext` via `DelegatingSecurityContextExecutor`.

## Why

A correção de fail-closed do `KubernetesClientFactory` (sprint 94/95) expôs que `SecurityContextHolder`, baseado em `ThreadLocal`, não propaga para virtual threads. Cada uma das ~10 views que rodavam trabalho em background tinha reimplementado sua própria forma de disparar virtual threads — a maioria sem propagação de contexto nenhuma. A sprint 95 corrigiu isso ponto a ponto com `DelegatingSecurityContextRunnable`/`Executor` aplicado manualmente em cada call site, mas a causa raiz — ausência de um único ponto de acesso assíncrono — permaneceu, deixando o projeto vulnerável à mesma classe de bug em qualquer código novo que não conhecesse o padrão.

Um scheduler por chamador (como hoje) resolveria só a duplicação de código, não o desperdício de recursos: cada view visitada cria e destrói um `ScheduledExecutorService` inteiro, mesmo o disparo em si sendo trivial. Um clock único e compartilhado, que apenas dispara o tick e delega o trabalho ao executor central, resolve as duas coisas ao mesmo tempo e elimina a necessidade de qualquer wrapping manual de `SecurityContext` em novo código — quem quiser rodar algo em background só chama `AsyncTasks`, e a propagação vem de graça.

## Consequences

- `UiConstants.VIRTUAL_THREADS` é removido; todos os call sites (`DashboardView`, `DeployApplicationView`, `DeploymentsView`, `HelmReleasesView`, `InstalledOperatorsView`, `NamespacesView`, `OperatorCatalogView`, `PodsView`, `StatefulSetsView`, `TopologiaView`, `MainLayout`) passam a usar `AsyncTasks.execute(...)`.
- `BuildLogsView`, `DeployFromDockerfileView`, `ImportComposeView`, `MainLayout` e `PodLogsView` perdem seus campos `ScheduledExecutorService`/`pollExecutor` e o `shutdown()` no detach — o `ScheduledFuture` retornado por `schedulePolling` continua sendo cancelado normalmente no `stopPolling()`/detach, sem mudança de padrão aí.
- Novo código assíncrono não deve criar `Thread.ofVirtual()` ou `ScheduledExecutorService` próprios — deve sempre passar por `AsyncTasks`.
- O `Thread.ofVirtual().start(...)` cru em `MainLayout` (linha ~227, usado para forçar um ciclo de push separado do Vaadin, não faz chamada Kubernetes) fica **fora** do escopo desta consolidação — não tem o bug de propagação de contexto que motivou a mudança, e generalizar o escopo aumentaria o blast radius sem benefício real.

## Alternatives considered

**Scheduler dedicado por chamador (apenas encapsular o padrão atual):** resolveria a duplicação de código, mas manteria um `ScheduledExecutorService` completo sendo criado/destruído a cada visita de view — desperdício de recursos sem necessidade. Rejeitado.

**Unificar todos os `Thread.ofVirtual()` do projeto, incluindo o de `MainLayout` usado para forçar ciclo de push:** rejeitado — esse ponto não tem o bug de segurança que motivou a sprint, e não é chamada Kubernetes; mexer nele é risco desnecessário para um objetivo que não é o dele.
