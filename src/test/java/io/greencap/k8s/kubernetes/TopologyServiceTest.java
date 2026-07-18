package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.greencap.k8s.domain.cluster.Cluster;
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
