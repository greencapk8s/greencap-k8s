package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.Severity;
import io.greencap.k8s.kubernetes.dto.TopologyEdge;
import io.greencap.k8s.kubernetes.dto.TopologyEdgeType;
import io.greencap.k8s.kubernetes.dto.TopologyGraph;
import io.greencap.k8s.kubernetes.dto.TopologyNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@EnableKubernetesMockClient(crud = true)
class TopologyServiceTest {

    static KubernetesClient client;

    private static final String NAMESPACE = "default";

    private TopologyService topologyService;
    private Cluster cluster;

    @BeforeEach
    void setup() {
        KubernetesClientFactory mockFactory = mock(KubernetesClientFactory.class);
        KubernetesClient spyClient = spy(client);
        doNothing().when(spyClient).close();
        when(mockFactory.buildClient(any())).thenReturn(spyClient);
        topologyService = new TopologyService(mockFactory);

        cluster = new Cluster();
        cluster.setName("test-cluster");
        cluster.setKubeconfigContent("encrypted");

        client.pods().inAnyNamespace().list().getItems()
                .forEach(p -> client.pods().inNamespace(p.getMetadata().getNamespace()).resource(p).delete());
        client.apps().statefulSets().inAnyNamespace().list().getItems()
                .forEach(s -> client.apps().statefulSets().inNamespace(s.getMetadata().getNamespace()).resource(s).delete());
        client.services().inAnyNamespace().list().getItems()
                .forEach(s -> client.services().inNamespace(s.getMetadata().getNamespace()).resource(s).delete());
        client.configMaps().inAnyNamespace().list().getItems()
                .forEach(c -> client.configMaps().inNamespace(c.getMetadata().getNamespace()).resource(c).delete());
        client.secrets().inAnyNamespace().list().getItems()
                .forEach(s -> client.secrets().inNamespace(s.getMetadata().getNamespace()).resource(s).delete());
        client.persistentVolumeClaims().inAnyNamespace().list().getItems()
                .forEach(p -> client.persistentVolumeClaims().inNamespace(p.getMetadata().getNamespace()).resource(p).delete());
        client.network().v1().ingresses().inAnyNamespace().list().getItems()
                .forEach(i -> client.network().v1().ingresses().inNamespace(i.getMetadata().getNamespace()).resource(i).delete());
    }

    private void createStatefulSetWithPod(String name, java.util.Map<String, String> podLabels) {
        client.apps().statefulSets().inNamespace(NAMESPACE).resource(
                new StatefulSetBuilder()
                        .withNewMetadata().withName(name).withNamespace(NAMESPACE).endMetadata()
                        .withNewSpec().withReplicas(1).withServiceName(name).endSpec()
                        .withNewStatus().withReadyReplicas(1).endStatus()
                        .build()
        ).create();

        var ownerRef = new OwnerReferenceBuilder()
                .withApiVersion("apps/v1")
                .withKind("StatefulSet")
                .withName(name)
                .withController(true)
                .build();

        client.pods().inNamespace(NAMESPACE).resource(
                new PodBuilder()
                        .withNewMetadata()
                            .withName(name + "-0")
                            .withNamespace(NAMESPACE)
                            .withLabels(podLabels)
                            .withOwnerReferences(ownerRef)
                        .endMetadata()
                        .withNewSpec().endSpec()
                        .withNewStatus().withPhase("Running").endStatus()
                        .build()
        ).create();
    }

    @Test
    void statefulSet_appearsAsNode_withReadyStatus_andPodGroupEdge() {
        createStatefulSetWithPod("postgres", java.util.Map.of("app", "postgres"));

        TopologyGraph graph = topologyService.buildGraph(cluster, NAMESPACE);

        TopologyNode statefulSetNode = graph.nodes().stream()
                .filter(n -> n.id().equals("statefulset/postgres"))
                .findFirst()
                .orElseThrow();
        assertThat(statefulSetNode.type()).isEqualTo("StatefulSet");
        assertThat(statefulSetNode.status()).isEqualTo("Running");
        assertThat(statefulSetNode.label()).isEqualTo("postgres");

        assertThat(graph.edges()).contains(
                TopologyEdge.structural("statefulset/postgres", "pod-group/postgres"));
    }

