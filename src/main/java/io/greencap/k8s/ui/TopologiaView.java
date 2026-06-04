package io.greencap.k8s.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
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
import io.greencap.k8s.kubernetes.ClusterContext;
import io.greencap.k8s.kubernetes.KubernetesOperationException;
import io.greencap.k8s.kubernetes.TopologyService;
import io.greencap.k8s.kubernetes.dto.TopologyGraph;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Route(value = "topologia", layout = MainLayout.class)
@PageTitle("Topologia — GreenCap K8s")
@PermitAll
public class TopologiaView extends VerticalLayout implements BeforeEnterObserver {

    private final TopologyService topologyService;
    private final ClusterContext clusterContext;
    private final ObjectMapper objectMapper;

    private final VerticalLayout noClusterMessage;
    private final VerticalLayout loadingLayout;
    private final VerticalLayout emptyLayout;
    private final TopologyGraphComponent graphComponent;
    private final TopologyNodeDrawer drawer;

    public TopologiaView(TopologyService topologyService, ClusterContext clusterContext, ObjectMapper objectMapper) {
        this.topologyService = topologyService;
        this.clusterContext = clusterContext;
        this.objectMapper = objectMapper;

        setSizeFull();
        setPadding(true);
        getStyle().set("position", "relative");

        noClusterMessage = UiConstants.buildNoClusterMessage();
        loadingLayout = buildLoadingLayout();
        emptyLayout = buildEmptyLayout();

        graphComponent = new TopologyGraphComponent();
        graphComponent.setSizeFull();

        drawer = new TopologyNodeDrawer();

        graphComponent.getElement().addEventListener("node-clicked", event -> {
            JsonObject detail = event.getEventData().getObject("event.detail");
            drawer.open(detail);
        }).addEventData("event.detail");

        graphComponent.getElement().addEventListener("canvas-tapped", event -> {
            drawer.close();
        });

        add(noClusterMessage, loadingLayout, emptyLayout, graphComponent, drawer);
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

        Thread.ofVirtual().start(() -> {
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
