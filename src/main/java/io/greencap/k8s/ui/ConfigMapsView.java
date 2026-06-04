package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

@Route(value = "config/configmaps", layout = MainLayout.class)
@PageTitle("ConfigMaps — GreenCap K8s")
@PermitAll
public class ConfigMapsView extends VerticalLayout implements BeforeEnterObserver {

    private final ConfigurationService configurationService;
    private final ClusterContext clusterContext;

    private final Grid<ConfigMapInfo> configMapGrid = new Grid<>(ConfigMapInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<ConfigMapInfo> allItems = new ArrayList<>();
    private final ListDataProvider<ConfigMapInfo> dataProvider = new ListDataProvider<>(allItems);

    public ConfigMapsView(ConfigurationService configurationService, ClusterContext clusterContext) {
        this.configurationService = configurationService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildConfigMapGrid();

        add(UiConstants.buildSectionHeader("ConfigMaps", this::loadConfigMaps), noClusterMessage, configMapGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
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
        configMapGrid.addComponentColumn(cm -> {
            var icon = VaadinIcon.CODE.create();
            icon.setSize(UiConstants.ICON_SIZE);
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            btn.getElement().setAttribute("title", "View Manifest");
            btn.addClickListener(e -> UI.getCurrent().navigate("yaml/configmap/" + cm.namespace() + "/" + cm.name()));
            return btn;
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        configMapGrid.setDataProvider(dataProvider);

        TextField nameFilter = buildFilterField();

        dataProvider.setFilter(item -> matches(item.name(), nameFilter.getValue()));

        nameFilter.addValueChangeListener(e -> dataProvider.refreshAll());

        HeaderRow filterRow = configMapGrid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);

        configMapGrid.setSizeFull();
        configMapGrid.setVisible(false);
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
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            allItems.clear();
            dataProvider.refreshAll();
            return false;
        }
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
