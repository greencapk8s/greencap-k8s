package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.stereotype.Component;

@Component
public class KubernetesClientFactory {

    private static final int CONNECTION_TIMEOUT_MS = 5_000;
    private static final int REQUEST_TIMEOUT_MS    = 10_000;

    public KubernetesClient buildClient(String kubeconfigContent) {
        Config config = Config.fromKubeconfig(kubeconfigContent);
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        config.setRequestTimeout(REQUEST_TIMEOUT_MS);
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
