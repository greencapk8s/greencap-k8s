package io.greencap.k8s.ui;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import io.greencap.k8s.domain.user.TopologyLayoutService;
import io.greencap.k8s.domain.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Server-side wrapper for the topology-graph LitElement Web Component.
 * Receives a serialized TopologyGraph JSON and fires node-click navigation events.
 */
@Slf4j
@Tag("topology-graph")
@NpmPackage(value = "cytoscape", version = "3.30.2")
@NpmPackage(value = "cytoscape-fcose", version = "2.2.0")
@NpmPackage(value = "@types/cytoscape", version = "3.21.7", dev = true)
@JsModule("./topology-graph.ts")
public class TopologyGraphComponent extends Component implements HasSize {

    private final TopologyLayoutService topologyLayoutService;
    private final UserRepository userRepository;

    private Long clusterId;
    private String namespace;

    public TopologyGraphComponent(TopologyLayoutService topologyLayoutService, UserRepository userRepository) {
        this.topologyLayoutService = topologyLayoutService;
        this.userRepository = userRepository;
    }

    public void setGraphData(String graphDataJson) {
        getElement().setProperty("graphData", graphDataJson);
    }

    public void setGroupingEnabled(boolean groupingEnabled) {
        getElement().setProperty("groupingEnabled", groupingEnabled);
    }

    public void setSavedPositions(String savedPositionsJson) {
        getElement().setProperty("savedPositions", savedPositionsJson != null ? savedPositionsJson : "");
    }

    public void setContext(Long clusterId, String namespace) {
        this.clusterId = clusterId;
        this.namespace = namespace;
    }

    @ClientCallable
    public void saveLayout(String nodePositionsJson, boolean groupingEnabled) {
        if (clusterId == null || namespace == null) {
            return;
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByUsername(username).ifPresent(user ->
                topologyLayoutService.upsertLayout(user.getId(), clusterId, namespace, nodePositionsJson, groupingEnabled)
        );
    }
}
