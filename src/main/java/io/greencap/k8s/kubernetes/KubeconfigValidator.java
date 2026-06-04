package io.greencap.k8s.kubernetes;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubeconfigValidator {

    private static final List<String> PATH_FIELDS = List.of(
            "certificate-authority",
            "client-certificate",
            "client-key"
    );

    public Optional<String> findPathReferencedCertificates(String kubeconfigContent) {
        try {
            Map<String, Object> parsed = new Yaml().load(kubeconfigContent);
            if (parsed == null) {
                return Optional.empty();
            }
            if (hasPathReferences(parsed)) {
                return Optional.of(
                        "This kubeconfig references certificates by local file path, " +
                        "which does not work on the server. Generate a portable version with:\n\n" +
                        "kubectl config view --flatten --minify\n\n" +
                        "and upload the result."
                );
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private boolean hasPathReferences(Map<String, Object> kubeconfig) {
        List<Map<String, Object>> clusters = (List<Map<String, Object>>) kubeconfig.get("clusters");
        if (clusters != null) {
            for (Map<String, Object> entry : clusters) {
                Map<String, Object> cluster = (Map<String, Object>) entry.get("cluster");
                if (cluster != null && containsPathField(cluster)) {
                    return true;
                }
            }
        }

        List<Map<String, Object>> users = (List<Map<String, Object>>) kubeconfig.get("users");
        if (users != null) {
            for (Map<String, Object> entry : users) {
                Map<String, Object> user = (Map<String, Object>) entry.get("user");
                if (user != null && containsPathField(user)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean containsPathField(Map<String, Object> section) {
        return PATH_FIELDS.stream().anyMatch(section::containsKey);
    }
}
