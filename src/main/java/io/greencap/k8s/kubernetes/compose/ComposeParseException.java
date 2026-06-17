package io.greencap.k8s.kubernetes.compose;

public class ComposeParseException extends RuntimeException {
    public ComposeParseException(String message) {
        super(message);
    }
}
