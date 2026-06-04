package io.greencap.k8s.kubernetes.dto;

public record PodMetricInfo(
        String name,
        String namespace,
        long cpuMillicores,
        long memoryMiB
) {}
