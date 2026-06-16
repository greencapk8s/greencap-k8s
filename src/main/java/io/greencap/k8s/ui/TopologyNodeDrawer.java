package io.greencap.k8s.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import elemental.json.JsonObject;

class TopologyNodeDrawer extends VerticalLayout {

    private final VerticalLayout body;

    TopologyNodeDrawer() {
        body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(true);

        getStyle()
                .set("position", "fixed")
                .set("top", "0")
                .set("right", "0")
                .set("height", "100%")
                .set("width", "340px")
                .set("z-index", "100")
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "-4px 0 16px rgba(0,0,0,0.12)")
                .set("overflow-y", "auto");

        setPadding(true);
        setSpacing(false);
        setVisible(false);
    }

    void open(JsonObject detail) {
        String nodeLabel = detail.getString("nodeLabel");
        String type = detail.getString("type");
        String status = detail.getString("status");
        String manifestUrl = detail.getString("manifestUrl");
        int readyReplicas = (int) detail.getNumber("readyReplicas");
        int desiredReplicas = (int) detail.getNumber("desiredReplicas");
        String serviceType = detail.getString("serviceType");
        String capacity = detail.getString("capacity");
        String accessMode = detail.getString("accessMode");
        JsonObject labelsJson = detail.getObject("labels");

        removeAll();
        add(buildHeader(nodeLabel, type, status), new Hr(), buildBody(
                type, status, readyReplicas, desiredReplicas, serviceType, capacity, accessMode, labelsJson, manifestUrl));
        setVisible(true);
    }

    void close() {
        setVisible(false);
        removeAll();
    }

    private HorizontalLayout buildHeader(String name, String type, String status) {
        Span nameLabel = new Span(name);
        nameLabel.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        nameLabel.getStyle().set("flex", "1").set("overflow", "hidden").set("text-overflow", "ellipsis").set("white-space", "nowrap");

        Span badge = buildStatusBadge(status);

        Button closeBtn = new Button(VaadinIcon.CLOSE.create(), e -> close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        closeBtn.getElement().setAttribute("title", "Close");

        Span typeLabel = new Span(type);
        typeLabel.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        VerticalLayout nameBlock = new VerticalLayout(nameLabel, typeLabel);
        nameBlock.setPadding(false);
        nameBlock.setSpacing(false);
        nameBlock.getStyle().set("flex", "1").set("min-width", "0");

        HorizontalLayout header = new HorizontalLayout(nameBlock, badge, closeBtn);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.setAlignItems(Alignment.START);
        return header;
    }

    private VerticalLayout buildBody(String type, String status, int ready, int desired,
                                     String serviceType, String capacity, String accessMode,
                                     JsonObject labelsJson, String manifestUrl) {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        boolean isIngress = "Ingress".equals(type);
        boolean isPodGroup = type.contains("Pod");
        boolean isPvc = "PersistentVolumeClaim".equals(type);

        if (isIngress) {
            if (!capacity.isBlank()) content.add(buildInfoRow("Hosts", capacity));
            content.add(buildTlsBadgeRow(accessMode));
            if (!serviceType.isBlank() && !"—".equals(serviceType)) content.add(buildInfoRow("IngressClass", serviceType));
        } else if (isPvc) {
            content.add(buildInfoRow("Status", status));
            if (!capacity.isBlank()) content.add(buildInfoRow("Capacity", capacity));
            if (!serviceType.isBlank()) content.add(buildInfoRow("Storage Class", serviceType));
            if (!accessMode.isBlank()) content.add(buildInfoRow("Access Mode", accessMode));
        } else if (!isPodGroup) {
            if (desired > 0) {
                content.add(buildInfoRow("Replicas", ready + " / " + desired + " ready"));
            }
            if (!serviceType.isBlank()) {
                content.add(buildInfoRow("Type", serviceType));
            }
            content.add(buildLabelsSection(labelsJson));
        } else {
            int count = desired > 0 ? desired : ready;
            content.add(buildInfoRow("Pods", count + " pod" + (count != 1 ? "s" : "")));
            content.add(buildInfoRow("Status", status));
        }

        content.add(buildActionButton(manifestUrl, isPodGroup));
        return content;
    }

    private HorizontalLayout buildInfoRow(String key, String value) {
        Span keySpan = new Span(key);
        keySpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        keySpan.getStyle().set("min-width", "90px");

        Span valueSpan = new Span(value);
        valueSpan.addClassNames(LumoUtility.FontSize.SMALL);

        HorizontalLayout row = new HorizontalLayout(keySpan, valueSpan);
        row.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        row.setWidthFull();
        return row;
    }

    private VerticalLayout buildLabelsSection(JsonObject labelsJson) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);

        Span title = new Span("Labels");
        title.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        section.add(title);

        if (labelsJson == null || labelsJson.keys().length == 0) {
            Span empty = new Span("—");
            empty.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            section.add(empty);
            return section;
        }

        Div badgeContainer = new Div();
        badgeContainer.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "4px").set("margin-top", "4px");
        for (String key : labelsJson.keys()) {
            String value = labelsJson.getString(key);
            Span chip = new Span(key + "=" + value);
            chip.getElement().getThemeList().add("badge contrast");
            chip.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("white-space", "normal").set("word-break", "break-all");
            badgeContainer.add(chip);
        }
        section.add(badgeContainer);
        return section;
    }

    private Button buildActionButton(String resourceUrl, boolean isPodGroup) {
        String label = isPodGroup ? "Go to Pods" : "Go to resource";
        Button btn = new Button(label, VaadinIcon.EXTERNAL_LINK.create());
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        btn.setWidthFull();
        btn.addClickListener(e -> btn.getUI().ifPresent(ui -> ui.navigate(resourceUrl)));
        return btn;
    }

    private HorizontalLayout buildTlsBadgeRow(String tlsValue) {
        Span keySpan = new Span("TLS");
        keySpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        keySpan.getStyle().set("min-width", "90px");

        Span badge = new Span(tlsValue);
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add("Secure".equals(tlsValue) ? "success" : "contrast");

        HorizontalLayout row = new HorizontalLayout(keySpan, badge);
        row.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        row.setWidthFull();
        return row;
    }

    private Span buildStatusBadge(String status) {
        Span badge = new Span(status);
        badge.getElement().getThemeList().add("badge");
        String variant = switch (status) {
            case "Running", "Active", "Bound" -> "success";
            case "Degraded", "Pending", "Unknown", "Terminating" -> "contrast";
            case "Failed", "Lost" -> "error";
            default -> "contrast";
        };
        badge.getElement().getThemeList().add(variant);
        return badge;
    }
}
