# TMAG kiosk

The TMAG kiosk is a Seamap deployment for a 4K touch table at the Tasmanian Museum and Art
Gallery. Visitors pick a story, and each story steps through map views with narration and
media. The code is in `frontend/src/cljs/imas_seamap/tmag_kiosk/`, styles in
`frontend/src/ui/css/tmag-kiosk.scss`, and the local entry pages in
`frontend/resources/public/tmag-kiosk.html` (dev build) and `tmag-kiosk-release.html` (release
build; see DOCKER.md).

This doc records the decisions behind the kiosk, how it fits together, how to test it, and
what's still open. Keep it current: update it in the same change as the code it describes.

## Decisions and requirements

### Display and hardware

- The table is 3840×2160 at devicePixelRatio 1. Styles are sized in design units, `u(n)`
  (`--kiosk-u`: one pixel of a 1920×1080 design, so 2px on the table, scaling down in smaller
  windows). Use `u()` for new styles, not px. Things sized in code (the media markers) use the
  same scale (`design-unit`).
- The kiosk PC hasn't been bought. Assume a mid-range CPU with integrated graphics and 16 GB
  RAM. Don't optimise for weaker hardware than that, but don't count on spare capacity
  either: the PC runs nothing else.
- Runtime: Edge in Windows kiosk mode, always InPrivate. Edge's own idle reset (which
  restarts the browser and wipes everything) stays off; the app resets itself.

### Map flights

- Animations must hold 60 fps at 4K on integrated graphics and feel quick, or not exist. Judge
  them by their worst frames (missed vsyncs, frame time against 16.6 ms), never by averages.
- A flight must set off on the frame after the tap, from the home screen into any story, and
  between any steps.
- The path is a parabolic zoom out and in (van Wijk and Nuij's, as Leaflet's `flyTo`), drawn on
  a canvas from tiles decoded in advance. Short hops don't zoom far out.
- Destination layers may appear a few hundred ms after landing, as long as the flight itself is
  smooth: overlays aren't drawn in flight (only the basemap, and the start view's overlays
  fading out).
- Prefetching tiles along flight paths is fine: these are targeted local paths whose tiles
  would be fetched in real time anyway, not bulk downloads.

### Map interaction

- The map uses Leaflet's default interaction and controls. Don't port the prototype's map
  interaction (drift detection, snap-back, gesture limits) or Seamap's own map tools (custom
  controls, scale, keyboard shortcuts, feature info on tap). Touch improvements are explored
  separately.

### Content and data

- Stories come live from the kiosk's own WordPress (story-map plugin), so WordPress edits reach
  the kiosk. A refresh never changes the story a visitor is in.
- Each step's map view is a Seamap save state (the step's shortcode), applied with Seamap's
  `:merge-state`.
- Story fields `read_time` and per-step `media` (url, lat, lon, caption, `overlay` readings)
  come from the story-map plugin; the main Seamap site doesn't use them.
- Every story has media: a media card in the left column shows the current step's items
  (photos; videos tap-to-play with controls), and each item has a marker on the map. The map
  doesn't move to a marker.
- The seamap-kiosk prototype is the source for the UI layout only. Its data handling (static
  feed, poi-content file) isn't authoritative, and its stories are placeholders.
- Never create or change data in the shared dev database, including through the dev API
  (eg POSTing save states). GETs are fine.

### Caching and runtime

- The app manages its own caches in memory rather than relying on Chromium's HTTP cache. The
  hosted URL must work on its own: no local service or proxy as a dependency.
- Offline operation is out of scope. Resilience to outages after warm-up is best effort.
- The app returns to the attract screen after inactivity without reloading, so the session
  (and the caches) live for days.
- Before a visitor taps Start exploring, everything every story needs should be cached, and
  each story's first flight decoded.

## How it works

