package io.greencap.k8s.kubernetes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.dto.TemplateManifest;
import io.greencap.k8s.kubernetes.dto.TemplateSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Parsing tests run against fixture strings, isolated from the HTTP fetch (SampleCatalogService
// never hits the real greencap-templates repository here). isInstalled is the one method that
// genuinely talks to a KubernetesClient, so it uses the mock API server like WorkloadServiceTest.
@EnableKubernetesMockClient(crud = true)
class SampleCatalogServiceTest {

    static KubernetesClient client;

    private SampleCatalogService sampleCatalogService;
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

        sampleCatalogService = new SampleCatalogService(mockFactory, new ObjectMapper());

        cluster = new Cluster();
        cluster.setName("test-cluster");

        client.namespaces().list().getItems().forEach(ns ->
                client.namespaces().withName(ns.getMetadata().getName()).delete());
    }

    @Test
    void parseCatalog_mapsEveryFieldFromJsonIndex() throws Exception {
        String json = """
                [
                  {
                    "id": "crud-flask-postgres",
                    "title": "CRUD in Python (Flask) + PostgreSQL",
                    "description": "A minimal CRUD application.",
                    "technologies": ["Python", "Flask", "PostgreSQL"],
                    "path": "crud-flask-postgres",
                    "namespace": "crud-flask-postgres"
                  }
                ]
                """;

        List<TemplateSummary> templates = sampleCatalogService.parseCatalog(json);

        assertThat(templates).containsExactly(TEMPLATE);
    }

    @Test
    void parseManifest_readsResourcesAndBuilds() throws Exception {
        String yaml = """
                resources:
                  - namespace.yaml
                  - postgres.yaml
                  - backend.yaml
                builds:
                  - name: backend
                    contextPath: app
                    dockerfilePath: Dockerfile
                    image: crud-flask-postgres/backend
                """;

        TemplateManifest manifest = sampleCatalogService.parseManifest(yaml);

        assertThat(manifest.resources()).containsExactly("namespace.yaml", "postgres.yaml", "backend.yaml");
        assertThat(manifest.builds()).hasSize(1);
        assertThat(manifest.builds().get(0).name()).isEqualTo("backend");
        assertThat(manifest.builds().get(0).contextPath()).isEqualTo("app");
        assertThat(manifest.builds().get(0).dockerfilePath()).isEqualTo("Dockerfile");
        assertThat(manifest.builds().get(0).image()).isEqualTo("crud-flask-postgres/backend");
    }

    @Test
    void parseManifest_defaultsBuildsToEmptyListWhenAbsent() throws Exception {
        String yaml = """
                resources:
                  - namespace.yaml
                """;

        TemplateManifest manifest = sampleCatalogService.parseManifest(yaml);

        assertThat(manifest.builds()).isEmpty();
    }

    @Test
    void isInstalled_falseWhenTemplateNamespaceDoesNotExist() {
        assertThat(sampleCatalogService.isInstalled(cluster, TEMPLATE)).isFalse();
    }

    @Test
    void isInstalled_trueWhenTemplateNamespaceAlreadyExists() {
        client.namespaces().resource(new NamespaceBuilder()
                .withNewMetadata().withName("crud-flask-postgres").endMetadata()
                .build()
        ).create();

        assertThat(sampleCatalogService.isInstalled(cluster, TEMPLATE)).isTrue();
    }
}
