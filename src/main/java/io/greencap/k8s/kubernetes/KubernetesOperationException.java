package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClientException;

public class KubernetesOperationException extends RuntimeException {

    public KubernetesOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static KubernetesOperationException from(String operation, Exception e) {
        String reason = extractReason(e);
        return new KubernetesOperationException(operation + ": " + reason, e);
    }

    private static String extractReason(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof KubernetesClientException kce) {
                int code = kce.getCode();
                if (code == 403 || isForbidden(kce)) {
                    return "permission denied — your account does not have the required RBAC permission for this operation";
                }
                if (code == 404) {
                    return "resource not found";
                }
                if (code == 409) {
                    return "conflict — resource already exists or is in an incompatible state";
                }
                if (kce.getStatus() != null && kce.getStatus().getMessage() != null) {
                    return kce.getStatus().getMessage();
                }
                if (kce.getMessage() != null) {
                    return kce.getMessage();
                }
            }
            cause = cause.getCause();
        }
        // Fallback: check raw message for common HTTP status patterns
        String msg = e.getMessage();
        if (msg != null) {
            if (msg.contains("403") || msg.toLowerCase().contains("forbidden")) {
                return "permission denied — your account does not have the required RBAC permission for this operation";
            }
            if (msg.contains("404") || msg.toLowerCase().contains("not found")) {
                return "resource not found";
            }
        }
        return msg;
    }

    private static boolean isForbidden(KubernetesClientException kce) {
        if (kce.getStatus() != null) {
            String reason = kce.getStatus().getReason();
            if ("Forbidden".equals(reason)) return true;
            String msg = kce.getStatus().getMessage();
            if (msg != null && msg.toLowerCase().contains("forbidden")) return true;
        }
        String msg = kce.getMessage();
        return msg != null && (msg.contains("403") || msg.toLowerCase().contains("forbidden"));
    }
}
