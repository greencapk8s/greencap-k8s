package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import io.greencap.k8s.kubernetes.NetworkingService;
import io.greencap.k8s.kubernetes.dto.IngressInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "networking/ingresses", layout = MainLayout.class)
@PageTitle("Ingresses — GreenCap K8s")
@PermitAll
public class IngressView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Ingresses";
    private static final String HELP_TEXT = "An Ingress is a resource that routes external HTTP/HTTPS traffic to Services, based on host and path rules. It can have an associated IngressClass, which defines which controller handles the requests.\n\nThe TLS badge indicates whether encryption is configured (\"Secure\") or not (\"Plain\").";

    private final NetworkingService networkingService;
    private final ClusterContext clusterContext;
    private final GridSelectionMemory selectionMemory;

    private final Grid<IngressInfo> ingressGrid = new Grid<>(IngressInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<IngressInfo> allItems = new ArrayList<>();
    private final ListDataProvider<IngressInfo> dataProvider = new ListDataProvider<>(allItems);

    private TextField nameFilter;

    public IngressView(NetworkingService networkingService, ClusterContext clusterContext, GridSelectionMemory selectionMemory) {
        this.networkingService = networkingService;
        this.clusterContext = clusterContext;
        this.selectionMemory = selectionMemory;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildIngressGrid();
        UiConstants.configureSingleSelection(ingressGrid, selectionMemory, getClass().getSimpleName(), IngressInfo::name);

        boolean canDelete = SecurityUtils.hasPermission(Permission.NETWORKING_INGRESS_DELETE);
        List<UiConstants.SelectionAction<IngressInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Delete", canDelete, this::openDeleteDialog),
                UiConstants.SelectionAction.of(VaadinIcon.CODE, "View Manifest",
                        i -> UI.getCurrent().navigate("yaml/ingress/" + i.namespace() + "/" + i.name()))
        );

        add(UiConstants.buildSectionHeader("Ingresses", this::loadIngresses, HELP_TITLE, HELP_TEXT, ingressGrid, selectionActions), noClusterMessage, ingressGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.NETWORKING_INGRESS_VIEW)) {
            event.forwardTo("");
            return;
        }
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        ingressGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadIngresses();
        }
        String nameParam = event.getLocation().getQueryParameters()
                .getParameters().getOrDefault("name", List.of()).stream().findFirst().orElse("");
        if (!nameParam.isBlank()) {
            nameFilter.setValue(nameParam);
        }
    }

    private void buildIngressGrid() {
        var nameCol = ingressGrid.addColumn(IngressInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var classCol = ingressGrid.addColumn(IngressInfo::ingressClass).setHeader("IngressClass").setWidth("160px").setResizable(true);
        ingressGrid.addColumn(IngressInfo::hosts).setHeader("Hosts").setFlexGrow(2).setResizable(true);
        ingressGrid.addComponentColumn(i -> tlsBadge(i.tls())).setHeader("TLS").setWidth("100px").setResizable(true);
        ingressGrid.addColumn(IngressInfo::address).setHeader("Address").setWidth("150px").setResizable(true);
        ingressGrid.addColumn(IngressInfo::age).setHeader("Age").setWidth("80px").setResizable(true);

        ingressGrid.setDataProvider(dataProvider);

        nameFilter = buildFilterField();
        TextField classFilter = buildFilterField();

        dataProvider.setFilter(item ->
                matches(item.name(), nameFilter.getValue()) &&
                matches(item.ingressClass(), classFilter.getValue()));

        nameFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(ingressGrid, dataProvider, IngressInfo::name);
        });
        classFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(ingressGrid, dataProvider, IngressInfo::name);
        });

        HeaderRow filterRow = ingressGrid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(classCol).setComponent(classFilter);

        ingressGrid.setSizeFull();
        ingressGrid.setVisible(false);
    }

    private boolean loadIngresses() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<IngressInfo> items = networkingService.listIngresses(cluster, namespace);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(ingressGrid, dataProvider, IngressInfo::name);
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            allItems.clear();
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(ingressGrid, dataProvider, IngressInfo::name);
            return false;
        }
    }

    private void openDeleteDialog(IngressInfo ingress) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Ingress");
        dialog.setText("Deleting Ingress \"" + ingress.name() + "\" will remove all its routing rules. External traffic to the associated hosts will stop. This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                networkingService.deleteIngress(cluster, ingress.namespace(), ingress.name());
                loadIngresses();
                notify("Ingress " + ingress.name() + " deleted", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private Span tlsBadge(boolean tls) {
        Span badge = new Span(tls ? "TLS" : "Plain");
        badge.getElement().getThemeList().add("badge");
        if (tls) {
            badge.getElement().getThemeList().add("success");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<IngressInfo> items = networkingService.listIngresses(cluster, clusterContext.getNamespace());
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(ingressGrid, dataProvider, IngressInfo::name);
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
