package io.greencap.k8s.kubernetes.dto;

public record TagInfo(String name, String digest, String size, String createdAt) {
}
