package io.greencap.k8s.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.greencap.k8s.domain.cluster.Cluster;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.ObservabilityService;
import io.greencap.k8s.kubernetes.WorkloadService;
import io.greencap.k8s.kubernetes.dto.PodInfo;
import jakarta.annotation.security.PermitAll;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Route(value = "workloads/pods", layout = MainLayout.class)
@PageTitle("Pods — GreenCap K8s")
@PermitAll
public class PodsView extends VerticalLayout implements BeforeEnterObserver, Refreshable {

    private static final String HELP_TITLE = "Pods";
    private static final String HELP_TEXT = "A Pod is the smallest Workload unit in Kubernetes — one or more containers running together, sharing network and storage.";

    private final WorkloadService workloadService;
    private final ObservabilityService observabilityService;
    private final ClusterContext clusterContext;
    private final GridSelectionMemory selectionMemory;

    private final Grid<PodInfo> podGrid = new Grid<>(PodInfo.class, false);
    private final VerticalLayout noClusterMessage;
    private final VerticalLayout clusterErrorMessage;
    private final HorizontalLayout jobFilterBanner = new HorizontalLayout();
    private final Checkbox hideCompletedJobPodsCheckbox = new Checkbox("Hide completed Job pods", true);

    private final List<PodInfo> allItems = new ArrayList<>();
    private final ListDataProvider<PodInfo> dataProvider = new ListDataProvider<>(allItems);

    private String jobFilter = "";
    private TextField nameFilter;
    private TextField statusFilter;
    private TextField nodeFilter;

