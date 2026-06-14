package io.greencap.k8s.kubernetes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.cluster.Cluster;
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
import java.util.List;

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

    private TagInfo fetchTagInfo(HttpClient httpClient, String baseUrl, String repository, String tag) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v2/" + repository + "/manifests/" + tag))
                    .timeout(HTTP_TIMEOUT)
                    .header("Accept", "application/vnd.docker.distribution.manifest.v2+json")
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
