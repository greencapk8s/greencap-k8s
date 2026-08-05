package io.greencap.k8s.kubernetes.dto;

/**
 * A Pod as the listing shows it. Carries both the derived {@link PodState}, which drives the
 * status badge and its filter, and the raw {@code phase} — they answer different questions,
 * and the phase still has consumers that genuinely need the lifecycle stage (identifying
 * successfully completed Job Pods). Neither replaces the other; see ADR 0021.
 */
public record PodInfo(
        String name,
        String namespace,
        String phase,
        PodState state,
        String node,
        int restarts,
        String age,
        String jobName
) {}
