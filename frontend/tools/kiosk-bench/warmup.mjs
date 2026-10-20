// Watches the kiosk's warm-up (see frontend/docs/tmag-kiosk.md) until it's
// done: save states, flight tiles prefetched, steps toured, first flights
// decoded. Prints a row every few seconds.
//
//   node warmup.mjs [--reload] [--port 9333] [--page tmag-kiosk]
//
// --reload reloads the page first, to time a cold warm-up.
import {connect, options, sleep} from './lib.mjs';

const opts = options();
let c = await connect(opts);
if (opts.reload) {
  await c.ev(`setTimeout(() => location.reload(), 50); 1`);
  c.close();
  await sleep(2000);
  c = await connect(opts);
}
const t0 = Date.now();
let calm = 0;
console.log('sec  stories steps saveStates prefetched toured | cached prefetching inFlight | decoded queued MB');
for (let i = 0; ; i++) {
  const s = await c.ev(`(() => {
    const k = window.imas_seamap && imas_seamap.tmag_kiosk;
    if (!k || !k.core.warm_up_status) return null;
    return {w: JSON.parse(JSON.stringify(k.core.warm_up_status())), b: JSON.parse(JSON.stringify(k.bitmaps.stats()))}; })()`).catch(() => null);
  if (s) {
    const {w, b} = s;
    const done = w.stories && w.saveStates >= w.steps && w.toured >= w.steps && !w.touring
                 && !w.tiles.prefetching && !w.tiles.inFlight && !b.queued && !b.active;
    if (i % 3 === 0 || done)
      console.log([Math.round((Date.now() - t0) / 1000), w.stories, w.steps, w.saveStates, w.prefetched, w.toured, '|',
                   w.tiles.entries, w.tiles.prefetching, w.tiles.inFlight, '|', b.decoded, b.queued, b.mb].join(' '));
    // Done for 5 s running: the tour's end resets the map, which re-prepares.
    calm = done ? calm + 1 : 0;
    if (calm >= 5) { console.log(`Warm after ${Math.round((Date.now() - t0) / 1000) - 5} s.`); break; }
  }
  await sleep(1000);
}
c.close();
