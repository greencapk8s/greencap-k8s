package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
import io.greencap.k8s.kubernetes.dto.PersistentVolumeClaimInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.user.Permission;

@Route(value = "storage/pvcs", layout = MainLayout.class)
@PageTitle("Volume Claims (PVC) — GreenCap K8s")
@PermitAll
public class PersistentVolumeClaimsView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Volume Claims (PVC)";
    private static final String HELP_TEXT = "A Persistent Volume Claim (PVC) is a request for storage made by a Workload. It is bound to a Persistent Volume that meets the requested requirements (size, access mode, Storage Class). Its binding status can be bound (fulfilled) or pending (waiting for a compatible volume).";

    private final StorageService storageService;
    private final ClusterContext clusterContext;

    private final Grid<PersistentVolumeClaimInfo> grid = new Grid<>(PersistentVolumeClaimInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<PersistentVolumeClaimInfo> allItems = new ArrayList<>();
    private final ListDataProvider<PersistentVolumeClaimInfo> dataProvider = new ListDataProvider<>(allItems);

    public PersistentVolumeClaimsView(StorageService storageService, ClusterContext clusterContext) {
        this.storageService = storageService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildGrid();

        add(UiConstants.buildSectionHeader("Volume Claims (PVC)", this::loadPersistentVolumeClaims, HELP_TITLE, HELP_TEXT), noClusterMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.STORAGE_PVC_VIEW)) {
            event.forwardTo("");
            return;
        }
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        if (hasCluster) {
            loadPersistentVolumeClaims();
        }
    }

    private void buildGrid() {
        var nameCol   = grid.addColumn(PersistentVolumeClaimInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var statusCol = grid.addComponentColumn(pvc -> statusBadge(pvc.status())).setHeader("Status").setWidth("110px").setResizable(true);
        grid.addColumn(PersistentVolumeClaimInfo::capacity).setHeader("Capacity").setWidth("100px").setResizable(true);
        grid.addColumn(PersistentVolumeClaimInfo::accessMode).setHeader("Access Mode").setWidth("150px").setResizable(true);
        grid.addColumn(PersistentVolumeClaimInfo::storageClass).setHeader("Storage Class").setFlexGrow(1).setResizable(true);
        grid.addColumn(PersistentVolumeClaimInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        boolean canDelete = SecurityUtils.hasPermission(Permission.STORAGE_PVC_DELETE);
        grid.addComponentColumn(pvc -> {
            var manifestIcon = VaadinIcon.CODE.create();
            manifestIcon.setSize(UiConstants.ICON_SIZE);
            Button manifestBtn = new Button(manifestIcon);
            manifestBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            manifestBtn.getElement().setAttribute("title", "View Manifest");
            manifestBtn.addClickListener(e -> UI.getCurrent().navigate(
                    "yaml/persistentvolumeclaim/" + pvc.namespace() + "/" + pvc.name()));

            var deleteIcon = VaadinIcon.TRASH.create();
            deleteIcon.setSize(UiConstants.ICON_SIZE);
            Button deleteBtn = new Button(deleteIcon);
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            deleteBtn.getElement().setAttribute("title", "Delete");
            deleteBtn.setEnabled(canDelete);
            deleteBtn.addClickListener(e -> openDeleteDialog(pvc));

            HorizontalLayout actions = new HorizontalLayout(deleteBtn, manifestBtn);
            actions.setSpacing(false);
            return actions;
        }).setHeader("").setWidth(UiConstants.actionsColumnWidth(2)).setFlexGrow(0);

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

    private boolean loadPersistentVolumeClaims() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<PersistentVolumeClaimInfo> items = storageService.listPersistentVolumeClaims(cluster, namespace);
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

    private void openDeleteDialog(PersistentVolumeClaimInfo pvc) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete PersistentVolumeClaim");
        dialog.setText("Deleting this PersistentVolumeClaim may result in permanent data loss depending on the reclaim policy. This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                storageService.deletePersistentVolumeClaim(cluster, pvc.namespace(), pvc.name());
                loadPersistentVolumeClaims();
                notify("PersistentVolumeClaim " + pvc.name() + " deleted", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private Span statusBadge(String status) {
        Span badge = new Span(status);
        badge.getElement().getThemeList().add("badge");
        switch (status) {
            case "Bound"        -> badge.getElement().getThemeList().add("success");
            case "Lost"         -> badge.getElement().getThemeList().add("error");
            case "Terminating"  -> badge.getElement().getThemeList().add("contrast");
            default             -> {}
        }
        return badge;
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<PersistentVolumeClaimInfo> items = storageService.listPersistentVolumeClaims(cluster, clusterContext.getNamespace());
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
