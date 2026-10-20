// Evaluates a script file (an async function body) in the kiosk page and prints
// what it returns, or saves a screenshot. For one-off checks.
//
//   node run.mjs script.js [--port 9333] [--page tmag-kiosk]
//   node run.mjs --screenshot shot.jpg
import fs from 'node:fs';
import {connect, options} from './lib.mjs';

const opts = options();
const c = await connect(opts);
if (opts.screenshot) {
  const r = await c.send('Page.captureScreenshot', {format: 'jpeg', quality: 80});
  fs.writeFileSync(opts.screenshot, Buffer.from(r.result.data, 'base64'));
  console.log('Saved', opts.screenshot);
} else {
  const v = await c.ev(`(async () => { ${fs.readFileSync(opts._[0], 'utf8')} })()`);
  console.log(typeof v === 'string' ? v : JSON.stringify(v, null, 1));
}
c.close();
