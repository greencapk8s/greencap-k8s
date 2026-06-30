package io.greencap.k8s.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.DeployApplicationService;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.NetworkingService;
import io.greencap.k8s.kubernetes.RegistryService;
import io.greencap.k8s.kubernetes.StorageService;
import io.greencap.k8s.kubernetes.dto.DeployApplicationRequest;
import io.greencap.k8s.kubernetes.dto.DeployApplicationResult;
import io.greencap.k8s.kubernetes.dto.StorageClassInfo;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

@Slf4j
@Route(value = "deploy", layout = MainLayout.class)
@PageTitle("Deploy Application — GreenCap K8s")
@PermitAll
public class DeployApplicationView extends VerticalLayout implements BeforeEnterObserver {

    private static final int TOTAL_STEPS = 6;
    private static final String[] STEP_LABELS = {"Name", "Image & Port", "Resources", "Volume", "External Access", "Review"};
    // registry-proxy exposes the internal registry on each node via hostPort 5000
    private static final String REGISTRY_INTERNAL_HOST = "localhost:5000";
    private static final String NAMESPACE_VALID_PATTERN = "[a-z0-9]([a-z0-9-]*[a-z0-9])?";

    private final ClusterContext clusterContext;
    private final DeployApplicationService deployApplicationService;
    private final RegistryService registryService;
    private final StorageService storageService;
    private final NetworkingService networkingService;
    private final UserService userService;

    // Step 1
    private final TextField namespaceField = new TextField("Target namespace");

    // Step 2
    private final ComboBox<String> imageField = new ComboBox<>("Container image");
    private final IntegerField portField = new IntegerField("Container port");

    // Step 3
    private final IntegerField replicasField = new IntegerField("Replicas");
    private final TextField cpuRequestField = new TextField("CPU request");
    private final TextField cpuLimitField = new TextField("CPU limit");
    private final TextField memoryRequestField = new TextField("Memory request");
    private final TextField memoryLimitField = new TextField("Memory limit");

    // Step 4
    private final Checkbox addVolumeCheckbox = new Checkbox("Add persistent storage");
    private final ComboBox<String> storageClassField = new ComboBox<>("Storage class");
    private final IntegerField storageSizeField = new IntegerField("Size (Gi)");
    private final TextField mountPathField = new TextField("Mount path");

    // Step 5
    private final Checkbox addIngressCheckbox = new Checkbox("Expose application externally (Ingress)");
    private final TextField hostField = new TextField("Host");
    private final ComboBox<String> ingressClassField = new ComboBox<>("Ingress class");

    private int currentStep = 1;
    private final HorizontalLayout stepIndicatorRow = new HorizontalLayout();
    private final Div stepContent = new Div();
    private final Button backButton = new Button(VaadinIcon.ARROW_LEFT.create());
    private final Button nextButton = new Button("Next", VaadinIcon.ARROW_RIGHT.create());
    private final Button deployButton = new Button("Deploy", VaadinIcon.ROCKET.create());

    public DeployApplicationView(ClusterContext clusterContext,
                                  DeployApplicationService deployApplicationService,
                                  RegistryService registryService,
                                  StorageService storageService,
                                  NetworkingService networkingService,
                                  UserService userService) {
        this.clusterContext = clusterContext;
        this.deployApplicationService = deployApplicationService;
        this.registryService = registryService;
        this.storageService = storageService;
        this.networkingService = networkingService;
        this.userService = userService;
        initLayout();
        initFields();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        loadRegistrySuggestions();
        loadClusterResources();
        renderStep(1);
    }

