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
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.user.Permission;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.StorageService;
import io.greencap.k8s.kubernetes.dto.NodeInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "infrastructure/nodes", layout = MainLayout.class)
@PageTitle("Nodes — GreenCap K8s")
@PermitAll
public class NodesView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private final StorageService storageService;
    private final ClusterContext clusterContext;

    private final Grid<NodeInfo> grid = new Grid<>(NodeInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<NodeInfo> allItems = new ArrayList<>();
    private final ListDataProvider<NodeInfo> dataProvider = new ListDataProvider<>(allItems);

    public NodesView(StorageService storageService, ClusterContext clusterContext) {
        this.storageService = storageService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildGrid();

        add(UiConstants.buildSectionHeader("Nodes", this::loadNodes), noClusterMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.SETTINGS_INFRASTRUCTURE_VIEW)) {
            event.forwardTo("");
            return;
        }
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        if (hasCluster) {
            loadNodes();
        }
    }

    private void buildGrid() {
        var nameCol   = grid.addColumn(NodeInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var statusCol = grid.addComponentColumn(node -> statusBadge(node.status())).setHeader("Status").setWidth("110px").setSortable(false).setResizable(true);
        grid.addColumn(NodeInfo::role).setHeader("Role").setWidth("130px").setSortable(true).setResizable(true);
        grid.addColumn(NodeInfo::version).setHeader("Version").setWidth("130px").setResizable(true);
        grid.addColumn(NodeInfo::os).setHeader("OS").setFlexGrow(2).setResizable(true);
        grid.addColumn(NodeInfo::cpu).setHeader("CPU").setWidth("80px").setResizable(true);
        grid.addColumn(NodeInfo::memory).setHeader("Memory").setWidth("100px").setResizable(true);
        grid.addColumn(NodeInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        grid.addComponentColumn(node -> {
            var icon = VaadinIcon.CODE.create();
            icon.setSize(UiConstants.ICON_SIZE);
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            btn.getElement().setAttribute("title", "View Manifest");
            btn.addClickListener(e -> UI.getCurrent().navigate("yaml/node/-/" + node.name()));
            return btn;
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        grid.setDataProvider(dataProvider);

        TextField nameFilter   = buildFilterField();
        TextField statusFilter = buildFilterField();

        dataProvider.setFilter(item ->
            matches(item.name(), nameFilter.getValue()) &&
            matches(item.status(), statusFilter.getValue()));

        nameFilter.addValueChangeListener(e -> dataProvider.refreshAll());
        statusFilter.addValueChangeListener(e -> dataProvider.refreshAll());

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(statusCol).setComponent(statusFilter);

        grid.setSizeFull();
        grid.setVisible(false);
    }

    private boolean loadNodes() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        try {
            List<NodeInfo> items = storageService.listNodes(cluster);
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

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<NodeInfo> items = storageService.listNodes(cluster);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
        } catch (KubernetesOperationException ignored) {}
    }

    private Span statusBadge(String status) {
        Span badge = new Span(status);
        badge.getElement().getThemeList().add("badge");
        switch (status) {
            case "Ready"    -> badge.getElement().getThemeList().add("success");
            case "NotReady" -> badge.getElement().getThemeList().add("error");
            default         -> badge.getElement().getThemeList().add("contrast");
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

    private boolean matches(String value, String filter) {
        return filter == null || filter.isBlank() ||
               (value != null && value.toLowerCase().contains(filter.toLowerCase().trim()));
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
