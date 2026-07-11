package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NamespaceableResource;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.BuildRequest;
import io.greencap.k8s.kubernetes.dto.TemplateBuild;
import io.greencap.k8s.kubernetes.dto.TemplateManifest;
import io.greencap.k8s.kubernetes.dto.TemplateSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// Deploy Template: applies the Namespace resource file first (not the active Namespace — the
// Template defines its own), builds any component without a ready-made public image via Kaniko
// (reusing RegistryService, pushing to the target Cluster's internal Registry, never an external
// one), then applies the remaining resource files with each build's sentinel placeholder resolved
// to the real pushed image reference. Aborts on the first conflict, with no rollback of resources
// already applied — same "no rollback on failure" convention as Import Compose/Deploy from
// Dockerfile. See ADR 0015.
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateDeploymentService {

    // Same convention already used by Deploy from Dockerfile/Import Compose: minikube's registry
    // addon makes the internal Registry reachable from every node at this address.
    private static final String REGISTRY_PULL_HOST = "localhost:5000";
    private static final String BUILD_IMAGE_TAG = "latest";
    private static final String TEMPLATES_REPOSITORY_GIT_URL = "https://github.com/greencapk8s/greencap-templates";
    private static final String TEMPLATES_REPOSITORY_BRANCH = "main";
    private static final String SENTINEL_PREFIX = "__BUILD__";

    private final KubernetesClientFactory clientFactory;
    private final SampleCatalogService sampleCatalogService;
    private final RegistryService registryService;

    public BuildRequest toBuildRequest(TemplateSummary template, TemplateBuild build) {
        // dockerfilePath is resolved by Kaniko relative to the context-sub-path (contextPath),
        // not to the repository root — same convention as Deploy from Dockerfile's "Dockerfile path"
        // field ("Leave blank to use 'Dockerfile' at the context root"). Only contextPath is prefixed
        // with the Template's own directory; dockerfilePath is used as declared in template.yaml.
        return new BuildRequest(
                TEMPLATES_REPOSITORY_GIT_URL,
                TEMPLATES_REPOSITORY_BRANCH,
                template.path() + "/" + build.contextPath(),
                build.dockerfilePath(),
                build.image(),
                BUILD_IMAGE_TAG);
    }

    public String startBuild(Cluster cluster, TemplateSummary template, TemplateBuild build) {
        return registryService.startBuild(cluster, toBuildRequest(template, build));
    }

    public String builtImageReference(TemplateBuild build) {
        return REGISTRY_PULL_HOST + "/" + build.image() + ":" + BUILD_IMAGE_TAG;
    }

    public void applyNamespace(Cluster cluster, TemplateSummary template, TemplateManifest manifest) {
        applyResourceFile(cluster, template, manifest.resources().get(0), Map.of());
    }

    public void applyRemainingResources(Cluster cluster, TemplateSummary template, TemplateManifest manifest,
                                         Map<String, String> builtImagesByBuildName) {
        List<String> remaining = manifest.resources().subList(1, manifest.resources().size());
        for (String fileName : remaining) {
            applyResourceFile(cluster, template, fileName, builtImagesByBuildName);
        }
    }

    private void applyResourceFile(Cluster cluster, TemplateSummary template, String fileName,
                                    Map<String, String> builtImagesByBuildName) {
        String content = sampleCatalogService.fetchResourceFile(template, fileName);
        String resolved = substitutePlaceholders(content, builtImagesByBuildName);

        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
            client.resourceList(resolved).resources().forEach(NamespaceableResource::create);
        } catch (Exception e) {
            log.error("Failed to apply resource file {} of Template {} on cluster {}: {}",
                    fileName, template.id(), cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to apply " + fileName, e);
        }
    }

    String substitutePlaceholders(String content, Map<String, String> builtImagesByBuildName) {
        String resolved = content;
        for (Map.Entry<String, String> entry : builtImagesByBuildName.entrySet()) {
            resolved = resolved.replace(SENTINEL_PREFIX + entry.getKey(), entry.getValue());
        }
        return resolved;
    }
}