    private HorizontalLayout buildModeSelector() {
        Button imageBtn = new Button("Deploy from Image", VaadinIcon.ROCKET.create());
        Button dockerfileBtn = new Button("Deploy from Dockerfile", VaadinIcon.CODE.create());
        Button composeBtn = new Button("Deploy from Compose", VaadinIcon.FILE_CODE.create());

        imageBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        dockerfileBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        composeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        Button helmBtn = new Button("Deploy from Helm", VaadinIcon.PACKAGE.create());
        helmBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        dockerfileBtn.addClickListener(e -> UI.getCurrent().navigate(DeployFromDockerfileView.class));
        composeBtn.addClickListener(e -> UI.getCurrent().navigate(ImportComposeView.class));
        helmBtn.addClickListener(e -> UI.getCurrent().navigate(DeployFromHelmView.class));

        HorizontalLayout selector = new HorizontalLayout(imageBtn, dockerfileBtn, composeBtn, helmBtn);
        selector.setSpacing(true);
        return selector;
    }

    private void initLayout() {
        setPadding(true);
        setSpacing(true);
        setMaxWidth("820px");
        setWidthFull();

        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backButton.addClickListener(e -> navigateBack());
        backButton.setVisible(false);

        nextButton.setIconAfterText(true);
        nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        nextButton.addClickListener(e -> navigateNext());

        deployButton.setIconAfterText(true);
        deployButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        deployButton.addClickListener(e -> onDeploy());
        deployButton.setVisible(false);

        Div spacer = new Div();
        HorizontalLayout footer = new HorizontalLayout(backButton, spacer, nextButton, deployButton);
        footer.expand(spacer);
        footer.setWidthFull();
        footer.setAlignItems(Alignment.CENTER);
        footer.addClassNames(LumoUtility.Padding.Vertical.MEDIUM);

        stepContent.setWidthFull();
        stepIndicatorRow.setWidthFull();
        stepIndicatorRow.setSpacing(true);

        add(buildModeSelector(), stepIndicatorRow, stepContent, footer);
    }

    private void initFields() {
        namespaceField.setWidthFull();
        namespaceField.setHelperText("Lowercase letters, numbers and hyphens. Becomes the Kubernetes Namespace.");
        namespaceField.setRequired(true);
        namespaceField.addValueChangeListener(e -> {
            String ns = e.getValue() != null ? e.getValue() : "";
            String suggested = ns + ".greencap.local";
            if (hostField.isEmpty() || hostField.getValue().equals(e.getOldValue() + ".greencap.local")) {
                hostField.setValue(suggested);
            }
        });

        imageField.setWidthFull();
        imageField.setAllowCustomValue(true);
        imageField.addCustomValueSetListener(e -> imageField.setValue(e.getDetail()));
        imageField.setPlaceholder("nginx:latest");
        imageField.setHelperText("Internal Registry: " + REGISTRY_INTERNAL_HOST + "/<repo>:<tag>");

        portField.setWidthFull();
        portField.setMin(1);
        portField.setMax(65535);
        portField.setHelperText("Port your application listens on. Required for Service and Ingress.");
        portField.setClearButtonVisible(true);

        replicasField.setValue(1);
        replicasField.setMin(1);
        replicasField.setWidthFull();
        cpuRequestField.setValue("100m");
        cpuRequestField.setWidthFull();
        cpuLimitField.setValue("500m");
        cpuLimitField.setWidthFull();
        memoryRequestField.setValue("128Mi");
        memoryRequestField.setWidthFull();
        memoryLimitField.setValue("512Mi");
        memoryLimitField.setWidthFull();

        storageClassField.setWidthFull();
        storageSizeField.setValue(1);
        storageSizeField.setMin(1);
        storageSizeField.setWidthFull();
        mountPathField.setValue("/data");
        mountPathField.setWidthFull();
        addVolumeCheckbox.addValueChangeListener(e -> {
            storageClassField.setVisible(e.getValue());
            storageSizeField.setVisible(e.getValue());
            mountPathField.setVisible(e.getValue());
        });
        storageClassField.setVisible(false);
        storageSizeField.setVisible(false);
        mountPathField.setVisible(false);

        hostField.setWidthFull();
        ingressClassField.setWidthFull();
        addIngressCheckbox.addValueChangeListener(e -> {
            hostField.setVisible(e.getValue());
            ingressClassField.setVisible(e.getValue());
        });
        hostField.setVisible(false);
        ingressClassField.setVisible(false);
    }

