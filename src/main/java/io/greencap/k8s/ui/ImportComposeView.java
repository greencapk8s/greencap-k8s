package io.greencap.k8s.ui;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
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
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.RegistryService;
import io.greencap.k8s.kubernetes.StorageService;
import io.greencap.k8s.kubernetes.compose.ComposeDocument;
import io.greencap.k8s.kubernetes.compose.ComposeParseException;
import io.greencap.k8s.kubernetes.compose.ComposeParser;
import io.greencap.k8s.kubernetes.compose.ImportComposeService;
import io.greencap.k8s.kubernetes.dto.BuildRequest;
import io.greencap.k8s.kubernetes.dto.ComposeImportRequest;
import io.greencap.k8s.kubernetes.dto.ImportComposeResult;
import io.greencap.k8s.kubernetes.dto.StorageClassInfo;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Route(value = "deploy/compose", layout = MainLayout.class)
@PageTitle("Deploy from Compose — GreenCap K8s")
@PermitAll
public class ImportComposeView extends VerticalLayout implements BeforeEnterObserver {

    private static final String NAMESPACE_PATTERN = "[a-z0-9]([a-z0-9-]*[a-z0-9])?";
    private static final String REGISTRY_PULL_HOST = "localhost:5000";
    private static final String BUILD_NAMESPACE = "greencap-system";
    private static final String KANIKO_CONTAINER = "kaniko";
    private static final int BUILD_TAIL_LINES = 1000;
    private static final int POLL_INTERVAL_SECONDS = 3;

    private final ClusterContext clusterContext;
    private final ComposeParser composeParser;
    private final ImportComposeService importComposeService;
    private final RegistryService registryService;
    private final ObservabilityService observabilityService;
    private final StorageService storageService;
    private final UserService userService;

    private final TextField gitUrlField = new TextField("Git repository URL");
    private final TextField branchField = new TextField("Branch");
    private final TextField composePathField = new TextField("Path to docker-compose.yml");
    private final TextField namespaceField = new TextField("Target namespace");

    private ComposeDocument parsedDocument;
    private final Map<String, TextField> imageFieldsByService = new LinkedHashMap<>();
    private final Map<String, Map<String, ComboBox<String>>> storageClassFieldsByServiceVolume = new LinkedHashMap<>();
    private final Map<String, Map<String, IntegerField>> storageSizeFieldsByServiceVolume = new LinkedHashMap<>();

    private final Map<String, String> buildJobsByService = new LinkedHashMap<>();
    private final Map<String, Span> buildStatusBadgeByService = new LinkedHashMap<>();
    private Pre buildLogArea;
    private ScheduledFuture<?> pollTask;

    private int currentStep = 1;
    private final HorizontalLayout stepIndicatorRow = new HorizontalLayout();
    private final Div stepContent = new Div();
    private final Button backButton = new Button(VaadinIcon.ARROW_LEFT.create());
    private final Button nextButton = new Button("Next", VaadinIcon.ARROW_RIGHT.create());
    private List<String> defaultStorageClasses = List.of();

    public ImportComposeView(ClusterContext clusterContext,
                              ComposeParser composeParser,
                              ImportComposeService importComposeService,
                              RegistryService registryService,
                              ObservabilityService observabilityService,
                              StorageService storageService,
                              UserService userService) {
        this.clusterContext = clusterContext;
        this.composeParser = composeParser;
        this.importComposeService = importComposeService;
        this.registryService = registryService;
        this.observabilityService = observabilityService;
        this.storageService = storageService;
        this.userService = userService;

        setPadding(true);
        setSpacing(true);
        setMaxWidth("820px");
        setWidthFull();

        initFields();
        initNavigation();

        stepContent.setWidthFull();
        stepIndicatorRow.setWidthFull();
        stepIndicatorRow.setSpacing(true);

        Div spacer = new Div();
        HorizontalLayout footer = new HorizontalLayout(backButton, spacer, nextButton);
        footer.expand(spacer);
        footer.setWidthFull();
        footer.setAlignItems(Alignment.CENTER);
        footer.addClassNames(LumoUtility.Padding.Vertical.MEDIUM);

        add(buildModeSelector(), stepIndicatorRow, stepContent, footer);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        loadStorageClasses();
        renderStep(1);
    }

