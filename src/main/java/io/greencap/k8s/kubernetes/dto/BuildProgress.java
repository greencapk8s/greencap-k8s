package io.greencap.k8s.kubernetes.dto;

public record BuildProgress(String podName, String status) {
}
