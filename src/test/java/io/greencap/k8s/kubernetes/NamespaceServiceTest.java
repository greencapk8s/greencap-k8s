package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.NamespaceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EnableKubernetesMockClient(crud = true)
class NamespaceServiceTest {

    static KubernetesClient client;

    private NamespaceService namespaceService;
    private Cluster cluster;

    @BeforeEach
    void setup() {
        KubernetesClientFactory mockFactory = mock(KubernetesClientFactory.class);
        EncryptionService mockEncryption = mock(EncryptionService.class);
        when(mockEncryption.decrypt(any())).thenReturn("irrelevant");
        KubernetesClient spyClient = spy(client);
        doNothing().when(spyClient).close();
        when(mockFactory.buildClient(any())).thenReturn(spyClient);
        namespaceService = new NamespaceService(mockFactory, mockEncryption);

        cluster = new Cluster();
        cluster.setName("test-cluster");
        cluster.setKubeconfigContent("encrypted");

        // Clear namespaces from previous tests
        client.namespaces().list().getItems()
              .forEach(ns -> client.namespaces().resource(ns).delete());
    }

    @Test
    void listNamespaceNames_excludesTerminatingNamespaces() {
        client.namespaces().resource(
            new NamespaceBuilder().withNewMetadata().withName("active").endMetadata()
                .withNewStatus().withPhase("Active").endStatus().build()
        ).create();
        client.namespaces().resource(
            new NamespaceBuilder().withNewMetadata().withName("terminating").endMetadata()
                .withNewStatus().withPhase("Terminating").endStatus().build()
        ).create();
        client.namespaces().resource(
            new NamespaceBuilder().withNewMetadata().withName("other").endMetadata()
                .withNewStatus().withPhase("Active").endStatus().build()
        ).create();

        List<String> names = namespaceService.listNamespaceNames(cluster);

        assertThat(names).containsExactlyInAnyOrder("active", "other");
        assertThat(names).doesNotContain("terminating");
    }

    @Test
    void listNamespacesWithCounts_returnsCorrectResourceCounts() {
        client.namespaces().resource(
            new NamespaceBuilder().withNewMetadata().withName("payments").endMetadata()
                .withNewStatus().withPhase("Active").endStatus().build()
        ).create();
        client.pods().inNamespace("payments").resource(
            new io.fabric8.kubernetes.api.model.PodBuilder()
                .withNewMetadata().withName("pod-1").withNamespace("payments").endMetadata()
                .withNewStatus().withPhase("Running").endStatus().build()
        ).create();
        client.pods().inNamespace("payments").resource(
            new io.fabric8.kubernetes.api.model.PodBuilder()
                .withNewMetadata().withName("pod-2").withNamespace("payments").endMetadata()
                .withNewStatus().withPhase("Running").endStatus().build()
        ).create();
        client.apps().deployments().inNamespace("payments").resource(
            new DeploymentBuilder()
                .withNewMetadata().withName("api").withNamespace("payments").endMetadata()
                .withNewSpec().withReplicas(1).withNewSelector().endSelector().endSpec().build()
        ).create();

        List<NamespaceInfo> namespaces = namespaceService.listNamespacesWithCounts(cluster);

        assertThat(namespaces).hasSize(1);
        NamespaceInfo payments = namespaces.get(0);
        assertThat(payments.name()).isEqualTo("payments");
        assertThat(payments.podCount()).isEqualTo(2);
        assertThat(payments.deploymentCount()).isEqualTo(1);
        assertThat(payments.serviceCount()).isEqualTo(0);
    }

    @Test
    void createNamespace_createsNamespaceInCluster() {
        namespaceService.createNamespace(cluster, "new-namespace");

        assertThat(client.namespaces().withName("new-namespace").get()).isNotNull();
    }

    @Test
    void listNamespaceNames_whenFabric8Throws_propagatesAsKubernetesOperationException() {
        KubernetesClientFactory failingFactory = mock(KubernetesClientFactory.class);
        EncryptionService mockEncryption = mock(EncryptionService.class);
        when(mockEncryption.decrypt(any())).thenReturn("irrelevant");
        when(failingFactory.buildClient(any())).thenThrow(new RuntimeException("connection refused"));
        NamespaceService failingService = new NamespaceService(failingFactory, mockEncryption);

        assertThatThrownBy(() -> failingService.listNamespaceNames(cluster))
            .isInstanceOf(KubernetesOperationException.class);
    }
}
