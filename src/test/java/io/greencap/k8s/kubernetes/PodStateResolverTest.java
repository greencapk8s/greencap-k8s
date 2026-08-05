package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodConditionBuilder;
import io.greencap.k8s.kubernetes.dto.PodState;
import io.greencap.k8s.kubernetes.dto.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PodStateResolverTest {

    @Test
    void resolve_awaitingDeletion_isTerminatingEvenWhenAContainerIsCrashing() {
        Pod pod = new PodBuilder()
                .withNewMetadata().withName("api").withDeletionTimestamp("2026-08-04T20:00:00Z").endMetadata()
                .withNewSpec().withContainers(container("api")).endSpec()
                .withNewStatus()
                    .withPhase("Running")
                    .withContainerStatuses(waiting("api", "CrashLoopBackOff", "back-off restarting"))
                .endStatus()
                .build();

        PodState state = PodStateResolver.resolve(pod);

        assertThat(state.label()).isEqualTo("Terminating");
        assertThat(state.severity()).isEqualTo(Severity.NEUTRAL);
    }

    @Test
    void resolve_troubledInitContainer_prefixesTheReason() {
        Pod pod = podOf(
                List.of(container("api")),
                List.of(container("migrate")),
                "Pending",
                List.of(waiting("api", "PodInitializing", null)),
                List.of(waiting("migrate", "CrashLoopBackOff", "back-off restarting")),
                List.of());

        PodState state = PodStateResolver.resolve(pod);

        assertThat(state.label()).isEqualTo("Init:CrashLoopBackOff");
        assertThat(state.severity()).isEqualTo(Severity.PROBLEM);
        assertThat(state.message()).isEqualTo("back-off restarting");
    }

    @Test
    void resolve_waitingContainer_usesTheRawReasonAndItsMessage() {
        Pod pod = podOf(
                List.of(container("api")),
                List.of(),
                "Pending",
                List.of(waiting("api", "ImagePullBackOff", "Back-off pulling image \"registry/app:latest\"")),
                List.of(),
                List.of());

        PodState state = PodStateResolver.resolve(pod);

        assertThat(state.label()).isEqualTo("ImagePullBackOff");
        assertThat(state.severity()).isEqualTo(Severity.PROBLEM);
        assertThat(state.message()).isEqualTo("Back-off pulling image \"registry/app:latest\"");
    }

    /**
     * The phase does not regress once a container that had started falls over, so a Pod can
     * report Running while nothing runs inside it. Observed on a real cluster — not theoretical.
     */
    @Test
    void resolve_runningPhaseWithNoRunningContainers_reportsTheContainerReason() {
        Pod pod = podOf(
                List.of(container("backend")),
                List.of(),
                "Running",
                List.of(waiting("backend", "ImagePullBackOff", null)),
                List.of(),
                List.of());

        PodState state = PodStateResolver.resolve(pod);

        assertThat(state.label()).isEqualTo("ImagePullBackOff");
        assertThat(state.severity()).isEqualTo(Severity.PROBLEM);
    }

    @Test
    void resolve_twoTroubledContainers_firstInSpecOrderWins() {
        Pod pod = podOf(
                List.of(container("api"), container("sidecar")),
                List.of(),
                "Running",
                List.of(waiting("sidecar", "ImagePullBackOff", null), waiting("api", "CrashLoopBackOff", null)),
                List.of(),
                List.of());

        PodState state = PodStateResolver.resolve(pod);

        assertThat(state.label()).isEqualTo("CrashLoopBackOff");
    }

    @Test
    void resolve_containerFinishedSuccessfully_doesNotStealTheLabelFromARunningSibling() {
        Pod pod = podOf(
                List.of(container("bootstrap"), container("web")),
                List.of(),
                "Running",
                List.of(terminated("bootstrap", 0, "Completed"), running("web")),
                List.of(),
                List.of());

        PodState state = PodStateResolver.resolve(pod);

        assertThat(state.label()).isEqualTo("Running");
        assertThat(state.severity()).isEqualTo(Severity.HEALTHY);
    }

    @Test
    void resolve_terminatedWithNonZeroExit_usesTheTerminationReason() {
        Pod pod = podOf(
                List.of(container("worker")),
                List.of(),
                "Running",
                List.of(terminated("worker", 137, "OOMKilled")),
                List.of(),
                List.of());

        PodState state = PodStateResolver.resolve(pod);

        assertThat(state.label()).isEqualTo("OOMKilled");
        assertThat(state.severity()).isEqualTo(Severity.PROBLEM);
    }

    @Test
    void resolve_terminatedWithoutReason_fallsBackToTheExitCode() {
        Pod pod = podOf(
                List.of(container("worker")),
                List.of(),
                "Failed",
                List.of(terminated("worker", 2, null)),
                List.of(),
                List.of());

        PodState state = PodStateResolver.resolve(pod);

        assertThat(state.label()).isEqualTo("ExitCode:2");
        assertThat(state.severity()).isEqualTo(Severity.PROBLEM);
    }

    @Test
    void resolve_containersRunningButNeverReady_reportsContainersNotReady() {
        Pod pod = podOf(
                List.of(container("api")),
                List.of(),
                "Running",
                List.of(running("api")),
                List.of(),
                List.of(readyCondition("ContainersNotReady", "containers with unready status: [api]")));

        PodState state = PodStateResolver.resolve(pod);

        assertThat(state.label()).isEqualTo("ContainersNotReady");
        assertThat(state.severity()).isEqualTo(Severity.PROBLEM);
        assertThat(state.message()).isEqualTo("containers with unready status: [api]");
    }

    /**
     * A finished Pod also carries a false Ready condition — keyed on the condition's reason,
     * not the phase, so completed Job Pods are not mistaken for broken ones.
     */
    @Test
    void resolve_completedPod_isNotReportedAsUnready() {
        Pod pod = podOf(
                List.of(container("job")),
                List.of(),
                "Succeeded",
                List.of(terminated("job", 0, "Completed")),
                List.of(),
                List.of(readyCondition("PodCompleted", null)));

        PodState state = PodStateResolver.resolve(pod);

        assertThat(state.label()).isEqualTo("Succeeded");
        assertThat(state.severity()).isEqualTo(Severity.HEALTHY);
    }

    @Test
    void resolve_unrecognizedReason_isTreatedAsAProblem() {
        Pod pod = podOf(
                List.of(container("api")),
                List.of(),
                "Pending",
                List.of(waiting("api", "SomeReasonKubernetesAddedLater", null)),
                List.of(),
                List.of());

        PodState state = PodStateResolver.resolve(pod);

        assertThat(state.label()).isEqualTo("SomeReasonKubernetesAddedLater");
        assertThat(state.severity()).isEqualTo(Severity.PROBLEM);
    }

    @Test
    void resolve_transientReason_isNeutral() {
        Pod pod = podOf(
                List.of(container("api")),
                List.of(),
                "Pending",
                List.of(waiting("api", "ContainerCreating", null)),
                List.of(),
                List.of());

        PodState state = PodStateResolver.resolve(pod);

        assertThat(state.label()).isEqualTo("ContainerCreating");
        assertThat(state.severity()).isEqualTo(Severity.NEUTRAL);
    }

    @Test
    void resolve_healthyPod_fallsBackToThePhase() {
        Pod pod = podOf(
                List.of(container("api")),
                List.of(),
                "Running",
                List.of(running("api")),
                List.of(),
                List.of());

        PodState state = PodStateResolver.resolve(pod);

        assertThat(state.label()).isEqualTo("Running");
        assertThat(state.severity()).isEqualTo(Severity.HEALTHY);
        assertThat(state.message()).isEmpty();
    }

    @Test
    void resolve_podWithoutStatus_isUnknown() {
        Pod pod = new PodBuilder()
                .withNewMetadata().withName("api").endMetadata()
                .withNewSpec().withContainers(container("api")).endSpec()
                .build();

        PodState state = PodStateResolver.resolve(pod);

        assertThat(state.label()).isEqualTo("Unknown");
        assertThat(state.severity()).isEqualTo(Severity.PROBLEM);
    }

    private static Pod podOf(List<Container> containers,
                             List<Container> initContainers,
                             String phase,
                             List<ContainerStatus> containerStatuses,
                             List<ContainerStatus> initContainerStatuses,
                             List<PodCondition> conditions) {
        return new PodBuilder()
                .withNewMetadata().withName("api").withNamespace("demo").endMetadata()
                .withNewSpec().withContainers(containers).withInitContainers(initContainers).endSpec()
                .withNewStatus()
                    .withPhase(phase)
                    .withContainerStatuses(containerStatuses)
                    .withInitContainerStatuses(initContainerStatuses)
                    .withConditions(conditions)
                .endStatus()
                .build();
    }

    private static Container container(String name) {
        return new ContainerBuilder().withName(name).build();
    }

    private static ContainerStatus waiting(String name, String reason, String message) {
        return new ContainerStatusBuilder()
                .withName(name)
                .withNewState().withNewWaiting().withReason(reason).withMessage(message).endWaiting().endState()
                .build();
    }

    private static ContainerStatus terminated(String name, int exitCode, String reason) {
        return new ContainerStatusBuilder()
                .withName(name)
                .withNewState().withNewTerminated().withExitCode(exitCode).withReason(reason).endTerminated().endState()
                .build();
    }

    private static ContainerStatus running(String name) {
        return new ContainerStatusBuilder()
                .withName(name)
                .withNewState().withNewRunning().endRunning().endState()
                .build();
    }

    private static PodCondition readyCondition(String reason, String message) {
        return new PodConditionBuilder()
                .withType("Ready")
                .withStatus("False")
                .withReason(reason)
                .withMessage(message)
                .build();
    }
}
