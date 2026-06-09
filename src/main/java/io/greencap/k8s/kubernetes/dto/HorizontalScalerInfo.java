package io.greencap.k8s.kubernetes.dto;

public record HorizontalScalerInfo(
        String name,
        String namespace,
        String target,
        int minReplicas,
        int maxReplicas,
        int currentReplicas,
        String metrics,
        String age,
        boolean targetMissing
) {}
