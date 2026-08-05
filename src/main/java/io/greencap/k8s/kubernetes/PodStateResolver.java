package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.greencap.k8s.kubernetes.dto.PodState;
import io.greencap.k8s.kubernetes.dto.Severity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Derives the {@link PodState} a Pod is shown with. See ADR 0021.
 *
 * <p>The Pod's {@code phase} is not enough on its own: it reports whether the Pod was
 * admitted and its containers created, not whether they work. A Pod whose image cannot be
 * pulled keeps reporting {@code Running}, and the phase does not even regress once a
 * container that had started falls over — Pods with phase {@code Running} and zero running
 * containers are a state observed in practice, not a theoretical one.
 */
public final class PodStateResolver {

    private static final String TERMINATING = "Terminating";
    private static final String UNKNOWN = "Unknown";
    private static final String CONTAINERS_NOT_READY = "ContainersNotReady";
    private static final String READY_CONDITION = "Ready";
    private static final String CONDITION_FALSE = "False";
    private static final String INIT_PREFIX = "Init:";
    private static final String EXIT_CODE_PREFIX = "ExitCode:";
    private static final String NO_MESSAGE = "";

    private static final Set<String> HEALTHY_LABELS = Set.of("Running", "Succeeded");

    /**
     * The only labels enumerated anywhere. Everything else is treated as a problem —
     * an unrecognized reason is more likely bad than good, the same fail-closed posture
     * GreenCap takes with access control.
     */
    private static final Set<String> NEUTRAL_LABELS =
            Set.of("Pending", "ContainerCreating", "PodInitializing", TERMINATING);

    private PodStateResolver() {
    }

    public static PodState resolve(Pod pod) {
        if (isAwaitingDeletion(pod)) {
            return labelled(TERMINATING, NO_MESSAGE);
        }

        PodStatus status = pod.getStatus();
        if (status == null) {
            return labelled(UNKNOWN, NO_MESSAGE);
        }

        Optional<PodState> initTrouble = firstTroubled(initContainers(pod), status.getInitContainerStatuses())
                .map(container -> stateOf(container, INIT_PREFIX));
        if (initTrouble.isPresent()) {
            return initTrouble.get();
        }

        Optional<PodState> trouble = firstTroubled(containers(pod), status.getContainerStatuses())
                .map(container -> stateOf(container, NO_MESSAGE));
        if (trouble.isPresent()) {
            return trouble.get();
        }

        Optional<PodCondition> unready = readyCondition(status).filter(PodStateResolver::reportsUnreadyContainers);
        if (unready.isPresent()) {
            return labelled(CONTAINERS_NOT_READY, unready.get().getMessage());
        }

        return labelled(Optional.ofNullable(status.getPhase()).orElse(UNKNOWN), NO_MESSAGE);
    }

    private static boolean isAwaitingDeletion(Pod pod) {
        return pod.getMetadata() != null && pod.getMetadata().getDeletionTimestamp() != null;
    }

    private static List<Container> containers(Pod pod) {
        return pod.getSpec() == null ? List.of() : orEmpty(pod.getSpec().getContainers());
    }

    private static List<Container> initContainers(Pod pod) {
        return pod.getSpec() == null ? List.of() : orEmpty(pod.getSpec().getInitContainers());
    }

    /**
     * Walks the spec rather than the reported statuses, because only the spec guarantees the
     * order the user sees in the manifest and in the log view's container selector.
     */
    private static Optional<ContainerStatus> firstTroubled(List<Container> declared, List<ContainerStatus> reported) {
        Map<String, ContainerStatus> byName = orEmpty(reported).stream()
                .filter(status -> status.getName() != null)
                .collect(Collectors.toMap(ContainerStatus::getName, Function.identity(), (first, second) -> first));

        return declared.stream()
                .map(container -> byName.get(container.getName()))
                .filter(Objects::nonNull)
                .filter(PodStateResolver::isTroubled)
                .findFirst();
    }

    private static boolean isTroubled(ContainerStatus status) {
        ContainerState state = status.getState();
        if (state == null) {
            return false;
        }
        if (hasReason(state.getWaiting())) {
            return true;
        }
        return failedOnExit(state.getTerminated());
    }

    /**
     * A container that finished successfully must not steal the label from a sibling that is
     * legitimately running, which is why the exit code decides rather than the mere presence
     * of a termination.
     */
    private static boolean failedOnExit(ContainerStateTerminated terminated) {
        return terminated != null && terminated.getExitCode() != null && terminated.getExitCode() != 0;
    }

    private static boolean hasReason(ContainerStateWaiting waiting) {
        return waiting != null && waiting.getReason() != null && !waiting.getReason().isBlank();
    }

    private static PodState stateOf(ContainerStatus status, String prefix) {
        ContainerState state = status.getState();
        if (hasReason(state.getWaiting())) {
            return prefixed(prefix, state.getWaiting().getReason(), state.getWaiting().getMessage());
        }
        ContainerStateTerminated terminated = state.getTerminated();
        String reason = terminated.getReason() != null && !terminated.getReason().isBlank()
                ? terminated.getReason()
                : EXIT_CODE_PREFIX + terminated.getExitCode();
        return prefixed(prefix, reason, terminated.getMessage());
    }

    private static Optional<PodCondition> readyCondition(PodStatus status) {
        return orEmpty(status.getConditions()).stream()
                .filter(condition -> READY_CONDITION.equals(condition.getType()))
                .findFirst();
    }

    /**
     * Kubernetes distinguishes containers that never became ready from a Pod that simply
     * finished, which reports {@code PodCompleted} on the same condition — so the reason
     * decides here, not the phase.
     */
    private static boolean reportsUnreadyContainers(PodCondition condition) {
        return CONDITION_FALSE.equals(condition.getStatus()) && CONTAINERS_NOT_READY.equals(condition.getReason());
    }

    private static PodState prefixed(String prefix, String reason, String message) {
        return new PodState(prefix + reason, severityOf(reason), message);
    }

    private static PodState labelled(String label, String message) {
        return new PodState(label, severityOf(label), message);
    }

    private static Severity severityOf(String label) {
        if (HEALTHY_LABELS.contains(label)) {
            return Severity.HEALTHY;
        }
        if (NEUTRAL_LABELS.contains(label)) {
            return Severity.NEUTRAL;
        }
        return Severity.PROBLEM;
    }

    private static <T> List<T> orEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }
}
