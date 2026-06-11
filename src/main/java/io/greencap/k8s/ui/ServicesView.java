package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
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
import io.greencap.k8s.kubernetes.NetworkingService;
import io.greencap.k8s.kubernetes.dto.ServiceInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.user.Permission;

@Route(value = "networking/services", layout = MainLayout.class)
@PageTitle("Services — GreenCap K8s")
@PermitAll
public class ServicesView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Services";
    private static final String HELP_TEXT = "A Service exposes a set of Pods under a stable IP and port, distributing network traffic among them. There are four types: ClusterIP, NodePort, LoadBalancer and ExternalName.\n\nA Service does not run code — it only routes traffic to the Pods.";

    private final NetworkingService networkingService;
    private final ClusterContext clusterContext;
    private final GridSelectionMemory selectionMemory;

    private final Grid<ServiceInfo> serviceGrid = new Grid<>(ServiceInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<ServiceInfo> allItems = new ArrayList<>();
    private final ListDataProvider<ServiceInfo> dataProvider = new ListDataProvider<>(allItems);

    public ServicesView(NetworkingService networkingService, ClusterContext clusterContext, GridSelectionMemory selectionMemory) {
        this.networkingService = networkingService;
        this.clusterContext = clusterContext;
        this.selectionMemory = selectionMemory;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildServiceGrid();
        UiConstants.configureSingleSelection(serviceGrid, selectionMemory, getClass().getSimpleName(), ServiceInfo::name);

        boolean canDelete = SecurityUtils.hasPermission(Permission.NETWORKING_SERVICES_DELETE);
        List<UiConstants.SelectionAction<ServiceInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Delete", canDelete, this::openDeleteDialog),
                UiConstants.SelectionAction.of(VaadinIcon.CODE, "View Manifest",
                        s -> UI.getCurrent().navigate("yaml/service/" + s.namespace() + "/" + s.name()))
        );

        add(UiConstants.buildSectionHeader("Services", this::loadServices, HELP_TITLE, HELP_TEXT, serviceGrid, selectionActions), noClusterMessage, serviceGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.NETWORKING_SERVICES_VIEW)) {
            event.forwardTo("");
            return;
        }
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        serviceGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadServices();
        }
    }

    private void buildServiceGrid() {
        var nameCol = serviceGrid.addColumn(ServiceInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var typeCol = serviceGrid.addComponentColumn(s -> typeCell(s.type(), s.hasReadyEndpoints())).setHeader("Type").setWidth("200px").setResizable(true);
        serviceGrid.addColumn(ServiceInfo::clusterIP).setHeader("Cluster IP").setWidth("140px").setResizable(true);
        serviceGrid.addColumn(ServiceInfo::ports).setHeader("Port(s)").setFlexGrow(1).setResizable(true);
        serviceGrid.addColumn(ServiceInfo::age).setHeader("Age").setWidth("80px").setResizable(true);

        serviceGrid.setDataProvider(dataProvider);

        TextField nameFilter = buildFilterField();
        TextField typeFilter = buildFilterField();

        dataProvider.setFilter(item ->
            matches(item.name(), nameFilter.getValue()) &&
            matches(item.type(), typeFilter.getValue()));

        nameFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(serviceGrid, dataProvider, ServiceInfo::name);
        });
        typeFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(serviceGrid, dataProvider, ServiceInfo::name);
        });

        HeaderRow filterRow = serviceGrid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(typeCol).setComponent(typeFilter);

        serviceGrid.setSizeFull();
        serviceGrid.setVisible(false);
    }

    private boolean loadServices() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<ServiceInfo> items = networkingService.listServices(cluster, namespace);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(serviceGrid, dataProvider, ServiceInfo::name);
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            allItems.clear();
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(serviceGrid, dataProvider, ServiceInfo::name);
            return false;
        }
    }

    private void openDeleteDialog(ServiceInfo service) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Service");
        dialog.setText("Deleting this Service will remove its network endpoint. Workloads targeting it will lose connectivity. This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                networkingService.deleteService(cluster, service.namespace(), service.name());
                loadServices();
                notify("Service " + service.name() + " deleted", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private com.vaadin.flow.component.Component typeCell(String type, boolean hasReadyEndpoints) {
        Span typeBadge = new Span(type);
        typeBadge.getElement().getThemeList().add("badge");
        switch (type) {
            case "LoadBalancer" -> typeBadge.getElement().getThemeList().add("success");
            case "ClusterIP"    -> typeBadge.getElement().getThemeList().add("contrast");
            default             -> {}
        }
        if (hasReadyEndpoints) return typeBadge;

        Span noEndpoints = new Span("No Endpoints");
        noEndpoints.getElement().getThemeList().add("badge");
        noEndpoints.getElement().getThemeList().add("error");
        noEndpoints.getStyle().set("margin-left", "6px");

        Div wrapper = new Div(typeBadge, noEndpoints);
        wrapper.getStyle().set("display", "flex").set("align-items", "center").set("gap", "4px");
        return wrapper;
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<ServiceInfo> items = networkingService.listServices(cluster, clusterContext.getNamespace());
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(serviceGrid, dataProvider, ServiceInfo::name);
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
