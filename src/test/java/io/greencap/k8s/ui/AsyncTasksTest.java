package io.greencap.k8s.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsyncTasksTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void execute_propagatesSecurityContextFromCallingThread() throws InterruptedException {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("joao");
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> observedUsername = new AtomicReference<>();

        AsyncTasks.execute(() -> {
            var currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
            observedUsername.set(currentAuthentication != null ? currentAuthentication.getName() : null);
            latch.countDown();
        });

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(observedUsername.get()).isEqualTo("joao");
    }

    @Test
    void schedulePolling_firesRepeatedlyAtGivenPeriod() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        ScheduledFuture<?> task = AsyncTasks.schedulePolling(
                latch::countDown, Duration.ZERO, Duration.ofMillis(20));

        try {
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            task.cancel(false);
        }
    }

    @Test
    void schedulePolling_stopsFiringAfterCancellation() throws InterruptedException {
        AtomicInteger executionCount = new AtomicInteger();
        CountDownLatch firstExecution = new CountDownLatch(1);

        ScheduledFuture<?> task = AsyncTasks.schedulePolling(() -> {
            executionCount.incrementAndGet();
            firstExecution.countDown();
        }, Duration.ZERO, Duration.ofMillis(20));

        assertThat(firstExecution.await(2, TimeUnit.SECONDS)).isTrue();
        task.cancel(false);
        int countAtCancellation = executionCount.get();

        Thread.sleep(100);

        assertThat(executionCount.get()).isEqualTo(countAtCancellation);
    }
}
