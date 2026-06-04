package io.greencap.k8s.kubernetes.dto;

public record EventInfo(
        String type,
        String reason,
        String involvedObject,
        String message,
        int count,
        String age
) {}