    @Test
    void podNode_carriesTheSamePodStateTheListingShows() {
        createOrphanPod("standalone", "ImagePullBackOff");

        TopologyNode pod = node(topologyService.buildGraph(cluster, NAMESPACE), "pod/standalone");

        assertThat(pod.status()).isEqualTo("ImagePullBackOff");
        assertThat(pod.severity()).isEqualTo(Severity.PROBLEM);
    }

    @Test
    void podGroup_withOneBrokenReplicaOfThree_reportsTheCauseInsteadOfSummarisingIt() {
        createStatefulSet("api", 3, 2);
        createOwnedPod("api", "api-0", null);
        createOwnedPod("api", "api-1", "CrashLoopBackOff");
        createOwnedPod("api", "api-2", null);

        TopologyNode group = node(topologyService.buildGraph(cluster, NAMESPACE), "pod-group/api");

        assertThat(group.status()).isEqualTo("CrashLoopBackOff");
        assertThat(group.severity()).isEqualTo(Severity.PROBLEM);
    }

    @Test
    void podGroup_withEveryReplicaHealthy_isRunning() {
        createStatefulSet("api", 2, 2);
        createOwnedPod("api", "api-0", null);
        createOwnedPod("api", "api-1", null);

        TopologyNode group = node(topologyService.buildGraph(cluster, NAMESPACE), "pod-group/api");

        assertThat(group.status()).isEqualTo("Running");
        assertThat(group.severity()).isEqualTo(Severity.HEALTHY);
    }

    /** The controller keeps answering "how many are ready" — the likeliest regression here. */
    @Test
    void controllerNode_withFewerReadyThanDesired_staysDegraded() {
        createStatefulSet("api", 3, 2);
        createOwnedPod("api", "api-0", "ImagePullBackOff");

        TopologyNode statefulSet = node(topologyService.buildGraph(cluster, NAMESPACE), "statefulset/api");

        assertThat(statefulSet.status()).isEqualTo("Degraded");
        assertThat(statefulSet.severity()).isEqualTo(Severity.DEGRADED);
    }

    @Test
    void podNode_pointsAtItsOwnPod_notAtTheUnfilteredListing() {
        createOrphanPod("standalone", null);

        TopologyNode pod = node(topologyService.buildGraph(cluster, NAMESPACE), "pod/standalone");

        assertThat(pod.manifestUrl()).isEqualTo("workloads/pods?name=standalone");
    }

    /** The base name prefixes every replica, so a substring filter reaches the whole group. */
    @Test
    void podGroupNode_pointsAtTheGroupBaseName() {
        createStatefulSet("api", 2, 2);
        createOwnedPod("api", "api-0", null);
        createOwnedPod("api", "api-1", null);

        TopologyNode group = node(topologyService.buildGraph(cluster, NAMESPACE), "pod-group/api");

        assertThat(group.manifestUrl()).isEqualTo("workloads/pods?name=api");
    }

    /** Green is reserved for what runs: a Service routes, and routing has no health of its own. */
    @Test
    void serviceNode_isNeutral_soGreenKeepsMeaningSomething() {
        createService("postgres-service");

        TopologyNode service = node(topologyService.buildGraph(cluster, NAMESPACE), "service/postgres-service");

        assertThat(service.severity()).isEqualTo(Severity.NEUTRAL);
    }

    @Test
    void ingressNode_isNeutral_forTheSameReasonAsService() {
        createIngress("api-ingress");

        TopologyNode ingress = node(topologyService.buildGraph(cluster, NAMESPACE), "ingress/api-ingress");

        assertThat(ingress.severity()).isEqualTo(Severity.NEUTRAL);
    }

    /** A lost volume is lost data, and it used to draw the same grey as a healthy one. */
    @Test
    void pvcNode_whenLost_alarms() {
        createPvc("data", "Lost");

        TopologyNode pvc = node(topologyService.buildGraph(cluster, NAMESPACE), "persistentvolumeclaim/data");

        assertThat(pvc.status()).isEqualTo("Lost");
        assertThat(pvc.severity()).isEqualTo(Severity.PROBLEM);
    }

