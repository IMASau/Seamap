// Builds a stories file for hold.mjs: the kiosk WordPress's real stories plus
// test stories made from existing Seamap save states, so 6-8 stories can be
// measured before they've been written. Reads only (GETs); creates nothing in
// WordPress or the database.
//
// Run it against the DEV page (tmag-kiosk.html): it decodes save states with
// the app's own code, which the release build doesn't expose.
//
//   node make-stories.mjs [--out stories.json] [--extra 7] [--steps 4]
//       [--api http://localhost:8000/api/] [--wordpress http://localhost:8888/]
//       [--bounds 143,-44.5,149.5,-39.5]   (west,south,east,north; default around Tasmania)
//       [--port 9333] [--page tmag-kiosk.html]
//
// Each test story's first step is one of the deeper views (zoom 13+), the
// costliest flights to keep ready; its other steps zoom 9-12. Test stories
// reuse the first real story's text and media.
import fs from 'node:fs';
import {connect, options, sleep} from './lib.mjs';

const opts = options();
opts.page = opts.page === 'tmag-kiosk' ? 'tmag-kiosk.html' : opts.page;
const out = opts.out ?? 'stories.json';
const extra = +(opts.extra ?? 7);
const steps = +(opts.steps ?? 4);
const api = opts.api ?? 'http://localhost:8000/api/';
const wordpress = opts.wordpress ?? 'http://localhost:8888/';
const [west, south, east, north] = String(opts.bounds ?? '143,-44.5,149.5,-39.5').split(',').map(Number);

const real = await (await fetch(`${wordpress}wp-json/wp/v2/story_map?acf_format=standard&per_page=100`)).json();
if (!real.length) throw new Error('No stories in WordPress to base test stories on.');

const c = await connect(opts);
for (let i = 0; i < 240 && !(await c.ev(`!!(window.re_frame && cljs.core.get(cljs.core.deref(re_frame.db.app_db), cljs.core.keyword('initialised')))`)); i++)
  await sleep(500);
const views = await c.ev(`(async () => {
  const k = cljs.core.keyword, db = cljs.core.deref(re_frame.db.app_db);
  const bases = cljs.core.clj__GT_js(cljs.core.get_in(db, cljs.core.vector(k('map'), k('grouped-base-layers'))));
  const tile = new Set(bases.filter(b => b.layer_type === 'tile').map(b => b.id));
  const list = await (await fetch(${JSON.stringify(api + 'savestates/')})).json();
  const views = [], seen = new Set();
  for (const s of list) {
    try {
      const m = cljs.core.clj__GT_js(imas_seamap.utils.parse_state(s.hashstate)).map || {};
      const [lat, lon] = m.center || [];
      const key = [lat?.toFixed(2), lon?.toFixed(2), m.zoom].join();
      if (m.zoom >= 9 && m.zoom <= 17 && lon > ${west} && lon < ${east} && lat > ${south} && lat < ${north}
          && tile.has(m['active-base']) && (m.active || []).length && !seen.has(key)) {
        seen.add(key);
        views.push({id: s.id, zoom: m.zoom, base: m['active-base']});
      }
    } catch (e) {}
  }
  return views; })()`);
c.close();

const deep = views.filter(v => v.zoom >= 13), shallow = views.filter(v => v.zoom < 13);
if (!deep.length || shallow.length < steps - 1) throw new Error(`Not enough save states in bounds (${deep.length} deep, ${shallow.length} others).`);
const base = real[0];
const stories = [...real];
for (let n = 0; n < extra; n++) {
  const story = structuredClone(base);
  story.id = 9000 + n;
  story.title.rendered = `Test story ${n + 1}`;
  const picked = [deep[n % deep.length], ...Array.from({length: steps - 1}, (_, i) => shallow[(n * (steps - 1) + i) % shallow.length])];
  const links = base.acf.map_links;
  story.acf.map_links = picked.map((v, i) => ({...structuredClone(links[Math.min(i, links.length - 1)]),
                                              shortcode: v.id, subtitle: `Step ${i + 1} (zoom ${v.zoom}, basemap ${v.base})`}));
  stories.push(story);
  console.log(story.title.rendered, picked.map(v => `z${v.zoom}`).join(' '));
}
fs.writeFileSync(out, JSON.stringify(stories));
console.log(`Wrote ${out}: ${real.length} real + ${extra} test stories (from ${views.length} save states in bounds).`);