    private HorizontalLayout buildModeSelector() {
        Button imageBtn = new Button("Deploy from Image", VaadinIcon.ROCKET.create());
        Button dockerfileBtn = new Button("Deploy from Dockerfile", VaadinIcon.CODE.create());
        Button composeBtn = new Button("Deploy from Compose", VaadinIcon.FILE_CODE.create());

        Button helmBtn = new Button("Deploy from Helm", VaadinIcon.PACKAGE.create());

        imageBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        dockerfileBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        composeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        helmBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        imageBtn.addClickListener(e -> UI.getCurrent().navigate(DeployApplicationView.class));
        dockerfileBtn.addClickListener(e -> UI.getCurrent().navigate(DeployFromDockerfileView.class));
        helmBtn.addClickListener(e -> UI.getCurrent().navigate(DeployFromHelmView.class));

        HorizontalLayout selector = new HorizontalLayout(imageBtn, dockerfileBtn, composeBtn, helmBtn);
        selector.setSpacing(true);
        return selector;
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

        composePathField.setWidthFull();
        composePathField.setValue("docker-compose.yml");
        composePathField.setRequired(true);

        namespaceField.setWidthFull();
        namespaceField.setHelperText("Lowercase letters, numbers and hyphens.");
        namespaceField.setRequired(true);
    }

    private void initNavigation() {
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backButton.addClickListener(e -> navigateBack());
        backButton.setVisible(false);

        nextButton.setIconAfterText(true);
        nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        nextButton.addClickListener(e -> navigateNext());
    }

    private void renderStep(int step) {
        currentStep = step;
        updateStepIndicator();
        stepContent.removeAll();
        stepContent.add(buildStepContent(step));
        backButton.setVisible(step == 2);
        nextButton.setVisible(step < 3);
        nextButton.setText(step == 2 ? "Deploy" : "Next");
        nextButton.setIcon(step == 2 ? VaadinIcon.ROCKET.create() : VaadinIcon.ARROW_RIGHT.create());
        nextButton.setIconAfterText(true);
        if (step == 1) gitUrlField.focus();
    }

    private void updateStepIndicator() {
        String[] labels = {"Source & Target", "Review", "Execution"};
        stepIndicatorRow.removeAll();
        for (int i = 0; i < labels.length; i++) {
            int step = i + 1;
            Span badge = new Span(step + ". " + labels[i]);
            badge.getElement().getThemeList().add("badge");
            if (step == currentStep) badge.getElement().getThemeList().add("primary");
            else if (step < currentStep) badge.getElement().getThemeList().add("success");
            else badge.getElement().getThemeList().add("contrast");
            stepIndicatorRow.add(badge);
        }
    }

    private com.vaadin.flow.component.Component buildStepContent(int step) {
        return switch (step) {
            case 1 -> buildStep1();
            case 2 -> buildStep2Review();
            case 3 -> buildStep3Execution();
            default -> new Div();
        };
    }

