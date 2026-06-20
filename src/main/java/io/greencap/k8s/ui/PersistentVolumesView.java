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
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.StorageService;
import io.greencap.k8s.kubernetes.dto.PersistentVolumeInfo;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

@Route(value = "infrastructure/pvs", layout = MainLayout.class)
@PageTitle("PersistentVolumes — GreenCap K8s")
@PermitAll
public class PersistentVolumesView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Persistent Volumes";
    private static final String HELP_TEXT = "A Persistent Volume (PV) is a storage resource provisioned in the cluster, independent of any Pod's lifecycle. It is bound to a Persistent Volume Claim (PVC) that requests compatible characteristics. Its status indicates whether it is already bound to a PVC or still available.";

    private final StorageService storageService;
    private final ClusterContext clusterContext;
    private final UserService userService;
    private final GridSelectionMemory selectionMemory;

    private final Grid<PersistentVolumeInfo> grid = new Grid<>(PersistentVolumeInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<PersistentVolumeInfo> allItems = new ArrayList<>();
    private final ListDataProvider<PersistentVolumeInfo> dataProvider = new ListDataProvider<>(allItems);

    public PersistentVolumesView(StorageService storageService, ClusterContext clusterContext, UserService userService, GridSelectionMemory selectionMemory) {
        this.storageService = storageService;
        this.clusterContext = clusterContext;
        this.userService = userService;
        this.selectionMemory = selectionMemory;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildGrid();
        UiConstants.configureSingleSelection(grid, selectionMemory, getClass().getSimpleName(), PersistentVolumeInfo::name);

        boolean canDelete = SecurityUtils.hasPermission(Permission.GLOBAL_INFRASTRUCTURE_PV_DELETE);

        var deleteIcon = VaadinIcon.TRASH.create();
        deleteIcon.setSize(UiConstants.ICON_SIZE);
        Button deleteBtn = new Button(deleteIcon, e -> {
            PersistentVolumeInfo selected = grid.asSingleSelect().getValue();
            if (selected != null) openDeleteDialog(selected);
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        deleteBtn.setEnabled(false);
        deleteBtn.setVisible(canDelete);
        deleteBtn.getElement().setAttribute("title", "Delete");

        grid.asSingleSelect().addValueChangeListener(e ->
                deleteBtn.setEnabled(canDelete && e.getValue() != null));

        List<UiConstants.SelectionAction<PersistentVolumeInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.of(VaadinIcon.CODE, "View Manifest",
                        pv -> UI.getCurrent().navigate("yaml/persistentvolume/-/" + pv.name()))
        );

        add(UiConstants.buildSectionHeader("PersistentVolumes", this::loadPersistentVolumes, HELP_TITLE, HELP_TEXT,
                        grid, selectionActions, canDelete ? List.of(deleteBtn) : List.of()),
                noClusterMessage, grid);
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
            loadPersistentVolumes();
        }
    }

    private void openDeleteDialog(PersistentVolumeInfo pv) {
        if ("Bound".equals(pv.status())) {
            ConfirmDialog guard = new ConfirmDialog();
            guard.setHeader("Cannot Delete PersistentVolume");
            guard.setText("\"" + pv.name() + "\" is currently Bound to claim \"" + pv.claim() + "\". " +
                    "Delete the PersistentVolumeClaim first to release this volume before deleting it.");
            guard.setCancelable(false);
            guard.setConfirmText("OK");
            guard.open();
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete PersistentVolume");
        dialog.setText("Deleting PersistentVolume \"" + pv.name() + "\" is irreversible. Make sure no application depends on this volume.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                storageService.deletePersistentVolume(cluster, pv.name());
                loadPersistentVolumes();
                notify("PersistentVolume " + pv.name() + " deleted", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private void buildGrid() {
        var nameCol   = grid.addColumn(PersistentVolumeInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var statusCol = grid.addComponentColumn(pv -> statusBadge(pv.status())).setHeader("Status").setWidth("110px").setResizable(true);
        grid.addColumn(PersistentVolumeInfo::capacity).setHeader("Capacity").setWidth("100px").setResizable(true);
        grid.addColumn(PersistentVolumeInfo::accessMode).setHeader("Access Mode").setWidth("150px").setResizable(true);
        grid.addColumn(PersistentVolumeInfo::reclaimPolicy).setHeader("Reclaim Policy").setWidth("130px").setResizable(true);
        grid.addColumn(PersistentVolumeInfo::storageClass).setHeader("Storage Class").setFlexGrow(1).setResizable(true);
        var claimCol  = grid.addComponentColumn(pv -> claimLink(pv.claim())).setHeader("Claim").setFlexGrow(2).setResizable(true);
        grid.addColumn(PersistentVolumeInfo::age).setHeader("Age").setWidth("80px").setResizable(true);

        grid.setDataProvider(dataProvider);

        TextField nameFilter   = buildFilterField();
        TextField statusFilter = buildFilterField();
        TextField claimFilter  = buildFilterField();

        dataProvider.setFilter(item ->
            matches(item.name(), nameFilter.getValue()) &&
            matches(item.status(), statusFilter.getValue()) &&
            matches(item.claim(), claimFilter.getValue()));

        nameFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, PersistentVolumeInfo::name);
        });
        statusFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, PersistentVolumeInfo::name);
        });
        claimFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, PersistentVolumeInfo::name);
        });

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(statusCol).setComponent(statusFilter);
        filterRow.getCell(claimCol).setComponent(claimFilter);

        grid.setSizeFull();
        grid.setVisible(false);
    }

    private boolean loadPersistentVolumes() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        try {
            List<PersistentVolumeInfo> items = storageService.listPersistentVolumes(cluster);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, PersistentVolumeInfo::name);
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            allItems.clear();
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, PersistentVolumeInfo::name);
            return false;
        }
    }

    private com.vaadin.flow.component.Component claimLink(String claim) {
        if ("—".equals(claim)) return new Span(claim);
        Button link = new Button(claim);
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        link.getStyle().set("cursor", "pointer");
        link.addClickListener(e -> {
            String[] parts = claim.split("/", 2);
            if (parts.length == 2) {
                String namespace = parts[0];
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                clusterContext.setNamespace(namespace);
                userService.updateActiveNamespace(username, namespace);
            }
            UI.getCurrent().navigate(PersistentVolumeClaimsView.class);
        });
        return link;
    }

    private Span statusBadge(String status) {
        Span badge = new Span(status);
        badge.getElement().getThemeList().add("badge");
        switch (status) {
            case "Available"  -> badge.getElement().getThemeList().add("success");
            case "Bound"      -> badge.getElement().getThemeList().add("success");
            case "Failed"     -> badge.getElement().getThemeList().add("error");
            default           -> badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<PersistentVolumeInfo> items = storageService.listPersistentVolumes(cluster);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, PersistentVolumeInfo::name);
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
