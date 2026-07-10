package io.greencap.k8s.ui;

import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// Single point of access for background execution in the UI layer. Virtual threads don't
// inherit the Spring Security SecurityContext (ThreadLocal-based), so any code running on a
// virtual thread it creates directly would see no authenticated user for Kubernetes calls.
// Routing every background task through here means SecurityContext propagation only has to
// be solved once (see ADR 0014).
final class AsyncTasks {

    private static final Executor VIRTUAL_THREADS =
            new DelegatingSecurityContextExecutor(Executors.newVirtualThreadPerTaskExecutor());

    // The clock only ticks; it never runs application code, so it stays a single platform
    // thread instead of a per-caller virtual-thread executor. Each tick's actual work is
    // dispatched to VIRTUAL_THREADS, which is where SecurityContext propagation happens.
    private static final ScheduledExecutorService CLOCK =
            Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("async-tasks-clock").factory());

    static void execute(Runnable command) {
        VIRTUAL_THREADS.execute(command);
    }

    // DelegatingSecurityContextExecutor captures SecurityContext from whichever thread calls
    // .execute() — for a recurring tick, that is the CLOCK thread itself, which never has an
    // authenticated user. Capturing the caller's context here, once, up front, and wrapping
    // command with it is what makes every tick run as the user who scheduled the polling.
    static ScheduledFuture<?> schedulePolling(Runnable command, Duration initialDelay, Duration period) {
        Runnable commandWithCallerContext =
                new DelegatingSecurityContextRunnable(command, SecurityContextHolder.getContext());
        return CLOCK.scheduleAtFixedRate(() -> VIRTUAL_THREADS.execute(commandWithCallerContext),
                initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
    }

    private AsyncTasks() {}
}
