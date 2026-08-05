package io.greencap.k8s.kubernetes.dto;

/**
 * The single label GreenCap shows for a Pod, answering "is this healthy?" rather than
 * "what lifecycle stage is this in?". No Kubernetes field carries it — see
 * {@code PodStateResolver} for how it is derived, and ADR 0021 for why it exists at all.
 *
 * <p>The label is the raw Kubernetes reason, never translated, so it stays searchable.
 * The message is whatever detail the kubelet attached to that reason, surfaced as a
 * tooltip; it is empty, never null, when there is nothing to add.
 */
public record PodState(
        String label,
        Severity severity,
        String message
) {

    public PodState {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("PodState label is required");
        }
        if (severity == null) {
            throw new IllegalArgumentException("PodState severity is required");
        }
        message = message == null ? "" : message;
    }
}
