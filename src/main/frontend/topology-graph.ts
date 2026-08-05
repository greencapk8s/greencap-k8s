import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import cytoscape, { ElementDefinition, EventObject, NodeSingular } from 'cytoscape';
import fcose from 'cytoscape-fcose';

cytoscape.use(fcose);

interface NodeData {
  id: string;
  label: string;
  type: string;
  subtitle: string;
  status: string;
  alert: string;
  severity: string;
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
  type: string;
  matchedEnvVar: string;
  matchedValue: string;
}

interface ServiceDependencyDetail {
  targetLabel: string;
  targetManifestUrl: string;
  matchedEnvVar: string;
  matchedValue: string;
}

interface GraphData {
  nodes: NodeData[];
  edges: EdgeData[];
}

// The body carries no colour of its own, so the type has to be said some other way: an icon
// inside the node, plus the kind spelled out in the subtitle. Keyed by the node type, which is
// the resource kind — never the subtitle, where the replica count lives.
const ICON_BASE_PATH = '/icons/topology/';

const NODE_ICONS: Record<string, string> = {
  Deployment: 'deployment.svg',
  StatefulSet: 'stateful-set.svg',
  ReplicaSet: 'replica-set.svg',
  Pod: 'pod.svg',
  PodGroup: 'pods.svg',
  Service: 'service.svg',
  PersistentVolumeClaim: 'persistent-volume-claim.svg',
  Ingress: 'ingress.svg',
};

// Colour follows the severity decided server-side, never the status text: the labels are an
// open set that grows with Kubernetes, so recognising words here would silently paint every
// future reason neutral — the very defect this replaced.
const SEVERITY_BORDER: Record<string, string> = {
  HEALTHY: '#10B981',
  NEUTRAL: '#94A3B8',
  DEGRADED: '#F59E0B',
  PROBLEM: '#EF4444',
};

// What needs attention is said by the border and nothing else: a body washed in the severity
// competed with the icon for the eye and, in the dark theme, left white label text sitting on a
// pale card. Degraded and problem nodes are found by their thicker, coloured outline instead.
const ATTENTION_SEVERITIES = ['DEGRADED', 'PROBLEM'];
const ATTENTION_BORDER_WIDTH = 6;
const DEFAULT_BORDER_WIDTH = 3;

const ICON_SIZE_PX = 48;
const NODE_PADDING_PX = 14;
const ICON_TEXT_GAP_PX = 12;
// The minimum belongs to the text column, not to the node: label and badge are both centred on
// the column, so padding the node instead would slide the centred label off the badge under it.
const MIN_TEXT_COLUMN_WIDTH_PX = 92;

const LABEL_FONT_FAMILY = 'Helvetica Neue, Helvetica, sans-serif';
const LABEL_FONT_SIZE_PX = 13;
const LABEL_LINE_HEIGHT_PX = 18;
const LABEL_LINE_COUNT = 2;

// Cytoscape centres the label on the whole node box, so half the icon column has to be given
// back for the text to sit clear of the icon instead of on top of it.
const LABEL_TEXT_MARGIN_PX = (ICON_SIZE_PX + ICON_TEXT_GAP_PX) / 2;

// The reason a node needs attention is drawn as a pill rather than written as a third line of
// label: a node has exactly one text label in Cytoscape, so the badge is generated as an SVG and
// handed to the node as a second background image.
const BADGE_HEIGHT_PX = 22;
const BADGE_FONT_SIZE_PX = 11;
const BADGE_FONT_WEIGHT = 600;
const BADGE_PADDING_X_PX = 10;
const BADGE_GAP_PX = 6;
// Ink on amber, white on red: the two severity colours sit on opposite sides of the contrast line.
const BADGE_TEXT_COLOR: Record<string, string> = {
  DEGRADED: '#422006',
  PROBLEM: '#FFFFFF',
};

// Both are constants because every node carries the same two label lines, name and subtitle.
const TEXT_COLUMN_LEFT_PX = NODE_PADDING_PX + ICON_SIZE_PX + ICON_TEXT_GAP_PX;
const BADGE_TOP_PX = NODE_PADDING_PX + LABEL_LINE_COUNT * LABEL_LINE_HEIGHT_PX + BADGE_GAP_PX;

