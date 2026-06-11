package io.greencap.k8s.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.ObservabilityService;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Route(value = "logs/pod/:namespace/:name", layout = MainLayout.class)
@PageTitle("Pod Logs — GreenCap K8s")
@PermitAll
public class PodLogsView extends VerticalLayout implements BeforeEnterObserver {

    private static final String NO_PREVIOUS_LOG_MESSAGE = "No previous log available for this container.";

    private final ObservabilityService observabilityService;
    private final ClusterContext clusterContext;

    private final Span titleSpan = new Span();
    private final Select<String> containerSelect = new Select<>();
    private final Select<Integer> tailSelect = new Select<>();
    private final Select<Integer> pollIntervalSelect = new Select<>();
    private final Checkbox previousCheckbox = new Checkbox("Previous container");
    private final Button pauseResumeBtn = new Button();
    private final Pre logContent = new Pre();

    private final ScheduledExecutorService pollExecutor =
            Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private ScheduledFuture<?> pollTask;

    private String namespace;
    private String podName;
    private boolean polling = true;

    public PodLogsView(ObservabilityService observabilityService, ClusterContext clusterContext) {
        this.observabilityService = observabilityService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        styleLogContent();
        buildToolbar();

        add(buildHeader(), logContent);
        setFlexGrow(1, logContent);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters params = event.getRouteParameters();
        namespace = params.get("namespace").orElse("");
        podName   = params.get("name").orElse("");

        titleSpan.setText("Logs / " + podName);

        if (clusterContext.getCluster() == null) {
            logContent.setText("No active cluster.");
            return;
        }

        loadContainers();
        startPolling();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopPolling();
        pollExecutor.shutdown();
        super.onDetach(detachEvent);
    }

    private void loadContainers() {
        try {
            List<String> containers = observabilityService.listContainersForPod(
                    clusterContext.getCluster(), namespace, podName);

            boolean multiContainer = containers.size() > 1;
            containerSelect.setVisible(multiContainer);
            if (multiContainer) {
                containerSelect.setItems(containers);
                containerSelect.setValue(containers.get(0));
            }
        } catch (KubernetesOperationException e) {
            showError(e.getMessage());
        }
    }

    private void startPolling() {
        stopPolling();
        polling = true;
        updatePauseResumeButton();
        int intervalSeconds = pollIntervalSelect.getValue() != null ? pollIntervalSelect.getValue() : 3;
        UI ui = UI.getCurrent();
        pollTask = pollExecutor.scheduleAtFixedRate(
                () -> ui.access(this::fetchAndRenderLogs),
                0, intervalSeconds, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (pollTask != null && !pollTask.isDone()) {
            pollTask.cancel(false);
        }
    }

    private void fetchAndRenderLogs() {
        if (clusterContext.getCluster() == null) return;

        String container = containerSelect.isVisible() ? containerSelect.getValue() : null;
        int tailLines    = tailSelect.getValue() != null ? tailSelect.getValue() : 100;
        boolean previous = previousCheckbox.getValue();

        try {
            Optional<String> logs = observabilityService.fetchPodLogs(
                    clusterContext.getCluster(), namespace, podName, container, tailLines, previous);

            if (previous && logs.isEmpty()) {
                logContent.setText(NO_PREVIOUS_LOG_MESSAGE);
            } else {
                logContent.setText(logs.orElse(""));
                scrollToBottom();
            }
        } catch (KubernetesOperationException e) {
            showError(e.getMessage());
        }
    }

    private void scrollToBottom() {
        logContent.getElement().executeJs(
                "this.scrollTop = this.scrollHeight");
    }

    private void styleLogContent() {
        logContent.addClassNames(LumoUtility.FontSize.SMALL);
        logContent.getStyle()
                .set("font-family", "monospace")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("overflow", "auto")
                .set("overflow-x", "hidden")
                .set("white-space", "pre-wrap")
                .set("overflow-wrap", "anywhere")
                .set("width", "100%")
                .set("flex", "1")
                .set("padding", "var(--lumo-space-m)");
    }

    private void buildToolbar() {
        containerSelect.setPlaceholder("Container");
        containerSelect.setWidth("180px");
        containerSelect.setVisible(false);
        containerSelect.addValueChangeListener(e -> {
            if (polling) {
                stopPolling();
                startPolling();
            } else {
                fetchAndRenderLogs();
            }
        });

        tailSelect.setItems(100, 500, 1000);
        tailSelect.setValue(100);
        tailSelect.setWidth("100px");
        tailSelect.addValueChangeListener(e -> {
            if (polling) {
                stopPolling();
                startPolling();
            } else {
                fetchAndRenderLogs();
            }
        });

        previousCheckbox.setValue(false);
        previousCheckbox.addValueChangeListener(e -> {
            if (polling) {
                stopPolling();
                startPolling();
            } else {
                fetchAndRenderLogs();
            }
        });

        pollIntervalSelect.setItems(1, 3, 5, 10);
        pollIntervalSelect.setValue(3);
        pollIntervalSelect.setWidth("80px");
        pollIntervalSelect.setItemLabelGenerator(s -> s + "s");
        pollIntervalSelect.addValueChangeListener(e -> {
            if (polling) {
                stopPolling();
                startPolling();
            }
        });

        updatePauseResumeButton();
        pauseResumeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        pauseResumeBtn.addClickListener(e -> togglePolling());
    }

    private HorizontalLayout buildHeader() {
        Button backBtn = new Button(VaadinIcon.ARROW_LEFT.create(),
                e -> UI.getCurrent().getPage().getHistory().go(-1));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        backBtn.getElement().setAttribute("title", "Back");

        titleSpan.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);

        Span linesLabel = new Span("Lines:");
        Span pollLabel = new Span("Poll:");

        HorizontalLayout header = new HorizontalLayout(
                backBtn, titleSpan, containerSelect, linesLabel, tailSelect, previousCheckbox, pollLabel, pollIntervalSelect, pauseResumeBtn);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.setWidthFull();
        header.expand(titleSpan);
        return header;
    }

    private void togglePolling() {
        if (polling) {
            stopPolling();
            polling = false;
        } else {
            startPolling();
        }
        updatePauseResumeButton();
    }

    private void updatePauseResumeButton() {
        if (polling) {
            pauseResumeBtn.setIcon(VaadinIcon.PAUSE.create());
            pauseResumeBtn.setText("Pause");
        } else {
            pauseResumeBtn.setIcon(VaadinIcon.PLAY.create());
            pauseResumeBtn.setText("Resume");
        }
    }

    private void showError(String message) {
        Notification notification = Notification.show(
                message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
