// Checks the test page is fit to measure: viewport, DPR, visibility and frame
// rate (Chrome takes its frame rate from the primary display; at 30 Hz every
// frame reads 33 ms).
//
//   node check.mjs [--port 9333] [--page tmag-kiosk]
import {connect, options} from './lib.mjs';

const c = await connect(options());
const r = await c.ev(`(async () => {
  const visible = document.visibilityState;
  if (visible !== 'visible') return {visible};
  const ts = [];
  await new Promise(r => { const f = t => { ts.push(t); ts.length < 121 ? requestAnimationFrame(f) : r(); }; requestAnimationFrame(f); });
  const iv = ts.slice(1).map((t, i) => t - ts[i]).sort((a, b) => a - b);
  return {visible, viewport: [innerWidth, innerHeight], dpr: devicePixelRatio,
          frameMs: +iv[60].toFixed(2), hz: Math.round(1000 / iv[60])}; })()`);
console.log(r);
const problems = [];
if (r.visible !== 'visible') problems.push('page is hidden (window covered or off-screen?): see README');
if (r.viewport && (r.viewport[0] !== 3840 || r.viewport[1] !== 2160)) problems.push('viewport is not 3840x2160: run hold.mjs');
if (r.dpr && r.dpr !== 1) problems.push('DPR is not 1');
if (r.hz && Math.abs(r.hz - 60) > 2) problems.push(`frame rate is ${r.hz} Hz, not 60: set the primary display to 60 Hz`);
console.log(problems.length ? 'NOT READY:\n- ' + problems.join('\n- ') : 'Ready to measure.');
c.close();
