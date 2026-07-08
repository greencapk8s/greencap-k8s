package io.greencap.k8s.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
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
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.RegistryService;
import io.greencap.k8s.kubernetes.dto.BuildProgress;
import jakarta.annotation.security.PermitAll;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Route(value = "registry/build/:jobName", layout = MainLayout.class)
@PageTitle("Build — GreenCap K8s")
@PermitAll
public class BuildLogsView extends VerticalLayout implements BeforeEnterObserver {

    private static final String BUILD_NAMESPACE = "greencap-system";
    private static final String KANIKO_CONTAINER = "kaniko";
    private static final int POLL_INTERVAL_SECONDS = 3;
    private static final int TAIL_LINES = 1000;
    private static final String WAITING_MESSAGE = "Waiting for build pod to start...";

    private final RegistryService registryService;
    private final ObservabilityService observabilityService;
    private final ClusterContext clusterContext;

    private final Span titleSpan = new Span();
    private final Span statusBadge = new Span();
    private final Button pauseResumeBtn = new Button();
    private final Pre logContent = new Pre();

    private ScheduledFuture<?> pollTask;

    private String jobName;
    private boolean polling = true;

    public BuildLogsView(RegistryService registryService, ObservabilityService observabilityService, ClusterContext clusterContext) {
        this.registryService = registryService;
        this.observabilityService = observabilityService;
        this.clusterContext = clusterContext;

        setSizeFull();
        setPadding(true);

        styleLogContent();
        logContent.setText(WAITING_MESSAGE);

        pauseResumeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        pauseResumeBtn.addClickListener(e -> togglePolling());
        updatePauseResumeButton();

        add(buildHeader(), logContent);
        setFlexGrow(1, logContent);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        RouteParameters params = event.getRouteParameters();
        jobName = params.get("jobName").orElse("");
        titleSpan.setText("Build / " + jobName);

        if (clusterContext.getCluster() == null) {
            logContent.setText("No active cluster.");
            return;
        }

        startPolling();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopPolling();
        super.onDetach(detachEvent);
    }

    private void startPolling() {
        stopPolling();
        polling = true;
        updatePauseResumeButton();
        UI ui = UI.getCurrent();
        Runnable pollCommand = () -> ui.access(this::fetchAndRenderProgress);
        pollTask = AsyncTasks.schedulePolling(pollCommand, Duration.ZERO, Duration.ofSeconds(POLL_INTERVAL_SECONDS));
    }

    private void stopPolling() {
        if (pollTask != null && !pollTask.isDone()) {
            pollTask.cancel(false);
        }
    }

    private void fetchAndRenderProgress() {
        if (clusterContext.getCluster() == null) return;

        try {
            BuildProgress progress = registryService.getBuildProgress(clusterContext.getCluster(), jobName);
            updateStatusBadge(progress.status());

            if (progress.podName() != null) {
                Optional<String> logs = observabilityService.fetchPodLogs(
                        clusterContext.getCluster(), BUILD_NAMESPACE, progress.podName(), KANIKO_CONTAINER, TAIL_LINES, false);
                logContent.setText(logs.orElse(""));
                scrollToBottom();
            }

            if (!"Running".equals(progress.status())) {
                stopPolling();
                polling = false;
                updatePauseResumeButton();
            }
        } catch (KubernetesOperationException e) {
            stopPolling();
            polling = false;
            updatePauseResumeButton();
            showError(e.getMessage());
        }
    }

    private void updateStatusBadge(String status) {
        statusBadge.setText(status);
        statusBadge.getElement().getThemeList().clear();
        statusBadge.getElement().getThemeList().add("badge");
        switch (status) {
            case "Complete" -> statusBadge.getElement().getThemeList().add("success");
            case "Failed" -> statusBadge.getElement().getThemeList().add("error");
            default -> {}
        }
    }

    private void scrollToBottom() {
        logContent.getElement().executeJs("this.scrollTop = this.scrollHeight");
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

    private HorizontalLayout buildHeader() {
        Button backBtn = new Button(VaadinIcon.ARROW_LEFT.create(), e -> UI.getCurrent().navigate(RegistryView.class));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        backBtn.getElement().setAttribute("title", "Back to Registry");

        titleSpan.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout header = new HorizontalLayout(backBtn, titleSpan, statusBadge, pauseResumeBtn);
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
