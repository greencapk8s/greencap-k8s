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
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.ConfigurationService;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.dto.SecretInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.user.Permission;

@Route(value = "config/secrets", layout = MainLayout.class)
@PageTitle("Secrets — GreenCap K8s")
@PermitAll
public class SecretsView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Secrets";
    private static final String HELP_TEXT = "A Secret stores sensitive data (credentials, tokens, certificates) in the cluster, in key-value format. For security reasons, only metadata (name, type and key count) is shown — the values are never decoded or displayed.";

    private final ConfigurationService configurationService;
    private final ClusterContext clusterContext;
    private final GridSelectionMemory selectionMemory;

    private final Grid<SecretInfo> secretGrid = new Grid<>(SecretInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<SecretInfo> allItems = new ArrayList<>();
    private final ListDataProvider<SecretInfo> dataProvider = new ListDataProvider<>(allItems);

    public SecretsView(ConfigurationService configurationService, ClusterContext clusterContext, GridSelectionMemory selectionMemory) {
        this.configurationService = configurationService;
        this.clusterContext = clusterContext;
        this.selectionMemory = selectionMemory;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildSecretGrid();
        UiConstants.configureSingleSelection(secretGrid, selectionMemory, getClass().getSimpleName(), SecretInfo::name);

        boolean canDelete = SecurityUtils.hasPermission(Permission.PARAMETERS_SECRETS_DELETE);
        List<UiConstants.SelectionAction<SecretInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Delete", canDelete, this::openDeleteDialog),
                UiConstants.SelectionAction.of(VaadinIcon.CODE, "View Manifest",
                        s -> UI.getCurrent().navigate("yaml/secret/" + s.namespace() + "/" + s.name()))
        );

        add(UiConstants.buildSectionHeader("Secrets", this::loadSecrets, HELP_TITLE, HELP_TEXT, secretGrid, selectionActions), noClusterMessage, secretGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.PARAMETERS_SECRETS_VIEW)) {
            event.forwardTo("");
            return;
        }
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

        secretGrid.setDataProvider(dataProvider);

        TextField nameFilter = buildFilterField();
        TextField typeFilter = buildFilterField();

        dataProvider.setFilter(item ->
            matches(item.name(), nameFilter.getValue()) &&
            matches(item.type(), typeFilter.getValue()));

        nameFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(secretGrid, dataProvider, SecretInfo::name);
        });
        typeFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(secretGrid, dataProvider, SecretInfo::name);
        });

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
            UiConstants.selectFirstOrPreserve(secretGrid, dataProvider, SecretInfo::name);
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            allItems.clear();
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(secretGrid, dataProvider, SecretInfo::name);
            return false;
        }
    }

    private void openDeleteDialog(SecretInfo secret) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Secret");
        dialog.setText("Deleting this Secret will remove it from the cluster. Workloads that depend on it may fail. This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                configurationService.deleteSecret(cluster, secret.namespace(), secret.name());
                loadSecrets();
                notify("Secret " + secret.name() + " deleted", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
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
            UiConstants.selectFirstOrPreserve(secretGrid, dataProvider, SecretInfo::name);
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
