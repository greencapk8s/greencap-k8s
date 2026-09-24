package io.greencap.k8s.ui;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import io.greencap.k8s.KaribuTest;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.NetworkingService;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.RegistryService;
import io.greencap.k8s.kubernetes.StorageService;
import io.greencap.k8s.kubernetes.compose.ComposeParser;
import io.greencap.k8s.kubernetes.compose.ImportComposeService;
import io.greencap.k8s.kubernetes.dto.ComposeImportRequest;
import io.greencap.k8s.kubernetes.dto.ImportComposeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportComposeViewTest extends KaribuTest {

    private static final String COMPOSE_WITH_TWO_EXPOSABLE_SERVICES = """
            services:
              api:
                image: nginx:latest
                ports: ["8080:8080"]
              worker:
                image: busybox:latest
              web:
                image: nginx:latest
                ports: ["80:80"]
            """;
    private static final String EXPOSE_LABEL = "Expose application externally (Ingress)";

    @Mock private ClusterContext clusterContext;
    @Mock private ImportComposeService importComposeService;
    @Mock private RegistryService registryService;
    @Mock private ObservabilityService observabilityService;
    @Mock private StorageService storageService;
    @Mock private NetworkingService networkingService;
    @Mock private UserService userService;

    private final ComposeParser composeParser = spy(new ComposeParser());
    private ImportComposeView view;

    @BeforeEach
    void setupView() {
        lenient().when(clusterContext.getCluster()).thenReturn(null);
        loginAs("GLOBAL_DEPLOY");
        view = new ImportComposeView(clusterContext, composeParser, importComposeService,
                registryService, observabilityService, storageService, networkingService, userService);
        view.beforeEnter(null);
    }

    @Test
    void gitIsTheDefaultSourceAndThePickerStaysHidden() {
        assertThat(originSelector().getValue()).isEqualTo(BuildContextOrigin.GIT_REPOSITORY);
        assertThat(visibleFieldLabels()).contains("Git repository URL", "Branch", "Path to docker-compose.yml");
        assertThat(_find(view, BuildContextPicker.class)).isEmpty();
    }

    // The Compose path survives the switch to a local folder: it is how the user points at a file whose
    // name or location departs from the default, now that picking the file on its own is gone.
    @Test
    void choosingLocalFolderHidesTheGitFieldsButKeepsTheComposePath() {
        selectLocalOrigin();

        assertThat(visibleFieldLabels()).doesNotContain("Git repository URL", "Branch");
        assertThat(visibleFieldLabels()).contains("Path to docker-compose.yml", "Target namespace");
        assertThat(_find(view, BuildContextPicker.class)).hasSize(1);
    }

    @Test
    void switchingOriginLeavesNoResidualStateBehind() {
        gitUrlField().setValue("https://github.com/usuario/repo");

        selectLocalOrigin();
        BuildContextPicker picker = picker();
        picker.contextPacked("minha-app", 12, 4096);
        assertThat(picker.getFolderName()).contains("minha-app");

        originSelector().setValue(BuildContextOrigin.GIT_REPOSITORY);
        assertThat(picker.getFolderName()).isEmpty();
        assertThat(gitUrlField().getValue()).isEmpty();
    }

    @Test
    void localOriginDoesNotAdvanceWhileNoFolderIsSelected() {
        selectLocalOrigin();
        namespaceField().setValue("minha-app");

        clickNext();

        assertThat(visibleFieldLabels()).contains("Target namespace");
    }

    @Test
    void namespaceIsSuggestedFromTheSelectedFolderName() {
        selectLocalOrigin();

        picker().contextPacked("Minha Pasta", 12, 4096);

        assertThat(namespaceField().getValue()).isEqualTo("minha-pasta");
    }

    // The picker must sit outside the container each step replaces: detaching it discards the uploaded
    // archive and the folder the browser still holds, so the Build would start with nothing.
    @Test
    void thePickerLivesOutsideTheContainerReplacedOnEveryStep() {
        selectLocalOrigin();

        assertThat(picker().getParent().flatMap(Component::getParent)).contains(view);
    }

    @Test
    void onlyServicesWithPortsOfferExposureAndItStartsUnchecked() {
        reachReviewStep("shop");

        assertThat(exposeCheckbox("api").getValue()).isFalse();
        assertThat(exposeCheckbox("web").getValue()).isFalse();
        assertThat(_find(panelOf("worker"), Checkbox.class)).isEmpty();
    }

    @Test
    void exposingAServiceSuggestsItsHostAndPreselectsTheFirstIngressClass() {
        reachReviewStep("shop");

        exposeCheckbox("api").setValue(true);

        assertThat(hostField("api").getValue()).isEqualTo("api.shop.greencap.local");
        assertThat(ingressClassField("api").getValue()).isEqualTo("nginx");
    }

    @Test
    void uncheckingExposureHidesTheHostAndIngressClass() {
        reachReviewStep("shop");
        exposeCheckbox("api").setValue(true);

        exposeCheckbox("api").setValue(false);

        assertThat(_find(panelOf("api"), TextField.class)).isEmpty();
        assertThat(_find(panelOf("api"), ComboBox.class)).isEmpty();
    }

    @Test
    void theRequestCarriesAnIngressOnlyForExposedServices() {
        reachReviewStep("shop");
        exposeCheckbox("api").setValue(true);
        hostField("api").setValue("shop-api.greencap.local");

        clickNext();

        Map<String, ComposeImportRequest.ServiceConfig> configs = capturedRequest().serviceConfigs().stream()
                .collect(Collectors.toMap(ComposeImportRequest.ServiceConfig::serviceName, config -> config));
        assertThat(configs.get("api").ingress())
                .isEqualTo(new ComposeImportRequest.IngressConfig("shop-api.greencap.local", "nginx"));
        assertThat(configs.get("web").isExposed()).isFalse();
        assertThat(configs.get("worker").isExposed()).isFalse();
    }

    // The Git fetch is stubbed with the parsed Compose so the wizard reaches the review step without
    // network; the IngressClasses load in the background and must land before the panels are built.
    private void reachReviewStep(String namespace) {
        Cluster cluster = new Cluster();
        when(clusterContext.getCluster()).thenReturn(cluster);
        when(networkingService.listIngressClassNames(cluster)).thenReturn(List.of("nginx", "traefik"));
        doReturn(composeParser.parse(COMPOSE_WITH_TWO_EXPOSABLE_SERVICES))
                .when(composeParser).fetch(anyString(), anyString(), anyString());
        // A failed result keeps the wizard on its inline result screen instead of navigating away.
        lenient().when(importComposeService.provision(any(), any(), any())).thenReturn(new ImportComposeResult(
                List.of(new ImportComposeResult.ServiceResult("api", List.of(), "stubbed"))));
        view.beforeEnter(null);
        verify(networkingService, timeout(2000)).listIngressClassNames(cluster);

        gitUrlField().setValue("https://github.com/usuario/shop");
        fieldWithLabel("Branch").setValue("main");
        fieldWithLabel("Path to docker-compose.yml").setValue("docker-compose.yml");
        namespaceField().setValue(namespace);
        clickNext();

        awaitUntil(() -> !_find(view, H4.class, spec -> spec.withText("api")).isEmpty());
    }

    private ComposeImportRequest capturedRequest() {
        ArgumentCaptor<ComposeImportRequest> request = ArgumentCaptor.forClass(ComposeImportRequest.class);
        verify(importComposeService, timeout(2000)).provision(any(), any(), request.capture());
        return request.getValue();
    }

    private Component panelOf(String serviceName) {
        return _get(view, H4.class, spec -> spec.withText(serviceName)).getParent().orElseThrow();
    }

    private Checkbox exposeCheckbox(String serviceName) {
        return _get(panelOf(serviceName), Checkbox.class, spec -> spec.withLabel(EXPOSE_LABEL));
    }

    private TextField hostField(String serviceName) {
        return _get(panelOf(serviceName), TextField.class, spec -> spec.withLabel("Host"));
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> ingressClassField(String serviceName) {
        return _get(panelOf(serviceName), ComboBox.class, spec -> spec.withLabel("Ingress class"));
    }

    // Background tasks hand their result back through ui.access, which Karibu only runs once the
    // UI queue is drained, so the wait drives the queue between checks.
    private void awaitUntil(BooleanSupplier condition) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(2));
        while (Instant.now().isBefore(deadline)) {
            MockVaadin.clientRoundtrip();
            if (condition.getAsBoolean()) return;
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("Condition not met in time");
    }

    private void selectLocalOrigin() {
        originSelector().setValue(BuildContextOrigin.LOCAL_FOLDER);
    }

    private void clickNext() {
        _get(view, Button.class, spec -> spec.withPredicate(
                button -> "Next".equals(button.getText()) || "Deploy".equals(button.getText()))).click();
    }

    @SuppressWarnings("unchecked")
    private RadioButtonGroup<BuildContextOrigin> originSelector() {
        return _get(view, RadioButtonGroup.class);
    }

    private BuildContextPicker picker() {
        return _get(view, BuildContextPicker.class);
    }

    private TextField gitUrlField() {
        return fieldWithLabel("Git repository URL");
    }

    private TextField namespaceField() {
        return fieldWithLabel("Target namespace");
    }

    private TextField fieldWithLabel(String label) {
        return _get(view, TextField.class, spec -> spec.withPredicate(field -> label.equals(field.getLabel())));
    }

    private List<String> visibleFieldLabels() {
        return _find(view, TextField.class).stream().map(TextField::getLabel).toList();
    }
}
