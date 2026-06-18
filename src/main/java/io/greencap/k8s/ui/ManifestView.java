package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.user.Permission;
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
    private final TextArea yamlEditor = new TextArea();
    private final Button editButton = new Button("Edit", VaadinIcon.EDIT.create());
    private final Button applyButton = new Button("Apply", VaadinIcon.CHECK.create());

    private String resourceType = "";
    private String namespace = "";
    private String name = "";

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
                .set("overflow-x", "hidden")
                .set("white-space", "pre-wrap")
                .set("overflow-wrap", "anywhere")
                .set("width", "100%")
                .set("flex", "1");

        yamlEditor.setSizeFull();
        yamlEditor.addClassNames(LumoUtility.FontSize.SMALL);
        yamlEditor.getStyle().set("font-family", "monospace");
        yamlEditor.getElement().getStyle().set("flex", "1");
        yamlEditor.setVisible(false);

        configureActionButtons();

        add(buildHeader(), yamlContent, yamlEditor);
        setFlexGrow(1, yamlContent);
        setFlexGrow(1, yamlEditor);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters params = event.getRouteParameters();
        resourceType = params.get("resourceType").orElse("");
        namespace = params.get("namespace").orElse("");
        name = params.get("name").orElse("");

        titleSpan.setText(resourceType + " / " + name);
        exitEditMode();

        boolean editable = ManifestService.isEditable(resourceType);
        editButton.setVisible(editable);
        if (editable && !SecurityUtils.hasPermission(Permission.MANIFEST_EDIT)) {
            editButton.setEnabled(false);
            editButton.getElement().setAttribute("title", "You do not have permission to edit this resource");
        } else {
            editButton.setEnabled(true);
            editButton.getElement().removeAttribute("title");
        }

        if (clusterContext.getCluster() == null) {
            yamlContent.setText("No active cluster.");
            return;
        }

        loadManifest();
    }

    private void loadManifest() {
        try {
            String yaml = manifestService.fetchYaml(clusterContext.getCluster(), resourceType, namespace, name);
            yamlContent.setText(yaml);
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
            yamlContent.setText("Failed to load manifest.");
        }
    }

    private void configureActionButtons() {
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editButton.addClickListener(e -> {
            if (yamlEditor.isVisible()) {
                exitEditMode();
            } else {
                enterEditMode();
            }
        });

        applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        applyButton.setVisible(false);
        applyButton.addClickListener(e -> openApplyConfirmation());
    }

    private void enterEditMode() {
        yamlEditor.setValue(yamlContent.getText());
        yamlContent.setVisible(false);
        yamlEditor.setVisible(true);
        applyButton.setVisible(true);
        editButton.setText("Cancel");
        editButton.setIcon(VaadinIcon.CLOSE_SMALL.create());
        yamlEditor.focus();
    }

    private void exitEditMode() {
        yamlEditor.setVisible(false);
        yamlContent.setVisible(true);
        applyButton.setVisible(false);
        editButton.setText("Edit");
        editButton.setIcon(VaadinIcon.EDIT.create());
    }

    private void openApplyConfirmation() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Apply Changes");
        dialog.setText("Apply changes to " + resourceType + " \"" + name + "\"? This updates the resource directly in the cluster.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Apply");
        dialog.addConfirmListener(e -> applyChanges());
        dialog.open();
    }

    private void applyChanges() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;

        try {
            manifestService.applyYaml(cluster, resourceType, namespace, name, yamlEditor.getValue());
            loadManifest();
            exitEditMode();
            notify("Manifest applied successfully", NotificationVariant.LUMO_SUCCESS);
        } catch (KubernetesOperationException e) {
            notify(e.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private HorizontalLayout buildHeader() {
        Button backBtn = new Button(VaadinIcon.ARROW_LEFT.create(), e -> UI.getCurrent().getPage().getHistory().go(-1));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        backBtn.getElement().setAttribute("title", "Back");

        titleSpan.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout header = new HorizontalLayout(backBtn, titleSpan, editButton, applyButton);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.setWidthFull();
        header.setFlexGrow(1, titleSpan);
        return header;
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