    @Test
    void pvcNode_whenPending_asksForAttentionWithoutAlarming() {
        createPvc("data", "Pending");

        TopologyNode pvc = node(topologyService.buildGraph(cluster, NAMESPACE), "persistentvolumeclaim/data");

        assertThat(pvc.status()).isEqualTo("Pending");
        assertThat(pvc.severity()).isEqualTo(Severity.DEGRADED);
    }

    /** No status yet is not damage — a fresh claim must not flash red before the phase lands. */
    @Test
    void pvcNode_withNoPhaseYet_asksForAttentionInsteadOfAlarming() {
        client.persistentVolumeClaims().inNamespace(NAMESPACE).resource(
                new PersistentVolumeClaimBuilder()
                        .withNewMetadata().withName("data").withNamespace(NAMESPACE).endMetadata()
                        .withNewSpec().endSpec()
                        .build()
        ).create();

        TopologyNode pvc = node(topologyService.buildGraph(cluster, NAMESPACE), "persistentvolumeclaim/data");

        assertThat(pvc.status()).isEqualTo("Unknown");
        assertThat(pvc.severity()).isEqualTo(Severity.DEGRADED);
    }

    @Test
    void pvcNode_whenBound_staysNeutral() {
        createPvc("data", "Bound");

        TopologyNode pvc = node(topologyService.buildGraph(cluster, NAMESPACE), "persistentvolumeclaim/data");

        assertThat(pvc.status()).isEqualTo("Bound");
        assertThat(pvc.severity()).isEqualTo(Severity.NEUTRAL);
    }

    /** The count used to live in `type`, which is why the graph's colour table never matched a Pod. */
    @Test
    void podNode_carriesPodAsTypeAndTheCountAsSubtitle() {
        createOrphanPod("standalone", null);

        TopologyNode pod = node(topologyService.buildGraph(cluster, NAMESPACE), "pod/standalone");

        assertThat(pod.type()).isEqualTo("Pod");
        assertThat(pod.subtitle()).isEqualTo("1 Pod");
    }

    @Test
    void podGroupNode_carriesPodGroupAsTypeAndTheReplicaCountAsSubtitle() {
        createStatefulSet("api", 3, 3);
        createOwnedPod("api", "api-0", null);
        createOwnedPod("api", "api-1", null);
        createOwnedPod("api", "api-2", null);

        TopologyNode group = node(topologyService.buildGraph(cluster, NAMESPACE), "pod-group/api");

        assertThat(group.type()).isEqualTo("PodGroup");
        assertThat(group.subtitle()).isEqualTo("3 Pods");
    }

    @Test
    void podGroupNode_withASingleReplica_usesTheSingularSubtitle() {
        createStatefulSet("api", 1, 1);
        createOwnedPod("api", "api-0", null);

        TopologyNode group = node(topologyService.buildGraph(cluster, NAMESPACE), "pod-group/api");

        assertThat(group.type()).isEqualTo("PodGroup");
        assertThat(group.subtitle()).isEqualTo("1 Pod");
    }

    @Test
    void nodesOfEveryOtherType_repeatTheKindInTypeAndSubtitle() {
        createStatefulSet("api", 1, 1);
        createService("postgres-service");

        TopologyGraph graph = topologyService.buildGraph(cluster, NAMESPACE);

        assertThat(node(graph, "statefulset/api").type()).isEqualTo("StatefulSet");
        assertThat(node(graph, "statefulset/api").subtitle()).isEqualTo("StatefulSet");
        assertThat(node(graph, "service/postgres-service").type()).isEqualTo("Service");
        assertThat(node(graph, "service/postgres-service").subtitle()).isEqualTo("Service");
    }

    /** The proportion ADR 0021 promised was visible next to the group, and never was. */
    @Test
    void degradedController_writesTheReadyProportionOnTheNode() {
        createStatefulSet("api", 3, 2);
        createOwnedPod("api", "api-0", "CrashLoopBackOff");

        TopologyNode statefulSet = node(topologyService.buildGraph(cluster, NAMESPACE), "statefulset/api");

        assertThat(statefulSet.alert()).isEqualTo("2/3 ready");
    }

