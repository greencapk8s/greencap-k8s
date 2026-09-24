package io.greencap.k8s.kubernetes.compose;

import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.KubernetesClientFactory;
import io.greencap.k8s.kubernetes.dto.ComposeImportRequest;
import io.greencap.k8s.kubernetes.dto.ImportComposeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@EnableKubernetesMockClient(crud = true)
class ImportComposeServiceTest {

    static KubernetesClient client;

    private ImportComposeService importComposeService;
    private Cluster cluster;

    @BeforeEach
    void setup() {
        KubernetesClientFactory mockFactory = mock(KubernetesClientFactory.class);
        KubernetesClient spyClient = spy(client);
        doNothing().when(spyClient).close();
        when(mockFactory.buildClient(any())).thenReturn(spyClient);
        importComposeService = new ImportComposeService(mockFactory);

        cluster = new Cluster();
        cluster.setName("test-cluster");
        cluster.setKubeconfigContent("encrypted");
    }

    @Test
    void anExposedServiceGetsAnIngressRoutingToItsServiceOnTheFirstPort() {
        ComposeDocument document = composeWith(serviceWithPorts("api", 8080, 9090));
        ComposeImportRequest request = requestFor("shop",
                exposed("api", new ComposeImportRequest.IngressConfig("api.shop.greencap.local", "nginx")));

        ImportComposeResult result = importComposeService.provision(cluster, document, request);

        Ingress ingress = client.network().v1().ingresses().inNamespace("shop").withName("api-ingress").get();
        assertThat(ingress).isNotNull();
        assertThat(ingress.getSpec().getIngressClassName()).isEqualTo("nginx");
        IngressRule rule = ingress.getSpec().getRules().get(0);
        assertThat(rule.getHost()).isEqualTo("api.shop.greencap.local");
        HTTPIngressPath path = rule.getHttp().getPaths().get(0);
        assertThat(path.getPath()).isEqualTo("/");
        assertThat(path.getPathType()).isEqualTo("Prefix");
        assertThat(path.getBackend().getService().getName()).isEqualTo("api");
        assertThat(path.getBackend().getService().getPort().getNumber()).isEqualTo(8080);
        assertThat(ingress.getMetadata().getLabels())
                .containsEntry("app.kubernetes.io/part-of", "shop")
                .containsEntry("app.kubernetes.io/component", "api");
        assertThat(result.serviceResults().get(0).createdResources()).contains("Ingress: api-ingress");
        assertThat(result.isFullSuccess()).isTrue();
    }

    @Test
    void aServiceNotExposedGetsNoIngress() {
        ComposeDocument document = composeWith(serviceWithPorts("web", 80));
        ComposeImportRequest request = requestFor("blog", notExposed("web"));

        ImportComposeResult result = importComposeService.provision(cluster, document, request);

        assertThat(client.network().v1().ingresses().inNamespace("blog").list().getItems()).isEmpty();
        assertThat(result.serviceResults().get(0).createdResources())
                .containsExactly("Deployment: web", "Service: web");
    }

    @Test
    void anIngressThatAlreadyExistsIsReportedAsFailedWithWhatWasCreated() {
        client.network().v1().ingresses().inNamespace("store").resource(new IngressBuilder()
                .withNewMetadata().withName("api-ingress").withNamespace("store").endMetadata()
                .build()).create();
        ComposeDocument document = composeWith(serviceWithPorts("api", 8080));
        ComposeImportRequest request = requestFor("store",
                exposed("api", new ComposeImportRequest.IngressConfig("api.store.greencap.local", "nginx")));

        ImportComposeResult result = importComposeService.provision(cluster, document, request);

        ImportComposeResult.ServiceResult serviceResult = result.serviceResults().get(0);
        assertThat(serviceResult.isSuccess()).isFalse();
        assertThat(serviceResult.failureMessage()).startsWith("Ingress failed: ");
        assertThat(serviceResult.createdResources()).containsExactly("Deployment: api", "Service: api");
    }

    private ComposeDocument composeWith(ComposeDocument.ParsedService service) {
        return new ComposeDocument(List.of(service), List.of());
    }

    private ComposeDocument.ParsedService serviceWithPorts(String name, Integer... ports) {
        return new ComposeDocument.ParsedService(name, "nginx:latest", null,
                List.of(ports), List.of(), List.of(), List.of());
    }

    private ComposeImportRequest requestFor(String namespace, ComposeImportRequest.ServiceConfig config) {
        return new ComposeImportRequest(namespace, List.of(config));
    }

    private ComposeImportRequest.ServiceConfig exposed(String name, ComposeImportRequest.IngressConfig ingress) {
        return new ComposeImportRequest.ServiceConfig(name, "nginx:latest", List.of(), ingress);
    }

    private ComposeImportRequest.ServiceConfig notExposed(String name) {
        return new ComposeImportRequest.ServiceConfig(name, "nginx:latest", List.of(), null);
    }
}
