package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import io.greencap.k8s.kubernetes.ConfigurationService;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.dto.ConfigMapInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.user.Permission;

@Route(value = "config/configmaps", layout = MainLayout.class)
@PageTitle("ConfigMaps — GreenCap K8s")
@PermitAll
public class ConfigMapsView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "ConfigMaps";
    private static final String HELP_TEXT = "A ConfigMap stores configuration data in key-value format, unencrypted, that can be injected into Workloads as environment variables or mounted files.\n\nThis screen shows only metadata and the key count — the values are not displayed.";

    private final ConfigurationService configurationService;
    private final ClusterContext clusterContext;
    private final GridSelectionMemory selectionMemory;

    private final Grid<ConfigMapInfo> configMapGrid = new Grid<>(ConfigMapInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<ConfigMapInfo> allItems = new ArrayList<>();
    private final ListDataProvider<ConfigMapInfo> dataProvider = new ListDataProvider<>(allItems);

    public ConfigMapsView(ConfigurationService configurationService, ClusterContext clusterContext, GridSelectionMemory selectionMemory) {
        this.configurationService = configurationService;
        this.clusterContext = clusterContext;
        this.selectionMemory = selectionMemory;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildConfigMapGrid();
        UiConstants.configureSingleSelection(configMapGrid, selectionMemory, getClass().getSimpleName(), ConfigMapInfo::name);

        boolean canDelete = SecurityUtils.hasPermission(Permission.PARAMETERS_CONFIGMAPS_DELETE);
        List<UiConstants.SelectionAction<ConfigMapInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Delete", canDelete, this::openDeleteDialog),
                UiConstants.SelectionAction.of(VaadinIcon.CODE, "View Manifest",
                        cm -> UI.getCurrent().navigate("yaml/configmap/" + cm.namespace() + "/" + cm.name()))
        );

        add(UiConstants.buildSectionHeader("ConfigMaps", this::loadConfigMaps, HELP_TITLE, HELP_TEXT, configMapGrid, selectionActions), noClusterMessage, configMapGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.PARAMETERS_CONFIGMAPS_VIEW)) {
            event.forwardTo("");
            return;
        }
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        configMapGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadConfigMaps();
        }
    }

    private void buildConfigMapGrid() {
        var nameCol = configMapGrid.addColumn(ConfigMapInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        configMapGrid.addColumn(cm -> cm.keyCount() + " keys").setHeader("Keys").setWidth("100px").setResizable(true);
        configMapGrid.addColumn(ConfigMapInfo::age).setHeader("Age").setWidth("80px").setResizable(true);

        configMapGrid.setDataProvider(dataProvider);

        TextField nameFilter = buildFilterField();

        dataProvider.setFilter(item -> matches(item.name(), nameFilter.getValue()));

        nameFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(configMapGrid, dataProvider, ConfigMapInfo::name);
        });

        HeaderRow filterRow = configMapGrid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);

        configMapGrid.setSizeFull();
        configMapGrid.setVisible(false);
    }

    private void openDeleteDialog(ConfigMapInfo configMap) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete ConfigMap");
        dialog.setText("Deleting this ConfigMap will remove it from the cluster. Workloads that depend on it may fail. This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                configurationService.deleteConfigMap(cluster, configMap.namespace(), configMap.name());
                loadConfigMaps();
                notify("ConfigMap " + configMap.name() + " deleted", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private boolean loadConfigMaps() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<ConfigMapInfo> items = configurationService.listConfigMaps(cluster, namespace);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(configMapGrid, dataProvider, ConfigMapInfo::name);
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            allItems.clear();
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(configMapGrid, dataProvider, ConfigMapInfo::name);
            return false;
        }
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<ConfigMapInfo> items = configurationService.listConfigMaps(cluster, clusterContext.getNamespace());
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(configMapGrid, dataProvider, ConfigMapInfo::name);
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
