package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.AutoScalingService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.ConfigurationService;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.NetworkingService;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.StorageService;
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.kubernetes.dto.PodMetricInfo;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.user.Permission;

@Slf4j
@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard — GreenCap K8s")
@PermitAll
public class DashboardView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final Executor VIRTUAL_THREADS = Executors.newVirtualThreadPerTaskExecutor();

    private final ClusterContext clusterContext;
    private final WorkloadService workloadService;
    private final NetworkingService networkingService;
    private final ConfigurationService configurationService;
    private final StorageService storageService;
    private final AutoScalingService autoScalingService;
    private final ObservabilityService observabilityService;

    private final VerticalLayout content = new VerticalLayout();

    // Tracks which cluster/namespace the current DOM was built for
    private Cluster builtForCluster;
    private String builtForNamespace;
    private boolean ctaVisible = false;

    // Data-binding spans — updated in-place on each refresh to avoid DOM rebuild flicker
    private Span deploymentSpan;
    private Span podSpan;
    private Span serviceSpan;
    private Span configMapSpan;
    private Span secretSpan;
    private Span pvcSpan;
    private Span hpaSpan;
    private Span cpuValueText;
    private Span memValueText;
    private Div cpuCard;
    private Div memCard;
    private Div ctaPlaceholder;

    public DashboardView(ClusterContext clusterContext,
                         WorkloadService workloadService,
                         NetworkingService networkingService,
                         ConfigurationService configurationService,
                         StorageService storageService,
                         AutoScalingService autoScalingService,
                         ObservabilityService observabilityService) {
        this.clusterContext = clusterContext;
        this.workloadService = workloadService;
        this.networkingService = networkingService;
        this.configurationService = configurationService;
        this.storageService = storageService;
        this.autoScalingService = autoScalingService;
        this.observabilityService = observabilityService;

        setPadding(true);
        setSpacing(true);
        content.setPadding(false);
        content.setSpacing(true);
        add(new H2("Dashboard"), content);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.OBSERVABILITY_DASHBOARD_VIEW)) {
            event.forwardTo("");
            return;
        }
        loadContent();
    }

    @Override
    public void refresh() {
        loadContent();
    }

    private void loadContent() {
        Cluster cluster = clusterContext.getCluster();
        String namespace = clusterContext.getNamespace();
        UI ui = UI.getCurrent();

        if (cluster == null) {
            if (builtForCluster != null) {
                content.removeAll();
                builtForCluster = null;
                builtForNamespace = null;
                content.add(buildEmptyState());
            }
            return;
        }

        boolean needsRebuild = !cluster.equals(builtForCluster) || !Objects.equals(namespace, builtForNamespace);

        if (needsRebuild) {
            buildLayout(cluster, namespace);
            builtForCluster = cluster;
            builtForNamespace = namespace;
        }

        refreshData(cluster, namespace, ui);
    }

    private void buildLayout(Cluster cluster, String namespace) {
        content.removeAll();
        ctaVisible = false;

        ctaPlaceholder = new Div();
        content.add(ctaPlaceholder);
        content.add(buildResourceCountSection(namespace));
        content.add(buildMetricsSection());
    }

    private void refreshData(Cluster cluster, String namespace, UI ui) {
        fetchCount(deploymentSpan,  () -> workloadService.listDeployments(cluster, namespace).size(),            ui);
        fetchCount(podSpan,         () -> workloadService.listPods(cluster, namespace).size(),                   ui);
        fetchCount(serviceSpan,     () -> networkingService.listServices(cluster, namespace).size(),             ui);
        fetchCount(configMapSpan,   () -> configurationService.listConfigMaps(cluster, namespace).size(),        ui);
        fetchCount(secretSpan,      () -> configurationService.listSecrets(cluster, namespace).size(),           ui);
        fetchCount(pvcSpan,         () -> storageService.listPersistentVolumeClaims(cluster, namespace).size(),  ui);
        fetchCount(hpaSpan,         () -> autoScalingService.listHorizontalScalers(cluster, namespace).size(),   ui);
        fetchMetrics(cluster, namespace, ui);
        refreshCta(cluster, namespace, ui);
    }

    private void fetchCount(Span span, CountSupplier supplier, UI ui) {
        CompletableFuture.runAsync(() -> {
            String value;
            try {
                value = String.valueOf(supplier.get());
            } catch (Exception e) {
                log.debug("Failed to fetch resource count: {}", e.getMessage());
                value = "—";
            }
            String resolved = value;
            ui.access(() -> {
                span.setText(resolved);
                span.getStyle().remove("color");
            });
        }, VIRTUAL_THREADS);
    }

    private void fetchMetrics(Cluster cluster, String namespace, UI ui) {
        CompletableFuture.runAsync(() -> {
            String cpuValue;
            String memValue;
            boolean unavailable = false;
            try {
                List<PodMetricInfo> metrics = observabilityService.listPodMetrics(cluster, namespace);
                long totalCpu = metrics.stream().mapToLong(PodMetricInfo::cpuMillicores).sum();
                long totalMem = metrics.stream().mapToLong(PodMetricInfo::memoryMiB).sum();
                cpuValue = formatCpu(totalCpu);
                memValue = formatMemory(totalMem);
            } catch (KubernetesOperationException e) {
                log.debug("metrics-server unavailable for cluster {}: {}", cluster.getName(), e.getMessage());
                cpuValue = "N/A";
                memValue = "N/A";
                unavailable = true;
            }
            String resolvedCpu = cpuValue;
            String resolvedMem = memValue;
            boolean resolvedUnavailable = unavailable;
            ui.access(() -> {
                cpuValueText.setText(resolvedCpu);
                memValueText.setText(resolvedMem);
                if (resolvedUnavailable) {
                    cpuValueText.getStyle().set("color", "var(--lumo-secondary-text-color)");
                    memValueText.getStyle().set("color", "var(--lumo-secondary-text-color)");
                    cpuCard.getElement().setAttribute("title", "metrics-server not available on this cluster");
                    memCard.getElement().setAttribute("title", "metrics-server not available on this cluster");
                } else {
                    cpuValueText.getStyle().remove("color");
                    memValueText.getStyle().remove("color");
                }
            });
        }, VIRTUAL_THREADS);
    }

    private void refreshCta(Cluster cluster, String namespace, UI ui) {
        if (!SecurityUtils.hasPermission(Permission.PROJECT_DEPLOY_APPLICATION)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                boolean empty = workloadService.listDeployments(cluster, namespace).isEmpty();
                ui.access(() -> {
                    if (empty && !ctaVisible) {
                        ctaPlaceholder.add(buildDeployApplicationCta());
                        ctaVisible = true;
                    } else if (!empty && ctaVisible) {
                        ctaPlaceholder.removeAll();
                        ctaVisible = false;
                    }
                });
            } catch (Exception ignored) {}
        }, VIRTUAL_THREADS);
    }

    private VerticalLayout buildResourceCountSection(String namespace) {
        H3 title = new H3("Resources in " + namespace);
        title.getStyle().set("margin", "0");

        FlexLayout row = new FlexLayout();
        row.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        row.getStyle().set("gap", "var(--lumo-space-m)");

        deploymentSpan = buildLoadingSpan();
        podSpan        = buildLoadingSpan();
        serviceSpan    = buildLoadingSpan();
        configMapSpan  = buildLoadingSpan();
        secretSpan     = buildLoadingSpan();
        pvcSpan        = buildLoadingSpan();
        hpaSpan        = buildLoadingSpan();

        row.add(buildCountCard("Deployments",    deploymentSpan, VaadinIcon.ROCKET,    DeploymentsView.class));
        row.add(buildCountCard("Pods",           podSpan,        VaadinIcon.CUBE,      PodsView.class));
        row.add(buildCountCard("Services",       serviceSpan,    VaadinIcon.SHARE,     ServicesView.class));
        row.add(buildCountCard("ConfigMaps",     configMapSpan,  VaadinIcon.FILE_TEXT, ConfigMapsView.class));
        row.add(buildCountCard("Secrets",        secretSpan,     VaadinIcon.LOCK,      SecretsView.class));
        row.add(buildCountCard("Volume Claims",  pvcSpan,        VaadinIcon.DATABASE,  PersistentVolumeClaimsView.class));
        row.add(buildCountCard("Horiz. Scalers", hpaSpan,        VaadinIcon.RESIZE_H,  HorizontalScalerView.class));

        VerticalLayout section = new VerticalLayout(title, row);
        section.setPadding(false);
        section.setSpacing(true);
        return section;
    }

    private VerticalLayout buildMetricsSection() {
        H3 title = new H3("Resource Usage");
        title.getStyle().set("margin", "0");

        FlexLayout row = new FlexLayout();
        row.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        row.getStyle().set("gap", "var(--lumo-space-m)");

        cpuValueText = buildLoadingSpan();
        memValueText = buildLoadingSpan();
        cpuCard = buildMetricCard("CPU",    cpuValueText, VaadinIcon.CHART_LINE);
        memCard = buildMetricCard("Memory", memValueText, VaadinIcon.STORAGE);
        row.add(cpuCard, memCard);

        VerticalLayout section = new VerticalLayout(title, row);
        section.setPadding(false);
        section.setSpacing(true);
        return section;
    }

    private Div buildDeployApplicationCta() {
        com.vaadin.flow.component.html.Span title = new com.vaadin.flow.component.html.Span("This namespace is empty");
        title.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-l)");

        com.vaadin.flow.component.html.Span subtitle = new com.vaadin.flow.component.html.Span(
                "Deploy your first application from a container image.");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        com.vaadin.flow.component.button.Button deployButton = new com.vaadin.flow.component.button.Button(
                "Deploy Application", VaadinIcon.ROCKET.create(),
                e -> UI.getCurrent().navigate(DeployApplicationView.class));
        deployButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);

        com.vaadin.flow.component.orderedlayout.VerticalLayout ctaContent =
                new com.vaadin.flow.component.orderedlayout.VerticalLayout(title, subtitle, deployButton);
        ctaContent.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        ctaContent.setPadding(false);
        ctaContent.setSpacing(true);

        Div cta = new Div(ctaContent);
        cta.getStyle()
                .set("border", "2px dashed var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-xl)")
                .set("text-align", "center")
                .set("width", "100%")
                .set("margin-bottom", "var(--lumo-space-m)");
        return cta;
    }

    private Div buildEmptyState() {
        Span message = new Span("Select a cluster in the top bar to get started.");
        message.getStyle().set("color", "var(--lumo-secondary-text-color)");

        com.vaadin.flow.component.button.Button goToClusters = new com.vaadin.flow.component.button.Button(
                "Go to Clusters", VaadinIcon.SERVER.create(),
                e -> UI.getCurrent().navigate(ClustersView.class));
        goToClusters.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);

        Div empty = new Div(message, goToClusters);
        empty.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-m)")
                .set("padding", "var(--lumo-space-xl)");
        return empty;
    }

    private Div buildCountCard(String label, Span valueText, VaadinIcon icon,
                               Class<? extends com.vaadin.flow.component.Component> target) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        com.vaadin.flow.component.icon.Icon iconEl = icon.create();
        iconEl.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("width", "1.25rem")
                .set("height", "1.25rem");

        com.vaadin.flow.component.orderedlayout.HorizontalLayout labelRow =
                new com.vaadin.flow.component.orderedlayout.HorizontalLayout(iconEl, labelSpan);
        labelRow.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        labelRow.setSpacing(true);
        labelRow.setPadding(false);

        VerticalLayout cardContent = new VerticalLayout(valueText, labelRow);
        cardContent.setPadding(false);
        cardContent.setSpacing(false);
        cardContent.getStyle().set("gap", "var(--lumo-space-xs)");

        Div card = new Div(cardContent);
        applyCardStyle(card);
        card.addClickListener(e -> UI.getCurrent().navigate(target));
        return card;
    }

    private Div buildMetricCard(String label, Span valueText, VaadinIcon icon) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        com.vaadin.flow.component.icon.Icon iconEl = icon.create();
        iconEl.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("width", "1.25rem")
                .set("height", "1.25rem");

        com.vaadin.flow.component.orderedlayout.HorizontalLayout labelRow =
                new com.vaadin.flow.component.orderedlayout.HorizontalLayout(iconEl, labelSpan);
        labelRow.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        labelRow.setSpacing(true);
        labelRow.setPadding(false);

        VerticalLayout cardContent = new VerticalLayout(valueText, labelRow);
        cardContent.setPadding(false);
        cardContent.setSpacing(false);
        cardContent.getStyle().set("gap", "var(--lumo-space-xs)");

        Div card = new Div(cardContent);
        applyCardStyle(card);
        return card;
    }

    private void applyCardStyle(Div card) {
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-m)")
                .set("min-width", "160px")
                .set("cursor", "pointer")
                .set("box-shadow", "var(--lumo-box-shadow-xs)");
    }

    private Span buildLoadingSpan() {
        Span span = new Span("…");
        span.getStyle()
                .set("font-size", "2.5rem")
                .set("font-weight", "bold")
                .set("line-height", "1")
                .set("color", "var(--lumo-contrast-30pct)");
        return span;
    }

    private String formatCpu(long millicores) {
        if (millicores >= 1000) {
            return String.format("%.1f cores", millicores / 1000.0);
        }
        return millicores + "m";
    }

    private String formatMemory(long mib) {
        if (mib >= 1024) {
            return String.format("%.1f GiB", mib / 1024.0);
        }
        return mib + " MiB";
    }

    @FunctionalInterface
    private interface CountSupplier {
        int get() throws Exception;
    }
}
