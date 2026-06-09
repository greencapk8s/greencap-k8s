import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import cytoscape, { ElementDefinition, EventObject, NodeSingular } from 'cytoscape';
import fcose from 'cytoscape-fcose';

cytoscape.use(fcose);

interface NodeData {
  id: string;
  label: string;
  type: string;
  status: string;
  manifestUrl: string;
  labels: Record<string, string>;
  readyReplicas: number;
  desiredReplicas: number;
  serviceType: string;
  capacity: string;
  accessMode: string;
  partOfGroup: string;
  componentGroup: string;
}

interface EdgeData {
  sourceId: string;
  targetId: string;
}

interface GraphData {
  nodes: NodeData[];
  edges: EdgeData[];
}

const NODE_COLORS: Record<string, string> = {
  Deployment: '#1676F3',
  ReplicaSet: '#8B5CF6',
  Pod: '#10B981',
  Service: '#F59E0B',
  PersistentVolumeClaim: '#F97316',
};

const STATUS_BORDER: Record<string, string> = {
  Running: '#10B981',
  Active: '#10B981',
  Degraded: '#F59E0B',
  Failed: '#EF4444',
  Pending: '#94A3B8',
  Unknown: '#94A3B8',
};

interface SavedPosition {
  x: number;
  y: number;
}

@customElement('topology-graph')
export class TopologyGraph extends LitElement {
  @property({ type: String })
  graphData = '';

  @property({ type: Boolean })
  groupingEnabled = true;

  @property({ type: String })
  savedPositions = '';

  static styles = css`
    :host {
      display: block;
      width: 100%;
      height: 100%;
    }
    #cy {
      width: 100%;
      height: 100%;
      background: var(--lumo-base-color, #fff);
    }
  `;

  private cy: cytoscape.Core | null = null;

  render() {
    return html`<div id="cy"></div>`;
  }

  updated(changedProps: Map<string, unknown>) {
    const graphChanged = changedProps.has('graphData');
    const groupingChanged = changedProps.has('groupingEnabled');

    if ((graphChanged || groupingChanged) && this.graphData) {
      this._renderGraph();
      // Persist toggle changes immediately; graph reloads are not persisted here
      // (positions are persisted via dragfree)
      if (groupingChanged && !graphChanged) {
        this._saveLayout();
      }
    }
  }

  /**
   * Resolves the compound parent id for a node, creating the group container
   * elements (part-of outer box, optional component inner box) along the way.
   */
  private _resolveParent(
    node: NodeData,
    groupElements: Map<string, ElementDefinition>,
  ): string | undefined {
    if (!this.groupingEnabled) return undefined;

    const partOf = node.partOfGroup;
    const component = node.componentGroup;

    if (partOf) {
      const partOfId = `group/part-of/${partOf}`;
      if (!groupElements.has(partOfId)) {
        groupElements.set(partOfId, {
          data: { id: partOfId, label: `part-of: ${partOf}`, isGroup: true },
        });
      }
      if (!component) return partOfId;

      const componentId = `${partOfId}/component/${component}`;
      if (!groupElements.has(componentId)) {
        groupElements.set(componentId, {
          data: { id: componentId, label: `component: ${component}`, isGroup: true, parent: partOfId },
        });
      }
      return componentId;
    }

    if (component) {
      const componentId = `group/component/${component}`;
      if (!groupElements.has(componentId)) {
        groupElements.set(componentId, {
          data: { id: componentId, label: `component: ${component}`, isGroup: true },
        });
      }
      return componentId;
    }

    return undefined;
  }