    @Test
    void healthyController_saysNothingOnTheNode() {
        createStatefulSet("api", 2, 2);
        createOwnedPod("api", "api-0", null);
        createOwnedPod("api", "api-1", null);

        TopologyNode statefulSet = node(topologyService.buildGraph(cluster, NAMESPACE), "statefulset/api");

        assertThat(statefulSet.alert()).isEmpty();
    }

    /** The group answers "why" with the raw reason, never with a second count of its own. */
    @Test
    void troubledPodGroup_writesTheReasonOnTheNode() {
        createStatefulSet("api", 3, 2);
        createOwnedPod("api", "api-0", null);
        createOwnedPod("api", "api-1", "CrashLoopBackOff");
        createOwnedPod("api", "api-2", null);

        TopologyNode group = node(topologyService.buildGraph(cluster, NAMESPACE), "pod-group/api");

        assertThat(group.alert()).isEqualTo("CrashLoopBackOff");
    }

    @Test
    void healthyPodGroup_saysNothingOnTheNode() {
        createStatefulSet("api", 2, 2);
        createOwnedPod("api", "api-0", null);
        createOwnedPod("api", "api-1", null);

        TopologyNode group = node(topologyService.buildGraph(cluster, NAMESPACE), "pod-group/api");

        assertThat(group.alert()).isEmpty();
    }

    @Test
    void lostPvc_writesItsPhaseOnTheNode() {
        createPvc("data", "Lost");

        TopologyNode pvc = node(topologyService.buildGraph(cluster, NAMESPACE), "persistentvolumeclaim/data");

        assertThat(pvc.alert()).isEqualTo("Lost");
    }

    /** Neutral nodes never speak: a Service has nothing to report, so it must stay quiet. */
    @Test
    void serviceNode_saysNothingOnTheNode() {
        createService("postgres-service");

        TopologyNode service = node(topologyService.buildGraph(cluster, NAMESPACE), "service/postgres-service");

        assertThat(service.alert()).isEmpty();
    }

    private TopologyNode node(TopologyGraph graph, String id) {
        return graph.nodes().stream().filter(n -> n.id().equals(id)).findFirst().orElseThrow();
    }

    private void createStatefulSet(String name, int desired, int ready) {
        client.apps().statefulSets().inNamespace(NAMESPACE).resource(
                new StatefulSetBuilder()
                        .withNewMetadata().withName(name).withNamespace(NAMESPACE).endMetadata()
                        .withNewSpec().withReplicas(desired).withServiceName(name).endSpec()
                        .withNewStatus().withReadyReplicas(ready).endStatus()
                        .build()
        ).create();
    }

    private void createOwnedPod(String ownerName, String podName, String waitingReason) {
        var ownerRef = new OwnerReferenceBuilder()
                .withApiVersion("apps/v1").withKind("StatefulSet").withName(ownerName).withController(true)
                .build();
        client.pods().inNamespace(NAMESPACE).resource(
                podBuilder(podName, waitingReason).editMetadata().withOwnerReferences(ownerRef).endMetadata().build()
        ).create();
    }

    private void createOrphanPod(String podName, String waitingReason) {
        client.pods().inNamespace(NAMESPACE).resource(podBuilder(podName, waitingReason).build()).create();
    }

    /** Phase stays Running even when nothing runs inside — the case the sprint exists for. */
    private PodBuilder podBuilder(String podName, String waitingReason) {
        PodBuilder builder = new PodBuilder()
                .withNewMetadata().withName(podName).withNamespace(NAMESPACE).endMetadata()
                .withNewSpec().addNewContainer().withName("app").endContainer().endSpec()
                .withNewStatus().withPhase("Running").endStatus();

        if (waitingReason == null) {
            return builder.editStatus()
                    .addNewContainerStatus()
                        .withName("app")
                        .withNewState().withNewRunning().endRunning().endState()
                    .endContainerStatus()
                    .endStatus();
        }
        return builder.editStatus()
                .addNewContainerStatus()
                    .withName("app")
                    .withNewState().withNewWaiting().withReason(waitingReason).endWaiting().endState()
                .endContainerStatus()
                .endStatus();
    }

