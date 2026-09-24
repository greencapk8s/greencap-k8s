package io.greencap.k8s.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.greencap.k8s.kubernetes.dto.IngressConfig;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class ComposeServiceExposure extends VerticalLayout {

    private static final int MAX_HOST_LENGTH = 253;
    private static final int MAX_HOST_LABEL_LENGTH = 63;
    // The first and last characters of a label sit outside the quantifier, hence the minus two.
    private static final String HOST_LABEL_PATTERN =
            "[a-z0-9]([a-z0-9-]{0," + (MAX_HOST_LABEL_LENGTH - 2) + "}[a-z0-9])?";
    private static final String HOST_PATTERN = HOST_LABEL_PATTERN + "(\\." + HOST_LABEL_PATTERN + ")*";

    private final Checkbox exposeCheckbox = new Checkbox("Expose application externally (Ingress)");
    private final TextField hostField = new TextField("Host");
    private final ComboBox<String> ingressClassField = new ComboBox<>("Ingress class");
    private final FormLayout ingressForm = new FormLayout(hostField, ingressClassField);

    ComposeServiceExposure(String serviceName, String namespace, List<String> ingressClasses) {
        setPadding(false);
        setSpacing(true);

        hostField.setValue(serviceName + "." + namespace + UiConstants.LOCAL_INGRESS_DOMAIN);
        hostField.setWidthFull();
        ingressClassField.setItems(ingressClasses);
        if (!ingressClasses.isEmpty()) ingressClassField.setValue(ingressClasses.get(0));
        ingressClassField.setWidthFull();
        ingressForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        ingressForm.setVisible(false);

        exposeCheckbox.addValueChangeListener(e -> ingressForm.setVisible(e.getValue()));
        add(exposeCheckbox, ingressForm);
    }

    // Hosts are checked across services because two Ingresses with the same host in one Namespace
    // would compete for the same traffic; a service that is not exposed takes no part.
    static boolean validateHosts(Collection<ComposeServiceExposure> exposures) {
        exposures.forEach(exposure -> exposure.hostField.setInvalid(false));
        List<ComposeServiceExposure> exposed = exposures.stream()
                .filter(exposure -> exposure.exposeCheckbox.getValue()).toList();
        Map<String, Long> serviceCountByHost = exposed.stream()
                .collect(Collectors.groupingBy(ComposeServiceExposure::host, Collectors.counting()));

        boolean isValid = true;
        for (ComposeServiceExposure exposure : exposed) {
            Optional<String> error = hostError(exposure.host(), serviceCountByHost);
            error.ifPresent(exposure::showHostError);
            isValid &= error.isEmpty();
        }
        return isValid;
    }

    private static Optional<String> hostError(String host, Map<String, Long> serviceCountByHost) {
        if (host.isEmpty()) return Optional.of("Host is required");
        if (host.length() > MAX_HOST_LENGTH || !host.matches(HOST_PATTERN)) {
            return Optional.of("Lowercase letters, numbers, hyphens and dots only, each part starting and "
                    + "ending with a letter or number and at most " + MAX_HOST_LABEL_LENGTH + " chars, "
                    + "max " + MAX_HOST_LENGTH + " chars");
        }
        if (serviceCountByHost.get(host) > 1) return Optional.of("Another exposed service uses this host");
        return Optional.empty();
    }

    Optional<IngressConfig> ingressConfig() {
        if (!exposeCheckbox.getValue()) return Optional.empty();
        return Optional.of(new IngressConfig(host(), ingressClassField.getValue()));
    }

    private String host() {
        return hostField.getValue().trim();
    }

    private void showHostError(String message) {
        hostField.setErrorMessage(message);
        hostField.setInvalid(true);
    }
}