| File | Role |
|---|---|
| `core.cljs` | Handler registration (the deployment's `config-handlers`), `init`, `warm_up_status()` |
| `events.cljs` | Boot, stories, steps, flight targets, warm-up and the attract-screen tour, idle reset |
| `subs.cljs` | Kiosk subscriptions (story, step, media, markers) |
| `views.cljs`, `map/views.cljs` | Layout (story list, story card, media card, attract screen), the map and media markers |
| `idle.cljs` | Inactivity timer (`timeout-ms`, 2 minutes); a playing video counts as activity |
| `tile_cache.cljs` | In-memory tile cache: Leaflet tiles fetched (CORS) into Blobs, LRU within `budget-bytes` (1 GB), plus a background prefetch queue |
| `bitmaps.cljs` | Decoded tiles (ImageBitmaps) held in named sets, one per flight path |
| `flight.cljs` | Canvas flights: path, planning, drawing, landing, and preparing flights at rest |

**Flights.** Whenever the map comes to rest, the flights a visitor could take from there
(back, next, home; or into each story from home) are planned and their tiles decoded. The
tiles on screen (basemap and overlays) are decoded from the tile cache and uploaded to the GPU,
so every flight starts on Leaflet's own full-resolution tiles. A tap then only shows the canvas
and starts the clock. A flight tapped before its tiles are ready sets off anyway, decoding the
rest two at a time and drawing coarser tiles meanwhile. Leaflet stays hidden under the canvas
until landing, then jumps to the destination; the canvas goes once Leaflet's basemap is shown,
and each overlay fades in once loaded.

**Warm-up.** As soon as the stories and the catalogue are in (`events.cljs` `warm-up`):

1. every step's save state is fetched;
2. every flight of every story has its tiles fetched into the tile cache;
3. the flights from home into each story are decoded, and kept throughout;
4. behind the attract screen, the map visits every step (`tour-next`), so its layers' tiles and
   its photos are cached too. Leaving the attract screen ends the tour.

It's redone for new stories or revisions after a refresh.
`imas_seamap.tmag_kiosk.core.warm_up_status()` shows progress in the console.

**Console diagnostics** (release build too): `imas_seamap.tmag_kiosk.flight.stats()` (the last
flight's frames and timings), `imas_seamap.tmag_kiosk.bitmaps.stats()` (decoded tiles and
memory), `imas_seamap.tmag_kiosk.tile_cache.stats()`.

## Testing

- **Judge performance in the release build only** (`yarn release-local`, then
  `tmag-kiosk-release.html`; see DOCKER.md). The dev build is several times slower and its
  spec checks add pauses.
- **Measure at exactly 3840×2160, DPR 1, at 60 Hz, on integrated graphics.** On a machine with
  a discrete GPU, start a separate Chrome with `--force_low_power_gpu --use-angle=d3d11
  --force-device-scale-factor=1 --remote-debugging-port=9333`, and hold the size with DevTools'
  device mode or the DevTools Protocol's `Emulation.setDeviceMetricsOverride`.
- Chrome takes its frame rate from the primary display: if that's at 30 Hz, every frame reads
  33 ms whatever the page does. Running uncapped (`--disable-gpu-vsync
  --disable-frame-rate-limit`) isn't a substitute: it floods the GPU and some draws stall for
  seconds.
- Chrome stops drawing a window it thinks is covered (`document.visibilityState` is
  "hidden"); for a background test window add `--disable-features=CalculateNativeWinOcclusion
  --disable-backgrounding-occluded-windows --disable-renderer-backgrounding`.
- Reloading a tab can drop DevTools' device emulation: check `innerWidth` after a reload.
- Tap with trusted input (DevTools Protocol `Input.dispatchMouseEvent`), not `element.click()`,
  to measure tap-to-frame latency.
- With only one or two real stories, test 6–8 by serving extra stories to the test browser
  (eg the DevTools Protocol's `Fetch` domain on the `story_map` request), built from
  existing save states. Don't create them in the shared dev database.

**Last measured** (2026-10-20, laptop Intel UHD, 8 stories): cold warm-up 80–165 s (about
16,000 tiles); 48 flights with 0 missed frames, worst frame interval 18.0 ms, worst draw
10.1 ms, tap to first moving frame 18–39 ms; decoded tiles 1.3 GB at home, peak 1.8 GB in a
story.

## Open items

Items marked *(needs a person)* need a decision, access or hardware.

### Decisions *(needs a person)*

- [ ] **Idle timeout:** set the real figure (now 2 minutes); configurable without a rebuild?
- [ ] **Idle warning:** an "Are you still there?" countdown before the reset? (The prototype
      has a 10-second one.)
- [ ] **Hidden start-up errors:** the attract screen covers Seamap's "Something went wrong"
      screen, so a failed start-up is only seen after Start exploring. Acceptable with a quiet
      reload on fatal error (below)?
- [ ] **Story-step framing:** the story card covers the right ~22% of the map, and step views
      aren't framed around it. Solve here, or in the touch-interaction work?
- [ ] **Flight tuning by eye** on the release page: zoom-out (`flight/rho`), pace
      (`seconds-per-unit`, `min-duration`, `max-duration`), the mid-flight detail drop.
- [ ] **Page pinch-zoom:** disabling it was planned; check it fits "default Leaflet
      interaction only".
- [ ] **Home view:** `events/home-bounds` is a placeholder (Tasmania and Bass Strait). Set the
      real extent; allow for the story card; configurable without a rebuild?
- [ ] **Start exploring during warm-up:** a visitor who taps it in the first 1–3 minutes after
      a cold start gets flights from coarse or missing tiles. Hold the button until warm?

### Deployment and hardware *(needs a person)*

- [ ] Find out the kiosk PC's exact CPU/GPU. Flights need a GPU: on CPU alone no 4K animation
      reaches 60 fps, while a weak Intel UHD holds 60 fps drawing two full-screen tile layers.
- [ ] Verify memory and timings on the kiosk hardware: long runs, large videos, decoded tiles
      (about 160 MB per story, plus up to 0.5 GB in a story), the tile cache's 1 GB budget.
- [ ] Turn off Edge's kiosk idle reset on the kiosk PC.
- [ ] Enable CORS on the kiosk WordPress uploads (needed to fetch media into Blobs).
- [ ] IMAS GeoServer's caching headers vary by layer (some WMS layers send `no-store`, others
      `max-age=86400`), so no-store layers are re-downloaded on every visit in every Seamap
      deployment. Deliberate? Per-layer settings or GeoWebCache would help everywhere.
- [ ] Test on touch hardware: a touch mid-flight is swallowed by the canvas.

### Content *(needs a person)*

- [ ] The Seamounts story's shortcodes are borrowed from other story maps: its steps show
      Bass Strait while its media are at the Huon seamounts (about −44.3, 147.2), so most
      markers are off screen; and one step shows GEBCO greyscale at zoom 18, past its last
      tiles (about zoom 10), so its basemap is blank.

### Shared Seamap code *(needs a person)*

- [ ] The dev database lacks catalogue migrations 0037–0039; 0039
      (`DataClassification.summary`, NATHAZ-142) makes `GET /api/classifications/` fail
      against it, which halts Seamap's start-up. Locally, NATHAZ-142's two backend lines
      (`backend/catalogue/models.py`, `serializers.py`) are reverted and must not be
      committed; restore them with `git checkout` once dev is migrated.
- [ ] App-state specs don't match real data (hundreds of problems in dev builds): rich layers'
      `alternate-views` and other rich-layer entries aren't vectors, some layers'
      `layer_name` isn't a string, state-of-knowledge boundaries aren't maps, and a few keys
      are missing. Decide per mismatch whether the spec or the data is wrong. The warm-up's
      tour surfaces many more of the rich-layer ones.

### Runtime

- [ ] Version polling, reloading only on the attract screen.
- [ ] Planned daily restart (postponed during an outage).
- [ ] Reload on fatal error, including a failed start-up hidden by the attract screen.
- [ ] Reset the map view without animation on going idle.

### Caching

- [ ] Media as Blobs (attract and story videos, photos), protecting the attract video and the
      current story's media from eviction. Story videos aren't warmed yet (photos are).
- [ ] esri-leaflet tiled layers have their own `createTile`, so they bypass the tile cache.

### Map flights

- [ ] Taps the instant a flight lands: an unplanned flight is planned at the tap (up to about
      70 ms to the first frame), and one tap in 24 dropped a frame while the map was still
      loading the step it had landed on. A tap 150 ms after landing was clean.
- [ ] The story card changes as a flight starts; Start again closes it only on landing.
- [ ] On a cold visit the canvas gives up after 3 s if Leaflet's basemap hasn't loaded, and
      shows a part-loaded map. Keep it up longer?
- [ ] Basemap sub-layers (eg ESRI Ocean's labels) aren't drawn in flight; only `:tile`
      basemaps fly (others jump).

### UI (prototype layout)

- [ ] Attract screen: looping video with Start exploring (needs a video).
- [ ] Graphic map legend beside the story card.
- [ ] Closing panel ("Take this story with you"), if stories gain a takeaway or QR code.
- [ ] Idle countdown, if wanted (see Decisions).
