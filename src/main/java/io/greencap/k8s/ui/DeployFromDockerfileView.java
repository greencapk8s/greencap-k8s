package io.greencap.k8s.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
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
import io.greencap.k8s.config.SecurityUtils;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.user.Permission;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.DeployApplicationService;
import io.greencap.k8s.kubernetes.DockerfileParser;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.NetworkingService;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.RegistryService;
import io.greencap.k8s.kubernetes.StorageService;
import io.greencap.k8s.kubernetes.dto.BuildRequest;
import io.greencap.k8s.kubernetes.dto.DeployApplicationRequest;
import io.greencap.k8s.kubernetes.dto.DeployApplicationResult;
import io.greencap.k8s.kubernetes.dto.StorageClassInfo;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Route(value = "deploy/dockerfile", layout = MainLayout.class)
@PageTitle("Deploy from Dockerfile — GreenCap K8s")
@PermitAll
public class DeployFromDockerfileView extends VerticalLayout implements BeforeEnterObserver {

    private static final int TOTAL_STEPS = 6;
    private static final String[] STEP_LABELS = {"Source & Name", "Image & Port", "Resources", "Volume", "External Access", "Review"};
    private static final String REGISTRY_PULL_HOST = "localhost:5000";
    private static final String BUILD_NAMESPACE = "greencap-system";
    private static final String KANIKO_CONTAINER = "kaniko";
    private static final int BUILD_TAIL_LINES = 1000;
    private static final int POLL_INTERVAL_SECONDS = 3;
    private static final String NAMESPACE_PATTERN = "[a-z0-9]([a-z0-9-]*[a-z0-9])?";

    private final ClusterContext clusterContext;
    private final RegistryService registryService;
    private final ObservabilityService observabilityService;
    private final DeployApplicationService deployApplicationService;
    private final StorageService storageService;
    private final NetworkingService networkingService;
    private final UserService userService;
    private final DockerfileParser dockerfileParser;

    // Step 1 — Source + Name
    private final TextField gitUrlField = new TextField("Git repository URL");
    private final TextField branchField = new TextField("Branch");
    private final TextField dockerfilePathField = new TextField("Dockerfile path");
    private final TextField contextPathField = new TextField("Context path");
    private final TextField namespaceField = new TextField("Target namespace");

    // Step 2 — Image & Port
    private final TextField applicationNameField = new TextField("Application name");
    private final TextField imageTagField = new TextField("Image tag");
    private final IntegerField portField = new IntegerField("Container port");

    // Step 3 — Resources
    private final IntegerField replicasField = new IntegerField("Replicas");
    private final TextField cpuRequestField = new TextField("CPU request");
    private final TextField cpuLimitField = new TextField("CPU limit");
    private final TextField memoryRequestField = new TextField("Memory request");
    private final TextField memoryLimitField = new TextField("Memory limit");

    // Step 4 — Volume
    private final Checkbox addVolumeCheckbox = new Checkbox("Add persistent storage");
    private final ComboBox<String> storageClassField = new ComboBox<>("Storage class");
    private final IntegerField storageSizeField = new IntegerField("Size (Gi)");
    private final TextField mountPathField = new TextField("Mount path");

    // Step 5 — External Access
    private final Checkbox addIngressCheckbox = new Checkbox("Expose application externally (Ingress)");
    private final TextField hostField = new TextField("Host");
    private final ComboBox<String> ingressClassField = new ComboBox<>("Ingress class");

    // Navigation
    private int currentStep = 1;
    private final HorizontalLayout stepIndicatorRow = new HorizontalLayout();
    private final Div stepContent = new Div();
    private final Button backButton = new Button(VaadinIcon.ARROW_LEFT.create());
    private final Button nextButton = new Button("Next", VaadinIcon.ARROW_RIGHT.create());
    private final Button deployButton = new Button("Build & Deploy", VaadinIcon.ROCKET.create());

    // Execution state
    private final ScheduledExecutorService pollExecutor =
            Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private ScheduledFuture<?> pollTask;
    private Span executionStatusBadge;
    private Pre buildLogArea;

