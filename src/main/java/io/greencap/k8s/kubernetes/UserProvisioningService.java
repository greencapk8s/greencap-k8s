package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.authentication.TokenRequest;
import io.fabric8.kubernetes.api.model.authentication.TokenRequestBuilder;
import io.fabric8.kubernetes.api.model.rbac.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.user.User;
import io.greencap.k8s.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private static final String GREENCAP_NAMESPACE = "greencap-system";
    private static final String SA_PREFIX = "greencap-user-";
    private static final String CRB_PREFIX = "greencap-user-";
    private static final long TOKEN_EXPIRATION_SECONDS = 365L * 24 * 60 * 60; // 1 year

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;
    private final UserService userService;

    public List<String> listClusterRoles(Cluster cluster) {
        try (KubernetesClient client = clientFactory.buildAdminClient(cluster)) {
            return client.rbac().clusterRoles().list().getItems().stream()
                    .map(cr -> cr.getMetadata().getName())
                    .sorted()
                    .toList();
        }
    }

    public void provisionUser(User user, Cluster cluster, String clusterRoleName) {
        String saName = SA_PREFIX + user.getUsername();
        String crbName = CRB_PREFIX + user.getUsername();

        try (KubernetesClient client = clientFactory.buildAdminClient(cluster)) {
            ensureNamespaceExists(client);
            createServiceAccount(client, saName, user.getUsername());
            createClusterRoleBinding(client, crbName, saName, clusterRoleName, user.getUsername());

            String rawToken = requestToken(client, saName);
            String syntheticKubeconfig = buildKubeconfig(cluster, rawToken);
            String encryptedKubeconfig = encryptionService.encrypt(syntheticKubeconfig);

            userService.assignKubernetesIdentity(user.getId(), saName, clusterRoleName, encryptedKubeconfig);
            log.info("Provisioned K8s identity for user '{}': SA={}, ClusterRole={}", user.getUsername(), saName, clusterRoleName);
        }
    }

    public void updateClusterRole(User user, Cluster cluster, String newClusterRoleName) {
        String crbName = CRB_PREFIX + user.getUsername();
        String saName = user.getServiceaccountName();

        try (KubernetesClient client = clientFactory.buildAdminClient(cluster)) {
            client.rbac().clusterRoleBindings().withName(crbName).delete();
            createClusterRoleBinding(client, crbName, saName, newClusterRoleName, user.getUsername());
            String rawToken = requestToken(client, saName);
            String newKubeconfig = buildKubeconfig(cluster, rawToken);
            String encryptedKubeconfig = encryptionService.encrypt(newKubeconfig);
            userService.assignKubernetesIdentity(user.getId(), saName, newClusterRoleName, encryptedKubeconfig);
            log.info("Updated ClusterRole for user '{}' to '{}'", user.getUsername(), newClusterRoleName);
        }
    }

    public void deprovisionUser(User user, Cluster cluster) {
        if (user.getServiceaccountName() == null) {
            return;
        }
        String saName = user.getServiceaccountName();
        String crbName = CRB_PREFIX + user.getUsername();

        try (KubernetesClient client = clientFactory.buildAdminClient(cluster)) {
            client.rbac().clusterRoleBindings().withName(crbName).delete();
            client.serviceAccounts().inNamespace(GREENCAP_NAMESPACE).withName(saName).delete();
            userService.clearKubernetesIdentity(user.getId());
            log.info("Deprovisioned K8s identity for user '{}'", user.getUsername());
        }
    }

    private void ensureNamespaceExists(KubernetesClient client) {
        if (client.namespaces().withName(GREENCAP_NAMESPACE).get() == null) {
            client.namespaces().resource(new NamespaceBuilder()
                    .withNewMetadata().withName(GREENCAP_NAMESPACE).endMetadata()
                    .build()).create();
        }
    }

    private void createServiceAccount(KubernetesClient client, String saName, String username) {
        ServiceAccount sa = new ServiceAccountBuilder()
                .withNewMetadata()
                    .withName(saName)
                    .withNamespace(GREENCAP_NAMESPACE)
                    .withLabels(Map.of("app.kubernetes.io/managed-by", "greencap", "greencap/user", username))
                .endMetadata()
                .build();
        client.serviceAccounts().inNamespace(GREENCAP_NAMESPACE).resource(sa).serverSideApply();
    }

    private void createClusterRoleBinding(KubernetesClient client, String crbName, String saName,
                                          String clusterRoleName, String username) {
        ClusterRoleBinding crb = new ClusterRoleBindingBuilder()
                .withNewMetadata()
                    .withName(crbName)
                    .withLabels(Map.of("app.kubernetes.io/managed-by", "greencap", "greencap/user", username))
                .endMetadata()
                .withNewRoleRef()
                    .withApiGroup("rbac.authorization.k8s.io")
                    .withKind("ClusterRole")
                    .withName(clusterRoleName)
                .endRoleRef()
                .withSubjects(new SubjectBuilder()
                        .withKind("ServiceAccount")
                        .withName(saName)
                        .withNamespace(GREENCAP_NAMESPACE)
                        .build())
                .build();
        client.rbac().clusterRoleBindings().resource(crb).serverSideApply();
    }

    private String requestToken(KubernetesClient client, String saName) {
        TokenRequest tokenRequest = new TokenRequestBuilder()
                .withNewSpec()
                    .withExpirationSeconds(TOKEN_EXPIRATION_SECONDS)
                .endSpec()
                .build();
        TokenRequest result = client.serviceAccounts()
                .inNamespace(GREENCAP_NAMESPACE)
                .withName(saName)
                .tokenRequest(tokenRequest);
        return result.getStatus().getToken();
    }

    private String buildKubeconfig(Cluster cluster, String bearerToken) {
        // Extract server URL and CA from the cluster's admin kubeconfig
        String adminKubeconfig = encryptionService.decrypt(cluster.getKubeconfigContent());
        io.fabric8.kubernetes.client.Config adminConfig = io.fabric8.kubernetes.client.Config.fromKubeconfig(adminKubeconfig);
        String masterUrl = adminConfig.getMasterUrl();
        // getCaCertData() already returns the base64-encoded string as stored in kubeconfig
        String caCertData = adminConfig.getCaCertData();

        StringBuilder sb = new StringBuilder();
        sb.append("apiVersion: v1\nkind: Config\n");
        sb.append("clusters:\n- cluster:\n    server: ").append(masterUrl).append("\n");
        if (caCertData != null && !caCertData.isBlank()) {
            sb.append("    certificate-authority-data: ").append(caCertData).append("\n");
        } else {
            sb.append("    insecure-skip-tls-verify: true\n");
        }
        sb.append("  name: cluster\n");
        sb.append("contexts:\n- context:\n    cluster: cluster\n    user: user\n  name: ctx\n");
        sb.append("current-context: ctx\n");
        sb.append("users:\n- name: user\n  user:\n    token: ").append(bearerToken).append("\n");
        return sb.toString();
    }
}
