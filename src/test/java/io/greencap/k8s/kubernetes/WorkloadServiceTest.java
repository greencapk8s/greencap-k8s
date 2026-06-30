package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;

import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.DeploymentInfo;
import io.greencap.k8s.kubernetes.dto.PodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EnableKubernetesMockClient(crud = true)
class WorkloadServiceTest {

    static KubernetesClient client;

    private WorkloadService workloadService;
    private Cluster cluster;

    @BeforeEach
    void setup() {
        KubernetesClientFactory mockFactory = mock(KubernetesClientFactory.class);
        KubernetesClient spyClient = spy(client);
        doNothing().when(spyClient).close();
        when(mockFactory.buildClient(any())).thenReturn(spyClient);
        workloadService = new WorkloadService(mockFactory);

        cluster = new Cluster();
        cluster.setName("test-cluster");
        cluster.setKubeconfigContent("encrypted");

        // Clear resources from previous tests
        client.pods().inAnyNamespace().list().getItems()
              .forEach(p -> client.pods().inNamespace(p.getMetadata().getNamespace()).resource(p).delete());
        client.apps().deployments().inAnyNamespace().list().getItems()
              .forEach(d -> client.apps().deployments().inNamespace(d.getMetadata().getNamespace()).resource(d).delete());
    }

    @Test
    void listPods_returnsMappedFields() {
        client.pods().inNamespace("payments").resource(
            new PodBuilder()
                .withNewMetadata().withName("api-pod").withNamespace("payments").endMetadata()
                .withNewSpec().withNodeName("node-1").endSpec()
                .withNewStatus()
                    .withPhase("Running")
                    .addNewContainerStatus().withRestartCount(3).endContainerStatus()
                .endStatus()
                .build()
        ).create();

        List<PodInfo> pods = workloadService.listPods(cluster, "payments");

        assertThat(pods).hasSize(1);
        PodInfo pod = pods.get(0);
        assertThat(pod.name()).isEqualTo("api-pod");
        assertThat(pod.namespace()).isEqualTo("payments");
        assertThat(pod.phase()).isEqualTo("Running");
        assertThat(pod.node()).isEqualTo("node-1");
        assertThat(pod.restarts()).isEqualTo(3);
    }

    @Test
    void listPods_withSpecificNamespace_filtersOtherNamespaces() {
        client.pods().inNamespace("payments").resource(
            new PodBuilder().withNewMetadata().withName("payments-pod").withNamespace("payments").endMetadata()
                .withNewStatus().withPhase("Running").endStatus().build()
        ).create();
        client.pods().inNamespace("monitoring").resource(
            new PodBuilder().withNewMetadata().withName("monitor-pod").withNamespace("monitoring").endMetadata()
                .withNewStatus().withPhase("Running").endStatus().build()
        ).create();

        List<PodInfo> paymentsPods = workloadService.listPods(cluster, "payments");
        List<PodInfo> allPods = workloadService.listPods(cluster, "all");

        assertThat(paymentsPods).extracting(PodInfo::name).containsExactly("payments-pod");
        assertThat(allPods).extracting(PodInfo::name).containsExactlyInAnyOrder("payments-pod", "monitor-pod");
    }

    @Test
    void listDeployments_returnsMappedFields() {
        client.apps().deployments().inNamespace("default").resource(
            new DeploymentBuilder()
                .withNewMetadata().withName("api").withNamespace("default").endMetadata()
                .withNewSpec().withReplicas(3).withNewSelector().endSelector().endSpec()
                .withNewStatus().withReadyReplicas(2).withAvailableReplicas(2).endStatus()
                .build()
        ).create();

        List<DeploymentInfo> deployments = workloadService.listDeployments(cluster, "default");

        assertThat(deployments).hasSize(1);
        DeploymentInfo deployment = deployments.get(0);
        assertThat(deployment.name()).isEqualTo("api");
        assertThat(deployment.desired()).isEqualTo(3);
        assertThat(deployment.ready()).isEqualTo(2);
        assertThat(deployment.available()).isEqualTo(2);
    }

    @Test
    void listPods_whenFabric8Throws_propagatesAsKubernetesOperationException() {
        KubernetesClientFactory failingFactory = mock(KubernetesClientFactory.class);
        when(failingFactory.buildClient(any())).thenThrow(new RuntimeException("connection refused"));
        WorkloadService failingService = new WorkloadService(failingFactory);

        assertThatThrownBy(() -> failingService.listPods(cluster, "default"))
            .isInstanceOf(KubernetesOperationException.class);
    }
}
