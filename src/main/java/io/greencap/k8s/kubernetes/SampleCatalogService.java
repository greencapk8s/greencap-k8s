package io.greencap.k8s.kubernetes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.TemplateBuild;
import io.greencap.k8s.kubernetes.dto.TemplateManifest;
import io.greencap.k8s.kubernetes.dto.TemplateSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

// Reads the Sample Catalog from greencap-templates — the official, GreenCap-curated repository of
// Templates. Fetched over plain HTTP (raw content), with no Git client and no caching layer: every
// call re-fetches the current state of the "main" branch. See ADR 0015 for the reasoning.
@Slf4j
@Service
@RequiredArgsConstructor
public class SampleCatalogService {

    // Curated and controlled by the GreenCap team, unlike the user-supplied Git repository fields
    // in Deploy from Dockerfile/Import Compose — fixed on purpose, no environment override.
    private static final String CATALOG_REPOSITORY_RAW_URL =
            "https://raw.githubusercontent.com/greencapk8s/greencap-templates/main";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);

    private final KubernetesClientFactory clientFactory;
    private final ObjectMapper objectMapper;
    private final YAMLMapper yamlMapper = new YAMLMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    public List<TemplateSummary> fetchCatalog() {
        try {
            String content = fetchContent(CATALOG_REPOSITORY_RAW_URL + "/catalog.json");
            return parseCatalog(content);
        } catch (Exception e) {
            log.error("Failed to fetch Sample Catalog index: {}", e.getMessage());
            throw new KubernetesOperationException("Failed to load the Sample Catalog", e);
        }
    }

    public TemplateManifest fetchManifest(TemplateSummary template) {
        try {
            String content = fetchContent(templateFileUrl(template, "template.yaml"));
            return parseManifest(content);
        } catch (Exception e) {
            log.error("Failed to fetch manifest for Template {}: {}", template.id(), e.getMessage());
            throw new KubernetesOperationException("Failed to load Template " + template.id(), e);
        }
    }

    // Package-private parsing seams, isolated from the HTTP fetch above, so the catalog.json/
    // template.yaml formats can be tested directly against fixture strings.
    List<TemplateSummary> parseCatalog(String json) throws com.fasterxml.jackson.core.JsonProcessingException {
        return List.of(objectMapper.readValue(json, TemplateSummary[].class));
    }

    TemplateManifest parseManifest(String yaml) throws com.fasterxml.jackson.core.JsonProcessingException {
        TemplateManifest manifest = yamlMapper.readValue(yaml, TemplateManifest.class);
        List<TemplateBuild> builds = manifest.builds() == null ? List.of() : manifest.builds();
        return new TemplateManifest(manifest.resources(), builds);
    }

    public String fetchResourceFile(TemplateSummary template, String fileName) {
        try {
            return fetchContent(templateFileUrl(template, fileName));
        } catch (Exception e) {
            log.error("Failed to fetch resource file {} for Template {}: {}", fileName, template.id(), e.getMessage());
            throw new KubernetesOperationException("Failed to load resource file " + fileName + " of Template " + template.id(), e);
        }
    }

    // Read-only preview shown before Deploy Template — resource files concatenated in the order
    // declared by template.yaml, sentinel image placeholders left as-is (the build hasn't run yet).
    public String buildPreview(TemplateSummary template, TemplateManifest manifest) {
        return manifest.resources().stream()
                .map(fileName -> fetchResourceFile(template, fileName))
                .collect(Collectors.joining("\n---\n"));
    }

    // "Installed" is a property of the Cluster, not of the deploying User — every Template declares
    // a fixed Namespace name in catalog.json, so its existence is what the card badge reflects.
    public boolean isInstalled(Cluster cluster, TemplateSummary template) {
        try (KubernetesClient client = clientFactory.buildClient(cluster)) {
            return client.namespaces().withName(template.namespace()).get() != null;
        } catch (Exception e) {
            log.warn("Failed to check installed state for Template {} on cluster {}: {}",
                    template.id(), cluster.getName(), e.getMessage());
            return false;
        }
    }

    private String templateFileUrl(TemplateSummary template, String fileName) {
        return CATALOG_REPOSITORY_RAW_URL + "/" + template.path() + "/" + fileName;
    }

    private String fetchContent(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " fetching " + url);
        }
        return response.body();
    }
}
