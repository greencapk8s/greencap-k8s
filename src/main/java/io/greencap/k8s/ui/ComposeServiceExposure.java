package io.greencap.k8s.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.greencap.k8s.kubernetes.dto.ComposeImportRequest;

import java.util.List;
import java.util.Optional;

class ComposeServiceExposure extends VerticalLayout {

    private final Checkbox exposeCheckbox = new Checkbox("Expose application externally (Ingress)");
    private final TextField hostField = new TextField("Host");
    private final ComboBox<String> ingressClassField = new ComboBox<>("Ingress class");
    private final FormLayout ingressForm = new FormLayout(hostField, ingressClassField);

    ComposeServiceExposure(String serviceName, String namespace, List<String> ingressClasses) {
        setPadding(false);
        setSpacing(true);

        hostField.setValue(serviceName + "." + namespace + ".greencap.local");
        hostField.setWidthFull();
        ingressClassField.setItems(ingressClasses);
        if (!ingressClasses.isEmpty()) ingressClassField.setValue(ingressClasses.get(0));
        ingressClassField.setWidthFull();
        ingressForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        ingressForm.setVisible(false);

        exposeCheckbox.addValueChangeListener(e -> ingressForm.setVisible(e.getValue()));
        add(exposeCheckbox, ingressForm);
    }

    Optional<ComposeImportRequest.IngressConfig> ingressConfig() {
        if (!exposeCheckbox.getValue()) return Optional.empty();
        return Optional.of(new ComposeImportRequest.IngressConfig(
                hostField.getValue().trim(), ingressClassField.getValue()));
    }
}
