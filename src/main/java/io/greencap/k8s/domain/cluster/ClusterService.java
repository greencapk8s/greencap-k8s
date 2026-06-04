package io.greencap.k8s.domain.cluster;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.user.UserRepository;
import io.greencap.k8s.kubernetes.KubernetesClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClusterService {

    private final ClusterRepository clusterRepository;
    private final EncryptionService encryptionService;
    private final KubernetesClientFactory clientFactory;
    private final UserRepository userRepository;

    public List<Cluster> findAll() {
        return clusterRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Cluster createCluster(CreateClusterRequest request) {
        Cluster cluster = new Cluster();
        cluster.setName(request.name());
        cluster.setProvider(request.provider());
        cluster.setConnectionStatus(testWithPlaintext(request.kubeconfigContent()));
        cluster.setKubeconfigContent(encryptionService.encrypt(request.kubeconfigContent()));

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            userRepository.findByUsername(authentication.getName()).ifPresent(cluster::setCreatedBy);
        }

        return clusterRepository.save(cluster);
    }

    @Transactional
    public ConnectionStatus testConnection(Cluster cluster) {
        if (cluster.getKubeconfigContent() == null || cluster.getKubeconfigContent().isBlank()) {
            return ConnectionStatus.UNKNOWN;
        }
        String plaintext = encryptionService.decrypt(cluster.getKubeconfigContent());
        ConnectionStatus status = testWithPlaintext(plaintext);
        cluster.setConnectionStatus(status);
        clusterRepository.save(cluster);
        return status;
    }

    @Transactional
    public void markAsDisconnectedIfConnected(Cluster cluster) {
        if (cluster.getConnectionStatus() == ConnectionStatus.CONNECTED) {
            cluster.setConnectionStatus(ConnectionStatus.DISCONNECTED);
            clusterRepository.save(cluster);
        }
    }

    @Transactional
    public void deleteCluster(Cluster cluster) {
        clusterRepository.delete(cluster);
    }

    private ConnectionStatus testWithPlaintext(String kubeconfigContent) {
        try (KubernetesClient client = clientFactory.buildClient(kubeconfigContent)) {
            client.namespaces().list();
            return ConnectionStatus.CONNECTED;
        } catch (Exception e) {
            log.debug("Cluster connection test failed: {}", e.getMessage());
            return ConnectionStatus.ERROR;
        }
    }

}