interface NodeVisuals {
  width: number;
  height: number;
  textMarginY: number;
  badge?: string;
}

type Rgba = [number, number, number, number];

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

  private canvasContext: CanvasRenderingContext2D | null = null;

  /**
   * A 2D context kept for two jobs Cytoscape cannot do for us: measuring label text so nodes can
   * be sized exactly, and normalising CSS colours into rgba components.
   */
  private _context(): CanvasRenderingContext2D {
    if (!this.canvasContext) {
      this.canvasContext = document.createElement('canvas').getContext('2d')!;
    }
    return this.canvasContext;
  }

  /**
   * Normalises any CSS colour — including the hsla() forms Lumo uses — into rgba components.
   * Assigning to fillStyle is the browser's own parser; an unparseable value leaves the previous
   * assignment in place, which is why the context is primed with an opaque colour first.
   */
  private _toRgba(value: string, fallback: Rgba): Rgba {
    const context = this._context();
    context.fillStyle = '#000000';
    context.fillStyle = value;
    const normalized = context.fillStyle as string;

    if (normalized.startsWith('#')) {
      const hex = normalized.slice(1);
      return [
        parseInt(hex.slice(0, 2), 16),
        parseInt(hex.slice(2, 4), 16),
        parseInt(hex.slice(4, 6), 16),
        1,
      ];
    }

    const parts = normalized.match(/[\d.]+/g);
    if (!parts || parts.length < 3) return fallback;
    return [Number(parts[0]), Number(parts[1]), Number(parts[2]), parts.length > 3 ? Number(parts[3]) : 1];
  }

  /**
   * Flattens a translucent colour onto an opaque one. Cytoscape paints each node onto a bare
   * canvas rather than onto the page, so a Lumo token carrying alpha — most of the contrast
   * scale does — would otherwise be drawn at full strength: in the dark theme that turns a
   * 10% white veil into a near-white card with white text on it.
   */
  private _flatten(color: Rgba, base: Rgba): string {
    const blend = (channel: number, baseChannel: number) =>
      Math.round(channel * color[3] + baseChannel * (1 - color[3]));
    return `rgb(${blend(color[0], base[0])}, ${blend(color[1], base[1])}, ${blend(color[2], base[2])})`;
  }

  /**
   * Resolves the Lumo tokens into concrete opaque colours. Cytoscape paints onto a canvas, where
   * CSS custom properties never resolve, so the values have to be read here and handed over
   * literally. Reading them at render time is enough: the theme is switched from another view,
   * and this component is rebuilt when Topologia is reopened.
   */
  private _readThemeColors() {
    const styles = getComputedStyle(this);
    const token = (name: string, fallback: string) => styles.getPropertyValue(name).trim() || fallback;
    const rgba = (name: string, fallback: string) => this._toRgba(token(name, fallback), [148, 163, 184, 1]);

    const base = rgba('--lumo-base-color', '#ffffff');
    // A step off the canvas, which already paints itself with --lumo-base-color. Sharing that
    // token would make the node and its background the same colour and leave only the border.
    const nodeBody = this._flatten(rgba('--lumo-contrast-10pct', 'rgba(100, 116, 139, 0.12)'), base);
    const nodeBodyRgba = this._toRgba(nodeBody, base);

    return {
      nodeBody,
      nodeText: this._flatten(rgba('--lumo-body-text-color', '#1A1A1A'), nodeBodyRgba),
      groupBody: this._flatten(rgba('--lumo-contrast-5pct', 'rgba(100, 116, 139, 0.06)'), base),
      groupBorder: this._flatten(rgba('--lumo-contrast-30pct', 'rgba(100, 116, 139, 0.3)'), base),
      groupText: this._flatten(rgba('--lumo-secondary-text-color', '#5A5A5A'), base),
    };
  }

  /**
   * Sizes the node from its own label and lays out the badge under it. Cytoscape's `width: label`
   * cannot reserve the icon column: its per-side padding properties are aliases of a single
   * uniform `padding`, so the space meant for the icon would be taken from every side at once and
   * the text would run over both the icon and the border. Measuring here is what keeps names from
   * colliding with the icon.
   */
  private _measureNode(label: string, alert: string, severity: string): NodeVisuals {
    const context = this._context();
    context.font = `${LABEL_FONT_SIZE_PX}px ${LABEL_FONT_FAMILY}`;

    const lines = label.split('\n');
    const textWidth = lines.reduce((widest, line) => Math.max(widest, context.measureText(line).width), 0);
    const pillWidth = alert ? this._measureBadgePill(alert) : 0;
    const columnWidth = Math.max(MIN_TEXT_COLUMN_WIDTH_PX, Math.ceil(Math.max(textWidth, pillWidth)));
    const columnHeight = lines.length * LABEL_LINE_HEIGHT_PX + (alert ? BADGE_GAP_PX + BADGE_HEIGHT_PX : 0);

    return {
      width: TEXT_COLUMN_LEFT_PX + columnWidth + NODE_PADDING_PX,
      height: Math.max(ICON_SIZE_PX, columnHeight) + NODE_PADDING_PX * 2,
      // The badge takes the bottom of the column, so the text lines give up half of what it costs
      // to stay centred on the space left above it.
      textMarginY: alert ? -(BADGE_HEIGHT_PX + BADGE_GAP_PX) / 2 : 0,
      badge: alert ? this._buildBadge(alert, severity, columnWidth, pillWidth) : undefined,
    };
  }

  private _measureBadgePill(text: string): number {
    const context = this._context();
    context.font = `${BADGE_FONT_WEIGHT} ${BADGE_FONT_SIZE_PX}px ${LABEL_FONT_FAMILY}`;
    return Math.ceil(context.measureText(text).width) + BADGE_PADDING_X_PX * 2;
  }

  /**
   * Draws the badge as an SVG data URI. The image spans the whole text column with the pill
   * centred inside it, which is what lets a single fixed background position serve every node:
   * only the drawing varies, never where Cytoscape puts it.
   */
  private _buildBadge(text: string, severity: string, columnWidth: number, pillWidth: number): string {
    const fill = SEVERITY_BORDER[severity] ?? SEVERITY_BORDER.PROBLEM;
    const color = BADGE_TEXT_COLOR[severity] ?? BADGE_TEXT_COLOR.PROBLEM;
    const pillLeft = (columnWidth - pillWidth) / 2;
    // Chrome's dominant-baseline support inside an SVG loaded as an image is uneven, so the
    // baseline is placed by hand.
    const baseline = BADGE_HEIGHT_PX / 2 + BADGE_FONT_SIZE_PX * 0.35;

    const svg =
      `<svg xmlns="http://www.w3.org/2000/svg" width="${columnWidth}" height="${BADGE_HEIGHT_PX}"` +
      ` viewBox="0 0 ${columnWidth} ${BADGE_HEIGHT_PX}">` +
      `<rect x="${pillLeft}" y="0" width="${pillWidth}" height="${BADGE_HEIGHT_PX}"` +
      ` rx="${BADGE_HEIGHT_PX / 2}" fill="${fill}"/>` +
      `<text x="${columnWidth / 2}" y="${baseline}" text-anchor="middle"` +
      ` font-family="${LABEL_FONT_FAMILY}" font-size="${BADGE_FONT_SIZE_PX}"` +
      ` font-weight="${BADGE_FONT_WEIGHT}" fill="${color}">${this._escapeXml(text)}</text></svg>`;

    return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
  }

  private _escapeXml(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }

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

    const theme = this._readThemeColors();

    const groupElements = new Map<string, ElementDefinition>();
    const nodeElements: ElementDefinition[] = graph.nodes.map((n: NodeData) => {
      // The alert is not part of the label: it is drawn as a badge, so a healthy graph shows
      // name and kind alone and the troubled node is the one wearing a coloured pill.
      const label = `${n.label}\n${n.subtitle}`;
      const visuals = this._measureNode(label, n.alert, n.severity);
      const icon = NODE_ICONS[n.type] ? ICON_BASE_PATH + NODE_ICONS[n.type] : undefined;
      return {
        data: {
          id: n.id,
          label,
          type: n.type,
          subtitle: n.subtitle,
          status: n.status,
          severity: n.severity,
          manifestUrl: n.manifestUrl,
          labels: n.labels,
          readyReplicas: n.readyReplicas,
          desiredReplicas: n.desiredReplicas,
          serviceType: n.serviceType,
          capacity: n.capacity,
          accessMode: n.accessMode,
          nodeLabel: n.label,
          icon,
          // The icon always holds the first slot so the badge can be positioned by a constant.
          // A type with no icon of its own loses the badge too — every type the server emits has
          // one, and the state is still carried by the border.
          images: visuals.badge && icon ? [icon, visuals.badge] : icon,
          bodyColor: theme.nodeBody,
          textColor: theme.nodeText,
          borderColor: SEVERITY_BORDER[n.severity] ?? SEVERITY_BORDER.PROBLEM,
          borderWidth: ATTENTION_SEVERITIES.includes(n.severity) ? ATTENTION_BORDER_WIDTH : DEFAULT_BORDER_WIDTH,
          nodeWidth: visuals.width,
          nodeHeight: visuals.height,
          textMarginY: visuals.textMarginY,
          parent: this._resolveParent(n, groupElements),
        },
      };
    });

    const elements: ElementDefinition[] = [
      ...groupElements.values(),
      ...nodeElements,
      ...graph.edges.map((e: EdgeData) => ({
        data: { source: e.sourceId, target: e.targetId, type: e.type },
      })),
    ];

    // fcose does not support fixedNodeConstraint when compound nodes (groups) are present;
    // when grouping is active, saved positions are applied manually after the layout run.
    const hasCompoundNodes = groupElements.size > 0;
    const hasSavedPositions = Object.keys(positionMap).length > 0;
    const fixedNodeConstraint = (!hasCompoundNodes && hasSavedPositions)
      ? Object.entries(positionMap).map(([nodeId, pos]) => ({ nodeId, position: { x: pos.x, y: pos.y } }))
      : undefined;
    // randomize: true on first visit so fcose can spread nodes from random initial positions;
    // false when saved positions exist so the layout starts from the last known state.
    const randomize = !hasSavedPositions;

    this.cy = cytoscape({
      container,
      elements,
      layout: { name: 'preset' },
      style: [
        {
          // Group containers are excluded: they size themselves from their children, and the
          // width/height mappings below have no data to read on them.
          selector: 'node[!isGroup]',
          style: {
            'background-color': 'data(bodyColor)',
            'border-color': 'data(borderColor)',
            'border-width': 'data(borderWidth)',
            label: 'data(label)',
            color: 'data(textColor)',
            'text-valign': 'center',
            'text-halign': 'center',
            'text-margin-x': LABEL_TEXT_MARGIN_PX,
            'text-margin-y': (node: NodeSingular) => node.data('textMarginY') as number,
            'font-family': LABEL_FONT_FAMILY,
            'font-size': `${LABEL_FONT_SIZE_PX}px`,
            'line-height': LABEL_LINE_HEIGHT_PX / LABEL_FONT_SIZE_PX,
            'text-wrap': 'wrap',
            // Measured in _measureNode: the font here must stay in step with the one used there.
            width: 'data(nodeWidth)',
            height: 'data(nodeHeight)',
            shape: 'round-rectangle',
          } as cytoscape.Css.Node,
        },
        {
          // Nodes whose type has no icon still render; they simply keep the plain body.
          // Every icon here must declare width and height on its root <svg>: Cytoscape draws the
          // image using its intrinsic size as the source rectangle, and a viewBox alone leaves
          // Chrome with its 300x150 default for replaced elements, squashing the icon.
          selector: 'node[icon]',
          style: {
            // Two slots, in order: the type icon, then the state badge when there is one. The
            // badge keeps its natural size — the SVG is generated as wide as the text column.
            'background-image': 'data(images)',
            'background-fit': 'none',
            'background-width': `${ICON_SIZE_PX}px auto`,
            'background-height': `${ICON_SIZE_PX}px auto`,
            'background-position-x': `${NODE_PADDING_PX}px ${TEXT_COLUMN_LEFT_PX}px`,
            'background-position-y': `50% ${BADGE_TOP_PX}px`,
          } as cytoscape.Css.Node,
        },
        {
          selector: 'node[?isGroup]',
          style: {
            'background-color': theme.groupBody,
            'border-color': theme.groupBorder,
            'border-width': 2,
            'border-style': 'dashed',
            shape: 'round-rectangle',
            label: 'data(label)',
            color: theme.groupText,
            'text-valign': 'top',
            'text-halign': 'center',
            'font-family': LABEL_FONT_FAMILY,
            'font-size': '16px',
            'font-weight': 'bold',
            'text-margin-y': -10,
            // Cancels the offset the node rule applies to make room for the icon; groups have none.
            'text-margin-x': 0,
            padding: '24px',
          } as cytoscape.Css.Node,
        },
        {
          selector: 'edge',
          style: {
            width: 2,
            'line-color': '#64748B',
            'target-arrow-color': '#64748B',
            'target-arrow-shape': 'triangle',
            'curve-style': 'bezier',
          },
        },
        {
          // ServiceDependency: inferred from application config, not a Kubernetes-enforced
          // relationship — dashed to signal it's a heuristic, same color as structural edges.
          selector: 'edge[type = "SERVICE_DEPENDENCY"]',
          style: {
            'line-style': 'dashed',
          },
        },
        {
          // Selection draws its own ring instead of repainting the border: severity lives in
          // that border, and the moment a user clicks a broken node to inspect it is exactly
          // when its colour matters most.
          selector: 'node:selected',
          style: {
            'outline-width': 4,
            'outline-color': '#1676F3',
            'outline-offset': 2,
            // Cast: outline-* is supported by Cytoscape 3.30 but missing from its type defs.
          } as cytoscape.Css.Node,
        },
      ],
      userZoomingEnabled: true,
      userPanningEnabled: true,
      boxSelectionEnabled: false,
    });

    this.cy.layout({
      name: 'fcose',
      quality: 'default',
      randomize,
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
      fixedNodeConstraint,
    } as unknown as cytoscape.LayoutOptions).run();

    // When compound nodes are present fixedNodeConstraint is not used by fcose;
    // apply saved positions manually after layout so user-dragged positions are preserved.
    if (hasCompoundNodes && Object.keys(positionMap).length > 0) {
      this.cy.nodes().forEach(node => {
        const saved = positionMap[node.id()];
        if (saved && !node.data('isGroup')) {
          node.position(saved);
        }
      });
    }

    this.cy.on('tap', 'node', (event: EventObject) => {
      const node = event.target as NodeSingular;
      if (node.data('isGroup')) return;

      const nodeId = node.data('id') as string;
      const serviceDependencies: ServiceDependencyDetail[] = graph.edges
        .filter((e: EdgeData) => e.sourceId === nodeId && e.type === 'SERVICE_DEPENDENCY')
        .map((e: EdgeData) => {
          const targetNode = graph.nodes.find((n: NodeData) => n.id === e.targetId);
          return {
            targetLabel: targetNode?.label ?? e.targetId,
            targetManifestUrl: targetNode?.manifestUrl ?? '',
            matchedEnvVar: e.matchedEnvVar,
            matchedValue: e.matchedValue,
          };
        });

      this.dispatchEvent(new CustomEvent('node-clicked', {
        detail: {
          id: nodeId,
          nodeLabel: node.data('nodeLabel') as string,
          type: node.data('type') as string,
          subtitle: node.data('subtitle') as string,
          status: node.data('status') as string,
          manifestUrl: node.data('manifestUrl') as string,
          labels: node.data('labels') as Record<string, string>,
          readyReplicas: node.data('readyReplicas') as number,
          desiredReplicas: node.data('desiredReplicas') as number,
          serviceType: node.data('serviceType') as string,
          capacity: node.data('capacity') as string,
          accessMode: node.data('accessMode') as string,
          serviceDependencies,
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
