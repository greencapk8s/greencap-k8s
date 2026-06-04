package io.greencap.k8s.kubernetes;

public class KubernetesOperationException extends RuntimeException {

    public KubernetesOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