    private FormLayout buildStep1() {
        FormLayout form = new FormLayout(gitUrlField, branchField, composePathField, namespaceField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }

    private boolean validateStep1() {
        boolean valid = true;
        if (isBlank(gitUrlField.getValue())) {
            gitUrlField.setErrorMessage("Git repository URL is required");
            gitUrlField.setInvalid(true);
            valid = false;
        } else { gitUrlField.setInvalid(false); }
        if (isBlank(branchField.getValue())) {
            branchField.setErrorMessage("Branch is required");
            branchField.setInvalid(true);
            valid = false;
        } else { branchField.setInvalid(false); }
        if (isBlank(composePathField.getValue())) {
            composePathField.setErrorMessage("Path is required");
            composePathField.setInvalid(true);
            valid = false;
        } else { composePathField.setInvalid(false); }
        String ns = namespaceField.getValue();
        if (isBlank(ns)) {
            namespaceField.setErrorMessage("Namespace is required");
            namespaceField.setInvalid(true);
            valid = false;
        } else if (ns.length() > 63 || !ns.matches(NAMESPACE_PATTERN)) {
            namespaceField.setErrorMessage("Lowercase letters, numbers and hyphens only, max 63 chars");
            namespaceField.setInvalid(true);
            valid = false;
        } else { namespaceField.setInvalid(false); }
        return valid;
    }

    private VerticalLayout buildStep2Review() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        imageFieldsByService.clear();
        storageClassFieldsByServiceVolume.clear();
        storageSizeFieldsByServiceVolume.clear();

        for (ComposeDocument.ParsedService service : parsedDocument.services()) {
            layout.add(buildServiceReviewPanel(service));
        }
        if (!parsedDocument.ignoredDirectives().isEmpty()) {
            layout.add(buildWarningBox("Ignored directives (no Kubernetes equivalent): "
                    + String.join(", ", parsedDocument.ignoredDirectives())));
        }
        if (parsedDocument.services().stream().anyMatch(s -> !s.bindMounts().isEmpty())) {
            layout.add(buildWarningBox(
                    "Bind-mount volumes (e.g. ./local:/path) have no Kubernetes equivalent and were ignored."));
        }
        if (parsedDocument.services().stream().anyMatch(s -> !s.dependsOn().isEmpty())) {
            layout.add(buildWarningBox(
                    "depends_on: has no effect in Kubernetes — use readiness probes for startup dependencies."));
        }
        return layout;
    }

    private VerticalLayout buildServiceReviewPanel(ComposeDocument.ParsedService service) {
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(true);
        panel.setSpacing(true);
        panel.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        H4 header = new H4(service.name());
        header.getStyle().set("margin", "0");
        panel.add(header);

        panel.add(buildReviewItem("Deployment", service.name()));
        if (!service.containerPorts().isEmpty()) {
            panel.add(buildReviewItem("Service (ClusterIP)",
                    service.name() + " — port " + service.containerPorts().get(0)));
        }
        if (service.hasNonSensitiveEnv()) panel.add(buildReviewItem("ConfigMap", service.name() + "-config"));
        if (service.hasSensitiveEnv())    panel.add(buildReviewItem("Secret",    service.name() + "-secret"));

        if (service.hasBuild()) {
            String repoAndTag = service.image() != null ? service.image() : sanitizeK8sName(service.name()) + ":latest";
            String defaultImage = REGISTRY_PULL_HOST + "/" + namespaceField.getValue().trim() + "/" + repoAndTag;
            TextField imageField = new TextField("Image name (will be built and pushed to Registry)");
            imageField.setValue(defaultImage);
            imageField.setWidthFull();
            imageFieldsByService.put(service.name(), imageField);
            panel.add(imageField);
        }

        for (ComposeDocument.VolumeEntry volume : service.namedVolumes()) {
            panel.add(buildVolumeConfigRow(service.name(), volume));
        }
        return panel;
    }

