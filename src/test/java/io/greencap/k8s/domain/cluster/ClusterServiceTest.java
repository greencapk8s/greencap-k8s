package io.greencap.k8s.domain.cluster;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.PostgresIntegrationTest;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.kubernetes.KubernetesClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Transactional
class ClusterServiceTest extends PostgresIntegrationTest {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private EncryptionService encryptionService;

    @MockBean
    private KubernetesClientFactory clientFactory;

    @BeforeEach
    void setupMocks() {
        KubernetesClient mockClient = mock(KubernetesClient.class, RETURNS_DEEP_STUBS);
        when(clientFactory.buildClient(any())).thenReturn(mockClient);
    }

    @Test
    @WithMockUser(username = "admin")
    void createCluster_kubeconfigIsStoredEncrypted() {
        var request = new CreateClusterRequest("enc-cluster", "plaintext-kubeconfig");

        Cluster cluster = clusterService.createCluster(request);

        assertThat(cluster.getKubeconfigContent()).isNotEqualTo("plaintext-kubeconfig");
        assertThat(encryptionService.decrypt(cluster.getKubeconfigContent())).isEqualTo("plaintext-kubeconfig");
    }

    @Test
    @WithMockUser(username = "admin")
    void createCluster_setsCreatedByFromSecurityContext() {
        var request = new CreateClusterRequest("owned-cluster", "kc");

        Cluster cluster = clusterService.createCluster(request);

        assertThat(cluster.getCreatedBy()).isNotNull();
        assertThat(cluster.getCreatedBy().getUsername()).isEqualTo("admin");
    }

    @Test
    @WithMockUser(username = "admin")
    void markAsDisconnectedIfConnected_onlyChangesConnectedClusters() {
        Cluster connected = clusterService.createCluster(
            new CreateClusterRequest("connected-cluster", "kc"));

        when(clientFactory.buildClient(any())).thenThrow(new RuntimeException("unreachable"));
        Cluster error = clusterService.createCluster(
            new CreateClusterRequest("error-cluster", "kc"));

        clusterService.markAsDisconnectedIfConnected(connected);
        clusterService.markAsDisconnectedIfConnected(error);

        assertThat(connected.getConnectionStatus()).isEqualTo(ConnectionStatus.DISCONNECTED);
        assertThat(error.getConnectionStatus()).isEqualTo(ConnectionStatus.ERROR);
    }
}
