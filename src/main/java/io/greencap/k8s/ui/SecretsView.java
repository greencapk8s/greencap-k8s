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
import io.greencap.k8s.kubernetes.ConfigurationService;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.dto.SecretInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "config/secrets", layout = MainLayout.class)
@PageTitle("Secrets — GreenCap K8s")
@PermitAll
public class SecretsView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private final ConfigurationService configurationService;
    private final ClusterContext clusterContext;

    private final Grid<SecretInfo> secretGrid = new Grid<>(SecretInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<SecretInfo> allItems = new ArrayList<>();
    private final ListDataProvider<SecretInfo> dataProvider = new ListDataProvider<>(allItems);

    public SecretsView(ConfigurationService configurationService, ClusterContext clusterContext) {
        this.configurationService = configurationService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildSecretGrid();

        add(UiConstants.buildSectionHeader("Secrets", this::loadSecrets), noClusterMessage, secretGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        secretGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadSecrets();
        }
    }

    private void buildSecretGrid() {
        var nameCol = secretGrid.addColumn(SecretInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var typeCol = secretGrid.addComponentColumn(s -> typeBadge(s.type())).setHeader("Type").setFlexGrow(1).setResizable(true);
        secretGrid.addColumn(s -> s.keyCount() + " keys").setHeader("Keys").setWidth("100px").setResizable(true);
        secretGrid.addColumn(SecretInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        secretGrid.addComponentColumn(s -> {
            var icon = VaadinIcon.CODE.create();
            icon.setSize(UiConstants.ICON_SIZE);
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            btn.getElement().setAttribute("title", "View Manifest");
            btn.addClickListener(e -> UI.getCurrent().navigate("yaml/secret/" + s.namespace() + "/" + s.name()));
            return btn;
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        secretGrid.setDataProvider(dataProvider);

        TextField nameFilter = buildFilterField();
        TextField typeFilter = buildFilterField();

        dataProvider.setFilter(item ->
            matches(item.name(), nameFilter.getValue()) &&
            matches(item.type(), typeFilter.getValue()));

        nameFilter.addValueChangeListener(e -> dataProvider.refreshAll());
        typeFilter.addValueChangeListener(e -> dataProvider.refreshAll());

        HeaderRow filterRow = secretGrid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(typeCol).setComponent(typeFilter);

        secretGrid.setSizeFull();
        secretGrid.setVisible(false);
    }

    private boolean loadSecrets() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<SecretInfo> items = configurationService.listSecrets(cluster, namespace);
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

    private Span typeBadge(String type) {
        Span badge = new Span(type);
        badge.getElement().getThemeList().add("badge");
        if ("Opaque".equals(type)) {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<SecretInfo> items = configurationService.listSecrets(cluster, clusterContext.getNamespace());
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
