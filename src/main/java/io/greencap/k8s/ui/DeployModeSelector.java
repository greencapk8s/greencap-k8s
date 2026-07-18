package io.greencap.k8s.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

// Single source of truth for the "Deploy from X" button row shared by the four deploy wizards —
// previously copy-pasted into each view, which let their button order and highlighted (current)
// button drift out of sync with one another.
final class DeployModeSelector {

    private DeployModeSelector() {
    }

    static HorizontalLayout build(Class<? extends Component> currentView) {
        HorizontalLayout selector = new HorizontalLayout(
                modeButton("Deploy from Dockerfile", VaadinIcon.CODE, DeployFromDockerfileView.class, currentView),
                modeButton("Deploy from Compose", VaadinIcon.FILE_CODE, ImportComposeView.class, currentView),
                modeButton("Deploy from Image", VaadinIcon.ROCKET, DeployApplicationView.class, currentView),
                modeButton("Deploy from Helm", VaadinIcon.PACKAGE, DeployFromHelmView.class, currentView));
        selector.setSpacing(true);
        return selector;
    }

    private static Button modeButton(String label, VaadinIcon icon, Class<? extends Component> targetView, Class<? extends Component> currentView) {
        Button button = new Button(label, icon.create());
        boolean isCurrentView = targetView.equals(currentView);
        button.addThemeVariants(isCurrentView ? ButtonVariant.LUMO_PRIMARY : ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        if (!isCurrentView) {
            button.addClickListener(e -> UI.getCurrent().navigate(targetView));
        }
        return button;
    }
}
