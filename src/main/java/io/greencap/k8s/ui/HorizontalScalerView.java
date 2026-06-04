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
import io.greencap.k8s.kubernetes.AutoScalingService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.dto.HorizontalScalerInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "autoscaling/horizontalscalers", layout = MainLayout.class)
@PageTitle("Horizontal Scalers — GreenCap K8s")
@PermitAll
public class HorizontalScalerView extends VerticalLayout implements BeforeEnterObserver {

    private final AutoScalingService autoScalingService;
    private final ClusterContext clusterContext;

    private final Grid<HorizontalScalerInfo> grid = new Grid<>(HorizontalScalerInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<HorizontalScalerInfo> allItems = new ArrayList<>();
    private final ListDataProvider<HorizontalScalerInfo> dataProvider = new ListDataProvider<>(allItems);

    public HorizontalScalerView(AutoScalingService autoScalingService, ClusterContext clusterContext) {
        this.autoScalingService = autoScalingService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildGrid();

        add(UiConstants.buildSectionHeader("Horizontal Scalers", this::loadScalers), noClusterMessage, grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        grid.setVisible(hasCluster);
        if (hasCluster) {
            loadScalers();
        }
    }

    private void buildGrid() {
        var nameCol   = grid.addColumn(HorizontalScalerInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var targetCol = grid.addComponentColumn(h -> navigationLink(h.target(), DeploymentsView.class))
                .setHeader("Target").setFlexGrow(1).setResizable(true);
        grid.addColumn(HorizontalScalerInfo::minReplicas).setHeader("Min").setWidth("70px").setResizable(true);
        grid.addComponentColumn(h -> replicasBadge(h.currentReplicas(), h.maxReplicas()))
                .setHeader("Current / Max").setWidth("120px").setResizable(true);
        grid.addColumn(HorizontalScalerInfo::metrics).setHeader("Metrics").setFlexGrow(1).setResizable(true);
        grid.addColumn(HorizontalScalerInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        grid.addComponentColumn(h -> {
            var icon = VaadinIcon.CODE.create();
            icon.setSize(UiConstants.ICON_SIZE);
            Button btn = new Button(icon);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            btn.getElement().setAttribute("title", "View Manifest");
            btn.addClickListener(e -> UI.getCurrent().navigate(
                    "yaml/horizontalscaler/" + h.namespace() + "/" + h.name()));
            return btn;
        }).setHeader("").setWidth("60px").setFlexGrow(0);

        grid.setDataProvider(dataProvider);

        TextField nameFilter   = buildFilterField();
        TextField targetFilter = buildFilterField();

        dataProvider.setFilter(item ->
            matches(item.name(), nameFilter.getValue()) &&
            matches(item.target(), targetFilter.getValue()));

        nameFilter.addValueChangeListener(e -> dataProvider.refreshAll());
        targetFilter.addValueChangeListener(e -> dataProvider.refreshAll());

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(targetCol).setComponent(targetFilter);

        grid.setSizeFull();
        grid.setVisible(false);
    }

    private boolean loadScalers() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<HorizontalScalerInfo> items = autoScalingService.listHorizontalScalers(cluster, namespace);
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

    private com.vaadin.flow.component.Component navigationLink(String label, Class<? extends com.vaadin.flow.component.Component> target) {
        if ("—".equals(label)) return new Span(label);
        Button link = new Button(label);
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        link.getStyle().set("cursor", "pointer");
        link.addClickListener(e -> UI.getCurrent().navigate(target));
        return link;
    }

    private Span replicasBadge(int current, int max) {
        Span badge = new Span(current + "/" + max);
        badge.getElement().getThemeList().add("badge");
        if (max > 0 && current >= max) {
            badge.getElement().getThemeList().add("error");
        } else if (current == 0) {
            badge.getElement().getThemeList().add("contrast");
        } else {
            badge.getElement().getThemeList().add("success");
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
