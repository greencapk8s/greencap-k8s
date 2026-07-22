# 01 — Criar AsyncTasks como ponto único de execução assíncrona

Status: done

Uma nova classe utilitária `AsyncTasks`, no pacote `io.greencap.k8s.ui`, passa a ser o único ponto de acesso a execução assíncrona do projeto — tanto disparo único quanto polling recorrente — sempre com propagação de `SecurityContext` para a virtual thread executora. Ver ADR 0014 para o raciocínio completo por trás da decisão.

A classe expõe dois métodos: um para disparo único, equivalente ao `UiConstants.VIRTUAL_THREADS.execute(...)` existente hoje; e outro para agendar polling recorrente, recebendo o comando a executar, um atraso inicial e um período, ambos como `Duration`. O retorno do agendamento é o `ScheduledFuture` padrão da JDK, permitindo que quem chamou cancele o polling exatamente como as views já fazem hoje (`cancel(false)` para não interromper uma execução em andamento).

Internamente, o polling recorrente não cria um `ScheduledExecutorService` por chamada — usa uma única thread de "clock" compartilhada por todo o processo, cuja única responsabilidade é disparar o tick no intervalo configurado. O trabalho de cada disparo é despachado para o mesmo executor usado pelo disparo único (baseado em `DelegatingSecurityContextExecutor` sobre um executor de virtual threads), que já propaga o `SecurityContext` da thread que fez o agendamento — nenhum wrapping manual de `DelegatingSecurityContextRunnable` é necessário nos chamadores.

O `Thread.ofVirtual().start(...)` cru existente em `MainLayout` (usado para forçar um segundo ciclo de push do Vaadin ao selecionar namespace, sem chamada Kubernetes envolvida) fica fora do escopo desta consolidação — não tem o bug de propagação de contexto que motiva esta mudança.

Cobertura de teste: uma classe de teste JUnit simples, sem contexto Spring, similar em espírito ao `KubernetesClientFactoryTest`. Deve cobrir três comportamentos observáveis de dentro do próprio `Runnable` executado, usando `SecurityContextHolder` com uma `Authentication` mockada e `CountDownLatch` da JDK (sem introduzir Awaitility como dependência nova): que o disparo único propaga o `SecurityContext` da thread chamadora; que o polling recorrente dispara mais de uma vez dentro do intervalo esperado; e que cancelar o `ScheduledFuture` retornado impede disparos futuros.
