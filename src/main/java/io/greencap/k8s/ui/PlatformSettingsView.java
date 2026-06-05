package io.greencap.k8s.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.greencap.k8s.domain.user.UserService;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Platform Settings — GreenCap K8s")
@PermitAll
public class PlatformSettingsView extends VerticalLayout {

    private final UserService userService;

    public PlatformSettingsView(UserService userService) {
        this.userService = userService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 pageTitle = new H2("Platform Settings");
        pageTitle.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.Margin.Bottom.MEDIUM);

        add(pageTitle, buildRefreshCard());
    }

    private Div buildRefreshCard() {
        H3 cardTitle = new H3("Refresh");
        cardTitle.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.Bottom.SMALL);

        Span description = new Span("Automatically reload resource views at the selected interval.");
        description.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        ComboBox<RefreshInterval> intervalSelect = new ComboBox<>();
        intervalSelect.setItems(RefreshInterval.values());
        intervalSelect.setItemLabelGenerator(RefreshInterval::getLabel);
        intervalSelect.setWidth("200px");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.findRefreshInterval(username).ifPresent(seconds ->
                intervalSelect.setValue(RefreshInterval.fromSeconds(seconds))
        );
        if (intervalSelect.getValue() == null) {
            intervalSelect.setValue(RefreshInterval.NONE);
        }

        intervalSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                userService.updateRefreshInterval(username, e.getValue().getSeconds());
                getUI().flatMap(ui -> ui.getChildren()
                        .filter(c -> c instanceof MainLayout)
                        .map(c -> (MainLayout) c)
                        .findFirst())
                        .ifPresent(layout -> layout.applyAndPersistRefreshInterval(e.getValue()));
                Notification notification = Notification.show(
                        "Auto-refresh interval saved.", UiConstants.NOTIFICATION_DURATION_MS,
                        Notification.Position.BOTTOM_END);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        });

        HorizontalLayout row = new HorizontalLayout(new Span("Auto-refresh interval:"), intervalSelect);
        row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        VerticalLayout cardContent = new VerticalLayout(cardTitle, description, row);
        cardContent.setPadding(true);
        cardContent.setSpacing(false);

        Div card = new Div(cardContent);
        card.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BorderColor.CONTRAST_10
        );
        card.setMaxWidth("600px");
        return card;
    }
}