    public PodsView(WorkloadService workloadService, ObservabilityService observabilityService, ClusterContext clusterContext, GridSelectionMemory selectionMemory) {
        this.workloadService = workloadService;
        this.observabilityService = observabilityService;
        this.clusterContext = clusterContext;
        this.selectionMemory = selectionMemory;

        setSizeFull();
        setPadding(true);

        noClusterMessage = UiConstants.buildNoClusterMessage();
        clusterErrorMessage = UiConstants.buildClusterUnreachableMessage();
        buildJobFilterBanner();
        buildPodGrid();
        UiConstants.configureSingleSelection(podGrid, selectionMemory, getClass().getSimpleName(), PodInfo::name);

        boolean canDelete = true;
        List<UiConstants.SelectionAction<PodInfo>> selectionActions = List.of(
                UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Delete", canDelete, this::openDeleteDialog),
                UiConstants.SelectionAction.of(VaadinIcon.CODE, "View Manifest",
                        p -> UI.getCurrent().navigate("yaml/pod/" + p.namespace() + "/" + p.name())),
                UiConstants.SelectionAction.of(VaadinIcon.RECORDS, "Events",
                        p -> EventsDialog.open(observabilityService, clusterContext, "Pod", p.name(), p.namespace()))
        );

        add(UiConstants.buildSectionHeader("Pods", this::loadPods, HELP_TITLE, HELP_TEXT, podGrid, selectionActions),
                hideCompletedJobPodsCheckbox, jobFilterBanner, noClusterMessage, clusterErrorMessage, podGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        String jobParam = event.getLocation().getQueryParameters()
                .getParameters().getOrDefault("job", List.of()).stream().findFirst().orElse("");
        applyJobFilter(jobParam);

        boolean hasCluster = clusterContext.getCluster() != null;
        noClusterMessage.setVisible(!hasCluster);
        clusterErrorMessage.setVisible(false);
        podGrid.setVisible(hasCluster);
        if (hasCluster) {
            loadPodsAsync(UI.getCurrent());
        }
    }

    private void buildJobFilterBanner() {
        jobFilterBanner.setVisible(false);
        jobFilterBanner.setAlignItems(Alignment.CENTER);
        jobFilterBanner.getStyle().set("padding", "4px 0");
    }

    private void applyJobFilter(String jobName) {
        jobFilter = jobName == null ? "" : jobName.trim();
        hideCompletedJobPodsCheckbox.setValue(jobFilter.isBlank());
        jobFilterBanner.removeAll();
        jobFilterBanner.setVisible(!jobFilter.isBlank());
        if (!jobFilter.isBlank()) {
            Span label = new Span("Showing pods for Job: ");
            Span jobBadge = new Span(jobFilter);
            jobBadge.getElement().getThemeList().add("badge");
            jobBadge.getElement().getThemeList().add("contrast");

            var closeIcon = VaadinIcon.CLOSE_SMALL.create();
            Button dismissBtn = new Button(closeIcon);
            dismissBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
            dismissBtn.getElement().setAttribute("title", "Clear filter");
            dismissBtn.addClickListener(e -> {
                applyJobFilter("");
                dataProvider.refreshAll();
            });

            jobFilterBanner.add(label, jobBadge, dismissBtn);
        }
        if (dataProvider != null) {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(podGrid, dataProvider, PodInfo::name);
        }
    }

    private void buildPodGrid() {
        nameFilter   = buildFilterField();
        statusFilter = buildFilterField();
        nodeFilter   = buildFilterField();

        var nameCol   = podGrid.addColumn(PodInfo::name).setHeader("Name").setSortable(true).setFlexGrow(2).setResizable(true);
        var statusCol = podGrid.addComponentColumn(p -> phaseBadge(p.phase())).setHeader("Status").setWidth("120px").setResizable(true);
        var nodeCol   = podGrid.addColumn(PodInfo::node).setHeader("Node").setFlexGrow(1).setResizable(true);
        podGrid.addColumn(PodInfo::restarts).setHeader("Restarts").setWidth("90px").setResizable(true);
        podGrid.addColumn(PodInfo::age).setHeader("Age").setWidth("80px").setResizable(true);
        UiConstants.addActionsColumn(podGrid, 1, p -> {
            var logsIcon = VaadinIcon.TERMINAL.create();
            logsIcon.setSize(UiConstants.ICON_SIZE);
            Button logsBtn = new Button(logsIcon);
            logsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            logsBtn.getElement().setAttribute("title", "Logs");
            logsBtn.addClickListener(e -> UI.getCurrent().navigate("logs/pod/" + p.namespace() + "/" + p.name()));
            return List.of(logsBtn);
        });

        dataProvider.setFilter(item ->
            matches(item.name(), nameFilter.getValue()) &&
            matches(item.phase(), statusFilter.getValue()) &&
            matches(item.node(), nodeFilter.getValue()) &&
            (jobFilter.isBlank() || jobFilter.equals(item.jobName())) &&
            (!hideCompletedJobPodsCheckbox.getValue() || !isCompletedJobPod(item)));

        nameFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(podGrid, dataProvider, PodInfo::name);
        });
        statusFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(podGrid, dataProvider, PodInfo::name);
        });
        nodeFilter.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(podGrid, dataProvider, PodInfo::name);
        });
        hideCompletedJobPodsCheckbox.addValueChangeListener(e -> {
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(podGrid, dataProvider, PodInfo::name);
        });

        podGrid.setDataProvider(dataProvider);

        HeaderRow filterRow = podGrid.appendHeaderRow();
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(statusCol).setComponent(statusFilter);
        filterRow.getCell(nodeCol).setComponent(nodeFilter);

        podGrid.setSizeFull();
        podGrid.setVisible(false);
    }

    private boolean loadPods() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return false;
        String namespace = clusterContext.getNamespace();
        try {
            List<PodInfo> items = workloadService.listPods(cluster, namespace);
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(podGrid, dataProvider, PodInfo::name);
            clusterErrorMessage.setVisible(false);
            podGrid.setVisible(true);
            return true;
        } catch (KubernetesOperationException e) {
            allItems.clear();
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(podGrid, dataProvider, PodInfo::name);
            clusterErrorMessage.setVisible(true);
            podGrid.setVisible(false);
            return false;
        }
    }

    private void loadPodsAsync(UI ui) {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        String namespace = clusterContext.getNamespace();
        AsyncTasks.execute(() -> {
            try {
                List<PodInfo> items = workloadService.listPods(cluster, namespace);
                ui.access(() -> {
                    allItems.clear();
                    allItems.addAll(items);
                    dataProvider.refreshAll();
                    UiConstants.selectFirstOrPreserve(podGrid, dataProvider, PodInfo::name);
                    clusterErrorMessage.setVisible(false);
                    podGrid.setVisible(true);
                });
            } catch (KubernetesOperationException e) {
                log.debug("Failed to load pods for cluster {}: {}", cluster.getName(), e.getMessage());
                ui.access(() -> {
                    allItems.clear();
                    dataProvider.refreshAll();
                    UiConstants.selectFirstOrPreserve(podGrid, dataProvider, PodInfo::name);
                    clusterErrorMessage.setVisible(true);
                    podGrid.setVisible(false);
                });
            }
        });
    }

    private void openDeleteDialog(PodInfo pod) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Pod");
        dialog.setText("Deleting Pod \"" + pod.name() + "\" will remove it from the cluster. If managed by a controller, it will be recreated automatically. This action cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            Cluster cluster = clusterContext.getCluster();
            if (cluster == null) return;
            try {
                workloadService.deletePod(cluster, pod.namespace(), pod.name());
                loadPods();
                notify("Pod " + pod.name() + " deleted", NotificationVariant.LUMO_SUCCESS);
            } catch (KubernetesOperationException ex) {
                notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    private Span phaseBadge(String phase) {
        Span badge = new Span(phase);
        badge.getElement().getThemeList().add("badge");
        switch (phase) {
            case "Running", "Active" -> badge.getElement().getThemeList().add("success");
            case "Pending"           -> badge.getElement().getThemeList().add("contrast");
            case "Failed"            -> badge.getElement().getThemeList().add("error");
            default                  -> {}
        }
        return badge;
    }

    private TextField buildFilterField() {
        TextField field = new TextField();
        field.setPlaceholder("Filter...");
        field.setClearButtonVisible(true);
        field.setWidth("100%");
        field.getElement().getThemeList().add("small");
        return field;
    }

    @Override
    public void refresh() {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            List<PodInfo> items = workloadService.listPods(cluster, clusterContext.getNamespace());
            allItems.clear();
            allItems.addAll(items);
            dataProvider.refreshAll();
            UiConstants.selectFirstOrPreserve(podGrid, dataProvider, PodInfo::name);
        } catch (KubernetesOperationException ignored) {}
    }

    private boolean matches(String value, String filter) {
        return filter == null || filter.isBlank() ||
               (value != null && value.toLowerCase().contains(filter.toLowerCase().trim()));
    }

    private boolean isCompletedJobPod(PodInfo pod) {
        return !pod.jobName().isBlank() && "Succeeded".equals(pod.phase());
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, UiConstants.NOTIFICATION_DURATION_MS, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
