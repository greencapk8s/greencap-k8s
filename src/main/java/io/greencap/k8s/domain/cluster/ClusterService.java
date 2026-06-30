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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    public String synthesizeKubeconfig(String apiServerUrl, String bearerToken, String caCertificate) {
        String clusterEntry;
        if (caCertificate != null && !caCertificate.isBlank()) {
            String caData = caCertificate.trim().startsWith("-----BEGIN")
                    ? Base64.getEncoder().encodeToString(caCertificate.trim().getBytes(StandardCharsets.UTF_8))
                    : caCertificate.trim().replaceAll("\\s+", "");
            clusterEntry = "    server: " + apiServerUrl.trim() + "\n" +
                           "    certificate-authority-data: " + caData;
        } else {
            clusterEntry = "    server: " + apiServerUrl.trim() + "\n" +
                           "    insecure-skip-tls-verify: true";
        }
        return "apiVersion: v1\n" +
               "kind: Config\n" +
               "clusters:\n" +
               "- name: cluster\n" +
               "  cluster:\n" + clusterEntry + "\n" +
               "users:\n" +
               "- name: user\n" +
               "  user:\n" +
               "    token: " + bearerToken.trim() + "\n" +
               "contexts:\n" +
               "- name: ctx\n" +
               "  context:\n" +
               "    cluster: cluster\n" +
               "    user: user\n" +
               "current-context: ctx\n";
    }

    private ConnectionStatus testWithPlaintext(String kubeconfigContent) {
        try (KubernetesClient client = clientFactory.buildFromRawKubeconfig(kubeconfigContent)) {
            client.namespaces().list();
            return ConnectionStatus.CONNECTED;
        } catch (Exception e) {
            log.debug("Cluster connection test failed: {}", e.getMessage());
            return ConnectionStatus.ERROR;
        }
    }

}
