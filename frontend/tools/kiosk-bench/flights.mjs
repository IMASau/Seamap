// Taps through the kiosk's stories with trusted input, as a visitor would, and
// reports each flight: tap to first moving frame, frame intervals (a missed
// vsync at 60 Hz shows as about 33 ms), script time to draw a frame (budget
// 16.6 ms), and whether the flight was ready (planned, tiles decoded) at the tap.
// Use the release build, warmed up (warmup.mjs), checked (check.mjs).
//
//   node flights.mjs [mode] [--pause 3000] [--stories N] [--json] [--port 9333] [--page tmag-kiosk]
//
// Modes:
//   (none)                      every story: into it, each step, one Back, home
//   --repeat S,K,R              story S: to step K, then K->K+1 and back R times
//   --pan                       drag the home map, then tap a story after 0, 400 and 3000 ms
//
// --pause: ms to wait after each landing before the next tap (from 1000 up, it
// also waits for decoding to finish, as a visitor reading the step would).
// Tap sooner (eg --pause 0) to test taps the instant a flight lands.
import {connect, options, sleep} from './lib.mjs';

const opts = options();
const pause = +(opts.pause ?? 3000);
const c = await connect(opts);
const out = [];

// Waits for the flight canvas to appear and go again (landed and handed over).
const landed = () => c.ev(`(async () => {
  const sleep = ms => new Promise(r => setTimeout(r, ms));
  let seen = false;
  for (let i = 0; i < 300; i++) {
    const canvas = document.querySelector('canvas.kiosk-flight');
    if (canvas && canvas.style.display === 'block') seen = true;
    if (seen && canvas.style.display === 'none') break;
    await sleep(20);
  }
  return seen; })()`);

const rest = () => c.ev(`(async () => {
  const sleep = ms => new Promise(r => setTimeout(r, ms));
  await sleep(${pause});
  if (${pause} >= 1000)
    for (let i = 0; i < 300; i++) {
      const b = imas_seamap.tmag_kiosk.bitmaps.stats();
      if (b.queued === 0 && b.active === 0) break;
      await sleep(100);
    } })()`);

const fly = async (label, selector, nth = 0) => {
  await c.tap(selector, nth);
  const seen = await landed();
  const s = await c.ev(`(() => {
    const s = JSON.parse(JSON.stringify(imas_seamap.tmag_kiosk.flight.stats()));
    s.tap = window.__tap; return s; })()`);
  const f = {label, seen,
             tapToFrame: Math.round(s['first-frame'] - s.tap),
             prepared: s['prepared?'], missing: s.missing,
             frames: s.frames, missed: s.missed, max: +(+s.max).toFixed(1),
             drawMedian: +(+s.drawMedian).toFixed(1), drawMax: +(+s.drawMax).toFixed(1), drawSlow: s.drawSlow,
             duration: +(+s.duration).toFixed(2), tiles: s.tiles, decodedMB: s.bitmaps.mb};
  out.push(f);
  console.log(opts.json ? JSON.stringify(f)
    : `${label.padEnd(30)} tap->frame ${String(f.tapToFrame).padStart(4)} ms  ${f.prepared ? 'ready' : 'UNPLANNED'}`
      + `${f.missing ? ` (${f.missing} tiles missing)` : ''}  frames ${String(f.frames).padStart(3)}`
      + `  missed ${f.missed}  max ${f.max} ms  draw ${f.drawMedian}/${f.drawMax} ms`
      + `${f.drawSlow?.length ? `  slow draws [t, ms] ${JSON.stringify(f.drawSlow)}` : ''}  ${f.decodedMB} MB`
      + (seen ? '' : '  (NO FLIGHT SEEN)'));
  await rest();
};

const drag = async (dx, dy) => {
  const x = 1400, y = 1000;
  await c.send('Input.dispatchMouseEvent', {type: 'mousePressed', x, y, button: 'left', clickCount: 1});
  for (let k = 1; k <= 10; k++) {
    await c.send('Input.dispatchMouseEvent', {type: 'mouseMoved', x: x + dx * k / 10, y: y + dy * k / 10, button: 'left', buttons: 1});
    await sleep(16);
  }
  await c.send('Input.dispatchMouseEvent', {type: 'mouseReleased', x: x + dx, y: y + dy, button: 'left', clickCount: 1});
};

const steps = () => c.ev(`document.querySelectorAll('.progress-dots span').length`);

if (await c.ev(`!!document.querySelector('.kiosk-attract button')`)) {
  await c.tap('.kiosk-attract button');
  await sleep(1500);
}

if (opts.repeat) {
  const [story, from, reps] = String(opts.repeat).split(',').map(Number);
  await fly(`story ${story}: home->1`, '.browse-story', story - 1);
  for (let k = 1; k < from; k++) await fly(`story ${story}: ${k}->${k + 1}`, '.next-button');
  for (let r = 1; r <= reps; r++) {
    await fly(`story ${story}: ${from}->${from + 1} #${r}`, '.next-button');
    await fly(`story ${story}: ${from + 1}->${from} #${r}`, '.back-button');
  }
  await fly(`story ${story}: ${from}->home`, '.home-button');
} else if (opts.pan) {
  const count = await c.ev(`document.querySelectorAll('.browse-story').length`);
  for (const wait of [0, 400, 3000])
    for (const story of [1, count]) {
      await sleep(3000);
      await drag(-600, 300);
      await sleep(wait);
      await fly(`pan, tap after ${wait} ms: story ${story}`, '.browse-story', story - 1);
      await fly(`story ${story}: 1->home`, '.home-button');
    }
} else {
  const count = Math.min(+(opts.stories ?? Infinity), await c.ev(`document.querySelectorAll('.browse-story').length`));
  for (let i = 1; i <= count; i++) {
    await fly(`story ${i}: home->1`, '.browse-story', i - 1);
    const n = await steps();
    for (let k = 1; k < n; k++) await fly(`story ${i}: ${k}->${k + 1}`, '.next-button');
    if (n > 1) await fly(`story ${i}: ${n}->${n - 1}`, '.back-button');
    await fly(`story ${i}: ${Math.max(1, n - 1)}->home`, '.home-button');
  }
}

const flown = out.filter(f => f.seen);
const worst = k => Math.max(...flown.map(f => f[k]));
const median = k => flown.map(f => f[k]).sort((a, b) => a - b)[flown.length >> 1];
console.log(`\n${flown.length} flights: ${flown.reduce((a, f) => a + f.missed, 0)} missed frames,`
            + ` worst interval ${worst('max')} ms, worst draw ${worst('drawMax')} ms,`
            + ` tap->frame median ${median('tapToFrame')} ms (worst ${worst('tapToFrame')}),`
            + ` ${flown.filter(f => !f.prepared).length} unplanned, ${flown.filter(f => f.missing).length} with tiles missing,`
            + ` decoded tiles up to ${worst('decodedMB')} MB`);
c.close();
