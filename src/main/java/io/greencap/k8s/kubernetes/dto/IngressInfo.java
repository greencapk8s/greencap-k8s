package io.greencap.k8s.kubernetes.dto;

public record IngressInfo(
        String name,
        String namespace,
        String ingressClass,
        String hosts,
        boolean tls,
        String address,
        String age
) {}
