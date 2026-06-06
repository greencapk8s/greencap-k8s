package io.greencap.k8s.kubernetes.dto;

public record PodInfo(
        String name,
        String namespace,
        String phase,
        String node,
        int restarts,
        String age,
        String jobName
) {}