    @Test
    void headlessService_matchingStatefulSetPods_getsStructuralEdge_sameAsRegularService() {
        createStatefulSetWithPod("postgres", java.util.Map.of("app", "postgres"));

        client.services().inNamespace(NAMESPACE).resource(
                new ServiceBuilder()
                        .withNewMetadata().withName("postgres").withNamespace(NAMESPACE).endMetadata()
                        .withNewSpec().withClusterIP("None").addToSelector("app", "postgres").endSpec()
                        .build()
        ).create();

        TopologyGraph graph = topologyService.buildGraph(cluster, NAMESPACE);

        assertThat(graph.edges()).contains(
                TopologyEdge.structural("service/postgres", "pod-group/postgres"));
    }

    @Test
    void serviceDependency_hardcodedHostnameEnvValue_createsEdge() {
        createService("postgres-service");
        createPodWithEnvValue("backend", "DB_HOST", "postgres-service");

        TopologyGraph graph = topologyService.buildGraph(cluster, NAMESPACE);

        assertThat(serviceDependencyEdges(graph)).containsExactly(
                TopologyEdge.serviceDependency("pod/backend", "service/postgres-service", "DB_HOST", "postgres-service"));
    }

    @Test
    void serviceDependency_hostPortValue_createsEdge() {
        createService("postgres-service");
        createPodWithEnvValue("backend", "DB_ADDR", "postgres-service:5432");

        TopologyGraph graph = topologyService.buildGraph(cluster, NAMESPACE);

        assertThat(serviceDependencyEdges(graph)).containsExactly(
                TopologyEdge.serviceDependency("pod/backend", "service/postgres-service", "DB_ADDR", "postgres-service:5432"));
    }

    @Test
    void serviceDependency_fullConnectionStringValue_createsEdge() {
        createService("postgres-service");
        String connectionString = "jdbc:postgresql://postgres-service:5432/db";
        createPodWithEnvValue("backend", "DATABASE_URL", connectionString);

        TopologyGraph graph = topologyService.buildGraph(cluster, NAMESPACE);

        assertThat(serviceDependencyEdges(graph)).containsExactly(
                TopologyEdge.serviceDependency("pod/backend", "service/postgres-service", "DATABASE_URL", connectionString));
    }

    @Test
    void serviceDependency_configMapKeyRefValue_resolvesAndCreatesEdge() {
        createService("postgres-service");
        client.configMaps().inNamespace(NAMESPACE).resource(
                new ConfigMapBuilder()
                        .withNewMetadata().withName("app-config").withNamespace(NAMESPACE).endMetadata()
                        .addToData("db-host", "postgres-service")
                        .build()
        ).create();

        var envVar = new io.fabric8.kubernetes.api.model.EnvVarBuilder()
                .withName("DB_HOST")
                .withNewValueFrom()
                    .withNewConfigMapKeyRef("db-host", "app-config", false)
                .endValueFrom()
                .build();
        createPodWithEnv("backend", envVar);

        TopologyGraph graph = topologyService.buildGraph(cluster, NAMESPACE);

        assertThat(serviceDependencyEdges(graph)).containsExactly(
                TopologyEdge.serviceDependency("pod/backend", "service/postgres-service", "DB_HOST", "postgres-service"));
    }

    @Test
    void serviceDependency_secretKeyRefValue_decodesBase64AndCreatesEdge() {
        createService("postgres-service");
        client.secrets().inNamespace(NAMESPACE).resource(
                new SecretBuilder()
                        .withNewMetadata().withName("app-secret").withNamespace(NAMESPACE).endMetadata()
                        .addToData("db-host", Base64.getEncoder().encodeToString("postgres-service".getBytes()))
                        .build()
        ).create();

        var envVar = new io.fabric8.kubernetes.api.model.EnvVarBuilder()
                .withName("DB_HOST")
                .withNewValueFrom()
                    .withNewSecretKeyRef("db-host", "app-secret", false)
                .endValueFrom()
                .build();
        createPodWithEnv("backend", envVar);

        TopologyGraph graph = topologyService.buildGraph(cluster, NAMESPACE);

        assertThat(serviceDependencyEdges(graph)).containsExactly(
                TopologyEdge.serviceDependency("pod/backend", "service/postgres-service", "DB_HOST", "postgres-service"));
    }