    public DeployFromDockerfileView(ClusterContext clusterContext,
                                     RegistryService registryService,
                                     ObservabilityService observabilityService,
                                     DeployApplicationService deployApplicationService,
                                     StorageService storageService,
                                     NetworkingService networkingService,
                                     UserService userService,
                                     DockerfileParser dockerfileParser) {
        this.clusterContext = clusterContext;
        this.registryService = registryService;
        this.observabilityService = observabilityService;
        this.deployApplicationService = deployApplicationService;
        this.storageService = storageService;
        this.networkingService = networkingService;
        this.userService = userService;
        this.dockerfileParser = dockerfileParser;
        initLayout();
        initFields();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SecurityUtils.hasPermission(Permission.PROJECT_DEPLOY_APPLICATION)) {
            event.forwardTo("");
            return;
        }
        loadClusterResources();
        renderStep(1);
    }

    private HorizontalLayout buildModeSelector() {
        Button imageBtn = new Button("Deploy from Image", VaadinIcon.ROCKET.create());
        Button dockerfileBtn = new Button("Deploy from Dockerfile", VaadinIcon.CODE.create());
        Button composeBtn = new Button("Deploy from Compose", VaadinIcon.FILE_CODE.create());

        imageBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        dockerfileBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        composeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        imageBtn.addClickListener(e -> UI.getCurrent().navigate(DeployApplicationView.class));
        composeBtn.addClickListener(e -> UI.getCurrent().navigate(ImportComposeView.class));

        HorizontalLayout selector = new HorizontalLayout(imageBtn, dockerfileBtn, composeBtn);
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
        deployButton.addClickListener(e -> onBuildAndDeploy());
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
        gitUrlField.setWidthFull();
        gitUrlField.setPlaceholder("https://github.com/user/repo");
        gitUrlField.setRequired(true);
        gitUrlField.addValueChangeListener(e -> {
            String suggested = extractRepoName(e.getValue());
            String oldSuggested = extractRepoName(e.getOldValue());
            if (!suggested.isBlank() && (namespaceField.isEmpty() || namespaceField.getValue().equals(oldSuggested))) {
                namespaceField.setValue(suggested);
            }
        });

        branchField.setWidthFull();
        branchField.setValue("main");
        branchField.setRequired(true);

        dockerfilePathField.setWidthFull();
        dockerfilePathField.setPlaceholder("Dockerfile");
        dockerfilePathField.setHelperText("Leave blank to use 'Dockerfile' at the context root.");

        contextPathField.setWidthFull();
        contextPathField.setHelperText("Subdirectory to use as build context. Leave blank for the repository root.");

        namespaceField.setWidthFull();
        namespaceField.setHelperText("Lowercase letters, numbers and hyphens. Becomes the Kubernetes Namespace.");
        namespaceField.setRequired(true);
        namespaceField.addValueChangeListener(e -> {
            String ns = e.getValue() != null ? e.getValue().trim() : "";
            String oldNs = e.getOldValue() != null ? e.getOldValue().trim() : "";
            String appName = applicationNameField.getValue() != null ? applicationNameField.getValue().trim() : "";
            String oldSuggested = oldNs + "/" + appName + ":latest";
            String newSuggested = ns + "/" + appName + ":latest";
            if (imageTagField.isEmpty() || imageTagField.getValue().equals(oldSuggested)) {
                imageTagField.setValue(newSuggested);
            }
            String suggestedHost = ns + ".greencap.local";
            if (hostField.isEmpty() || hostField.getValue().equals(oldNs + ".greencap.local")) {
                hostField.setValue(suggestedHost);
            }
        });

        applicationNameField.setWidthFull();
        applicationNameField.setHelperText("Used as the image repository name in the Registry.");
        applicationNameField.setRequired(true);
        applicationNameField.addValueChangeListener(e -> {
            String appName = e.getValue() != null ? e.getValue().trim() : "";
            String oldAppName = e.getOldValue() != null ? e.getOldValue().trim() : "";
            String ns = namespaceField.getValue() != null ? namespaceField.getValue().trim() : "";
            String oldSuggested = ns + "/" + oldAppName + ":latest";
            String newSuggested = ns + "/" + appName + ":latest";
            if (imageTagField.isEmpty() || imageTagField.getValue().equals(oldSuggested)) {
                imageTagField.setValue(newSuggested);
            }
        });

        imageTagField.setWidthFull();
        imageTagField.setHelperText("Image will be pushed to the cluster's internal Registry. Format: <repo>:<tag>");
        imageTagField.setRequired(true);

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
            case 1 -> gitUrlField.focus();
            case 2 -> applicationNameField.focus();
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
            if (step == currentStep) badge.getElement().getThemeList().add("primary");
            else if (step < currentStep) badge.getElement().getThemeList().add("success");
            else badge.getElement().getThemeList().add("contrast");
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
        FormLayout form = new FormLayout(gitUrlField, branchField, dockerfilePathField, contextPathField, namespaceField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }

    private FormLayout buildStep2() {
        FormLayout form = new FormLayout(applicationNameField, imageTagField, portField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }

    private FormLayout buildStep3() {
        FormLayout form = new FormLayout(
                replicasField,
                cpuRequestField, cpuLimitField,
                memoryRequestField, memoryLimitField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(replicasField, 2);
        return form;
    }

    private VerticalLayout buildStep4() {
        VerticalLayout layout = new VerticalLayout(addVolumeCheckbox, storageClassField, storageSizeField, mountPathField);
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
        String fullImage = REGISTRY_PULL_HOST + "/" + imageTagField.getValue().trim();

        layout.add(buildReviewItem("Build (Kaniko)", imageTagField.getValue().trim()
                + " from " + gitUrlField.getValue().trim() + " [" + branchField.getValue().trim() + "]"));
        layout.add(buildReviewItem("Namespace", ns));
        layout.add(buildReviewItem("Deployment", ns + " — image: " + fullImage
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
            layout.add(buildReviewItem("Ingress", ns + "-ingress — host: " + hostField.getValue()
                    + " (" + ingressClassField.getValue() + ")"));
        }
        return layout;
    }

    private HorizontalLayout buildReviewItem(String resourceType, String description) {
        var icon = VaadinIcon.CHECK_CIRCLE.create();
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
        if (currentStep == 1) {
            fetchDockerfileAndAdvance();
        } else if (validateCurrentStep()) {
            renderStep(currentStep + 1);
        }
    }

    private void fetchDockerfileAndAdvance() {
        if (!validateCurrentStep()) return;
        nextButton.setEnabled(false);
        nextButton.setText("Fetching...");
        UI ui = UI.getCurrent();
        Thread.ofVirtual().start(() -> {
            Optional<Integer> exposePort = dockerfileParser.fetchExposePort(
                    gitUrlField.getValue().trim(),
                    branchField.getValue().trim(),
                    dockerfilePathField.getValue().trim());
            ui.access(() -> {
                exposePort.filter(p -> portField.isEmpty()).ifPresent(portField::setValue);
                nextButton.setEnabled(true);
                nextButton.setText("Next");
                renderStep(2);
            });
        });
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
        boolean valid = true;
        if (isBlank(gitUrlField.getValue())) {
            gitUrlField.setErrorMessage("Git repository URL is required");
            gitUrlField.setInvalid(true);
            valid = false;
        } else {
            gitUrlField.setInvalid(false);
        }
        if (isBlank(branchField.getValue())) {
            branchField.setErrorMessage("Branch is required");
            branchField.setInvalid(true);
            valid = false;
        } else {
            branchField.setInvalid(false);
        }
        String ns = namespaceField.getValue();
        if (isBlank(ns)) {
            namespaceField.setErrorMessage("Target namespace is required");
            namespaceField.setInvalid(true);
            valid = false;
        } else if (ns.length() > 63 || !ns.matches(NAMESPACE_PATTERN)) {
            namespaceField.setErrorMessage("Lowercase letters, numbers and hyphens only, max 63 chars");
            namespaceField.setInvalid(true);
            valid = false;
        } else {
            namespaceField.setInvalid(false);
        }
        return valid;
    }

    private boolean validateStep2() {
        boolean valid = true;
        if (isBlank(applicationNameField.getValue())) {
            applicationNameField.setErrorMessage("Application name is required");
            applicationNameField.setInvalid(true);
            valid = false;
        } else {
            applicationNameField.setInvalid(false);
        }
        if (isBlank(imageTagField.getValue())) {
            imageTagField.setErrorMessage("Image tag is required");
            imageTagField.setInvalid(true);
            valid = false;
        } else {
            imageTagField.setInvalid(false);
        }
        return valid;
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
            if (isBlank(field.getValue())) {
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
        if (isBlank(storageClassField.getValue())) {
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
        if (isBlank(mountPathField.getValue())) {
            mountPathField.setErrorMessage("Mount path is required");
            mountPathField.setInvalid(true);
            valid = false;
        } else {
            mountPathField.setInvalid(false);
        }
        return valid;
    }

    private void onBuildAndDeploy() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) {
            showError("No active cluster selected.");
            return;
        }
        deployButton.setEnabled(false);
        backButton.setVisible(false);
        nextButton.setVisible(false);

        VerticalLayout executionArea = buildExecutionArea();
        stepContent.removeAll();
        stepContent.add(executionArea);

        UI ui = UI.getCurrent();
        Thread.ofVirtual().start(() -> runBuildAndDeploy(executionArea, ui));
    }

    private VerticalLayout buildExecutionArea() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        H4 title = new H4("Building image");
        title.getStyle().set("margin-bottom", "0");

        executionStatusBadge = new Span("Pending");
        executionStatusBadge.getElement().getThemeList().add("badge");
        executionStatusBadge.getElement().getThemeList().add("contrast");

        buildLogArea = new Pre();
        styleLogArea(buildLogArea);

        layout.add(title, executionStatusBadge, buildLogArea);
        return layout;
    }

    private void runBuildAndDeploy(VerticalLayout executionArea, UI ui) {
        Cluster cluster = clusterContext.getCluster();
        String imageTag = imageTagField.getValue().trim();
        String[] parts = imageTag.split(":", 2);
        String repository = parts[0];
        String tag = parts.length > 1 ? parts[1] : "latest";

        BuildRequest buildRequest = new BuildRequest(
                gitUrlField.getValue().trim(),
                branchField.getValue().trim(),
                contextPathField.getValue().trim(),
                dockerfilePathField.getValue().trim(),
                repository,
                tag
        );

        ui.access(() -> updateExecutionBadge("Building", "primary"));

        try {
            String jobName = registryService.startBuild(cluster, buildRequest);
            boolean buildSuccess = waitForBuild(cluster, jobName, ui);

            if (!buildSuccess) {
                ui.access(() -> showError("Build failed. Check the logs above."));
                return;
            }

            String builtImage = REGISTRY_PULL_HOST + "/" + imageTag;
            DeployApplicationRequest deployRequest = buildDeployRequest(builtImage);

            ui.access(() -> {
                Span label = new Span("Build complete. Creating Kubernetes resources...");
                label.addClassNames(LumoUtility.FontWeight.BOLD);
                executionArea.add(label);
            });

            DeployApplicationResult result = deployApplicationService.deploy(cluster, deployRequest);

            ui.access(() -> {
                if (result.isFullSuccess()) {
                    clusterContext.setNamespace(deployRequest.namespace());
                    String username = SecurityContextHolder.getContext().getAuthentication().getName();
                    userService.updateActiveNamespace(username, deployRequest.namespace());
                    ui.navigate(TopologiaView.class);
                } else {
                    String created = String.join(", ", result.createdResources());
                    showError("Deploy failed at " + result.failedStep() + ": " + result.failureMessage()
                            + ". Already created: " + created);
                }
            });
        } catch (KubernetesOperationException e) {
            ui.access(() -> showError(e.getMessage()));
        } catch (Exception e) {
            log.error("Build & Deploy failed unexpectedly", e);
            ui.access(() -> showError(e.getMessage()));
        } finally {
            stopPolling();
        }
    }

    private boolean waitForBuild(Cluster cluster, String jobName, UI ui) {
        final boolean[] finished = {false};
        final boolean[] success = {false};

        pollTask = pollExecutor.scheduleAtFixedRate(() -> {
            try {
                var progress = registryService.getBuildProgress(cluster, jobName);
                if (progress.podName() != null) {
                    Optional<String> logs = observabilityService.fetchPodLogs(
                            cluster, BUILD_NAMESPACE, progress.podName(), KANIKO_CONTAINER, BUILD_TAIL_LINES, false);
                    ui.access(() -> {
                        if (buildLogArea != null) {
                            logs.ifPresent(buildLogArea::setText);
                            buildLogArea.getElement().executeJs("this.scrollTop = this.scrollHeight");
                        }
                    });
                }
                if (!"Running".equals(progress.status())) {
                    boolean isComplete = "Complete".equals(progress.status());
                    ui.access(() -> updateExecutionBadge(
                            isComplete ? "Complete" : "Failed",
                            isComplete ? "success" : "error"));
                    success[0] = isComplete;
                    finished[0] = true;
                    stopPolling();
                }
            } catch (Exception e) {
                log.warn("Error polling build progress for {}: {}", jobName, e.getMessage());
                finished[0] = true;
                stopPolling();
            }
        }, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);

        while (!finished[0]) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return success[0];
    }

    private void updateExecutionBadge(String status, String variant) {
        if (executionStatusBadge == null) return;
        executionStatusBadge.setText(status);
        executionStatusBadge.getElement().getThemeList().clear();
        executionStatusBadge.getElement().getThemeList().add("badge");
        executionStatusBadge.getElement().getThemeList().add(variant);
    }

    private DeployApplicationRequest buildDeployRequest(String image) {
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
                namespaceField.getValue().trim(),
                image,
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

    private void stopPolling() {
        if (pollTask != null && !pollTask.isDone()) pollTask.cancel(false);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopPolling();
        pollExecutor.shutdown();
        super.onDetach(detachEvent);
    }

    private void styleLogArea(Pre pre) {
        pre.addClassNames(LumoUtility.FontSize.SMALL);
        pre.getStyle()
                .set("font-family", "monospace")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("overflow", "auto")
                .set("white-space", "pre-wrap")
                .set("overflow-wrap", "anywhere")
                .set("width", "100%")
                .set("height", "350px")
                .set("padding", "var(--lumo-space-m)");
    }

    private String extractRepoName(String gitUrl) {
        if (gitUrl == null || gitUrl.isBlank()) return "";
        String cleaned = gitUrl.trim().replaceAll("\\.git$", "");
        String repoName = cleaned.substring(cleaned.lastIndexOf('/') + 1);
        String slug = repoName.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.length() > 63 ? slug.substring(0, 63) : slug;
    }

    private void showError(String message) {
        Notification notification = Notification.show(
                message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
