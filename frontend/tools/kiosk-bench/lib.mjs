// A minimal Chrome DevTools Protocol client for the kiosk bench scripts (Node 22+,
// for its built-in WebSocket and fetch).

export const sleep = ms => new Promise(r => setTimeout(r, ms));

// Options shared by the scripts: --port (default 9333) and --page (a substring
// of the page's URL, default "tmag-kiosk").
export function options(argv = process.argv.slice(2)) {
  const opts = {port: 9333, page: 'tmag-kiosk', _: []};
  for (let i = 0; i < argv.length; i++) {
    const m = argv[i].match(/^--([\w-]+)(?:=(.*))?$/);
    if (!m) { opts._.push(argv[i]); continue; }
    const value = m[2] ?? (argv[i + 1] && !argv[i + 1].startsWith('--') ? argv[++i] : true);
    opts[m[1]] = value;
  }
  opts.port = +opts.port;
  return opts;
}

async function connectTo(url) {
  const ws = new WebSocket(url);
  await new Promise((resolve, reject) => { ws.addEventListener('open', resolve); ws.addEventListener('error', reject); });
  let id = 0;
  const pending = new Map();
  const listeners = [];
  ws.addEventListener('message', e => {
    const m = JSON.parse(e.data);
    if (pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); }
    else for (const f of listeners) f(m);
  });
  const send = (method, params = {}) => new Promise(r => { pending.set(++id, r); ws.send(JSON.stringify({id, method, params})); });
  return {ws, send, on: f => listeners.push(f), close: () => ws.close()};
}

// Connects to the first page whose URL contains `match`.
export async function connect({port, page: match}) {
  const pages = await (await fetch(`http://127.0.0.1:${port}/json`)).json();
  const page = pages.find(p => p.type === 'page' && p.url.includes(match));
  if (!page) throw new Error(`No page matching "${match}": ${pages.map(p => p.url).join(', ')}`);
  const c = await connectTo(page.webSocketDebuggerUrl);
  // Evaluates `expression` in the page (awaiting a Promise) and returns its value.
  c.ev = async expression => {
    const r = await c.send('Runtime.evaluate', {expression, awaitPromise: true, returnByValue: true});
    if (r.result.exceptionDetails) throw new Error(JSON.stringify(r.result.exceptionDetails).slice(0, 1000));
    return r.result.result.value;
  };
  // Taps the `nth` element matching `selector` with trusted input (as a visitor
  // would), noting the pointerdown's time in the page as window.__tap.
  c.tap = async (selector, nth = 0) => {
    const at = await c.ev(`(() => {
      const el = document.querySelectorAll(${JSON.stringify(selector)})[${nth}];
      if (!el) return null;
      el.scrollIntoView({block: 'nearest'});
      const r = el.getBoundingClientRect();
      return {x: r.x + r.width / 2, y: r.y + r.height / 2}; })()`);
    if (!at) throw new Error(`Nothing to tap: ${selector} [${nth}]`);
    await c.ev(`window.__tap = null; addEventListener('pointerdown', e => { window.__tap = e.timeStamp; }, {once: true, capture: true}); 1`);
    for (const type of ['mousePressed', 'mouseReleased'])
      await c.send('Input.dispatchMouseEvent', {type, x: at.x, y: at.y, button: 'left', clickCount: 1});
  };
  return c;
}

// The browser itself (for window placement).
export async function connectBrowser({port}) {
  const v = await (await fetch(`http://127.0.0.1:${port}/json/version`)).json();
  return connectTo(v.webSocketDebuggerUrl);
}
