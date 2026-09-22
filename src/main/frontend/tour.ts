import { LitElement } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { driver } from 'driver.js';
import type { Driver, DriveStep } from 'driver.js';
import 'driver.js/dist/driver.css';

interface TourStep {
  targetId: string;
  title: string;
  description: string;
}

// The layout opens the drawer immediately before handing over the steps, and the drawer width
// animates. Highlighting mid-animation would cut the spotlight around the old geometry.
const TOUR_START_DELAY_MS = 350;

const POPOVER_CLASS = 'greencap-tour-popover';
const THEME_STYLE_ID = 'greencap-tour-theme';
const OVERLAY_OPACITY = 0.65;

/**
 * Renderer for the first-access Tour. It decides nothing: the server hands over the finished
 * step list, this passes it to Driver.js, and the only thing travelling back is the notice that
 * the Tour is over.
 */
@customElement('greencap-tour')
export class GreencapTour extends LitElement {
  @property({ type: String })
  steps = '';

  private tour?: Driver;

  // Light DOM. The host carries the server contract and nothing else — the overlay Driver.js
  // builds lives in the document body, outside any shadow root.
  createRenderRoot() {
    return this;
  }

  /** Invoked from the server once the steps property is in place. */
  startTour() {
    const steps = this.parseSteps();
    if (steps.length === 0) return;
    window.setTimeout(() => this.runTour(steps), TOUR_START_DELAY_MS);
  }

  // The server is the only author of this list, but a stale bundle or a half-written property
  // would still land here. A tour that cannot be read is a tour that does not run — never a
  // page that breaks.
  private parseSteps(): TourStep[] {
    try {
      const parsed: unknown = JSON.parse(this.steps);
      return Array.isArray(parsed) ? parsed.filter(isTourStep) : [];
    } catch {
      return [];
    }
  }

  private runTour(steps: TourStep[]) {
    this.tour?.destroy();
    this.applyThemeTokens();
    injectThemeStyle();

    this.tour = driver({
      showProgress: true,
      allowClose: true,
      // Closing marks the Tour as seen for good, so a stray click on the dimmed area would burn
      // the user's first access. Only the deliberate exits end it.
      overlayClickBehavior: () => {},
      // The spotlight is a window, not a door. The header step lights up the drawer toggle, and
      // one click on it would collapse the menu the remaining steps point at; the menu steps
      // light up links that would navigate away mid-Tour.
      disableActiveInteraction: true,
      overlayOpacity: OVERLAY_OPACITY,
      popoverClass: POPOVER_CLASS,
      nextBtnText: 'Next',
      prevBtnText: 'Back',
      doneBtnText: 'Done',
      steps: steps.map(toDriveStep),
      // Reached by every way out alike: the last step, skip, ESC and the close button.
      onDestroyed: () => this.reportTourAsSeen(),
    });
    this.tour.drive();
  }

  // The platform paints its dark theme on the AppLayout element, not on the document root, so a
  // popover appended to the body would read the light palette. Resolving the tokens here — from
  // a host that does sit inside the layout — and republishing them on the root is what carries
  // the active theme across.
  private applyThemeTokens() {
    const styles = getComputedStyle(this);
    const token = (name: string, fallback: string) => styles.getPropertyValue(name).trim() || fallback;
    const root = document.documentElement.style;

    root.setProperty('--greencap-tour-background', token('--lumo-base-color', '#ffffff'));
    root.setProperty('--greencap-tour-text', token('--lumo-body-text-color', '#1a1a1a'));
    root.setProperty('--greencap-tour-secondary-text', token('--lumo-secondary-text-color', '#5a5a5a'));
    root.setProperty('--greencap-tour-primary', token('--lumo-primary-color', '#3b82f6'));
    root.setProperty('--greencap-tour-primary-text', token('--lumo-primary-contrast-color', '#ffffff'));
    root.setProperty('--greencap-tour-radius', token('--lumo-border-radius-m', '6px'));
    root.setProperty('--greencap-tour-font', token('--lumo-font-family', 'inherit'));
    root.setProperty('--greencap-tour-shadow', token('--lumo-box-shadow-m', '0 1px 10px rgba(0, 0, 0, 0.4)'));
  }

  private reportTourAsSeen() {
    const server = (this as unknown as { $server?: { markTourAsSeen(): void } }).$server;
    if (!server) return;
    server.markTourAsSeen();
  }
}

function isTourStep(value: unknown): value is TourStep {
  const step = value as TourStep;
  return (
    !!step &&
    typeof step.targetId === 'string' &&
    typeof step.title === 'string' &&
    typeof step.description === 'string'
  );
}

function toDriveStep(step: TourStep): DriveStep {
  return {
    element: `#${step.targetId}`,
    // A target that was renamed out from under the tour drops its step instead of stranding a
    // popover in the middle of the screen pointing at nothing.
    skipMissingElement: true,
    popover: {
      title: step.title,
      description: step.description,
    },
  };
}

function injectThemeStyle() {
  if (document.getElementById(THEME_STYLE_ID)) return;

  const style = document.createElement('style');
  style.id = THEME_STYLE_ID;
  style.textContent = `
    greencap-tour {
      display: none;
    }

    .driver-popover.${POPOVER_CLASS} {
      background-color: var(--greencap-tour-background);
      color: var(--greencap-tour-text);
      border-radius: var(--greencap-tour-radius);
      box-shadow: var(--greencap-tour-shadow);
      font-family: var(--greencap-tour-font);
      max-width: 360px;
    }

    .driver-popover.${POPOVER_CLASS} .driver-popover-title {
      color: var(--greencap-tour-text);
    }

    .driver-popover.${POPOVER_CLASS} .driver-popover-description,
    .driver-popover.${POPOVER_CLASS} .driver-popover-progress-text {
      color: var(--greencap-tour-secondary-text);
      line-height: 1.5;
    }

    .driver-popover.${POPOVER_CLASS} .driver-popover-close-btn {
      color: var(--greencap-tour-secondary-text);
    }

    .driver-popover.${POPOVER_CLASS} .driver-popover-footer-btn {
      background-color: transparent;
      background-image: none;
      color: var(--greencap-tour-primary);
      border: none;
      text-shadow: none;
      font-weight: 500;
    }

    .driver-popover.${POPOVER_CLASS} .driver-popover-navigation-btns button:last-child {
      background-color: var(--greencap-tour-primary);
      color: var(--greencap-tour-primary-text);
      border-radius: var(--greencap-tour-radius);
      padding: 4px 12px;
    }

    .driver-popover.${POPOVER_CLASS} .driver-popover-arrow-side-left {
      border-left-color: var(--greencap-tour-background);
    }

    .driver-popover.${POPOVER_CLASS} .driver-popover-arrow-side-right {
      border-right-color: var(--greencap-tour-background);
    }

    .driver-popover.${POPOVER_CLASS} .driver-popover-arrow-side-top {
      border-top-color: var(--greencap-tour-background);
    }

    .driver-popover.${POPOVER_CLASS} .driver-popover-arrow-side-bottom {
      border-bottom-color: var(--greencap-tour-background);
    }
  `;
  document.head.appendChild(style);
}
