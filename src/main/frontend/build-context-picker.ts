import { LitElement, html, css, nothing } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

const TAR_BLOCK_SIZE = 512;
const TAR_NAME_FIELD_SIZE = 100;
const TAR_PREFIX_FIELD_SIZE = 155;
const MAX_COMPRESSED_BYTES = 50 * 1024 * 1024;
const LARGEST_FOLDERS_SHOWN = 3;

// Always dropped, before anything else. These are the directories every well written .dockerignore
// excludes, and without them a plain Node.js project would try to pack hundreds of megabytes.
const BUILT_IN_EXCLUSIONS = ['.git', 'node_modules', 'venv', '__pycache__', 'target', 'dist', 'build'];

const DOCKERIGNORE_FILE = '.dockerignore';

interface ExclusionCount {
  label: string;
  files: number;
}

interface FolderSize {
  name: string;
  bytes: number;
}

interface ContextSummary {
  folderName: string;
  fileCount: number;
  compressedBytes: number;
  builtInExclusions: ExclusionCount[];
  dockerignoreExclusions: ExclusionCount[];
  unsupportedPatterns: string[];
  largestFolders: FolderSize[];
}

interface SelectedEntry {
  path: string;
  file: File;
}

interface IgnoreRule {
  raw: string;
  negated: boolean;
  matcher: RegExp;
}

@customElement('build-context-picker')
export class BuildContextPicker extends LitElement {
  @property({ type: String })
  folderButtonLabel = 'Select project folder';

  @state()
  private _status: 'idle' | 'packing' | 'folder' | 'error' = 'idle';

  @state()
  private _summary: ContextSummary | null = null;

  @state()
  private _errorMessage = '';

  private _entries: SelectedEntry[] = [];

  static styles = css`
    :host {
      display: block;
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: var(--lumo-space-s);
    }
    button {
      font: inherit;
      font-size: var(--lumo-font-size-s);
      padding: var(--lumo-space-xs) var(--lumo-space-m);
      border: 1px solid var(--lumo-contrast-30pct);
      border-radius: var(--lumo-border-radius-m);
      background: var(--lumo-contrast-5pct);
      color: var(--lumo-body-text-color);
      cursor: pointer;
    }
    button:hover {
      background: var(--lumo-contrast-10pct);
    }
    button:disabled {
      cursor: default;
      opacity: 0.5;
    }
    .panel {
      margin-top: var(--lumo-space-s);
      padding: var(--lumo-space-m);
      border-radius: var(--lumo-border-radius-m);
      background: var(--lumo-contrast-5pct);
      font-size: var(--lumo-font-size-s);
    }
    .panel.error {
      background: var(--lumo-error-color-10pct);
      color: var(--lumo-error-text-color);
    }
    .headline {
      font-weight: 600;
    }
    dl {
      margin: var(--lumo-space-s) 0 0;
    }
    dt {
      margin-top: var(--lumo-space-xs);
      color: var(--lumo-secondary-text-color);
    }
    dd {
      margin: 0;
    }
    ul {
      margin: var(--lumo-space-xs) 0 0;
      padding-left: var(--lumo-space-l);
    }
    input[type='file'] {
      display: none;
    }
  `;

  render() {
    return html`
      <div class="actions">
        <button type="button" @click=${this._openFolderPicker} ?disabled=${this._status === 'packing'}>
          ${this._status === 'packing' ? 'Packing…' : this.folderButtonLabel}
        </button>
      </div>
      <input id="folderInput" type="file" @change=${this._onFolderChosen} />
      ${this._renderPanel()}
    `;
  }

  firstUpdated() {
    const folderInput = this.shadowRoot?.getElementById('folderInput') as HTMLInputElement | null;
    if (folderInput) {
      folderInput.webkitdirectory = true;
    }
  }

