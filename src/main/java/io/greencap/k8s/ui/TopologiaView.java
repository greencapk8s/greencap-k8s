package io.greencap.k8s.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import elemental.json.JsonObject;
import io.greencap.k8s.domain.user.TopologyLayout;
import io.greencap.k8s.domain.user.TopologyLayoutService;
import io.greencap.k8s.domain.user.UserRepository;
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.TopologyService;
import io.greencap.k8s.kubernetes.dto.TopologyGraph;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Slf4j
@Route(value = "topologia", layout = MainLayout.class)
@PageTitle("Topologia — GreenCap K8s")
@PermitAll
public class TopologiaView extends VerticalLayout implements BeforeEnterObserver {

    private static final String HELP_TITLE = "Topology";
    private static final String HELP_TEXT = "Topology displays a graph of the cluster's resources and their relationships: Deployments, ReplicaSets, Pods and Services in the active namespace.\n\nClick a node to see its details in the side panel. Pods created by Jobs or CronJobs do not appear in the graph — being ephemeral runs of finite tasks, they would clutter the visualization without adding value to understanding the topology.\n\nThe \"Group by labels\" checkbox draws a box around resources that share the same app.kubernetes.io/part-of and/or app.kubernetes.io/component labels, nesting component groups inside their part-of group, to help you spot which resources belong to the same application.";

    private final TopologyService topologyService;
    private final ClusterContext clusterContext;
    private final ObjectMapper objectMapper;
    private final TopologyLayoutService topologyLayoutService;
    private final UserRepository userRepository;

    private final VerticalLayout noClusterMessage;
    private final VerticalLayout loadingLayout;
    private final VerticalLayout emptyLayout;
    private final TopologyGraphComponent graphComponent;
    private final TopologyNodeDrawer drawer;
    private final Checkbox groupingToggle;

    public TopologiaView(TopologyService topologyService,
                         ClusterContext clusterContext,
                         ObjectMapper objectMapper,
                         TopologyLayoutService topologyLayoutService,
                         UserRepository userRepository) {
        this.topologyService = topologyService;
        this.clusterContext = clusterContext;
        this.objectMapper = objectMapper;
        this.topologyLayoutService = topologyLayoutService;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        getStyle().set("position", "relative");

        noClusterMessage = UiConstants.buildNoClusterMessage();
        loadingLayout = buildLoadingLayout();
        emptyLayout = buildEmptyLayout();

        graphComponent = new TopologyGraphComponent(topologyLayoutService, userRepository);
        graphComponent.setSizeFull();

        groupingToggle = new Checkbox("Group by labels", true);
        groupingToggle.getElement().setAttribute("title",
                "Group nodes that share app.kubernetes.io/part-of and app.kubernetes.io/component labels");
        groupingToggle.addValueChangeListener(e -> graphComponent.setGroupingEnabled(e.getValue()));
        groupingToggle.getStyle()
                .set("position", "absolute")
                .set("top", "var(--lumo-space-m)")
                .set("right", "var(--lumo-space-3xl)")
                .set("z-index", "1");

        Button resetPositionsBtn = new Button(VaadinIcon.REFRESH.create(), e -> resetPositions());
        resetPositionsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST);
        resetPositionsBtn.getElement().setAttribute("title", "Reset node positions");
        resetPositionsBtn.getStyle()
                .set("position", "absolute")
                .set("top", "var(--lumo-space-m)")
                .set("right", "calc(var(--lumo-space-m) + 36px)")
                .set("z-index", "1");

        drawer = new TopologyNodeDrawer();

        var helpIcon = VaadinIcon.QUESTION_CIRCLE.create();
        helpIcon.setSize(UiConstants.ICON_SIZE);
        Button helpBtn = new Button(helpIcon, e -> HelpDialog.open(HELP_TITLE, HELP_TEXT));
        helpBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_CONTRAST);
        helpBtn.getElement().setAttribute("title", "Help");
        helpBtn.getStyle()
                .set("position", "absolute")
                .set("top", "var(--lumo-space-m)")
                .set("right", "var(--lumo-space-m)")
                .set("z-index", "1");

        graphComponent.getElement().addEventListener("node-clicked", event -> {
            JsonObject detail = event.getEventData().getObject("event.detail");
            drawer.open(detail);
        }).addEventData("event.detail");

        graphComponent.getElement().addEventListener("canvas-tapped", event -> {
            drawer.close();
        });

        add(noClusterMessage, loadingLayout, emptyLayout, graphComponent, drawer, groupingToggle, resetPositionsBtn, helpBtn);
        setFlexGrow(1, graphComponent);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        showOnly(noClusterMessage);

        if (clusterContext.getCluster() == null) {
            noClusterMessage.setVisible(true);
            return;
        }

        showOnly(loadingLayout);

        UI ui = UI.getCurrent();
        String namespace = clusterContext.getNamespace();
        Long clusterId = clusterContext.getCluster().getId();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        graphComponent.setContext(clusterId, namespace);

        Optional<TopologyLayout> savedLayout = userRepository.findByUsername(username)
                .flatMap(user -> topologyLayoutService.findLayout(user.getId(), clusterId, namespace));

        boolean savedGroupingEnabled = savedLayout.map(TopologyLayout::isGroupingEnabled).orElse(true);
        String savedPositions = savedLayout.map(TopologyLayout::getNodePositions).orElse(null);

        groupingToggle.setValue(savedGroupingEnabled);
        graphComponent.setGroupingEnabled(savedGroupingEnabled);
        graphComponent.setSavedPositions(savedPositions);

        AsyncTasks.execute(() -> {
            try {
                TopologyGraph graph = topologyService.buildGraph(clusterContext.getCluster(), namespace);
                String graphJson = objectMapper.writeValueAsString(graph);

                ui.access(() -> {
                    if (graph.nodes().isEmpty()) {
                        showOnly(emptyLayout);
                    } else {
                        graphComponent.setGraphData(graphJson);
                        showOnly(graphComponent);
                    }
                });
            } catch (KubernetesOperationException | JsonProcessingException e) {
                log.error("Failed to load topology for namespace {}: {}", namespace, e.getMessage());
                ui.access(() -> {
                    showOnly(noClusterMessage);
                    Notification notification = Notification.show(
                            "Failed to load topology: " + e.getMessage(),
                            UiConstants.NOTIFICATION_DURATION_MS,
                            Notification.Position.BOTTOM_END);
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                });
            }
        });
    }

    private void resetPositions() {
        if (clusterContext.getCluster() == null) return;
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByUsername(username).ifPresent(user ->
                topologyLayoutService.deleteLayout(user.getId(), clusterContext.getCluster().getId(), clusterContext.getNamespace()));
        UI.getCurrent().navigate("topologia");
    }

    private void showOnly(com.vaadin.flow.component.Component visible) {
        noClusterMessage.setVisible(false);
        loadingLayout.setVisible(false);
        emptyLayout.setVisible(false);
        graphComponent.setVisible(false);
        visible.setVisible(true);
    }

    private VerticalLayout buildLoadingLayout() {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setWidth("240px");

        Span label = new Span("Loading topology…");
        label.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.MEDIUM);

        VerticalLayout layout = new VerticalLayout(label, progressBar);
        layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setSizeFull();
        layout.setVisible(false);
        return layout;
    }

    private VerticalLayout buildEmptyLayout() {
        Span text = new Span("No resources found in this namespace.");
        text.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.MEDIUM);

        VerticalLayout layout = new VerticalLayout(text);
        layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setSizeFull();
        layout.setVisible(false);
        return layout;
    }
}
