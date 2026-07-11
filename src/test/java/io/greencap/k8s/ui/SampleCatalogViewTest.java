package io.greencap.k8s.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import io.greencap.k8s.KaribuTest;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.RegistryService;
import io.greencap.k8s.kubernetes.SampleCatalogService;
import io.greencap.k8s.kubernetes.TemplateDeploymentService;
import io.greencap.k8s.kubernetes.dto.TemplateManifest;
import io.greencap.k8s.kubernetes.dto.TemplateSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SampleCatalogViewTest extends KaribuTest {

    @Mock private ClusterContext clusterContext;
    @Mock private SampleCatalogService sampleCatalogService;
    @Mock private TemplateDeploymentService templateDeploymentService;
    @Mock private RegistryService registryService;
    @Mock private ObservabilityService observabilityService;

    private static final TemplateSummary TEMPLATE = new TemplateSummary(
            "crud-flask-postgres", "CRUD in Python (Flask) + PostgreSQL",
            "A minimal CRUD application with a Python (Flask) backend and PostgreSQL.",
            List.of("Python", "Flask", "PostgreSQL"), "crud-flask-postgres", "crud-flask-postgres");

    private SampleCatalogView view;

    @BeforeEach
    void setupView() {
        Cluster cluster = new Cluster();
        cluster.setName("test-cluster");
        when(clusterContext.getCluster()).thenReturn(cluster);

        loginAs("testuser");
        view = new SampleCatalogView(clusterContext, sampleCatalogService, templateDeploymentService,
                registryService, observabilityService);
    }

    @Test
    void notInstalledTemplate_showsDeployButton_noInstalledBadge() {
        when(sampleCatalogService.fetchCatalog()).thenReturn(List.of(TEMPLATE));
        when(sampleCatalogService.isInstalled(any(), any())).thenReturn(false);

        clickRefresh();

        assertThat(_find(view, Button.class, s -> s.withText("Deploy"))).isNotEmpty();
        assertThat(_find(view, Span.class, s -> s.withText("Installed"))).isEmpty();
    }

    @Test
    void installedTemplate_hidesDeployButton_showsInstalledBadge() {
        when(sampleCatalogService.fetchCatalog()).thenReturn(List.of(TEMPLATE));
        when(sampleCatalogService.isInstalled(any(), any())).thenReturn(true);

        clickRefresh();

        assertThat(_find(view, Button.class, s -> s.withText("Deploy"))).isEmpty();
        assertThat(_find(view, Span.class, s -> s.withText("Installed"))).isNotEmpty();
    }

    @Test
    void clickingDeploy_opensReadOnlyPreview_doesNotDeployBeforeConfirmation() {
        when(sampleCatalogService.fetchCatalog()).thenReturn(List.of(TEMPLATE));
        when(sampleCatalogService.isInstalled(any(), any())).thenReturn(false);
        clickRefresh();

        TemplateManifest manifest = new TemplateManifest(List.of("namespace.yaml"), List.of());
        String previewYaml = "apiVersion: v1\nkind: Namespace\nmetadata:\n  name: crud-flask-postgres\n";
        when(sampleCatalogService.fetchManifest(TEMPLATE)).thenReturn(manifest);
        when(sampleCatalogService.buildPreview(TEMPLATE, manifest)).thenReturn(previewYaml);

        _click(_get(view, Button.class, s -> s.withText("Deploy")));

        Dialog dialog = waitForDialogWithButton("Deploy");

        assertThat(_get(dialog, Pre.class).getText()).contains("kind: Namespace");
        assertThat(_find(dialog, TextArea.class)).isEmpty();
        verifyNoInteractions(templateDeploymentService);
    }

    private void clickRefresh() {
        _click(_get(view, Button.class, s -> s.withPredicate(b ->
                "Refresh".equals(b.getElement().getAttribute("title")))));
    }

    // The preview dialog is populated in the background (AsyncTasks.execute + ui.access), so a
    // short poll is needed instead of asserting immediately after the click — same reasoning as
    // the production waitForBuild loop in this view, without adding a new test dependency.
    private Dialog waitForDialogWithButton(String buttonText) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(2));
        while (Instant.now().isBefore(deadline)) {
            var dialogs = _find(Dialog.class);
            if (!dialogs.isEmpty() && !_find(dialogs.get(0), Button.class, s -> s.withText(buttonText)).isEmpty()) {
                return dialogs.get(0);
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("Dialog with a '" + buttonText + "' button did not appear in time");
    }
}
