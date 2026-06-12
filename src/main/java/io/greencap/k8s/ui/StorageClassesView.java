package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
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
import io.greencap.k8s.kubernetes.StorageService;
import io.greencap.k8s.kubernetes.dto.StorageClassInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.user.Permission;

@Route(value = "infrastructure/storageclasses", layout = MainLayout.class)
@PageTitle("Storage Classes — GreenCap K8s")
@PermitAll
public class StorageClassesView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Storage Classes";
    private static final String HELP_TEXT = "A Storage Class defines how storage volumes are provisioned in the cluster — the storage type, the provisioner, and the retention and expansion policies.\n\nWhen a PVC references a Storage Class, the cluster automatically provisions a compatible Persistent Volume.";

    private final StorageService storageService;
    private final ClusterContext clusterContext;
    private final GridSelectionMemory selectionMemory;

    private final Grid<StorageClassInfo> grid = new Grid<>(StorageClassInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<StorageClassInfo> allItems = new ArrayList<>();
    private final ListDataProvider<StorageClassInfo> dataProvider = new ListDataProvider<>(allItems);

    public StorageClassesView(StorageService storageService, ClusterContext clusterContext, GridSelectionMemory selectionMemory) {
        this.storageService = storageService;
        this.clusterContext = clusterContext;
        this.selectionMemory = selectionMemory;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildGrid();
        UiConstants.configureSingleSelection(grid, selectionMemory, getClass().getSimpleName(), StorageClassInfo::name);

        List<UiConstants.SelectionAction<StorageClassInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.of(VaadinIcon.CODE, "View Manifest",
                        sc -> UI.getCurrent().navigate("yaml/storageclass/-/" + sc.name()))
        );

        add(UiConstants.buildSectionHeader("Storage Classes", this::loadStorageClasses, HELP_TITLE, HELP_TEXT, grid, selectionActions), noClusterMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.GLOBAL_INFRASTRUCTURE_VIEW)) {
            event.forwardTo("");
            return;
        }
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        if (hasCluster) {
            loadStorageClasses();
        }
    }

    private void buildGrid() {
        var nameCol        = grid.addColumn(StorageClassInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var provisionerCol = grid.addColumn(StorageClassInfo::provisioner).setHeader("Provisioner").setFlexGrow(2).setResizable(true);
        grid.addColumn(StorageClassInfo::reclaimPolicy).setHeader("Reclaim Policy").setWidth("130px").setResizable(true);
        grid.addColumn(StorageClassInfo::volumeBindingMode).setHeader("Binding Mode").setWidth("180px").setResizable(true);
        grid.addColumn(StorageClassInfo::allowVolumeExpansion).setHeader("Allow Expansion").setWidth("150px").setResizable(true);
        grid.addColumn(StorageClassInfo::age).setHeader("Age").setWidth("80px").setResizable(true);

        grid.setDataProvider(dataProvider);

        TextField nameFilter        = buildFilterField();
        TextField provisionerFilter = buildFilterField();

        dataProvider.setFilter(item ->
            matches(item.name(), nameFilter.getValue()) &&
            matches(item.provisioner(), provisionerFilter.getValue()));

        nameFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, StorageClassInfo::name);
        });
        provisionerFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, StorageClassInfo::name);
        });

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(provisionerCol).setComponent(provisionerFilter);

        grid.setSizeFull();
        grid.setVisible(false);
    }

    private boolean loadStorageClasses() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        try {
            List<StorageClassInfo> items = storageService.listStorageClasses(cluster);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, StorageClassInfo::name);
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            allItems.clear();
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, StorageClassInfo::name);
            return false;
        }
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<StorageClassInfo> items = storageService.listStorageClasses(cluster);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, StorageClassInfo::name);
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