    private void renderStep(int step) {
        currentStep = step;
        updateStepIndicator();
        stepContent.removeAll();
        stepContent.add(buildStepContent(step));
        backButton.setVisible(step > 1);
        nextButton.setVisible(step < TOTAL_STEPS);
        deployButton.setVisible(step == TOTAL_STEPS);
        focusFirstField(step);
    }

    private void focusFirstField(int step) {
        switch (step) {
            case 1 -> namespaceField.focus();
            case 2 -> imageField.focus();
            case 3 -> replicasField.focus();
            case 4 -> addVolumeCheckbox.focus();
            case 5 -> addIngressCheckbox.focus();
            default -> { }
        }
    }

    private void updateStepIndicator() {
        stepIndicatorRow.removeAll();
        for (int i = 0; i < TOTAL_STEPS; i++) {
            int step = i + 1;
            Span badge = new Span(step + ". " + STEP_LABELS[i]);
            badge.getElement().getThemeList().add("badge");
            if (step == currentStep) {
                badge.getElement().getThemeList().add("primary");
            } else if (step < currentStep) {
                badge.getElement().getThemeList().add("success");
            } else {
                badge.getElement().getThemeList().add("contrast");
            }
            stepIndicatorRow.add(badge);
        }
    }

    private Component buildStepContent(int step) {
        return switch (step) {
            case 1 -> buildStep1();
            case 2 -> buildStep2();
            case 3 -> buildStep3();
            case 4 -> buildStep4();
            case 5 -> buildStep5();
            case 6 -> buildStep6Review();
            default -> new Div();
        };
    }