    @Test
    void serviceDependency_multipleEnvVarsMatchingSameService_dedupToSingleEdge() {
        createService("postgres-service");
        var hostEnv = new io.fabric8.kubernetes.api.model.EnvVarBuilder()
                .withName("DB_HOST").withValue("postgres-service").build();
        var urlEnv = new io.fabric8.kubernetes.api.model.EnvVarBuilder()
                .withName("DATABASE_URL").withValue("jdbc:postgresql://postgres-service:5432/db").build();
        createPodWithEnv("backend", hostEnv, urlEnv);

        TopologyGraph graph = topologyService.buildGraph(cluster, NAMESPACE);

        assertThat(serviceDependencyEdges(graph)).hasSize(1);
        assertThat(serviceDependencyEdges(graph).get(0).matchedEnvVar()).isEqualTo("DB_HOST");
    }

    @Test
    void serviceDependency_fqdnWithDifferentNamespace_doesNotCreateEdge() {
        createService("postgres-service");
        createPodWithEnvValue("backend", "DB_HOST", "postgres-service.other-ns.svc.cluster.local");

        TopologyGraph graph = topologyService.buildGraph(cluster, NAMESPACE);

        assertThat(serviceDependencyEdges(graph)).isEmpty();
    }

    @Test
    void serviceDependency_fqdnWithActiveNamespace_createsEdge() {
        createService("postgres-service");
        String fqdn = "postgres-service." + NAMESPACE + ".svc.cluster.local";
        createPodWithEnvValue("backend", "DB_HOST", fqdn);

        TopologyGraph graph = topologyService.buildGraph(cluster, NAMESPACE);

        assertThat(serviceDependencyEdges(graph)).containsExactly(
                TopologyEdge.serviceDependency("pod/backend", "service/postgres-service", "DB_HOST", fqdn));
    }

    @Test
    void buildGraph_whenFabric8Throws_propagatesAsKubernetesOperationException() {
        KubernetesClientFactory failingFactory = mock(KubernetesClientFactory.class);
        when(failingFactory.buildClient(any())).thenThrow(new RuntimeException("connection refused"));
        TopologyService failingService = new TopologyService(failingFactory);

        assertThatThrownBy(() -> failingService.buildGraph(cluster, NAMESPACE))
                .isInstanceOf(KubernetesOperationException.class);
    }

    private void createPvc(String name, String phase) {
        client.persistentVolumeClaims().inNamespace(NAMESPACE).resource(
                new PersistentVolumeClaimBuilder()
                        .withNewMetadata().withName(name).withNamespace(NAMESPACE).endMetadata()
                        .withNewSpec().endSpec()
                        .withNewStatus().withPhase(phase).endStatus()
                        .build()
        ).create();
    }

    private void createIngress(String name) {
        client.network().v1().ingresses().inNamespace(NAMESPACE).resource(
                new IngressBuilder()
                        .withNewMetadata().withName(name).withNamespace(NAMESPACE).endMetadata()
                        .withNewSpec().endSpec()
                        .build()
        ).create();
    }

    private void createService(String name) {
        client.services().inNamespace(NAMESPACE).resource(
                new ServiceBuilder()
                        .withNewMetadata().withName(name).withNamespace(NAMESPACE).endMetadata()
                        .withNewSpec().endSpec()
                        .build()
        ).create();
    }

    private void createPodWithEnvValue(String podName, String envName, String envValue) {
        var envVar = new io.fabric8.kubernetes.api.model.EnvVarBuilder()
                .withName(envName).withValue(envValue).build();
        createPodWithEnv(podName, envVar);
    }

    private void createPodWithEnv(String podName, io.fabric8.kubernetes.api.model.EnvVar... envVars) {
        client.pods().inNamespace(NAMESPACE).resource(
                new PodBuilder()
                        .withNewMetadata().withName(podName).withNamespace(NAMESPACE).endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName("backend")
                                .withImage("backend:latest")
                                .withEnv(List.of(envVars))
                            .endContainer()
                        .endSpec()
                        .withNewStatus().withPhase("Running").endStatus()
                        .build()
        ).create();
    }

    private List<TopologyEdge> serviceDependencyEdges(TopologyGraph graph) {
        return graph.edges().stream()
                .filter(e -> e.type() == TopologyEdgeType.SERVICE_DEPENDENCY)
                .toList();
    }
}
