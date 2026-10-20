// Holds the kiosk page at exactly 3840x2160, DPR 1 (as DevTools' device mode
// does) for as long as this runs, whatever the window's size. Optionally serves
// a stories file (see make-stories.mjs) in place of WordPress's story_map
// response, in this browser only.
//
//   node hold.mjs [--stories stories.json] [--port 9333] [--page tmag-kiosk]
//
// Leave it running in its own terminal; reload the page after starting it if
// you're serving stories.
import fs from 'node:fs';
import {connect, options} from './lib.mjs';

const opts = options();
const c = await connect(opts);
await c.send('Emulation.setDeviceMetricsOverride',
             {width: 3840, height: 2160, deviceScaleFactor: 1, mobile: false, screenWidth: 3840, screenHeight: 2160});
if (opts.stories) {
  const body = fs.readFileSync(opts.stories).toString('base64');
  c.on(async m => {
    if (m.method !== 'Fetch.requestPaused') return;
    await c.send('Fetch.fulfillRequest', {
      requestId: m.params.requestId, responseCode: 200, body,
      responseHeaders: [{name: 'Content-Type', value: 'application/json'},
                        {name: 'Access-Control-Allow-Origin', value: '*'}]});
    console.log(new Date().toLocaleTimeString(), 'served', opts.stories);
  });
  await c.send('Fetch.enable', {patterns: [{urlPattern: '*wp-json/wp/v2/story_map*', requestStage: 'Request'}]});
}
console.log('Holding at 3840x2160, DPR 1' + (opts.stories ? `, serving ${opts.stories}` : '') + '. Ctrl+C to stop.');
setInterval(() => {}, 1 << 30);