    private FormLayout buildStep1() {
        FormLayout form = new FormLayout(namespaceField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }

    private FormLayout buildStep2() {
        FormLayout form = new FormLayout(imageField, portField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }

    private FormLayout buildStep3() {
        FormLayout form = new FormLayout(
                replicasField,
                cpuRequestField, cpuLimitField,
                memoryRequestField, memoryLimitField
        );
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(replicasField, 2);
        return form;
    }

    private VerticalLayout buildStep4() {
        VerticalLayout layout = new VerticalLayout(
                addVolumeCheckbox, storageClassField, storageSizeField, mountPathField);
        layout.setPadding(false);
        layout.setSpacing(true);
        return layout;
    }

    private VerticalLayout buildStep5() {
        boolean hasPort = portField.getValue() != null;
        addIngressCheckbox.setEnabled(hasPort);
        addIngressCheckbox.setHelperText(hasPort ? null : "Requires a container port (step 2)");
        VerticalLayout layout = new VerticalLayout(addIngressCheckbox, hostField, ingressClassField);
        layout.setPadding(false);
        layout.setSpacing(true);
        return layout;
    }

    private VerticalLayout buildStep6Review() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        H4 title = new H4("The following resources will be created:");
        title.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        layout.add(title);

        String ns = namespaceField.getValue();
        layout.add(buildReviewItem("Namespace", ns));
        layout.add(buildReviewItem("Deployment",
                ns + " — image: " + imageField.getValue()
                + ", replicas: " + replicasField.getValue()
                + ", CPU: " + cpuRequestField.getValue() + "/" + cpuLimitField.getValue()
                + ", Memory: " + memoryRequestField.getValue() + "/" + memoryLimitField.getValue()));

        if (portField.getValue() != null) {
            layout.add(buildReviewItem("Service (ClusterIP)", ns + " — port " + portField.getValue()));
        }
        if (addVolumeCheckbox.getValue()) {
            layout.add(buildReviewItem("PersistentVolumeClaim",
                    ns + "-pvc — " + storageSizeField.getValue() + "Gi"
                    + " (" + storageClassField.getValue() + ")"
                    + ", mounted at " + mountPathField.getValue()));
        }
        if (addIngressCheckbox.getValue() && portField.getValue() != null) {
            layout.add(buildReviewItem("Ingress",
                    ns + "-ingress — host: " + hostField.getValue()
                    + " (" + ingressClassField.getValue() + ")"));
        }
        return layout;
    }

    private HorizontalLayout buildReviewItem(String resourceType, String description) {
        Icon icon = VaadinIcon.CHECK_CIRCLE.create();
        icon.getStyle().set("color", "var(--lumo-success-color)").set("flex-shrink", "0");

        Span type = new Span(resourceType);
        type.addClassNames(LumoUtility.FontWeight.BOLD);

        Span desc = new Span(description);
        desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        VerticalLayout text = new VerticalLayout(type, desc);
        text.setPadding(false);
        text.setSpacing(false);

        HorizontalLayout item = new HorizontalLayout(icon, text);
        item.setAlignItems(Alignment.START);
        item.setSpacing(true);
        return item;
    }

    private void navigateNext() {
        if (validateCurrentStep()) {
            renderStep(currentStep + 1);
        }
    }

    private void navigateBack() {
        if (currentStep > 1) {
            renderStep(currentStep - 1);
        }
    }

    private boolean validateCurrentStep() {
        return switch (currentStep) {
            case 1 -> validateStep1();
            case 2 -> validateStep2();
            case 3 -> validateStep3();
            case 4 -> validateStep4();
            default -> true;
        };
    }

    private boolean validateStep1() {
        String val = namespaceField.getValue();
        if (val == null || val.isBlank()) {
            namespaceField.setErrorMessage("Application name is required");
            namespaceField.setInvalid(true);
            return false;
        }
        if (val.length() > 63 || !val.matches(NAMESPACE_VALID_PATTERN)) {
            namespaceField.setErrorMessage("Lowercase letters, numbers and hyphens only, max 63 chars");
            namespaceField.setInvalid(true);
            return false;
        }
        namespaceField.setInvalid(false);
        return true;
    }

    private boolean validateStep2() {
        String val = imageField.getValue();
        if (val == null || val.isBlank()) {
            imageField.setErrorMessage("Container image is required");
            imageField.setInvalid(true);
            return false;
        }
        imageField.setInvalid(false);
        return true;
    }

    private boolean validateStep3() {
        boolean valid = true;
        if (replicasField.getValue() == null || replicasField.getValue() < 1) {
            replicasField.setErrorMessage("Replicas must be at least 1");
            replicasField.setInvalid(true);
            valid = false;
        } else {
            replicasField.setInvalid(false);
        }
        for (TextField field : List.of(cpuRequestField, cpuLimitField, memoryRequestField, memoryLimitField)) {
            if (field.getValue() == null || field.getValue().isBlank()) {
                field.setErrorMessage("Required");
                field.setInvalid(true);
                valid = false;
            } else {
                field.setInvalid(false);
            }
        }
        return valid;
    }

    private boolean validateStep4() {
        if (!addVolumeCheckbox.getValue()) return true;
        boolean valid = true;
        if (storageClassField.getValue() == null || storageClassField.getValue().isBlank()) {
            storageClassField.setErrorMessage("Storage class is required");
            storageClassField.setInvalid(true);
            valid = false;
        } else {
            storageClassField.setInvalid(false);
        }
        if (storageSizeField.getValue() == null || storageSizeField.getValue() < 1) {
            storageSizeField.setErrorMessage("Size must be at least 1Gi");
            storageSizeField.setInvalid(true);
            valid = false;
        } else {
            storageSizeField.setInvalid(false);
        }
        if (mountPathField.getValue() == null || mountPathField.getValue().isBlank()) {
            mountPathField.setErrorMessage("Mount path is required");
            mountPathField.setInvalid(true);
            valid = false;
        } else {
            mountPathField.setInvalid(false);
        }
        return valid;
    }

    private void onDeploy() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) {
            showError("No active cluster selected.");
            return;
        }
        deployButton.setEnabled(false);
        deployButton.setText("Deploying...");

