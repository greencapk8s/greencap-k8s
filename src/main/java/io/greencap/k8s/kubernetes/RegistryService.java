package io.greencap.k8s.kubernetes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.BuildProgress;
import io.greencap.k8s.kubernetes.dto.BuildRequest;
import io.greencap.k8s.kubernetes.dto.RepositoryInfo;
import io.greencap.k8s.kubernetes.dto.TagInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryService {

    private static final String REGISTRY_NAMESPACE = "kube-system";
    private static final String REGISTRY_SERVICE_NAME = "registry";
    // Fabric8's ServiceResource#portForward forwards directly to this port on the matching Pod
    // (it does not resolve the Service's targetPort) — 5000 is the registry container's listening port.
    private static final int REGISTRY_CONTAINER_PORT = 5000;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

    // Build (Kaniko) — runs as a Job inside the cluster, so it reaches the Registry via cluster-internal
    // DNS directly, unlike the read path above which needs a port-forward from outside the cluster.
    // Port 80 is the Service port (mapped to the registry container's 5000), not 5000 itself —
    // the Service does not expose port 5000 directly.
    private static final String BUILD_NAMESPACE = "greencap-system";
    private static final String REGISTRY_INTERNAL_HOST = "registry.kube-system.svc.cluster.local:80";
    private static final String KANIKO_IMAGE = "gcr.io/kaniko-project/executor:v1.23.2";
    private static final long BUILD_JOB_TTL_SECONDS = 600;
    private static final String DEFAULT_BRANCH = "main";
    private static final String DEFAULT_DOCKERFILE_PATH = "Dockerfile";

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    public List<RepositoryInfo> listRepositories(Cluster cluster) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()));
             LocalPortForward portForward = client.services()
                     .inNamespace(REGISTRY_NAMESPACE)
                     .withName(REGISTRY_SERVICE_NAME)
                     .portForward(REGISTRY_CONTAINER_PORT)) {

            HttpClient httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
            String baseUrl = "http://localhost:" + portForward.getLocalPort();

            return fetchCatalog(httpClient, baseUrl).stream()
                    .map(name -> new RepositoryInfo(name, fetchTags(httpClient, baseUrl, name).size()))
                    .toList();
        } catch (Exception e) {
            // Absence of the registry Service (addon not enabled) is an expected state, not a cluster
            // operation failure — return an empty catalog instead of KubernetesOperationException.
            log.warn("Registry not available for cluster {}: {}", cluster.getName(), e.getMessage());
            return List.of();
        }
    }

    public List<TagInfo> listTags(Cluster cluster, String repository) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()));
             LocalPortForward portForward = client.services()
                     .inNamespace(REGISTRY_NAMESPACE)
                     .withName(REGISTRY_SERVICE_NAME)
                     .portForward(REGISTRY_CONTAINER_PORT)) {

            HttpClient httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
            String baseUrl = "http://localhost:" + portForward.getLocalPort();

            return fetchTags(httpClient, baseUrl, repository).stream()
                    .map(tag -> fetchTagInfo(httpClient, baseUrl, repository, tag))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to list tags for repository {} on cluster {}: {}", repository, cluster.getName(), e.getMessage());
            return List.of();
        }
    }

    public String startBuild(Cluster cluster, BuildRequest request) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {

            ensureBuildNamespaceExists(client);

            String jobName = "kaniko-build-" + (System.currentTimeMillis() / 1000);
            String dockerfilePath = resolveDockerfilePath(request.dockerfilePath());
            String gitContext = buildGitContext(request.gitRepositoryUrl(), request.branch());
            String destination = buildDestination(request.repository(), request.tag());

            List<String> args = new ArrayList<>(List.of(
                    "--dockerfile=" + dockerfilePath,
                    "--context=" + gitContext,
                    "--destination=" + destination,
                    "--insecure"));
            resolveContextSubPath(request.contextPath())
                    .ifPresent(contextSubPath -> args.add("--context-sub-path=" + contextSubPath));

            Job job = new JobBuilder()
                    .withNewMetadata()
                        .withName(jobName)
                        .withNamespace(BUILD_NAMESPACE)
                    .endMetadata()
                    .withNewSpec()
                        .withBackoffLimit(0)
                        .withTtlSecondsAfterFinished((int) BUILD_JOB_TTL_SECONDS)
                        .withNewTemplate()
                            .withNewSpec()
                                .withRestartPolicy("Never")
                                .addNewContainer()
                                    .withName("kaniko")
                                    .withImage(KANIKO_IMAGE)
                                    .withArgs(args)
                                    .withResources(new ResourceRequirementsBuilder()
                                            .addToRequests("cpu", new Quantity("250m"))
                                            .addToRequests("memory", new Quantity("256Mi"))
                                            .addToLimits("cpu", new Quantity("1"))
                                            .addToLimits("memory", new Quantity("1Gi"))
                                            .build())
                                .endContainer()
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                    .build();

            client.batch().v1().jobs().inNamespace(BUILD_NAMESPACE).resource(job).create();
            log.info("Started build job {} for cluster {}: {} -> {}", jobName, cluster.getName(), gitContext, destination);
            return jobName;
        } catch (Exception e) {
            log.error("Failed to start build for cluster {}: {}", cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to start Build: " + e.getMessage(), e);
        }
    }

    public BuildProgress getBuildProgress(Cluster cluster, String jobName) {
        try (KubernetesClient client = clientFactory.buildClient(
                encryptionService.decrypt(cluster.getKubeconfigContent()))) {

            Job job = client.batch().v1().jobs().inNamespace(BUILD_NAMESPACE).withName(jobName).get();
            if (job == null) {
                throw new KubernetesOperationException("Build job not found: " + jobName, null);
            }

            String podName = client.pods().inNamespace(BUILD_NAMESPACE)
                    .withLabel("job-name", jobName)
                    .list().getItems().stream()
                    .findFirst()
                    .map(pod -> pod.getMetadata().getName())
                    .orElse(null);

            return new BuildProgress(podName, deriveBuildStatus(job));
        } catch (KubernetesOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get build progress for job {}: {}", jobName, e.getMessage());
            throw new KubernetesOperationException("Failed to get Build progress: " + e.getMessage(), e);
        }
    }

    private void ensureBuildNamespaceExists(KubernetesClient client) {
        if (client.namespaces().withName(BUILD_NAMESPACE).get() == null) {
            client.namespaces().resource(new NamespaceBuilder()
                            .withNewMetadata()
                                .withName(BUILD_NAMESPACE)
                            .endMetadata()
                            .build())
                    .create();
            log.info("Created namespace {}", BUILD_NAMESPACE);
        }
    }

    private String deriveBuildStatus(Job job) {
        var conditions = Optional.ofNullable(job.getStatus())
                .map(status -> status.getConditions()).orElse(List.of());
        for (var condition : conditions) {
            if ("Complete".equals(condition.getType()) && "True".equals(condition.getStatus())) return "Complete";
            if ("Failed".equals(condition.getType()) && "True".equals(condition.getStatus())) return "Failed";
        }
        return "Running";
    }

    String buildGitContext(String repositoryUrl, String branch) {
        String url = repositoryUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (!url.endsWith(".git")) {
            url = url + ".git";
        }
        // Kaniko's "git://" prefix is followed by the URL without its own scheme;
        // Kaniko re-adds "https://" (or "http://" via GIT_PULL_METHOD) when cloning.
        url = url.replaceFirst("^https?://", "");
        String ref = (branch == null || branch.isBlank()) ? DEFAULT_BRANCH : branch.trim();
        return "git://" + url + "#refs/heads/" + ref;
    }

    String buildDestination(String repository, String tag) {
        return REGISTRY_INTERNAL_HOST + "/" + repository.trim() + ":" + tag.trim();
    }

    String resolveDockerfilePath(String dockerfilePath) {
        return (dockerfilePath == null || dockerfilePath.isBlank()) ? DEFAULT_DOCKERFILE_PATH : dockerfilePath.trim();
    }

    Optional<String> resolveContextSubPath(String contextPath) {
        if (contextPath == null || contextPath.isBlank()) {
            return Optional.empty();
        }
        String trimmed = contextPath.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }

    private TagInfo fetchTagInfo(HttpClient httpClient, String baseUrl, String repository, String tag) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v2/" + repository + "/manifests/" + tag))
                    .timeout(HTTP_TIMEOUT)
                    // Kaniko pushes OCI manifests; older images pushed via Docker use the Docker v2 schema —
                    // both have the same "config"/"layers" shape, so ManifestResponse handles either.
                    .header("Accept", "application/vnd.docker.distribution.manifest.v2+json, application/vnd.oci.image.manifest.v1+json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Unexpected status " + response.statusCode() + " for manifest " + repository + ":" + tag);
            }
            String digest = response.headers().firstValue("Docker-Content-Digest").orElse("-");
            ManifestResponse manifest = objectMapper.readValue(response.body(), ManifestResponse.class);
            long totalSize = manifest.config().size()
                    + manifest.layers().stream().mapToLong(LayerInfo::size).sum();

            ConfigBlob configBlob = get(httpClient, baseUrl + "/v2/" + repository + "/blobs/" + manifest.config().digest(), ConfigBlob.class);

            return new TagInfo(tag, digest, formatSize(totalSize), NamespaceService.age(configBlob.created()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch details for tag " + repository + ":" + tag, e);
        }
    }

    private String formatSize(long bytes) {
        double kilobytes = bytes / 1024.0;
        if (kilobytes < 1024) {
            return String.format("%.1f KB", kilobytes);
        }
        double megabytes = kilobytes / 1024.0;
        if (megabytes < 1024) {
            return String.format("%.1f MB", megabytes);
        }
        return String.format("%.1f GB", megabytes / 1024.0);
    }

    private List<String> fetchCatalog(HttpClient httpClient, String baseUrl) throws Exception {
        CatalogResponse response = get(httpClient, baseUrl + "/v2/_catalog", CatalogResponse.class);
        return response.repositories() != null ? response.repositories() : List.of();
    }

    private List<String> fetchTags(HttpClient httpClient, String baseUrl, String repository) {
        try {
            TagsResponse response = get(httpClient, baseUrl + "/v2/" + repository + "/tags/list", TagsResponse.class);
            return response.tags() != null ? response.tags() : List.of();
        } catch (Exception e) {
            log.warn("Failed to list tags for repository {}: {}", repository, e.getMessage());
            return List.of();
        }
    }

    private <T> T get(HttpClient httpClient, String url, Class<T> type) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Unexpected status " + response.statusCode() + " from " + url);
        }
        return objectMapper.readValue(response.body(), type);
    }

    private record CatalogResponse(List<String> repositories) {
    }

    private record TagsResponse(String name, List<String> tags) {
    }

    private record ManifestResponse(ConfigRef config, List<LayerInfo> layers) {
    }

    private record ConfigRef(String digest, long size) {
    }

    private record LayerInfo(long size) {
    }

    private record ConfigBlob(String created) {
    }
}
