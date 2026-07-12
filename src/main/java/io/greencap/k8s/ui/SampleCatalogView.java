package io.greencap.k8s.ui;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.RegistryService;
import io.greencap.k8s.kubernetes.SampleCatalogService;
import io.greencap.k8s.kubernetes.TemplateDeploymentService;
import io.greencap.k8s.kubernetes.dto.TemplateBuild;
import io.greencap.k8s.kubernetes.dto.TemplateManifest;
import io.greencap.k8s.kubernetes.dto.TemplateSummary;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

// Developer Experience → Templates Catalog: card list of Templates available for one-click deployment.
// No permission gate (ADR 0013 — RBAC replaces the internal permission system): the view is visible
// to any authenticated user, and the Kubernetes API itself authorizes (or 403s) the Deploy operation.
// See ADR 0015 and CONTEXT.md ("Templates Catalog", "Template", "Deploy Template") for the full design.
@Slf4j
@Route(value = "developer-experience/sample-catalog", layout = MainLayout.class)
@PageTitle("Templates Catalog — GreenCap K8s")
@PermitAll
public class SampleCatalogView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Templates Catalog";
    private static final String HELP_TEXT = "Deploy complete study applications (Templates) with one click, " +
            "from the official greencap-templates catalog. Each Template creates its own Namespace — " +
            "already deployed Templates show an Installed badge and cannot be deployed again in the same Cluster.";

    private static final String BUILD_NAMESPACE = "greencap-system";
    private static final String KANIKO_CONTAINER = "kaniko";
    private static final int BUILD_TAIL_LINES = 1000;
    private static final int POLL_INTERVAL_SECONDS = 3;

    private final ClusterContext clusterContext;
    private final SampleCatalogService sampleCatalogService;
    private final TemplateDeploymentService templateDeploymentService;
    private final RegistryService registryService;
    private final ObservabilityService observabilityService;

    private final VerticalLayout noClusterMessage;
    private final VerticalLayout clusterErrorMessage;
    private final ProgressBar progress = new ProgressBar();
    private final Div cardContainer = new Div();

    private boolean loading = false;
    private ScheduledFuture<?> pollTask;

    public SampleCatalogView(ClusterContext clusterContext,
                              SampleCatalogService sampleCatalogService,
                              TemplateDeploymentService templateDeploymentService,
                              RegistryService registryService,
                              ObservabilityService observabilityService) {
        this.clusterContext = clusterContext;
        this.sampleCatalogService = sampleCatalogService;
        this.templateDeploymentService = templateDeploymentService;
        this.registryService = registryService;
        this.observabilityService = observabilityService;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        clusterErrorMessage = UiConstants.buildClusterUnreachableMessage();

        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.setWidthFull();

        cardContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(300px, 1fr))")
                .set("gap", "var(--lumo-space-m)")
                .set("width", "100%")
                .set("align-items", "stretch");

        HorizontalLayout header = UiConstants.buildSectionHeader(
                "Templates Catalog", this::forceReload, HELP_TITLE, HELP_TEXT);

        add(header, noClusterMessage, clusterErrorMessage, progress, cardContainer);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        clusterErrorMessage.setVisible(false);
        cardContainer.setVisible(hasCluster);

        if (hasCluster) {
            loadAsync(UI.getCurrent());
        }
    }

    // Called by MainLayout's auto-refresh timer — no-op, same reasoning as OperatorCatalogView:
    // the catalog rarely changes and reloading it re-checks Installed for every Template.
    @Override
    public void refresh() {}

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopPolling();
        super.onDetach(detachEvent);
    }

    // Synchronous on purpose, unlike beforeEnter's loadAsync — same pattern as
    // NamespacesView.loadNamespaces(), wired to the manual Refresh button so it can be driven
    // deterministically from tests without racing a background virtual thread.
    private boolean forceReload() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        try {
            List<TemplateSummary> templates = sampleCatalogService.fetchCatalog();
            Map<String, Boolean> installedByTemplateId = new LinkedHashMap<>();
            for (TemplateSummary template : templates) {
                installedByTemplateId.put(template.id(), sampleCatalogService.isInstalled(cluster, template));
            }
            clusterErrorMessage.setVisible(false);
            renderCards(templates, installedByTemplateId);
            cardContainer.setVisible(true);
            return true;
        } catch (KubernetesOperationException e) {
            log.debug("Failed to load Templates Catalog for cluster {}: {}", cluster.getName(), e.getMessage());
            cardContainer.removeAll();
            clusterErrorMessage.setVisible(true);
            cardContainer.setVisible(false);
            return false;
        }
    }

    // Re-fetched on every visit — the Templates Catalog index has no caching layer (ADR 0015).
    private void loadAsync(UI ui) {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null || loading) return;
        loading = true;

        ui.access(() -> {
            progress.setVisible(true);
            cardContainer.setVisible(false);
        });

        AsyncTasks.execute(() -> {
            try {
                List<TemplateSummary> templates = sampleCatalogService.fetchCatalog();
                Map<String, Boolean> installedByTemplateId = new LinkedHashMap<>();
                for (TemplateSummary template : templates) {
                    installedByTemplateId.put(template.id(), sampleCatalogService.isInstalled(cluster, template));
                }
                ui.access(() -> {
                    loading = false;
                    clusterErrorMessage.setVisible(false);
                    renderCards(templates, installedByTemplateId);
                    progress.setVisible(false);
                    cardContainer.setVisible(true);
                });
            } catch (KubernetesOperationException e) {
                log.debug("Failed to load Templates Catalog for cluster {}: {}", cluster.getName(), e.getMessage());
                ui.access(() -> {
                    loading = false;
                    cardContainer.removeAll();
                    progress.setVisible(false);
                    clusterErrorMessage.setVisible(true);
                    cardContainer.setVisible(false);
                });
            }
        });
    }

    private void renderCards(List<TemplateSummary> templates, Map<String, Boolean> installedByTemplateId) {
        cardContainer.removeAll();
        for (TemplateSummary template : templates) {
            cardContainer.add(buildCard(template, installedByTemplateId.getOrDefault(template.id(), false)));
        }
    }

    private Div buildCard(TemplateSummary template, boolean installed) {
        Div card = new Div();
        card.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("transition", "box-shadow 0.15s ease-in-out, transform 0.15s ease-in-out");
        card.getElement().addEventListener("mouseenter", e -> card.getStyle()
                .set("box-shadow", "var(--lumo-box-shadow-m)")
                .set("transform", "translateY(-2px)"));
        card.getElement().addEventListener("mouseleave", e -> card.getStyle()
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("transform", "translateY(0)"));

        Div iconBadge = new Div(VaadinIcon.CUBES.create());
        iconBadge.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("width", "40px")
                .set("height", "40px")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background", "var(--lumo-primary-color-10pct)")
                .set("color", "var(--lumo-primary-text-color)")
                .set("flex-shrink", "0");

        H4 title = new H4(template.title());
        title.getStyle().set("margin", "0").set("line-height", "1.3");

        HorizontalLayout heading = new HorizontalLayout(iconBadge, title);
        heading.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        heading.setSpacing(true);
        heading.setPadding(false);
        heading.setWidthFull();

        Paragraph description = new Paragraph(template.description());
        description.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        description.getStyle().set("margin", "0").set("flex-grow", "1");

        Div techBadges = new Div();
        techBadges.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "var(--lumo-space-xs)");
        for (String technology : template.technologies()) {
            Span chip = new Span(technology);
            chip.getStyle()
                    .set("background", "var(--lumo-contrast-10pct)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("padding", "2px 10px")
                    .set("border-radius", "var(--lumo-border-radius-l)")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("font-weight", "500");
            techBadges.add(chip);
        }

        Hr divider = new Hr();
        divider.getStyle()
                .set("width", "100%")
                .set("margin", "0")
                .set("border-color", "var(--lumo-contrast-10pct)");

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        footer.setJustifyContentMode(JustifyContentMode.END);

        if (installed) {
            Span installedBadge = new Span(VaadinIcon.CHECK_CIRCLE.create(), new Span("Installed"));
            installedBadge.getElement().getThemeList().add("badge");
            installedBadge.getElement().getThemeList().add("success");
            footer.add(installedBadge);
        } else {
            Button deployButton = new Button("Deploy", VaadinIcon.ROCKET.create(), e -> openPreviewDialog(template));
            deployButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            deployButton.setWidthFull();
            footer.add(deployButton);
            footer.expand(deployButton);
        }

        card.add(heading, description, techBadges, divider, footer);
        return card;
    }

    private void openPreviewDialog(TemplateSummary template) {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Deploy " + template.title());
        dialog.setWidth("720px");
        dialog.setModal(true);
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        ProgressBar loadingBar = new ProgressBar();
        loadingBar.setIndeterminate(true);
        VerticalLayout loadingContent = new VerticalLayout(new Span("Loading preview..."), loadingBar);
        loadingContent.setPadding(false);
        dialog.add(loadingContent);
        dialog.open();

        UI ui = UI.getCurrent();
        AsyncTasks.execute(() -> {
            try {
                TemplateManifest manifest = sampleCatalogService.fetchManifest(template);
                String preview = sampleCatalogService.buildPreview(template, manifest);
                ui.access(() -> showPreview(dialog, cluster, template, manifest, preview));
            } catch (KubernetesOperationException e) {
                ui.access(() -> {
                    dialog.close();
                    showError("Failed to load Template: " + e.getMessage());
                });
            }
        });
    }

    private void showPreview(Dialog dialog, Cluster cluster, TemplateSummary template,
                              TemplateManifest manifest, String preview) {
        dialog.removeAll();

        Pre previewArea = new Pre(preview);
        styleLogArea(previewArea);

        Span note = new Span("Read-only preview — Templates are curated and cannot be edited before deploying.");
        note.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        VerticalLayout content = new VerticalLayout(note, previewArea);
        content.setPadding(false);
        dialog.add(content);

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button deployButton = new Button("Deploy", e -> runDeploy(dialog, cluster, template, manifest));
        deployButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancelButton, deployButton);
    }

    private void runDeploy(Dialog dialog, Cluster cluster, TemplateSummary template, TemplateManifest manifest) {
        dialog.removeAll();
        dialog.getFooter().removeAll();

        Span statusBadge = new Span("Applying Namespace...");
        statusBadge.getElement().getThemeList().add("badge");
        statusBadge.getElement().getThemeList().add("contrast");

        Pre logArea = new Pre();
        styleLogArea(logArea);

        VerticalLayout content = new VerticalLayout(statusBadge, logArea);
        content.setPadding(false);
        dialog.add(content);

        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(closeButton);

        UI ui = UI.getCurrent();
        AsyncTasks.execute(() -> runDeployBackground(ui, statusBadge, logArea, cluster, template, manifest));
    }

    private void runDeployBackground(UI ui, Span statusBadge, Pre logArea,
                                      Cluster cluster, TemplateSummary template, TemplateManifest manifest) {
        try {
            templateDeploymentService.applyNamespace(cluster, template, manifest);

            Map<String, String> builtImagesByBuildName = new LinkedHashMap<>();
            for (TemplateBuild build : manifest.builds()) {
                ui.access(() -> updateStatusBadge(statusBadge, "Building " + build.name() + "...", "primary"));
                String jobName = templateDeploymentService.startBuild(cluster, template, build);
                boolean buildSuccess = waitForBuild(cluster, jobName, ui, logArea);
                if (!buildSuccess) {
                    ui.access(() -> {
                        updateStatusBadge(statusBadge, "Failed", "error");
                        showError("Build failed for " + build.name() + ". Check the logs above.");
                    });
                    return;
                }
                builtImagesByBuildName.put(build.name(), templateDeploymentService.builtImageReference(build));
            }

            ui.access(() -> updateStatusBadge(statusBadge, "Applying resources...", "contrast"));
            templateDeploymentService.applyRemainingResources(cluster, template, manifest, builtImagesByBuildName);

            ui.access(() -> {
                updateStatusBadge(statusBadge, "Complete", "success");
                Notification notification = Notification.show(
                        template.title() + " deployed", UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                MainLayout.refreshNamespaceSelector(ui);
                loadAsync(ui);
            });
        } catch (KubernetesOperationException e) {
            log.error("Failed to deploy Template {} on cluster {}: {}", template.id(), cluster.getName(), e.getMessage());
            ui.access(() -> {
                updateStatusBadge(statusBadge, "Failed", "error");
                showError(e.getMessage());
            });
        } finally {
            stopPolling();
        }
    }

    private boolean waitForBuild(Cluster cluster, String jobName, UI ui, Pre logArea) {
        final boolean[] finished = {false};
        final boolean[] success = {false};

        Runnable pollCommand = () -> {
            try {
                var progress = registryService.getBuildProgress(cluster, jobName);
                if (progress.podName() != null) {
                    fetchAndDisplayBuildLogs(cluster, progress.podName(), ui, logArea);
                }
                if (!"Running".equals(progress.status())) {
                    success[0] = "Complete".equals(progress.status());
                    finished[0] = true;
                    stopPolling();
                }
            } catch (Exception e) {
                log.warn("Error polling build progress for {}: {}", jobName, e.getMessage());
                finished[0] = true;
                stopPolling();
            }
        };
        pollTask = AsyncTasks.schedulePolling(pollCommand, Duration.ZERO, Duration.ofSeconds(POLL_INTERVAL_SECONDS));

        while (!finished[0]) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return success[0];
    }

    // Isolated from the Job status check, same reasoning as Deploy from Dockerfile: a transient
    // failure reading the Kaniko container's logs has no bearing on whether the Job itself succeeded.
    private void fetchAndDisplayBuildLogs(Cluster cluster, String podName, UI ui, Pre logArea) {
        try {
            Optional<String> logs = observabilityService.fetchPodLogs(
                    cluster, BUILD_NAMESPACE, podName, KANIKO_CONTAINER, BUILD_TAIL_LINES, false);
            ui.access(() -> {
                logs.ifPresent(logArea::setText);
                logArea.getElement().executeJs("this.scrollTop = this.scrollHeight");
            });
        } catch (Exception e) {
            log.debug("Could not fetch build logs for pod {}: {}", podName, e.getMessage());
        }
    }

    private void updateStatusBadge(Span badge, String status, String variant) {
        badge.setText(status);
        badge.getElement().getThemeList().clear();
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add(variant);
    }

    private void stopPolling() {
        if (pollTask != null) {
            pollTask.cancel(false);
            pollTask = null;
        }
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void styleLogArea(Pre pre) {
        pre.addClassNames(LumoUtility.FontSize.SMALL);
        pre.getStyle()
                .set("font-family", "monospace")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("overflow", "auto")
                .set("white-space", "pre-wrap")
                .set("overflow-wrap", "anywhere")
                .set("width", "100%")
                .set("height", "350px")
                .set("padding", "var(--lumo-space-m)");
    }
}
