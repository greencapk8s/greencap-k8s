package io.greencap.k8s.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.dto.EventInfo;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "observability/events", layout = MainLayout.class)
@PageTitle("Events — GreenCap K8s")
@PermitAll
public class EventsView extends VerticalLayout implements BeforeEnterObserver {

    private final ObservabilityService observabilityService;
    private final ClusterContext clusterContext;

    private final Grid<EventInfo> eventGrid = new Grid<>(EventInfo.class, false);
    private final VerticalLayout noClusterMessage;

    private final List<EventInfo> allItems = new ArrayList<>();
    private final ListDataProvider<EventInfo> dataProvider = new ListDataProvider<>(allItems);

    public EventsView(ObservabilityService observabilityService, ClusterContext clusterContext) {
        this.observabilityService = observabilityService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        buildEventGrid();

        add(UiConstants.buildSectionHeader("Events", this::loadEvents), noClusterMessage, eventGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        eventGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadEvents();
        }
    }

    private void buildEventGrid() {
        var typeCol          = eventGrid.addComponentColumn(e -> typeBadge(e.type()))
                .setHeader("Type").setWidth("110px").setFlexGrow(0).setResizable(true);
        var reasonCol        = eventGrid.addColumn(EventInfo::reason)
                .setHeader("Reason").setWidth("180px").setFlexGrow(0).setResizable(true);
        var involvedObjCol   = eventGrid.addColumn(EventInfo::involvedObject)
                .setHeader("Object").setWidth("240px").setFlexGrow(0).setResizable(true);
        eventGrid.addComponentColumn(e -> wrappedText(e.message()))
                .setHeader("Message").setFlexGrow(1).setResizable(true);
        eventGrid.addColumn(EventInfo::count)
                .setHeader("Count").setWidth("80px").setFlexGrow(0).setResizable(true);
        eventGrid.addColumn(EventInfo::age)
                .setHeader("Age").setWidth("70px").setFlexGrow(0).setResizable(true);

        eventGrid.setDataProvider(dataProvider);

        TextField typeFilter          = buildFilterField();
        TextField reasonFilter        = buildFilterField();
        TextField involvedObjFilter   = buildFilterField();

        dataProvider.setFilter(item ->
            matches(item.type(), typeFilter.getValue()) &&
            matches(item.reason(), reasonFilter.getValue()) &&
            matches(item.involvedObject(), involvedObjFilter.getValue()));

        typeFilter.addValueChangeListener(e -> dataProvider.refreshAll());
        reasonFilter.addValueChangeListener(e -> dataProvider.refreshAll());
        involvedObjFilter.addValueChangeListener(e -> dataProvider.refreshAll());

        HeaderRow filterRow = eventGrid.appendHeaderRow();
        filterRow.getCell(typeCol).setComponent(typeFilter);
        filterRow.getCell(reasonCol).setComponent(reasonFilter);
        filterRow.getCell(involvedObjCol).setComponent(involvedObjFilter);

        eventGrid.setSizeFull();
        eventGrid.setVisible(false);
    }

    private Span wrappedText(String text) {
        Span span = new Span(text);
        span.getStyle().set("white-space", "normal").set("word-break", "break-word");
        return span;
    }

    private boolean loadEvents() {
        if (clusterContext.getCluster() == null) return false;
        try {
            List<EventInfo> items = observabilityService.listEvents(
                    clusterContext.getCluster(), clusterContext.getNamespace());
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
        if ("Warning".equals(type)) {
            badge.getElement().getThemeList().add("error");
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
