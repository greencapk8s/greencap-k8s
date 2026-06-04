package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.ManifestService;
import jakarta.annotation.security.PermitAll;

@Route(value = "yaml/:resourceType/:namespace/:name", layout = MainLayout.class)
@PageTitle("Manifest — GreenCap K8s")
@PermitAll
public class ManifestView extends VerticalLayout implements BeforeEnterObserver {

    private final ManifestService manifestService;
    private final ClusterContext clusterContext;

    private final Span titleSpan = new Span();
    private final Pre yamlContent = new Pre();

    public ManifestView(ManifestService manifestService, ClusterContext clusterContext) {
        this.manifestService = manifestService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        yamlContent.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.Padding.MEDIUM);
        yamlContent.getStyle().set("font-family", "monospace");
        yamlContent.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("overflow", "auto")
                .set("white-space", "pre")
                .set("width", "100%")
                .set("flex", "1");

        add(buildHeader(), yamlContent);
        setFlexGrow(1, yamlContent);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters params = event.getRouteParameters();
        String resourceType = params.get("resourceType").orElse("");
        String namespace = params.get("namespace").orElse("");
        String name = params.get("name").orElse("");

        titleSpan.setText(resourceType + " / " + name);

        if (clusterContext.getCluster() == null) {
            yamlContent.setText("No active cluster.");
            return;
        }

        try {
            String yaml = manifestService.fetchYaml(clusterContext.getCluster(), resourceType, namespace, name);
            yamlContent.setText(yaml);
        } catch (KubernetesOperationException e) {
            Notification notification = Notification.show(
                    e.getMessage(), UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            yamlContent.setText("Failed to load manifest.");
        }
    }

    private HorizontalLayout buildHeader() {
        Button backBtn = new Button(VaadinIcon.ARROW_LEFT.create(), e -> UI.getCurrent().getPage().getHistory().go(-1));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        backBtn.getElement().setAttribute("title", "Back");

        titleSpan.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout header = new HorizontalLayout(backBtn, titleSpan);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.setWidthFull();
        return header;
    }
}
