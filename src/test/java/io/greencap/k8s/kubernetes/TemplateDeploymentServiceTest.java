package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.BuildRequest;
import io.greencap.k8s.kubernetes.dto.TemplateBuild;
import io.greencap.k8s.kubernetes.dto.TemplateManifest;
import io.greencap.k8s.kubernetes.dto.TemplateSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EnableKubernetesMockClient(crud = true)
class TemplateDeploymentServiceTest {

    static KubernetesClient client;

    private SampleCatalogService sampleCatalogService;
    private TemplateDeploymentService templateDeploymentService;
    private Cluster cluster;

    private static final TemplateSummary TEMPLATE = new TemplateSummary(
            "crud-flask-postgres", "CRUD in Python (Flask) + PostgreSQL", "A minimal CRUD application.",
            List.of("Python", "Flask", "PostgreSQL"), "crud-flask-postgres", "crud-flask-postgres");

    @BeforeEach
    void setup() {
        KubernetesClientFactory mockFactory = mock(KubernetesClientFactory.class);
        KubernetesClient spyClient = spy(client);
        doNothing().when(spyClient).close();
        when(mockFactory.buildClient(any())).thenReturn(spyClient);

        sampleCatalogService = mock(SampleCatalogService.class);
        RegistryService registryService = mock(RegistryService.class);
        templateDeploymentService = new TemplateDeploymentService(mockFactory, sampleCatalogService, registryService);

        cluster = new Cluster();
        cluster.setName("test-cluster");

        client.namespaces().list().getItems().forEach(ns ->
                client.namespaces().withName(ns.getMetadata().getName()).delete());
    }

    @Test
    void applyNamespace_createsNamespaceFromFirstResourceFileOnly() {
        String namespaceYaml = """
                apiVersion: v1
                kind: Namespace
                metadata:
                  name: crud-flask-postgres
                """;
        when(sampleCatalogService.fetchResourceFile(TEMPLATE, "namespace.yaml")).thenReturn(namespaceYaml);
        TemplateManifest manifest = new TemplateManifest(List.of("namespace.yaml", "postgres.yaml"), List.of());

        templateDeploymentService.applyNamespace(cluster, TEMPLATE, manifest);

        assertThat(client.namespaces().withName("crud-flask-postgres").get()).isNotNull();
        verify(sampleCatalogService, never()).fetchResourceFile(TEMPLATE, "postgres.yaml");
    }

    @Test
    void applyRemainingResources_skipsFirstFile_appliesTheRestInOrder() {
        when(sampleCatalogService.fetchResourceFile(TEMPLATE, "a.yaml")).thenReturn(configMap("cfg-a"));
        when(sampleCatalogService.fetchResourceFile(TEMPLATE, "b.yaml")).thenReturn(configMap("cfg-b"));
        client.namespaces().resource(new NamespaceBuilder()
                .withNewMetadata().withName("crud-flask-postgres").endMetadata().build()
        ).create();

        TemplateManifest manifest = new TemplateManifest(List.of("namespace.yaml", "a.yaml", "b.yaml"), List.of());
        templateDeploymentService.applyRemainingResources(cluster, TEMPLATE, manifest, Map.of());

        assertThat(client.configMaps().inNamespace("crud-flask-postgres").withName("cfg-a").get()).isNotNull();
        assertThat(client.configMaps().inNamespace("crud-flask-postgres").withName("cfg-b").get()).isNotNull();
        verify(sampleCatalogService, never()).fetchResourceFile(TEMPLATE, "namespace.yaml");
    }

    @Test
    void applyRemainingResources_conflictAbortsWithoutRollingBackEarlierFiles() {
        when(sampleCatalogService.fetchResourceFile(TEMPLATE, "a.yaml")).thenReturn(configMap("cfg-a"));
        when(sampleCatalogService.fetchResourceFile(TEMPLATE, "b.yaml")).thenReturn(configMap("cfg-existing"));

        client.namespaces().resource(new NamespaceBuilder()
                .withNewMetadata().withName("crud-flask-postgres").endMetadata().build()
        ).create();
        // Pre-existing resource that b.yaml collides with — simulates a second Deploy Template
        // attempt of the same Template.
        client.configMaps().inNamespace("crud-flask-postgres").resource(new ConfigMapBuilder()
                .withNewMetadata().withName("cfg-existing").withNamespace("crud-flask-postgres").endMetadata()
                .build()
        ).create();

        TemplateManifest manifest = new TemplateManifest(List.of("namespace.yaml", "a.yaml", "b.yaml"), List.of());

        assertThatThrownBy(() ->
                templateDeploymentService.applyRemainingResources(cluster, TEMPLATE, manifest, Map.of()))
                .isInstanceOf(KubernetesOperationException.class);

        // a.yaml's resource is left in place — no rollback on failure (same convention as
        // Import Compose / Deploy from Dockerfile).
        assertThat(client.configMaps().inNamespace("crud-flask-postgres").withName("cfg-a").get()).isNotNull();
    }

    @Test
    void substitutePlaceholders_replacesOnlySentinelsWithAMapping() {
        String content = "image: __BUILD__backend\nother: __BUILD__frontend\n";

        String resolved = templateDeploymentService.substitutePlaceholders(
                content, Map.of("backend", "localhost:5000/crud-flask-postgres/backend:latest"));

        assertThat(resolved)
                .contains("image: localhost:5000/crud-flask-postgres/backend:latest")
                .contains("other: __BUILD__frontend");
    }

    @Test
    void toBuildRequest_prefixesContextPathWithTemplatePath_leavesDockerfilePathAsDeclared() {
        TemplateBuild build = new TemplateBuild("backend", "app", "Dockerfile", "crud-flask-postgres/backend");

        BuildRequest request = templateDeploymentService.toBuildRequest(TEMPLATE, build);

        assertThat(request.gitRepositoryUrl()).isEqualTo("https://github.com/greencapk8s/greencap-templates");
        assertThat(request.branch()).isEqualTo("main");
        assertThat(request.contextPath()).isEqualTo("crud-flask-postgres/app");
        assertThat(request.dockerfilePath()).isEqualTo("Dockerfile");
        assertThat(request.repository()).isEqualTo("crud-flask-postgres/backend");
        assertThat(request.tag()).isEqualTo("latest");
    }

    @Test
    void builtImageReference_usesLocalRegistryPullHostAndLatestTag() {
        TemplateBuild build = new TemplateBuild("backend", "app", "Dockerfile", "crud-flask-postgres/backend");

        assertThat(templateDeploymentService.builtImageReference(build))
                .isEqualTo("localhost:5000/crud-flask-postgres/backend:latest");
    }

    private String configMap(String name) {
        return """
                apiVersion: v1
                kind: ConfigMap
                metadata:
                  name: %s
                  namespace: crud-flask-postgres
                """.formatted(name);
    }
}