  /** Reads a file out of the selected folder, so the server can inspect it before the Build starts. */
  async readTextFile(path: string): Promise<string> {
    const normalized = path.replace(/^\.?\//, '').trim();
    const entry = this._entries.find(candidate => candidate.path === normalized);
    return entry ? entry.file.text() : '';
  }

  clearSelection() {
    this._entries = [];
    this._summary = null;
    this._errorMessage = '';
    this._status = 'idle';
  }

  private _renderPanel() {
    if (this._status === 'error') {
      return html`<div class="panel error">${this._errorMessage}</div>`;
    }
    if (this._status !== 'folder' || !this._summary) {
      return nothing;
    }
    const summary = this._summary;
    return html`
      <div class="panel">
        <div class="headline">
          ${summary.folderName} — ${summary.fileCount} files, ${formatBytes(summary.compressedBytes)} compressed
        </div>
        <dl>
          ${summary.builtInExclusions.length
            ? html`<dt>Excluded by GreenCap</dt>
                <dd>
                  <ul>
                    ${summary.builtInExclusions.map(entry => html`<li>${entry.label} — ${entry.files} files</li>`)}
                  </ul>
                </dd>`
            : nothing}
          ${summary.dockerignoreExclusions.length
            ? html`<dt>Excluded by .dockerignore</dt>
                <dd>
                  <ul>
                    ${summary.dockerignoreExclusions.map(entry => html`<li>${entry.label} — ${entry.files} files</li>`)}
                  </ul>
                </dd>`
            : nothing}
          ${summary.unsupportedPatterns.length
            ? html`<dt>.dockerignore patterns not understood (ignored)</dt>
                <dd>${summary.unsupportedPatterns.join(', ')}</dd>`
            : nothing}
        </dl>
      </div>
    `;
  }

  private _openFolderPicker() {
    (this.shadowRoot?.getElementById('folderInput') as HTMLInputElement | null)?.click();
  }

  private async _onFolderChosen(event: Event) {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    if (files.length === 0) return;

    if (typeof CompressionStream === 'undefined') {
      this._fail('This browser cannot compress the build context. Use a recent Chrome, Firefox or Safari.');
      return;
    }

    this._status = 'packing';
    this._summary = null;
    try {
      await this._packAndUpload(files);
    } catch (error) {
      this._fail(`Failed to pack the selected folder: ${(error as Error).message}`);
    }
  }

  private async _packAndUpload(files: File[]) {
    const folderName = rootFolderName(files[0]);
    const allEntries = files.map(file => ({ path: stripRootFolder(file), file }));

    const builtIn = applyBuiltInExclusions(allEntries);
    const dockerignore = await applyDockerignore(builtIn.kept);
    const included = dockerignore.kept.filter(entry => entry.path !== DOCKERIGNORE_FILE);

    if (included.length === 0) {
      this._fail('Every file in the selected folder was excluded — nothing left to build from.');
      return;
    }

    const packed = await packTarGz(included);
    const summary: ContextSummary = {
      folderName,
      fileCount: included.length,
      compressedBytes: packed.size,
      builtInExclusions: builtIn.excluded,
      dockerignoreExclusions: dockerignore.excluded,
      unsupportedPatterns: dockerignore.unsupportedPatterns,
      largestFolders: largestFolders(included),
    };

    if (packed.size > MAX_COMPRESSED_BYTES) {
      this._fail(
        `The packed context is ${formatBytes(packed.size)}, above the ${formatBytes(MAX_COMPRESSED_BYTES)} limit. ` +
          `Largest folders included: ${summary.largestFolders
            .map(folder => `${folder.name} (${formatBytes(folder.bytes)})`)
            .join(', ')}.`
      );
      return;
    }

    // The summary is on screen before a single byte leaves the browser.
    this._entries = included;
    this._summary = summary;
    this._status = 'folder';
    await this.updateComplete;

    await this._upload(packed);
    (this as any).$server?.contextPacked(folderName, included.length, packed.size);
  }

  private async _upload(archive: Blob) {
    const target = this.getAttribute('target');
    if (!target) {
      throw new Error('upload target is not available');
    }
    const response = await fetch(target, { method: 'POST', body: archive, credentials: 'same-origin' });
    if (!response.ok) {
      throw new Error(`upload rejected with HTTP ${response.status}`);
    }
  }

  private _fail(message: string) {
    this._entries = [];
    this._summary = null;
    this._errorMessage = message;
    this._status = 'error';
    (this as any).$server?.selectionCleared();
  }
}

function rootFolderName(file: File): string {
  const relativePath = (file as any).webkitRelativePath as string | undefined;
  const root = relativePath ? relativePath.split('/')[0] : '';
  return root || 'context';
}

function stripRootFolder(file: File): string {
  const relativePath = ((file as any).webkitRelativePath as string | undefined) ?? file.name;
  const separatorIndex = relativePath.indexOf('/');
  return separatorIndex >= 0 ? relativePath.substring(separatorIndex + 1) : relativePath;
}

function applyBuiltInExclusions(entries: SelectedEntry[]): { kept: SelectedEntry[]; excluded: ExclusionCount[] } {
  const counts = new Map<string, number>();
  const kept: SelectedEntry[] = [];

  for (const entry of entries) {
    const segments = entry.path.split('/').slice(0, -1);
    const hit = BUILT_IN_EXCLUSIONS.find(excluded => segments.includes(excluded));
    if (hit) {
      counts.set(hit, (counts.get(hit) ?? 0) + 1);
    } else {
      kept.push(entry);
    }
  }
  return { kept, excluded: toExclusionCounts(counts) };
}

async function applyDockerignore(
  entries: SelectedEntry[]
): Promise<{ kept: SelectedEntry[]; excluded: ExclusionCount[]; unsupportedPatterns: string[] }> {
  const dockerignore = entries.find(entry => entry.path === DOCKERIGNORE_FILE);
  if (!dockerignore) {
    return { kept: entries, excluded: [], unsupportedPatterns: [] };
  }

  const parsed = parseDockerignore(await dockerignore.file.text());
  const counts = new Map<string, number>();
  const kept: SelectedEntry[] = [];

  for (const entry of entries) {
    const decision = decideExclusion(entry.path, parsed.rules);
    if (decision) {
      counts.set(decision.raw, (counts.get(decision.raw) ?? 0) + 1);
    } else {
      kept.push(entry);
    }
  }
  return { kept, excluded: toExclusionCounts(counts), unsupportedPatterns: parsed.unsupportedPatterns };
}

function parseDockerignore(content: string): { rules: IgnoreRule[]; unsupportedPatterns: string[] } {
  const rules: IgnoreRule[] = [];
  const unsupportedPatterns: string[] = [];

  for (const line of content.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (trimmed === '' || trimmed.startsWith('#')) continue;

    const negated = trimmed.startsWith('!');
    const pattern = (negated ? trimmed.substring(1) : trimmed).replace(/^\.?\//, '').replace(/\/+$/, '');
    if (pattern === '') continue;

    const matcher = globToRegExp(pattern);
    if (!matcher) {
      unsupportedPatterns.push(trimmed);
      continue;
    }
    rules.push({ raw: trimmed, negated, matcher });
  }
  return { rules, unsupportedPatterns };
}

// A pragmatic subset of the Docker syntax: '*', '**', '?' and a trailing slash. Anything else
// (character classes, escapes) is reported back to the user instead of failing silently.
function globToRegExp(pattern: string): RegExp | null {
  if (/[[\]\\]/.test(pattern)) return null;

  let expression = '';
  for (let index = 0; index < pattern.length; index++) {
    const character = pattern[index];
    if (character === '*') {
      if (pattern[index + 1] === '*') {
        expression += '.*';
        index++;
        if (pattern[index + 1] === '/') index++;
      } else {
        expression += '[^/]*';
      }
    } else if (character === '?') {
      expression += '[^/]';
    } else if ('.+^${}()|'.includes(character)) {
      expression += `\\${character}`;
    } else {
      expression += character;
    }
  }
  return new RegExp(`^${expression}$`);
}

/** Returns the rule that excluded the path, or null when it survives. Later rules win, as in Docker. */
function decideExclusion(path: string, rules: IgnoreRule[]): IgnoreRule | null {
  let decision: IgnoreRule | null = null;
  for (const rule of rules) {
    if (matchesPathOrAncestor(path, rule.matcher)) {
      decision = rule.negated ? null : rule;
    }
  }
  return decision;
}

function matchesPathOrAncestor(path: string, matcher: RegExp): boolean {
  if (matcher.test(path)) return true;
  const segments = path.split('/');
  for (let depth = 1; depth < segments.length; depth++) {
    if (matcher.test(segments.slice(0, depth).join('/'))) return true;
  }
  return false;
}

function toExclusionCounts(counts: Map<string, number>): ExclusionCount[] {
  return Array.from(counts.entries())
    .map(([label, files]) => ({ label, files }))
    .sort((left, right) => right.files - left.files);
}

function largestFolders(entries: SelectedEntry[]): FolderSize[] {
  const sizes = new Map<string, number>();
  for (const entry of entries) {
    const separatorIndex = entry.path.indexOf('/');
    const folder = separatorIndex >= 0 ? entry.path.substring(0, separatorIndex) : '.';
    sizes.set(folder, (sizes.get(folder) ?? 0) + entry.file.size);
  }
  return Array.from(sizes.entries())
    .map(([name, bytes]) => ({ name, bytes }))
    .sort((left, right) => right.bytes - left.bytes)
    .slice(0, LARGEST_FOLDERS_SHOWN);
}

// Files go into the archive as Blob parts, never as bytes in the JS heap — only the 512 byte
// headers are materialized, so a large folder does not blow up the tab before compression starts.
async function packTarGz(entries: SelectedEntry[]): Promise<Blob> {
  const parts: BlobPart[] = [];

  for (const directory of directoryPaths(entries)) {
    parts.push(tarHeader(`${directory}/`, 0, Date.now() / 1000, true));
  }
  for (const entry of entries) {
    parts.push(tarHeader(entry.path, entry.file.size, entry.file.lastModified / 1000, false));
    parts.push(entry.file);
    const padding = (TAR_BLOCK_SIZE - (entry.file.size % TAR_BLOCK_SIZE)) % TAR_BLOCK_SIZE;
    if (padding > 0) parts.push(new Uint8Array(padding));
  }
  parts.push(new Uint8Array(TAR_BLOCK_SIZE * 2));

  const compressed = new Blob(parts).stream().pipeThrough(new CompressionStream('gzip'));
  return new Response(compressed).blob();
}

function directoryPaths(entries: SelectedEntry[]): string[] {
  const directories = new Set<string>();
  for (const entry of entries) {
    const segments = entry.path.split('/');
    for (let depth = 1; depth < segments.length; depth++) {
      directories.add(segments.slice(0, depth).join('/'));
    }
  }
  return Array.from(directories).sort();
}

function tarHeader(path: string, size: number, modifiedSeconds: number, isDirectory: boolean): Uint8Array {
  const header = new Uint8Array(TAR_BLOCK_SIZE);
  const encoder = new TextEncoder();
  const writeText = (value: string, offset: number, length: number) => {
    header.set(encoder.encode(value).subarray(0, length), offset);
  };
  const writeOctal = (value: number, offset: number, length: number) => {
    writeText(Math.floor(value).toString(8).padStart(length - 1, '0'), offset, length);
  };

  const { name, prefix } = splitTarPath(path);
  writeText(name, 0, TAR_NAME_FIELD_SIZE);
  writeText(prefix, 345, TAR_PREFIX_FIELD_SIZE);
  writeOctal(isDirectory ? 0o755 : 0o644, 100, 8);
  writeOctal(0, 108, 8);
  writeOctal(0, 116, 8);
  writeOctal(size, 124, 12);
  writeOctal(modifiedSeconds, 136, 12);
  writeText(isDirectory ? '5' : '0', 156, 1);
  writeText('ustar', 257, 6);
  writeText('00', 263, 2);

  // The checksum is computed with its own field filled with spaces, then written back over them.
  header.fill(0x20, 148, 156);
  let checksum = 0;
  for (const byte of header) checksum += byte;
  writeOctal(checksum, 148, 7);
  header[154] = 0;
  header[155] = 0x20;
  return header;
}

function splitTarPath(path: string): { name: string; prefix: string } {
  const encoder = new TextEncoder();
  if (encoder.encode(path).length <= TAR_NAME_FIELD_SIZE) {
    return { name: path, prefix: '' };
  }
  const separatorIndex = path.lastIndexOf('/', TAR_PREFIX_FIELD_SIZE);
  if (separatorIndex <= 0) {
    throw new Error(`path is too long for a tar archive: ${path}`);
  }
  const name = path.substring(separatorIndex + 1);
  if (encoder.encode(name).length > TAR_NAME_FIELD_SIZE) {
    throw new Error(`path is too long for a tar archive: ${path}`);
  }
  return { name, prefix: path.substring(0, separatorIndex) };
}

function formatBytes(bytes: number): string {
  const megabytes = bytes / (1024 * 1024);
  if (megabytes >= 1) return `${megabytes.toFixed(1)} MB`;
  return `${(bytes / 1024).toFixed(1)} KB`;
}
