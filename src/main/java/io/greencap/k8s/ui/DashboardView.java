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
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.user.Permission;

@Slf4j
@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard — GreenCap K8s")
@PermitAll
public class DashboardView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private final ClusterContext clusterContext;
    private final WorkloadService workloadService;
    private final NetworkingService networkingService;
    private final ConfigurationService configurationService;
    private final StorageService storageService;
    private final AutoScalingService autoScalingService;
    private final ObservabilityService observabilityService;

    private final VerticalLayout content = new VerticalLayout();

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
        content.removeAll();
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) {
            content.add(buildEmptyState());
            return;
        }
        String namespace = clusterContext.getNamespace();
        content.add(buildResourceCountSection(cluster, namespace));
        content.add(buildMetricsSection(cluster, namespace));
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

    private VerticalLayout buildResourceCountSection(Cluster cluster, String namespace) {
        H3 title = new H3("Resources in " + namespace);
        title.getStyle().set("margin", "0");

        FlexLayout row = new FlexLayout();
        row.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        row.getStyle().set("gap", "var(--lumo-space-m)");

        row.add(buildCountCard("Deployments",       safeCount(() -> workloadService.listDeployments(cluster, namespace).size()),        VaadinIcon.ROCKET,   DeploymentsView.class));
        row.add(buildCountCard("Pods",              safeCount(() -> workloadService.listPods(cluster, namespace).size()),               VaadinIcon.CUBE,     PodsView.class));
        row.add(buildCountCard("Services",          safeCount(() -> networkingService.listServices(cluster, namespace).size()),         VaadinIcon.SHARE,    ServicesView.class));
        row.add(buildCountCard("ConfigMaps",        safeCount(() -> configurationService.listConfigMaps(cluster, namespace).size()),    VaadinIcon.FILE_TEXT, ConfigMapsView.class));
        row.add(buildCountCard("Secrets",           safeCount(() -> configurationService.listSecrets(cluster, namespace).size()),       VaadinIcon.LOCK,     SecretsView.class));
        row.add(buildCountCard("Volume Claims",     safeCount(() -> storageService.listPersistentVolumeClaims(cluster, namespace).size()), VaadinIcon.DATABASE, PersistentVolumeClaimsView.class));
        row.add(buildCountCard("Horiz. Scalers",    safeCount(() -> autoScalingService.listHorizontalScalers(cluster, namespace).size()), VaadinIcon.RESIZE_H, HorizontalScalerView.class));

        VerticalLayout section = new VerticalLayout(title, row);
        section.setPadding(false);
        section.setSpacing(true);
        return section;
    }

    private VerticalLayout buildMetricsSection(Cluster cluster, String namespace) {
        H3 title = new H3("Resource Usage");
        title.getStyle().set("margin", "0");

        FlexLayout row = new FlexLayout();
        row.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        row.getStyle().set("gap", "var(--lumo-space-m)");

        String cpuValue;
        String memValue;
        boolean metricsUnavailable = false;

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
            metricsUnavailable = true;
        }

        Div cpuCard = buildMetricCard("CPU", cpuValue, VaadinIcon.CHART_LINE, metricsUnavailable);
        Div memCard = buildMetricCard("Memory", memValue, VaadinIcon.STORAGE, metricsUnavailable);
        row.add(cpuCard, memCard);

        VerticalLayout section = new VerticalLayout(title, row);
        section.setPadding(false);
        section.setSpacing(true);
        return section;
    }

    private Div buildCountCard(String label, String value, VaadinIcon icon, Class<? extends com.vaadin.flow.component.Component> target) {
        Span valueText = new Span(value);
        valueText.getStyle()
                .set("font-size", "2.5rem")
                .set("font-weight", "bold")
                .set("line-height", "1");

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

    private Div buildMetricCard(String label, String value, VaadinIcon icon, boolean unavailable) {
        Span valueText = new Span(value);
        valueText.getStyle()
                .set("font-size", "2.5rem")
                .set("font-weight", "bold")
                .set("line-height", "1");
        if (unavailable) {
            valueText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        }

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

        if (unavailable) {
            card.getElement().setAttribute("title", "metrics-server not available on this cluster");
        }

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

    private String safeCount(CountSupplier supplier) {
        try {
            return String.valueOf(supplier.get());
        } catch (Exception e) {
            log.debug("Failed to fetch resource count: {}", e.getMessage());
            return "—";
        }
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
