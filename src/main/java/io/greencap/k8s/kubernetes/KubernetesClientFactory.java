package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KubernetesClientFactory {

    private static final int CONNECTION_TIMEOUT_MS = 5_000;
    private static final int REQUEST_TIMEOUT_MS    = 10_000;

    private final EncryptionService encryptionService;
    private final UserRepository userRepository;

    public KubernetesClient buildClient(Cluster cluster) {
        String kubeconfig = resolveKubeconfig(cluster);
        return buildFromKubeconfig(kubeconfig);
    }

    // Explicit admin client — used by UserProvisioningService and ClusterService
    public KubernetesClient buildAdminClient(Cluster cluster) {
        return buildFromKubeconfig(encryptionService.decrypt(cluster.getKubeconfigContent()));
    }

    // For connection testing with a raw (already decrypted) kubeconfig string
    public KubernetesClient buildFromRawKubeconfig(String kubeconfigContent) {
        return buildFromKubeconfig(kubeconfigContent);
    }

    private String resolveKubeconfig(Cluster cluster) {
        if (SecurityUtils.isAdmin()) {
            return encryptionService.decrypt(cluster.getKubeconfigContent());
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return userRepository.findByUsername(auth.getName())
                    .filter(u -> u.getServiceaccountToken() != null)
                    .map(u -> encryptionService.decrypt(u.getServiceaccountToken()))
                    .orElseGet(() -> encryptionService.decrypt(cluster.getKubeconfigContent()));
        }
        return encryptionService.decrypt(cluster.getKubeconfigContent());
    }

    private KubernetesClient buildFromKubeconfig(String kubeconfigContent) {
        Config config = Config.fromKubeconfig(kubeconfigContent);
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        config.setRequestTimeout(REQUEST_TIMEOUT_MS);
        // Disable retries so failures surface within the configured timeout instead of after 10x backoff
        config.setRequestRetryBackoffLimit(0);
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
