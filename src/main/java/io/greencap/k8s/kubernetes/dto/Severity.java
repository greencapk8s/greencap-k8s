package io.greencap.k8s.kubernetes.dto;

/**
 * How bad a resource's reported state is, decided server-side so that no view has to
 * recognize Kubernetes vocabulary of its own. The labels GreenCap shows form an open set
 * that grows with Kubernetes; severity is the closed set the UI can safely switch on.
 *
 * <p>{@link #DEGRADED} belongs to controller nodes in the Topology, which compare ready
 * against desired replicas — a Pod is never degraded, it either works or it does not.
 */
public enum Severity {

    HEALTHY,

    /** Nothing to report: a transient step on the way up or down, or a resource with no health to speak of. */
    NEUTRAL,

    /** Partially available — fewer ready than desired. */
    DEGRADED,

    PROBLEM
}
