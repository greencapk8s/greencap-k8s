package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.kubernetes.dto.PodInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "workloads/pods", layout = MainLayout.class)
@PageTitle("Pods — GreenCap K8s")
@PermitAll
public class PodsView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private final WorkloadService workloadService;
    private final ClusterContext clusterContext;

    private final Grid<PodInfo> podGrid = new Grid<>(PodInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<PodInfo> allItems = new ArrayList<>();
    private final ListDataProvider<PodInfo> dataProvider = new ListDataProvider<>(allItems);

    public PodsView(WorkloadService workloadService, ClusterContext clusterContext) {
        this.workloadService = workloadService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildPodGrid();

        add(UiConstants.buildSectionHeader("Pods", this::loadPods), noClusterMessage, podGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        podGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadPods();
        }
    }

    private void buildPodGrid() {
        var nameCol   = podGrid.addColumn(PodInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var statusCol = podGrid.addComponentColumn(p -> phaseBadge(p.phase())).setHeader("Status").setWidth("120px").setResizable(true);
        podGrid.addColumn(PodInfo::node).setHeader("Node").setFlexGrow(1).setResizable(true);
        podGrid.addColumn(PodInfo::restarts).setHeader("Restarts").setWidth("90px").setResizable(true);
        podGrid.addColumn(PodInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        podGrid.addComponentColumn(p -> {
            var icon = VaadinIcon.CODE.create();
            icon.setSize(UiConstants.ICON_SIZE);
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            btn.getElement().setAttribute("title", "View Manifest");
            btn.addClickListener(e -> UI.getCurrent().navigate("yaml/pod/" + p.namespace() + "/" + p.name()));
            return btn;
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        podGrid.setDataProvider(dataProvider);

        TextField nameFilter   = buildFilterField();
        TextField statusFilter = buildFilterField();

        dataProvider.setFilter(item ->
            matches(item.name(), nameFilter.getValue()) &&
            matches(item.phase(), statusFilter.getValue()));

        nameFilter.addValueChangeListener(e -> dataProvider.refreshAll());
        statusFilter.addValueChangeListener(e -> dataProvider.refreshAll());

        HeaderRow filterRow = podGrid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(statusCol).setComponent(statusFilter);

        podGrid.setSizeFull();
        podGrid.setVisible(false);
    }

    private boolean loadPods() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<PodInfo> items = workloadService.listPods(cluster, namespace);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            allItems.clear();
            dataProvider.refreshAll();
            return false;
        }
    }

    private Span phaseBadge(String phase) {
        Span badge = new Span(phase);
        badge.getElement().getThemeList().add("badge");
        switch (phase) {
            case "Running", "Active" -> badge.getElement().getThemeList().add("success");
            case "Pending"           -> badge.getElement().getThemeList().add("contrast");
            case "Failed"            -> badge.getElement().getThemeList().add("error");
            default                  -> {}
        }
        return badge;
    }

    private TextField buildFilterField() {
        TextField field = new TextField();
        field.setPlaceholder("Filter...");
        field.setClearButtonVisible(true);
        field.setWidth("100%");
        field.getElement().getThemeList().add("small");
        return field;
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<PodInfo> items = workloadService.listPods(cluster, clusterContext.getNamespace());
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
        } catch (KubernetesOperationException ignored) {}
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
