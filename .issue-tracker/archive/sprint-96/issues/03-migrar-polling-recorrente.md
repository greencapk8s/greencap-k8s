# 03 — Migrar polling recorrente das views para AsyncTasks.schedulePolling

Status: done

Depende da issue 01. `BuildLogsView`, `DeployFromDockerfileView`, `ImportComposeView`, `MainLayout` e `PodLogsView` deixam de criar seu próprio `ScheduledExecutorService` com fábrica de virtual threads e passam a chamar `AsyncTasks.schedulePolling(...)` para agendar seu polling recorrente (progresso de build, logs de pod, atualização automática do dashboard conforme o `RefreshInterval` do usuário, conforme o caso de cada view).

Em cada uma dessas views, o campo do `ScheduledExecutorService` e a chamada de `shutdown()` no detach são removidos — deixam de existir, já que o agendamento passa a rodar sobre o clock compartilhado de `AsyncTasks`. O campo `ScheduledFuture` que guarda a tarefa agendada permanece, assim como a lógica de `stopPolling()`/`cancel(false)` no detach ou ao pausar/reiniciar o polling — nenhuma mudança de comportamento aí. O `Runnable` passado para `schedulePolling` deixa de precisar de wrapping manual em `DelegatingSecurityContextRunnable`, já que a propagação de `SecurityContext` passa a ser responsabilidade de `AsyncTasks`.

Atenção especial ao `MainLayout`: o intervalo de atualização pode mudar em tempo de execução (`applyRefreshInterval`/`restartRefreshTimer` conforme a preferência do usuário) — o novo agendamento deve substituir corretamente o anterior, cancelando o `ScheduledFuture` em vigor antes de criar um novo, como já acontece hoje.

Comportamento observável não muda em nenhuma dessas views — os intervalos de polling, o comportamento de pausa/retomada e o cancelamento ao sair da view continuam idênticos ao atual.
