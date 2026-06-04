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
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.kubernetes.dto.DeploymentInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "workloads/deployments", layout = MainLayout.class)
@PageTitle("Deployments — GreenCap K8s")
@PermitAll
public class DeploymentsView extends VerticalLayout implements BeforeEnterObserver {

    private final WorkloadService workloadService;
    private final ClusterContext clusterContext;

    private final Grid<DeploymentInfo> deployGrid = new Grid<>(DeploymentInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<DeploymentInfo> allItems = new ArrayList<>();
    private final ListDataProvider<DeploymentInfo> dataProvider = new ListDataProvider<>(allItems);

    public DeploymentsView(WorkloadService workloadService, ClusterContext clusterContext) {
        this.workloadService = workloadService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildDeployGrid();

        add(UiConstants.buildSectionHeader("Deployments", this::loadDeployments), noClusterMessage, deployGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        deployGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadDeployments();
        }
    }

    private void buildDeployGrid() {
        var nameCol = deployGrid.addColumn(DeploymentInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        deployGrid.addComponentColumn(d -> replicasBadge(d.ready(), d.desired()))
                .setHeader("Replicas").setWidth("100px").setResizable(true);
        deployGrid.addColumn(DeploymentInfo::available).setHeader("Available").setWidth("110px").setResizable(true);
        deployGrid.addColumn(DeploymentInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        deployGrid.addComponentColumn(d -> {
            var icon = VaadinIcon.CODE.create();
            icon.setSize(UiConstants.ICON_SIZE);
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            btn.getElement().setAttribute("title", "View Manifest");
            btn.addClickListener(e -> UI.getCurrent().navigate("yaml/deployment/" + d.namespace() + "/" + d.name()));
            return btn;
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        deployGrid.setDataProvider(dataProvider);

        TextField nameFilter = buildFilterField();

        dataProvider.setFilter(item -> matches(item.name(), nameFilter.getValue()));

        nameFilter.addValueChangeListener(e -> dataProvider.refreshAll());

        HeaderRow filterRow = deployGrid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);

        deployGrid.setSizeFull();
        deployGrid.setVisible(false);
    }

    private boolean loadDeployments() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<DeploymentInfo> items = workloadService.listDeployments(cluster, namespace);
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

    private Span replicasBadge(int ready, int desired) {
        Span badge = new Span(ready + "/" + desired);
        badge.getElement().getThemeList().add("badge");
        if (ready >= desired && desired > 0) {
            badge.getElement().getThemeList().add("success");
        } else if (ready == 0) {
            badge.getElement().getThemeList().add("error");
        } else {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
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
