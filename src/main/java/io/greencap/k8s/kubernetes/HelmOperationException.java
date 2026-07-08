package io.greencap.k8s.kubernetes;

public class HelmOperationException extends RuntimeException {

    public HelmOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public HelmOperationException(String message) {
        super(message);
    }
}
