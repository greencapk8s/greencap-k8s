package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.function.BooleanSupplier;

final class UiConstants {

    static final int NOTIFICATION_DURATION_MS = 6000;
    static final String ICON_SIZE = "28px";

    static VerticalLayout buildNoClusterMessage() {
        Span text = new Span("No active cluster. Select a cluster in Settings → Clusters.");
        text.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.MEDIUM);

        Button goToClusters = new Button("Go to Clusters", VaadinIcon.SERVER.create(),
                e -> UI.getCurrent().navigate(ClustersView.class));
        goToClusters.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(text, goToClusters);
        layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setSizeFull();
        layout.setVisible(false);
        return layout;
    }

    static HorizontalLayout buildSectionHeader(String title, BooleanSupplier onRefresh) {
        H3 heading = new H3(title);

        var refreshIcon = VaadinIcon.REFRESH.create();
        refreshIcon.setSize(ICON_SIZE);
        Button refreshBtn = new Button(refreshIcon, e -> {
            boolean success = onRefresh.getAsBoolean();
            if (success) {
                Notification notification = Notification.show(
                        "Data updated", NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        });
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        refreshBtn.getElement().setAttribute("title", "Refresh");

        HorizontalLayout header = new HorizontalLayout(heading, refreshBtn);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.setWidthFull();
        header.expand(heading);
        return header;
    }

    private UiConstants() {}
}
