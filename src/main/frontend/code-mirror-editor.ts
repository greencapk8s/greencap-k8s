import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { Compartment, EditorState } from '@codemirror/state';
import { EditorView, basicSetup } from 'codemirror';
import { indentUnit } from '@codemirror/language';
import { yaml } from '@codemirror/lang-yaml';
import { oneDark } from '@codemirror/theme-one-dark';

@customElement('code-mirror-editor')
export class CodeMirrorEditor extends LitElement {
  @property({ type: String })
  value = '';

  @property({ type: Boolean })
  readOnly = false;

  private _view: EditorView | null = null;
  private readonly _readOnlyCompartment = new Compartment();
  private readonly _themeCompartment = new Compartment();
  private _themeObserver: MutationObserver | null = null;
  private _updatingFromServer = false;

  static styles = css`
    :host {
      display: block;
      height: 100%;
      overflow: hidden;
    }
    #editor {
      height: 100%;
    }
  `;

  render() {
    return html`<div id="editor"></div>`;
  }

  firstUpdated() {
    this._initEditor();
    this._watchTheme();
  }

  updated(changedProps: Map<string, unknown>) {
    if (!this._view) return;

    if (changedProps.has('value')) {
      this._updatingFromServer = true;
      const current = this._view.state.doc.toString();
      if (current !== this.value) {
        this._view.dispatch({
          changes: { from: 0, to: current.length, insert: this.value ?? '' },
        });
      }
      this._updatingFromServer = false;
    }

    if (changedProps.has('readOnly')) {
      this._view.dispatch({
        effects: this._readOnlyCompartment.reconfigure(EditorState.readOnly.of(this.readOnly)),
      });
    }
  }

  override focus() {
    this._view?.focus();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._themeObserver?.disconnect();
    this._view?.destroy();
    this._view = null;
  }

  private _isDark(): boolean {
    // Check light-DOM ancestors first (catches vaadin-dialog-overlay and vaadin-app-layout)
    if (this.closest('[theme~="dark"]')) return true;
    return document.documentElement.getAttribute('theme')?.split(/\s+/).includes('dark') ?? false;
  }

  private _initEditor() {
    const container = this.shadowRoot?.getElementById('editor');
    if (!container) return;

    this._view = new EditorView({
      state: EditorState.create({
        doc: this.value ?? '',
        extensions: [
          basicSetup,
          yaml(),
          indentUnit.of('  '),
          EditorState.tabSize.of(2),
          this._readOnlyCompartment.of(EditorState.readOnly.of(this.readOnly)),
          this._themeCompartment.of(this._isDark() ? oneDark : []),
          EditorView.theme({
            '&': { height: '100%' },
            '.cm-scroller': { overflow: 'auto' },
          }),
          EditorView.updateListener.of(update => {
            if (update.docChanged && !this._updatingFromServer) {
              const newValue = update.state.doc.toString();
              this.value = newValue;
              this.dispatchEvent(new CustomEvent('value-changed', {
                detail: { value: newValue },
                bubbles: true,
                composed: true,
              }));
            }
          }),
        ],
      }),
      parent: container,
    });
  }

  private _watchTheme() {
    let currentDark = this._isDark();
    this._themeObserver = new MutationObserver(() => {
      const nowDark = this._isDark();
      if (nowDark !== currentDark) {
        currentDark = nowDark;
        this._view?.dispatch({
          effects: this._themeCompartment.reconfigure(nowDark ? oneDark : []),
        });
      }
    });
    // subtree: true catches vaadin-app-layout, vaadin-dialog-overlay, and any other
    // Vaadin element that receives the theme attribute propagation
    this._themeObserver.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['theme'],
      subtree: true,
    });
  }
}