        DeployApplicationRequest request = buildRequest();
        UI ui = UI.getCurrent();
        Thread.ofVirtual().start(() -> {
            try {
                DeployApplicationResult result = deployApplicationService.deploy(cluster, request);
                ui.access(() -> {
                    if (result.isFullSuccess()) {
                        clusterContext.setNamespace(request.namespace());
                        String username = SecurityContextHolder.getContext().getAuthentication().getName();
                        userService.updateActiveNamespace(username, request.namespace());
                        ui.navigate(TopologiaView.class);
                    } else {
                        String created = String.join(", ", result.createdResources());
                        showError("Failed at " + result.failedStep() + ": " + result.failureMessage()
                                + ". Already created: " + created);
                        deployButton.setEnabled(true);
                        deployButton.setText("Deploy");
                    }
                });
            } catch (KubernetesOperationException e) {
                ui.access(() -> {
                    showError(e.getMessage());
                    deployButton.setEnabled(true);
                    deployButton.setText("Deploy");
                });
            }
        });
    }

    private DeployApplicationRequest buildRequest() {
        DeployApplicationRequest.PvcConfig volume = addVolumeCheckbox.getValue()
                ? new DeployApplicationRequest.PvcConfig(
                        storageClassField.getValue(),
                        Optional.ofNullable(storageSizeField.getValue()).orElse(1),
                        mountPathField.getValue())
                : null;

        DeployApplicationRequest.IngressConfig ingress =
                (addIngressCheckbox.getValue() && portField.getValue() != null)
                        ? new DeployApplicationRequest.IngressConfig(
                                hostField.getValue(),
                                ingressClassField.getValue())
                        : null;

        return new DeployApplicationRequest(
                namespaceField.getValue(),
                imageField.getValue(),
                Optional.ofNullable(replicasField.getValue()).orElse(1),
                cpuRequestField.getValue(),
                cpuLimitField.getValue(),
                memoryRequestField.getValue(),
                memoryLimitField.getValue(),
                portField.getValue(),
                volume,
                ingress
        );
    }

    private void loadRegistrySuggestions() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        UI ui = UI.getCurrent();
        Thread.ofVirtual().start(() -> {
            try {
                List<String> suggestions = registryService.listRepositories(cluster).stream()
                        .map(repo -> REGISTRY_INTERNAL_HOST + "/" + repo.name() + ":latest")
                        .toList();
                if (!suggestions.isEmpty()) {
                    ui.access(() -> imageField.setItems(suggestions));
                }
            } catch (Exception e) {
                log.debug("Registry not available for image suggestions: {}", e.getMessage());
            }
        });
    }

    private void loadClusterResources() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        UI ui = UI.getCurrent();
        Thread.ofVirtual().start(() -> {
            try {
                List<String> names = storageService.listStorageClasses(cluster).stream()
                        .map(StorageClassInfo::name).toList();
                Optional<String> defaultClass = storageService.findDefaultStorageClassName(cluster);
                ui.access(() -> {
                    storageClassField.setItems(names);
                    defaultClass.ifPresent(storageClassField::setValue);
                });
            } catch (Exception e) {
                log.debug("Failed to load StorageClasses: {}", e.getMessage());
            }
        });
        Thread.ofVirtual().start(() -> {
            try {
                List<String> ingressClasses = networkingService.listIngressClassNames(cluster);
                ui.access(() -> {
                    ingressClassField.setItems(ingressClasses);
                    if (!ingressClasses.isEmpty()) {
                        ingressClassField.setValue(ingressClasses.get(0));
                    }
                });
            } catch (Exception e) {
                log.debug("Failed to load IngressClasses: {}", e.getMessage());
            }
        });
    }

    private void showError(String message) {
        Notification notification = Notification.show(
                message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
