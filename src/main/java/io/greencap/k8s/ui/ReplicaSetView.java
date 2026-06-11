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
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.kubernetes.dto.ReplicaSetInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.user.Permission;

@Route(value = "workloads/replicasets", layout = MainLayout.class)
@PageTitle("ReplicaSets — GreenCap K8s")
@PermitAll
public class ReplicaSetView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "ReplicaSets";
    private static final String HELP_TEXT = "A ReplicaSet maintains a stable set of replica Pods. It is almost always created and managed by a Deployment — each new rollout produces a new ReplicaSet, and previous ones are kept to allow rollback.\n\nThe Owner column indicates the responsible Deployment (or \"—\" for orphan ReplicaSets).";

    private final WorkloadService workloadService;
    private final ClusterContext clusterContext;
    private final GridSelectionMemory selectionMemory;

    private final Grid<ReplicaSetInfo> grid = new Grid<>(ReplicaSetInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<ReplicaSetInfo> allItems = new ArrayList<>();
    private final ListDataProvider<ReplicaSetInfo> dataProvider = new ListDataProvider<>(allItems);

    public ReplicaSetView(WorkloadService workloadService, ClusterContext clusterContext, GridSelectionMemory selectionMemory) {
        this.workloadService = workloadService;
        this.clusterContext = clusterContext;
        this.selectionMemory = selectionMemory;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildGrid();
        UiConstants.configureSingleSelection(grid, selectionMemory, getClass().getSimpleName(), ReplicaSetInfo::name);

        boolean canDelete = SecurityUtils.hasPermission(Permission.WORKLOADS_REPLICASETS_DELETE);
        List<UiConstants.SelectionAction<ReplicaSetInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Delete", canDelete, this::openDeleteDialog),
                UiConstants.SelectionAction.of(VaadinIcon.CODE, "View Manifest",
                        rs -> UI.getCurrent().navigate("yaml/replicaset/" + rs.namespace() + "/" + rs.name()))
        );

        add(UiConstants.buildSectionHeader("ReplicaSets", this::loadReplicaSets, HELP_TITLE, HELP_TEXT, grid, selectionActions),
                noClusterMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.WORKLOADS_REPLICASETS_VIEW)) {
            event.forwardTo("");
            return;
        }
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        if (hasCluster) {
            loadReplicaSets();
        }
    }

    private void buildGrid() {
        var nameCol  = grid.addColumn(ReplicaSetInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var ownerCol = grid.addComponentColumn(rs -> navigationLink(rs.owner(), DeploymentsView.class))
                .setHeader("Owner").setFlexGrow(1).setResizable(true);
        grid.addComponentColumn(rs -> replicasBadge(rs.ready(), rs.desired()))
                .setHeader("Ready / Desired").setWidth("130px").setResizable(true);
        grid.addColumn(ReplicaSetInfo::age).setHeader("Age").setWidth("80px").setResizable(true);

        grid.setDataProvider(dataProvider);

        TextField nameFilter  = buildFilterField();
        TextField ownerFilter = buildFilterField();

        dataProvider.setFilter(item ->
            matches(item.name(), nameFilter.getValue()) &&
            matches(item.owner(), ownerFilter.getValue()));

        nameFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, ReplicaSetInfo::name);
        });
        ownerFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, ReplicaSetInfo::name);
        });

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(ownerCol).setComponent(ownerFilter);

        grid.setSizeFull();
        grid.setVisible(false);
    }

    private boolean loadReplicaSets() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<ReplicaSetInfo> items = workloadService.listReplicaSets(cluster, namespace);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, ReplicaSetInfo::name);
            return true;
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            allItems.clear();
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, ReplicaSetInfo::name);
            return false;
        }
    }

    private void openDeleteDialog(ReplicaSetInfo rs) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete ReplicaSet");
        dialog.setText("Deleting this ReplicaSet will also remove all its Pods. If owned by a Deployment, a new ReplicaSet will be created. This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                workloadService.deleteReplicaSet(cluster, rs.namespace(), rs.name());
                loadReplicaSets();
                notify("ReplicaSet " + rs.name() + " deleted", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private com.vaadin.flow.component.Component navigationLink(String label, Class<? extends com.vaadin.flow.component.Component> target) {
        if ("—".equals(label)) return new Span(label);
        Button link = new Button(label);
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        link.getStyle().set("cursor", "pointer");
        link.addClickListener(e -> UI.getCurrent().navigate(target));
        return link;
    }

    private Span replicasBadge(int ready, int desired) {
        Span badge = new Span(ready + "/" + desired);
        badge.getElement().getThemeList().add("badge");
        if (desired > 0 && ready >= desired) {
            badge.getElement().getThemeList().add("success");
        } else if (ready == 0) {
            badge.getElement().getThemeList().add("error");
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
            List<ReplicaSetInfo> items = workloadService.listReplicaSets(cluster, clusterContext.getNamespace());
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(grid, dataProvider, ReplicaSetInfo::name);
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
