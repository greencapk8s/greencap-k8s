import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import cytoscape, { ElementDefinition, EventObject, NodeSingular } from 'cytoscape';

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

@customElement('topology-graph')
export class TopologyGraph extends LitElement {
  @property({ type: String })
  graphData = '';

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
    if (changedProps.has('graphData') && this.graphData) {
      this._renderGraph();
    }
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

    const elements: ElementDefinition[] = [
      ...graph.nodes.map((n: NodeData) => ({
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
        },
      })),
      ...graph.edges.map((e: EdgeData) => ({
        data: { source: e.sourceId, target: e.targetId },
      })),
    ];

    const deploymentIds = graph.nodes
      .filter((n: NodeData) => n.type === 'Deployment')
      .map((n: NodeData) => n.id);

    const serviceIds = graph.nodes
      .filter((n: NodeData) => n.type === 'Service')
      .map((n: NodeData) => n.id);

    const connectedTargets = new Set(graph.edges.map((e: EdgeData) => e.targetId));
    const rootIds = [
      ...deploymentIds,
      ...serviceIds.filter((id) => !connectedTargets.has(id)),
    ];

    this.cy = cytoscape({
      container,
      elements,
      layout: {
        name: 'breadthfirst',
        directed: true,
        roots: rootIds.length > 0 ? rootIds : undefined,
        padding: 32,
        spacingFactor: 1.4,
      } as cytoscape.BreadthFirstLayoutOptions,
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
            'text-max-width': '124px',
            width: 144,
            height: 76,
            shape: 'round-rectangle',
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
  }
}
