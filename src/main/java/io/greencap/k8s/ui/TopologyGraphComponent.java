package io.greencap.k8s.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

/**
 * Server-side wrapper for the topology-graph LitElement Web Component.
 * Receives a serialized TopologyGraph JSON and fires node-click navigation events.
 */
@Tag("topology-graph")
@NpmPackage(value = "cytoscape", version = "3.30.2")
@NpmPackage(value = "cytoscape-fcose", version = "2.2.0")
@NpmPackage(value = "@types/cytoscape", version = "3.21.7", dev = true)
@JsModule("./topology-graph.ts")
public class TopologyGraphComponent extends Component implements HasSize {

    public void setGraphData(String graphDataJson) {
        getElement().setProperty("graphData", graphDataJson);
    }

    public void setGroupingEnabled(boolean groupingEnabled) {
        getElement().setProperty("groupingEnabled", groupingEnabled);
    }
}