    private VerticalLayout buildVolumeConfigRow(String serviceName, ComposeDocument.VolumeEntry volume) {
        Span pvcLabel = new Span("PVC: " + volumeDisplay(volume.name()) + " → " + volume.mountPath());
        pvcLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        ComboBox<String> scField = new ComboBox<>("Storage class");
        scField.setItems(defaultStorageClasses);
        if (!defaultStorageClasses.isEmpty()) scField.setValue(defaultStorageClasses.get(0));
        scField.setWidthFull();

        IntegerField sizeField = new IntegerField("Size (Gi)");
        sizeField.setValue(1);
        sizeField.setMin(1);
        sizeField.setWidthFull();

        storageClassFieldsByServiceVolume.computeIfAbsent(serviceName, k -> new LinkedHashMap<>()).put(volume.name(), scField);
        storageSizeFieldsByServiceVolume.computeIfAbsent(serviceName, k -> new LinkedHashMap<>()).put(volume.name(), sizeField);

        FormLayout form = new FormLayout(scField, sizeField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        VerticalLayout container = new VerticalLayout(pvcLabel, form);
        container.setPadding(false);
        container.setSpacing(true);
        return container;
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

    private Div buildWarningBox(String message) {
        Div box = new Div(new Span("⚠ "), new Span(message));
        box.getStyle()
                .set("background", "var(--lumo-warning-color-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("font-size", "var(--lumo-font-size-s)");
        return box;
    }

    private VerticalLayout buildStep3Execution() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        buildJobsByService.clear();
        buildStatusBadgeByService.clear();

        List<ComposeDocument.ParsedService> buildServices = parsedDocument.services().stream()
                .filter(ComposeDocument.ParsedService::hasBuild).toList();

        if (!buildServices.isEmpty()) {
            layout.add(buildBuildSection(buildServices));
        } else {
            startProvisioning(layout, UI.getCurrent());
        }
        return layout;
    }

    private VerticalLayout buildBuildSection(List<ComposeDocument.ParsedService> buildServices) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);

        H4 title = new H4("Building images");
        title.getStyle().set("margin-bottom", "0");
        section.add(title);

        Div statusGrid = new Div();
        statusGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr auto")
                .set("gap", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("align-items", "center")
                .set("width", "100%");

        for (ComposeDocument.ParsedService service : buildServices) {
            Span badge = new Span("Pending");
            badge.getElement().getThemeList().add("badge");
            badge.getElement().getThemeList().add("contrast");
            buildStatusBadgeByService.put(service.name(), badge);

            Span nameLabel = new Span(service.name());
            nameLabel.addClassNames(LumoUtility.FontWeight.BOLD);

            statusGrid.add(nameLabel, badge);
        }
        section.add(statusGrid);

        buildLogArea = new Pre();
        styleLogArea(buildLogArea);
        section.add(buildLogArea);
        section.add(new Hr());

        UI ui = UI.getCurrent();
        AsyncTasks.execute(() -> runBuildsSequentially(buildServices, section, ui));
        return section;
    }

    private void runBuildsSequentially(List<ComposeDocument.ParsedService> buildServices,
                                        VerticalLayout section, UI ui) {
        Cluster cluster = clusterContext.getCluster();

        for (ComposeDocument.ParsedService service : buildServices) {
            String resolvedImage = imageFieldsByService.containsKey(service.name())
                    ? imageFieldsByService.get(service.name()).getValue()
                    : REGISTRY_PULL_HOST + "/" + namespaceField.getValue().trim() + "/" + sanitizeK8sName(service.name()) + ":latest";

            String imageForBuild = resolvedImage.startsWith(REGISTRY_PULL_HOST + "/")
                    ? resolvedImage.substring(REGISTRY_PULL_HOST.length() + 1)
                    : resolvedImage;
            String[] imageParts = imageForBuild.split(":", 2);
            String repository = imageParts[0];
            String tag = imageParts.length > 1 ? imageParts[1] : "latest";

            String resolvedContext = resolveRepoBuildContext(
                    composePathField.getValue().trim(), service.build().context());

            BuildRequest buildRequest = new BuildRequest(
                    gitUrlField.getValue().trim(),
                    branchField.getValue().trim(),
                    resolvedContext,
                    service.build().dockerfile(),
                    repository,
                    tag
            );

            ui.access(() -> updateBuildBadge(service.name(), "Building", "primary"));

            try {
                String jobName = registryService.startBuild(cluster, buildRequest);
                buildJobsByService.put(service.name(), jobName);
                boolean isComplete = waitForBuild(cluster, service.name(), jobName, ui);
                if (!isComplete) log.warn("Build incomplete for service {}", service.name());
            } catch (Exception e) {
                log.error("Build failed for service {}: {}", service.name(), e.getMessage());
                ui.access(() -> {
                    updateBuildBadge(service.name(), "Failed", "error");
                    if (buildLogArea != null) {
                        buildLogArea.setText(buildLogArea.getText() + "\nBuild error: " + e.getMessage());
                    }
                });
            }
        }

        stopPolling();
        ui.access(() -> startProvisioning(section, ui));
    }

    private boolean waitForBuild(Cluster cluster, String serviceName, String jobName, UI ui) {
        final boolean[] finished = {false};
        final boolean[] success = {false};

        Runnable pollCommand = () -> {
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
                    ui.access(() -> updateBuildBadge(serviceName,
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
        };
        pollTask = AsyncTasks.schedulePolling(pollCommand, Duration.ZERO, Duration.ofSeconds(POLL_INTERVAL_SECONDS));

        while (!finished[0]) {
            try { Thread.sleep(500); } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return success[0];
    }

    private void updateBuildBadge(String serviceName, String status, String variant) {
        Span badge = buildStatusBadgeByService.get(serviceName);
        if (badge == null) return;
        badge.setText(status);
        badge.getElement().getThemeList().clear();
        badge.getElement().getThemeList().add("badge");
        badge.getElement().getThemeList().add(variant);
    }

    private void startProvisioning(VerticalLayout section, UI ui) {
        Span label = new Span("Creating Kubernetes resources...");
        label.addClassNames(LumoUtility.FontWeight.BOLD);
        section.add(label);

        Cluster cluster = clusterContext.getCluster();
        ComposeImportRequest request = buildImportRequest();

        AsyncTasks.execute(() -> {
            try {
                ImportComposeResult result = importComposeService.provision(cluster, parsedDocument, request);
                ui.access(() -> renderResult(section, result, request.namespace(), ui));
            } catch (KubernetesOperationException e) {
                ui.access(() -> {
                    showError(e.getMessage());
                    section.remove(label);
                });
            }
        });
    }

    private void renderResult(VerticalLayout section, ImportComposeResult result, String namespace, UI ui) {
        section.removeAll();
        section.add(new H4(result.isFullSuccess() ? "All resources created successfully" : "Completed with errors"));

        for (ImportComposeResult.ServiceResult sr : result.serviceResults()) {
            VerticalLayout block = new VerticalLayout();
            block.setPadding(false);
            block.setSpacing(false);
            Span serviceHeader = new Span(sr.serviceName());
            serviceHeader.addClassNames(LumoUtility.FontWeight.BOLD);
            block.add(serviceHeader);
            for (String resource : sr.createdResources()) {
                Span item = new Span("✓ " + resource);
                item.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SUCCESS);
                block.add(item);
            }
            if (!sr.isSuccess()) {
                Span failure = new Span("✗ " + sr.failureMessage());
                failure.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.ERROR);
                block.add(failure);
            }
            section.add(block);
        }

        Button viewTopologyBtn = new Button("View in Topology", VaadinIcon.CLUSTER.create());
        viewTopologyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        viewTopologyBtn.addClickListener(e -> navigateToTopology(namespace, ui));
        section.add(viewTopologyBtn);

        if (result.isFullSuccess()) navigateToTopology(namespace, ui);
    }

    private void navigateToTopology(String namespace, UI ui) {
        clusterContext.setNamespace(namespace);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.updateActiveNamespace(username, namespace);
        MainLayout.refreshNamespaceSelector(ui);
        ui.navigate(TopologiaView.class);
    }

    private void navigateNext() {
        if (currentStep == 1) fetchAndParse();
        else if (currentStep == 2) renderStep(3);
    }

    private void navigateBack() {
        if (currentStep == 2) renderStep(1);
    }

    private void fetchAndParse() {
        if (!validateStep1()) return;
        nextButton.setEnabled(false);
        nextButton.setText("Fetching...");
        UI ui = UI.getCurrent();
        AsyncTasks.execute(() -> {
            try {
                ComposeDocument document = composeParser.fetch(
                        gitUrlField.getValue().trim(),
                        branchField.getValue().trim(),
                        composePathField.getValue().trim());
                ui.access(() -> {
                    this.parsedDocument = document;
                    nextButton.setEnabled(true);
                    nextButton.setText("Next");
                    renderStep(2);
                });
            } catch (ComposeParseException e) {
                ui.access(() -> {
                    showError(e.getMessage());
                    nextButton.setEnabled(true);
                    nextButton.setText("Next");
                });
            }
        });
    }

    private ComposeImportRequest buildImportRequest() {
        List<ComposeImportRequest.ServiceConfig> configs = new ArrayList<>();
        for (ComposeDocument.ParsedService service : parsedDocument.services()) {
            String image = imageFieldsByService.containsKey(service.name())
                    ? imageFieldsByService.get(service.name()).getValue()
                    : (service.image() != null ? service.image() : service.name() + ":latest");
            List<ComposeImportRequest.VolumeConfig> volumes = new ArrayList<>();
            for (ComposeDocument.VolumeEntry volume : service.namedVolumes()) {
                String sc = Optional.ofNullable(storageClassFieldsByServiceVolume.get(service.name()))
                        .map(m -> m.get(volume.name())).map(ComboBox::getValue).orElse("");
                int size = Optional.ofNullable(storageSizeFieldsByServiceVolume.get(service.name()))
                        .map(m -> m.get(volume.name())).map(IntegerField::getValue).orElse(1);
                volumes.add(new ComposeImportRequest.VolumeConfig(volume.name(), volume.mountPath(), sc, size));
            }
            configs.add(new ComposeImportRequest.ServiceConfig(service.name(), image, volumes));
        }
        return new ComposeImportRequest(namespaceField.getValue().trim(), configs);
    }

    private String resolveRepoBuildContext(String composePath, String buildContext) {
        String composeDir = composePath.contains("/")
                ? composePath.substring(0, composePath.lastIndexOf('/')) : "";
        String cleaned = buildContext.startsWith("./") ? buildContext.substring(2) : buildContext;
        while (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
        if (cleaned.isEmpty() || ".".equals(cleaned)) return composeDir;
        return composeDir.isEmpty() ? cleaned : composeDir + "/" + cleaned;
    }

    private void loadStorageClasses() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        UI ui = UI.getCurrent();
        AsyncTasks.execute(() -> {
            try {
                List<String> names = storageService.listStorageClasses(cluster).stream()
                        .map(StorageClassInfo::name).toList();
                Optional<String> defaultClass = storageService.findDefaultStorageClassName(cluster);
                ui.access(() -> {
                    List<String> ordered = new ArrayList<>();
                    defaultClass.ifPresent(ordered::add);
                    names.stream().filter(n -> !ordered.contains(n)).forEach(ordered::add);
                    this.defaultStorageClasses = ordered;
                });
            } catch (Exception e) {
                log.debug("Failed to load StorageClasses: {}", e.getMessage());
            }
        });
    }

    private void stopPolling() {
        if (pollTask != null && !pollTask.isDone()) pollTask.cancel(false);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopPolling();
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
                .set("height", "250px")
                .set("padding", "var(--lumo-space-m)");
    }

    private String volumeDisplay(String name) {
        return name.length() > 20 ? name.substring(0, 20) + "…" : name;
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    private void showError(String message) {
        Notification n = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private String sanitizeK8sName(String name) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.length() > 63 ? slug.substring(0, 63) : slug;
    }

    private String extractRepoName(String gitUrl) {
        if (gitUrl == null || gitUrl.isBlank()) return "";
        String cleaned = gitUrl.trim().replaceAll("\\.git$", "");
        String repoName = cleaned.substring(cleaned.lastIndexOf('/') + 1);
        String slug = repoName.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.length() > 63 ? slug.substring(0, 63) : slug;
    }
}
