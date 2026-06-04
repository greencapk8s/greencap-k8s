package io.greencap.k8s.kubernetes.dto;

public record SecretInfo(
        String name,
        String namespace,
        String type,
        int keyCount,
        String age
) {}
