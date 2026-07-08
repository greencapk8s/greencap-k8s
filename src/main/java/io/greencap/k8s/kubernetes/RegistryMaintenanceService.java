package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.TagInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryMaintenanceService {

    private static final String REGISTRY_NAMESPACE = "kube-system";
    private static final String REGISTRY_SERVICE_NAME = "registry";
    private static final int REGISTRY_CONTAINER_PORT = 5000;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final String REGISTRY_POD_LABEL = "actual-registry";
    private static final String REGISTRY_CONFIG_PATH = "/etc/distribution/config.yml";
    private static final Duration GARBAGE_COLLECT_TIMEOUT = Duration.ofSeconds(60);
    // Repositories root inside the registry container — standard path for distribution/distribution.
    // GC only removes unreferenced blobs; the repository directory itself must be removed explicitly
    // so the /v2/_catalog endpoint stops listing the repository.
    private static final String REGISTRY_REPOSITORIES_PATH = "/var/lib/registry/docker/registry/v2/repositories/";

    private final KubernetesClientFactory clientFactory;
    private final RegistryService registryService;

    public void deleteRepository(Cluster cluster, String repository) {
        List<TagInfo> tags = registryService.listTags(cluster, repository);

        try (RegistryConnection connection = openConnection(cluster)) {
            deleteManifests(connection, repository, tags);
            runGarbageCollect(connection.client());
            deleteRepositoryDirectory(connection.client(), repository);
        } catch (Exception e) {
            log.error("Failed to remove repository {} on cluster {}: {}", repository, cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to remove Repository", e);
        }
    }

    public void deleteTags(Cluster cluster, String repository, List<TagInfo> tags) {
        try (RegistryConnection connection = openConnection(cluster)) {
            deleteManifests(connection, repository, tags);
        } catch (Exception e) {
            log.error("Failed to remove tags from repository {} on cluster {}: {}", repository, cluster.getName(), e.getMessage());
            throw KubernetesOperationException.from("Failed to remove Tags", e);
        }
    }

    private RegistryConnection openConnection(Cluster cluster) {
        KubernetesClient client = clientFactory.buildClient(cluster);
        LocalPortForward portForward = client.services()
                .inNamespace(REGISTRY_NAMESPACE)
                .withName(REGISTRY_SERVICE_NAME)
                .portForward(REGISTRY_CONTAINER_PORT);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
        String baseUrl = "http://localhost:" + portForward.getLocalPort();
        return new RegistryConnection(client, portForward, httpClient, baseUrl);
    }

    private void deleteManifests(RegistryConnection connection, String repository, List<TagInfo> tags) throws Exception {
        Set<String> digests = tags.stream().map(TagInfo::digest).collect(Collectors.toSet());
        for (String digest : digests) {
            deleteManifest(connection.httpClient(), connection.baseUrl(), repository, digest);
        }
    }

    private void deleteManifest(HttpClient httpClient, String baseUrl, String repository, String digest) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v2/" + repository + "/manifests/" + digest))
                .timeout(HTTP_TIMEOUT)
                .DELETE()
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        // 202 = deleted, 404 = already gone (idempotent — safe to treat as success)
        if (response.statusCode() != 202 && response.statusCode() != 404) {
            throw new IllegalStateException("Unexpected status " + response.statusCode() + " deleting manifest " + digest);
        }
    }

    private void runGarbageCollect(KubernetesClient client) throws Exception {
        String podName = findRegistryPodName(client);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        try (ExecWatch watch = client.pods().inNamespace(REGISTRY_NAMESPACE).withName(podName)
                .writingOutput(stdout)
                .writingError(stderr)
                .exec("/bin/registry", "garbage-collect", REGISTRY_CONFIG_PATH, "--delete-untagged")) {

            Integer exitCode = watch.exitCode().get(GARBAGE_COLLECT_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            log.info("registry garbage-collect on {}: exit={}, output={}", podName, exitCode, stdout);
            if (exitCode == null || exitCode != 0) {
                throw new IllegalStateException("garbage-collect exited with code " + exitCode + ": " + stderr);
            }
        }
    }

    private void deleteRepositoryDirectory(KubernetesClient client, String repository) throws Exception {
        String podName = findRegistryPodName(client);
        String repoPath = REGISTRY_REPOSITORIES_PATH + repository;

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        try (ExecWatch watch = client.pods().inNamespace(REGISTRY_NAMESPACE).withName(podName)
                .writingOutput(stdout)
                .writingError(stderr)
                .exec("rm", "-rf", repoPath)) {

            Integer exitCode = watch.exitCode().get(GARBAGE_COLLECT_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            log.info("Deleted repository directory {} from pod {}: exit={}", repoPath, podName, exitCode);
            if (exitCode == null || exitCode != 0) {
                throw new IllegalStateException("Failed to delete repository directory, exit=" + exitCode + ": " + stderr);
            }
        }
    }

    private String findRegistryPodName(KubernetesClient client) {
        return client.pods().inNamespace(REGISTRY_NAMESPACE)
                .withLabel(REGISTRY_POD_LABEL, "true")
                .list().getItems().stream()
                .findFirst()
                .map(pod -> pod.getMetadata().getName())
                .orElseThrow(() -> new IllegalStateException("Registry pod not found (label " + REGISTRY_POD_LABEL + "=true)"));
    }

    private record RegistryConnection(KubernetesClient client, LocalPortForward portForward, HttpClient httpClient, String baseUrl)
            implements AutoCloseable {

        @Override
        public void close() throws Exception {
            portForward.close();
            client.close();
        }
    }
}