  private _renderGraph() {
    const container = this.shadowRoot?.getElementById('cy');
    if (!container) return;

    let graph: GraphData;
    try {
      graph = JSON.parse(this.graphData);
    } catch {
      return;
    }

    if (this.cy) {
      this.cy.destroy();
    }

    let positionMap: Record<string, SavedPosition> = {};
    if (this.savedPositions) {
      try {
        positionMap = JSON.parse(this.savedPositions);
      } catch {
        positionMap = {};
      }
    }

    const groupElements = new Map<string, ElementDefinition>();
    const nodeElements: ElementDefinition[] = graph.nodes.map((n: NodeData) => ({
      data: {
        id: n.id,
        label: `${n.label}\n${n.type}`,
        type: n.type,
        status: n.status,
        manifestUrl: n.manifestUrl,
        labels: n.labels,
        readyReplicas: n.readyReplicas,
        desiredReplicas: n.desiredReplicas,
        serviceType: n.serviceType,
        capacity: n.capacity,
        accessMode: n.accessMode,
        nodeLabel: n.label,
        color: NODE_COLORS[n.type] ?? '#64748B',
        borderColor: STATUS_BORDER[n.status] ?? '#94A3B8',
        parent: this._resolveParent(n, groupElements),
      },
    }));

    const elements: ElementDefinition[] = [
      ...groupElements.values(),
      ...nodeElements,
      ...graph.edges.map((e: EdgeData) => ({
        data: { source: e.sourceId, target: e.targetId },
      })),
    ];

    const fixedNodeConstraint = Object.entries(positionMap).map(([nodeId, pos]) => ({
      nodeId,
      position: { x: pos.x, y: pos.y },
    }));

    this.cy = cytoscape({
      container,
      elements,
      layout: {
        name: 'fcose',
        quality: 'default',
        randomize: false,
        animate: false,
        padding: 48,
        nodeSeparation: 80,
        idealEdgeLength: 120,
        nodeRepulsion: 12000,
        gravity: 0.4,
        gravityRange: 3.8,
        numIter: 2500,
        tile: true,
        tilingPaddingVertical: 32,
        tilingPaddingHorizontal: 32,
        fixedNodeConstraint: fixedNodeConstraint.length > 0 ? fixedNodeConstraint : undefined,
      } as unknown as cytoscape.LayoutOptions,
      style: [
        {
          selector: 'node',
          style: {
            'background-color': 'data(color)',
            'border-color': 'data(borderColor)',
            'border-width': 3,
            label: 'data(label)',
            color: '#fff',
            'text-valign': 'center',
            'text-halign': 'center',
            'font-size': '12px',
            'text-wrap': 'wrap',
            width: 'label',
            height: 'label',
            'min-width': 144,
            'min-height': 76,
            'padding-left': '16px',
            'padding-right': '16px',
            'padding-top': '12px',
            'padding-bottom': '12px',
            shape: 'round-rectangle',
          } as cytoscape.Css.Node,
        },
        {
          selector: 'node[?isGroup]',
          style: {
            'background-color': '#F1F5F9',
            'background-opacity': 0.6,
            'border-color': '#94A3B8',
            'border-width': 2,
            'border-style': 'dashed',
            shape: 'round-rectangle',
            label: 'data(label)',
            color: '#475569',
            'text-valign': 'top',
            'text-halign': 'center',
            'font-size': '16px',
            'font-weight': 'bold',
            'text-margin-y': -10,
            'padding-left': '24px',
            'padding-right': '24px',
            'padding-top': '24px',
            'padding-bottom': '24px',
          },
        },
        {
          selector: 'edge',
          style: {
            width: 2,
            'line-color': '#CBD5E1',
            'target-arrow-color': '#CBD5E1',
            'target-arrow-shape': 'triangle',
            'curve-style': 'bezier',
          },
        },
        {
          selector: 'node:selected',
          style: {
            'border-width': 4,
            'border-color': '#1676F3',
          },
        },
      ],
      userZoomingEnabled: true,
      userPanningEnabled: true,
      boxSelectionEnabled: false,
    });

    this.cy.on('tap', 'node', (event: EventObject) => {
      const node = event.target as NodeSingular;
      this.dispatchEvent(new CustomEvent('node-clicked', {
        detail: {
          id: node.data('id') as string,
          nodeLabel: node.data('nodeLabel') as string,
          type: node.data('type') as string,
          status: node.data('status') as string,
          manifestUrl: node.data('manifestUrl') as string,
          labels: node.data('labels') as Record<string, string>,
          readyReplicas: node.data('readyReplicas') as number,
          desiredReplicas: node.data('desiredReplicas') as number,
          serviceType: node.data('serviceType') as string,
          capacity: node.data('capacity') as string,
          accessMode: node.data('accessMode') as string,
        },
        bubbles: true,
        composed: true,
      }));
    });

    this.cy.on('tap', (event: EventObject) => {
      if (event.target === this.cy) {
        this.dispatchEvent(new CustomEvent('canvas-tapped', {
          bubbles: true,
          composed: true,
        }));
      }
    });

    this.cy.on('dragfree', 'node', () => {
      this._saveLayout();
    });
  }

  private _saveLayout() {
    if (!this.cy) return;
    const server = (this as unknown as { $server?: { saveLayout(json: string, grouping: boolean): void } }).$server;
    if (!server) return;

    const positions: Record<string, SavedPosition> = {};
    this.cy.nodes().forEach(node => {
      if (!node.data('isGroup')) {
        const pos = node.position();
        positions[node.id()] = { x: pos.x, y: pos.y };
      }
    });

    server.saveLayout(JSON.stringify(positions), this.groupingEnabled);
  }
}
