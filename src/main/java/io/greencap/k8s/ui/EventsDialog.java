package io.greencap.k8s.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.dto.EventInfo;

import java.util.ArrayList;
import java.util.List;

class EventsDialog {

    private EventsDialog() {}

    static void open(ObservabilityService observabilityService, ClusterContext clusterContext,
                     String kind, String name, String namespace) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Events — " + name);
        dialog.setWidth("900px");
        dialog.setMaxHeight("70vh");

        List<EventInfo> allItems = new ArrayList<>();
        ListDataProvider<EventInfo> dataProvider = new ListDataProvider<>(allItems);

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSizeFull();

        Paragraph emptyMessage = new Paragraph("No events found.");
        Grid<EventInfo> grid = buildGrid(dataProvider);

        content.add(emptyMessage, grid);
        loadEvents(observabilityService, clusterContext, kind, name, namespace, allItems, dataProvider, emptyMessage, grid);

        var refreshIcon = VaadinIcon.REFRESH.create();
        refreshIcon.setSize(UiConstants.ICON_SIZE);
        Button refreshBtn = new Button(refreshIcon, e ->
                loadEvents(observabilityService, clusterContext, kind, name, namespace, allItems, dataProvider, emptyMessage, grid));
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        refreshBtn.getElement().setAttribute("title", "Refresh");
        dialog.getHeader().add(refreshBtn);

        Button closeBtn = new Button("Close", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        dialog.add(content);
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }

    private static void loadEvents(ObservabilityService observabilityService, ClusterContext clusterContext,
                                   String kind, String name, String namespace,
                                   List<EventInfo> allItems, ListDataProvider<EventInfo> dataProvider,
                                   Paragraph emptyMessage, Grid<EventInfo> grid) {
        try {
            List<EventInfo> events = observabilityService.listEventsForResource(
                    clusterContext.getCluster(), namespace, kind, name);
            allItems.clear();
            allItems.addAll(events);
            dataProvider.refreshAll();
            boolean isEmpty = events.isEmpty();
            emptyMessage.setVisible(isEmpty);
            grid.setVisible(!isEmpty);
        } catch (KubernetesOperationException e) {
            Notification notification = Notification.show(
                    e.getMessage(), UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private static Grid<EventInfo> buildGrid(ListDataProvider<EventInfo> dataProvider) {
        Grid<EventInfo> grid = new Grid<>(EventInfo.class, false);

        grid.addComponentColumn(e -> typeBadge(e.type()))
                .setHeader("Type").setWidth("110px").setFlexGrow(0).setResizable(true);
        grid.addColumn(EventInfo::reason)
                .setHeader("Reason").setWidth("180px").setFlexGrow(0).setResizable(true);
        grid.addComponentColumn(e -> wrappedText(e.message()))
                .setHeader("Message").setFlexGrow(1).setResizable(true);
        grid.addColumn(EventInfo::count)
                .setHeader("Count").setWidth("80px").setFlexGrow(0).setResizable(true);
        grid.addColumn(EventInfo::age)
                .setHeader("Age").setWidth("70px").setFlexGrow(0).setResizable(true);

        grid.setDataProvider(dataProvider);
        grid.setAllRowsVisible(false);
        grid.setHeight("400px");

        return grid;
    }

    private static Span typeBadge(String type) {
        Span badge = new Span(type);
        badge.getElement().getThemeList().add("badge");
        if ("Warning".equals(type)) {
            badge.getElement().getThemeList().add("error");
        } else {
            badge.getElement().getThemeList().add("success");
        }
        return badge;
    }

    private static Span wrappedText(String text) {
        Span span = new Span(text);
        span.getStyle().set("white-space", "normal").set("word-break", "break-word");
        return span;
    }
}
