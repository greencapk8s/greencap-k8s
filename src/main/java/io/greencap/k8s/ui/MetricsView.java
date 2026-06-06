package io.greencap.k8s.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.dto.PodMetricInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.user.Permission;

@Route(value = "observability/metrics", layout = MainLayout.class)
@PageTitle("Metrics — GreenCap K8s")
@PermitAll
public class MetricsView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private final ObservabilityService observabilityService;
    private final ClusterContext clusterContext;

    private final Grid<PodMetricInfo> metricsGrid = new Grid<>(PodMetricInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<PodMetricInfo> allItems = new ArrayList<>();
    private final ListDataProvider<PodMetricInfo> dataProvider = new ListDataProvider<>(allItems);

    public MetricsView(ObservabilityService observabilityService, ClusterContext clusterContext) {
        this.observabilityService = observabilityService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildMetricsGrid();

        add(UiConstants.buildSectionHeader("Metrics: Top Pods", this::loadMetrics), noClusterMessage, metricsGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.OBSERVABILITY_METRICS_VIEW)) {
            event.forwardTo("");
            return;
        }
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        metricsGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadMetrics();
        }
    }

    private void buildMetricsGrid() {
        var nameCol = metricsGrid.addColumn(PodMetricInfo::name)
                .setHeader("Pod").setFlexGrow(1).setSortable(true).setResizable(true);
        metricsGrid.addColumn(m -> m.cpuMillicores() + "m")
                .setHeader("CPU").setWidth("110px").setFlexGrow(0).setSortable(true).setResizable(true)
                .setComparator(PodMetricInfo::cpuMillicores);
        metricsGrid.addColumn(m -> m.memoryMiB() + "Mi")
                .setHeader("Memory").setWidth("110px").setFlexGrow(0).setSortable(true).setResizable(true)
                .setComparator(PodMetricInfo::memoryMiB);

        metricsGrid.setDataProvider(dataProvider);

        TextField nameFilter = buildFilterField();

        dataProvider.setFilter(item -> matches(item.name(), nameFilter.getValue()));

        nameFilter.addValueChangeListener(e -> dataProvider.refreshAll());

        HeaderRow filterRow = metricsGrid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);

        metricsGrid.setSizeFull();
        metricsGrid.setVisible(false);
    }

    private boolean loadMetrics() {
        if (clusterContext.getCluster() == null) return false;
        try {
            List<PodMetricInfo> items = observabilityService.listPodMetrics(
                    clusterContext.getCluster(), clusterContext.getNamespace());
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            return true;
        } catch (KubernetesOperationException e) {
            String message = e.getMessage().contains("404") || e.getMessage().contains("metrics")
                    ? "Metrics server not available. Enable metrics-server in your cluster."
                    : e.getMessage();
            notify(message, NotificationVariant.LUMO_ERROR);
            allItems.clear();
            dataProvider.refreshAll();
            return false;
        }
    }

    @Override
    public void refresh() {
        if (clusterContext.getCluster() == null) return;
        try {
            List<PodMetricInfo> items = observabilityService.listPodMetrics(
                    clusterContext.getCluster(), clusterContext.getNamespace());
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
        } catch (KubernetesOperationException ignored) {}
    }

    private TextField buildFilterField() {
        TextField field = new TextField();
        field.setPlaceholder("Filter...");
        field.setClearButtonVisible(true);
        field.setWidth("100%");
        field.getElement().getThemeList().add("small");
        return field;
    }

    private boolean matches(String value, String filter) {
        return filter == null || filter.isBlank() ||
               (value != null && value.toLowerCase().contains(filter.toLowerCase().trim()));
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
