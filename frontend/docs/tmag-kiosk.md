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
- A flight must set off on the frame after the tap: Begin on a story's intro card (into its
  first step), between any steps, and home.
- Choosing a story doesn't move the map, and nothing on the cards waits for a flight: the
  intro card shows at once, and Start again (or Finish) brings the story list back at once,
  while the map flies home under it.
- Panel transitions (story list to a story and back) mustn't cost a flight under way frames.
  The blank panel's width is animated rather than scaled (scaling stretched its shadow and
  corners); it's an empty box, so a frame lays out one box and redraws a rounded rectangle
  and its shadow. Its shadow is handed to the arriving card as it fades out, so the shadow
  never doubles.
- The path is a parabolic zoom out and in (van Wijk and Nuij's, as Leaflet's `flyTo`), drawn on
  a canvas from tiles decoded in advance. Short hops don't zoom far out.
- Destination layers may appear a few hundred ms after landing, as long as the flight itself is
  smooth: overlays aren't drawn in flight (only the basemap, and the start view's overlays
  fading out).
- Prefetching tiles along flight paths is fine: these are targeted local paths whose tiles
  would be fetched in real time anyway, not bulk downloads.

### Map interaction

- The map uses Leaflet's interaction and controls, with the touch improvements explored in
  the touch-sandbox project (below). Don't port the prototype's map interaction (drift
  detection, snap-back) or Seamap's own map tools (custom controls, scale, keyboard
  shortcuts, the feature-info popup).
- Tapping a feature on a step's landed map puts its media in the media card in place of the
  step's (`map-clicked`): every visible WMS layer under the tap is asked for its features as
  JSON (GetFeatureInfo, `BUFFER` of 15 design units for a finger), and features with a photo
  or video become media items, nearest the tap first. A tap that finds none leaves the card as it was; moving to
  another step (or back to the intro) returns to the step's media. Tapping a media marker
  only selects that item.
- Feature properties are read as media items by `feature->media`: the story fields (`url`,
  `lat`, `lon`, `caption`, `overlay`), or the KIOSK layers' current ones (`media_URL` or
  `image_orig` before `url`, which is a web page on the iNaturalist layer; `latitude`/
  `longitude`; caption from `common_name` and `scientific_name`; `temperature` and `depth`
  as readings).
- Controls go where a hand reaches them, and the costly ones away from stray taps: the zoom
  control is bottom-left (a stray zoom is harmless), and Start again is a house icon at the
  left of the story card's Back/Next row, not in a screen corner.

### Layout

- Panels never change size. The story list has the story card's height and right edge, at
  twice its width, and fits 8 stories without scrolling: two columns of four cards, a fixed
  slot each. A card is a band of the story's picture (40% of its height) with the read time
  on it, then the title and summary (two lines each at most). Image-left and text-over-image
  cards were tried and dropped: the first squeezed both, the second darkened the pictures.
- A story opens on an intro card in the story card's place: its whole picture, uncropped
  (the only place it's shown whole; a very tall one is fitted into half the screen's
  height), title, subtitle (`description`) and "Estimated read time: N minutes" (`read_time`,
  if set), with Start again and a long Begin button. The
  map stays home; Begin flies to the first step. The media card and markers belong to the
  steps. Back on the first step returns to the intro card, leaving the map there.
- On the steps, the story card's title band shows the story's picture, cropped to fill it,
  under a dark gradient, with the title in white at the bottom (legible on light pictures
  too). A story without a picture keeps the contour pattern.
- Intro card to first step (Begin) and back: the story's picture crops into the title band
  and its title moves into it, as a view transition (`with-view-transition`), 0.45 s. Only
  those two are captured (the root isn't), so the map stays live. Begin's flight sets off
  once the transition has its snapshots (`views/begin!`, on `ready`): taken during the
  flight, the snapshots cost it a frame (the second, or the first) in about a third of
  flights into a story. Measured, Begin's click to the flight's first frame is 34–50 ms,
  against 35–66 ms for Start again's flight home, which sets off at once. The
  arriving card's body text waits for the title to pass (about 0.4 s). In headless Edge
  some transitions finish without animating; in a real window they all animate.
- Between steps, the copy fades out (0.15 s) and the next fades in (`step-copy`); opacity
  only, as a flight is under way.
- Between the story list and a story, a blank panel narrows (or widens) from one to the other
  and the arriving card fades in over it (`panel-morph`).
- The media card is 16:9, the shape of most media, above a caption strip (two lines) and a
  row for its controls. Items are laid over the window, so no photo can resize the card.
- Videos use the kiosk's own controls (big play button; play/pause, seek, time, mute), sized
  for touch, without the browser's download, speed and picture-in-picture options.
- The zoom buttons are 60 design units, separated, for touch.
- Zoom is continuous (`zoomSnap` 0, from the touch-sandbox project): a pinch stays where the
  fingers leave it rather than snapping to a whole level, and the buttons step a whole level
  from there. Step views that are bounds, and the home view, fit exactly rather than to the
  next whole level out.
- Every flick glides (`inertia.cljs`). The table reports a lift 44–47 ms after the finger's
  last move (measured over 23 swipes); Leaflet measures the release speed from the 50 ms
  before the lift, which held one move or two by chance, so two thirds of swipes didn't glide.
  The speed now comes from the 50 ms before the last move, and a finger still for over
  `max-still-ms` (100 ms) before lifting doesn't glide: on the table, 82 of 85 swipes then
  glided, and of the other three one was held still for 178 ms, one never started a drag and
  one was a short, slow nudge. Recheck the lift delay on the final hardware.
- A flicked map glides off at the speed the finger left it (`inertia.cljs`, from the
  touch-sandbox project). Leaflet's glide starts at half that, as it sizes the glide for a
  steady slow-down but animates it on a curve that slows sharply; measured, a 1500 px/s drag
  glided off at about 730 px/s, and 1490 px/s with the fix. Glides take as long as Leaflet's
  and so go twice as far. Inertia uses Leaflet's default settings for now: deceleration,
  `easeLinearity` and `inertiaMaxSpeed` (which caps speed x `easeLinearity`, not speed) are to
  be tuned on the table. A glide still starts a frame after release, as in Leaflet.
- A touch stops a glide (`inertia/attach!`, from the touch-sandbox project); Leaflet only
  stopped it once the finger moved. That touch isn't a tap on the map, so it doesn't ask for
  features. It's caught on `pointerdown`, as Leaflet's own `down` fires again for the
  mousedown the browser makes up after a tap.
- Never a grey map, whatever the network (`map/tiers.cljs`, from the touch-sandbox project,
  reworked). Tiles are loaded ahead of the visitor, and coarser copies show wherever a
  layer's own tiles haven't arrived. Under the basemap:
  - the basemap loads 1 tile beyond the screen on every side (`detail-ring`);
  - a copy 3 zoom levels coarser (`coarse-levels`) loads half a screen beyond each edge
    (`coarse-ring`): about 20 tiles, each covering 8 x 8 of the basemap's, so a gap shows
    the map blurred rather than flat colour, even zoomed far in;
  - the floor: the basemap at the kiosk's min zoom, covering its whole extent, and kept
    through jumps. Inside the extent something is always shown; zoomed out all the way, it's
    the map at full resolution. Warm-up fetches the floor of each basemap the stories use and
    pins it in the tile cache: zoom 6, 308 tiles each with the placeholder extent. A floor
    over `max-floor-tiles` (600, about two screens) goes a zoom level coarser. The coarse copy
    loads nothing at or below the floor's zoom (eg at home).

  Overlays (the catalogue's tile and WMS layers) load the same 1-tile ring, and each has a
  coarse copy too, 3 levels coarser with the same half-screen ring, in a hidden pane
  (`attach!`): a copy stacked under an overlay that isn't opaque would show through it.
  Instead, an overlay tile that isn't cached shows its part of the nearest coarser tile in
  the cache (up to 5 levels coarser) in an element just under it, or of the copy's tile once
  that arrives, and the tile fades in over it (`placeholder`, via
  `tile-cache/placeholder-by!`). Checked on a throttled network (300 ms latency, 3 Mbps):
  panning 0.8–1.5 screens, every overlay tile not yet in showed a placeholder within about
  half a second (those within the copy's ring at once), and none flashed out on arrival. A
  copy follows its overlay's `redraw` (new URL or WMS parameters). A WMS layer with
  scale-dependent styling may draw differently, or not at all, 3 levels coarser: then its
  placeholder does too.

  Most visitors stay near where a story step lands them. The attract screen's tour already
  visits every step; once a step's layers have loaded, it pins what every layer loads there,
  rings and coarse copies included (`keep-surroundings!`), and fetches any of it still
  missing. 430–920 tiles a step with the test stories. Steps no longer in any story are
  unpinned at the next warm-up. Flights are fixed paths, so they have no rings (see below).

  Fetches go on screen first, coarse first (`rank`, via `tile-cache/rank-by!`). Measured on
  the table on a throttled network (300 ms latency, 3 Mbps), from a cold cache, jumping to
  a new place at zoom 12 then six fast flicks (306 points sampled per frame, about 1,000
  frames a run; tile requests in brackets; before the coarse ring was halved):

  | | Frames with grey, jump | Frames with grey, flicks (worst) |
  |---|---|---|
  | North Tasmania: basemap alone | 100% | 72% (59% of the map) [425] |
  | the previous backdrop | 0% | 2.3% (17%) [419] |
  | tiers | 0% | 0% [512] |
  | Western Port (no story nearby): basemap alone | 100% | 87% (77%) [437] |
  | the previous backdrop | 42% | 8.6% (17%) [441] |
  | tiers | 0% | 0% [494] |

  Tiers also showed no grey at zoom 15 and 17 unthrottled, nor at story steps. They cost
  about 16% more tile requests. Flicks hold 60 fps with them (no frame over 20 ms in 5 of 6
  runs). A jump (`setView`) dropped 3–8 frames (up to 100 ms) as Leaflet made more tiles,
  against 2–4 (33 ms) for the basemap alone, but those runs were on a PC short of memory
  (a stray test process): recheck. Jumps only happen under a flight's canvas, on the
  attract screen and on the idle reset.
  Only tile basemaps have tiers. The sandbox stacked a layer every 4 zoom levels instead;
  those at or past the map's zoom loaded the basemap's own tiles again.
- Tile layers keep 4 rows of tiles beyond those they load (`keepBuffer`, Leaflet's default is
  2), so panning back shows tiles still in the page instead of decoding them again.
- The kiosk has one set of limits on where the visitor can take the map, the same for every
  story (decided: not per story or step) (`map/limits.cljs`, from the touch-sandbox
  project): an extent and a zoom range, `{:bounds :min-zoom :max-zoom}`, all optional, in
  `events/kiosk-limits`, for now a placeholder extent from Bass Strait to the Antarctic
  coast. With an extent and no min zoom, the map zooms out until the extent fits the screen.
  - Limits are set as the map is moved to a view (a flight landing, the attract screen's
    tour, the idle reset), and always take that view in: for a step outside them, the extent
    grows to hold it, and the zoom range to include its zoom, until the map next moves. So
    limits never move a landing (checked, when limits were per view, with a story whose
    limits left out its first step: the flight landed exactly, and home by flight and by idle
    reset landed exactly with the default limits back).
  - Drags and pinches past the limits are held back by `resistance` (0.75: the map follows
    the finger by a quarter), then spring back on release. Leaflet does this for drags
    (`maxBoundsViscosity`); `install!` adds it for pinches, both zoom and centre (replaces
    `Map.TouchZoom` `_onTouchMove`). Measured: a pinch asking for 1.28 levels past the min
    zoom went 0.32 past; an off-centre pinch at the extent's edge went 1.55° past it, against
    6.18° without resistance; a 2000 px drag past the edge moved the map 494 px.
  - Where the extent is narrower than the screen at some zoom, it's widened to the screen
    there, about its own centre; Leaflet would otherwise hold the extent's centre at the
    screen's centre, so every drag sprang straight back.
- Frame rate, measured on the table with touch input: in the release build, drags and glides
  hold 60 fps (worst frame 16.9 ms), including the first drag after the map jumps somewhere
  new and its tiles arrive. The dev build drops frames there (33–50 ms), with or without the
  backdrop, so judge feel on the release page.

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
- Before a visitor taps Start exploring, everything every story needs should be cached. A
  story's first flight is decoded when its intro card opens, while the visitor reads it,
  rather than every story's being held decoded at home.

## How it works

| File | Role |
|---|---|
| `core.cljs` | Handler registration (the deployment's `config-handlers`), `init`, `warm_up_status()` |
| `events.cljs` | Boot, stories, steps, flight targets, warm-up and the attract-screen tour, idle reset |
| `subs.cljs` | Kiosk subscriptions (story, step, media, markers) |
| `views.cljs`, `map/views.cljs` | Layout (story list, story card, media card, attract screen), the map and media markers |
| `idle.cljs` | Inactivity timer (`timeout-ms`, 2 minutes); a playing video counts as activity |
| `inertia.cljs` | Leaflet's drag inertia: every flick glides, starting at the release speed (replaces `Map.Drag` `_onDragEnd`); a touch stops a glide |
| `map/limits.cljs` | The kiosk's extent and zoom range, resisting drags and pinches past them (replaces `Map.TouchZoom` `_onTouchMove`) |
| `map/tiers.cljs` | No grey map: the basemap and overlays load a ring beyond the screen; the basemap sits over a coarse copy (a wider ring) and a floor (the whole extent at its min zoom); overlays' coarse copies are hidden and show as placeholders; each step's surroundings are pinned; which tiles are on screen (replaces `GridLayer` `_getTiledPixelBounds`) |
| `tile_cache.cljs` | In-memory tile cache: Leaflet tiles fetched (CORS) into Blobs, LRU within `budget-bytes` (1 GB) but for pinned tiles (floors, steps' surroundings) and those shown, fetches ranked (on screen first), placeholders while fetching, plus a background prefetch queue |
| `bitmaps.cljs` | Decoded tiles (ImageBitmaps) held in named sets, one per flight path |
| `flight.cljs` | Canvas flights: path, planning, drawing, landing, and preparing flights at rest |

**Flights.** Whenever the map comes to rest, the flights a visitor could take from there
(back, next, home; on an intro card, into its story) are planned and their tiles decoded. The
tiles on screen (basemap and overlays) are decoded from the tile cache and uploaded to the GPU,
so every flight starts on Leaflet's own full-resolution tiles. Each prepared flight's decoded
tiles are then uploaded too, `uploads-per-frame` (8) a frame while the map stays put
(`upload-gradually!`): uploaded on first draw instead, they held up the GPU early in the
flight (frames 4–15, as the zoom-out reached tiles that weren't on screen at rest), and a
quarter of flights into a story missed frames there. A tap then only shows the canvas
and starts the clock. A flight tapped before its tiles are ready sets off anyway, decoding the
rest two at a time and drawing coarser tiles meanwhile. Leaflet stays hidden under the canvas
until landing, then jumps to the destination; the canvas goes once Leaflet's basemap is shown,
and each overlay fades in once loaded. "On screen" and "loaded" mean the tiles actually on
screen (`tiers/on-screen`), not every tile a layer holds: layers keep tiles beyond the screen,
which were decoded at rest too (360 tiles, 90 MB, where 209 were on screen), and the basemap's
ring would have held up landings.

**Warm-up.** As soon as the stories and the catalogue are in (`events.cljs` `warm-up`):

1. every step's save state is fetched;
2. every flight of every story has its tiles fetched into the tile cache;
3. behind the attract screen, the map visits every step (`tour-next`), so its layers' tiles and
   its photos are cached too, and what its layers load there, rings included, is pinned in
   the tile cache (`tiers/keep-surroundings!`). Leaving the attract screen ends the tour;
4. the floor of each basemap the stories use is fetched and pinned in the tile cache
   (`:kiosk.map/keep-floors`).

It's redone for new stories or revisions after a refresh.
`imas_seamap.tmag_kiosk.core.warm_up_status()` shows progress in the console.

**Console diagnostics** (release build too): `imas_seamap.tmag_kiosk.flight.stats()` (the last
flight's frames and timings), `imas_seamap.tmag_kiosk.bitmaps.stats()` (decoded tiles and
memory), `imas_seamap.tmag_kiosk.tile_cache.stats()`.

## Testing

The scripts in `frontend/tools/kiosk-bench/` do all of the following (see its README).

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
- Test with 8 stories: add test stories (titles starting "TEST") to the local WordPress
  (`docker compose run --rm --entrypoint wp wp-init ...`), reusing existing save states.
  Don't create save states in the shared dev database.

**Last measured** (2026-10-20, laptop Intel UHD, 8 stories): cold warm-up 80–165 s (about
16,000 tiles); 48 flights with 0 missed frames, worst frame interval 18.0 ms, worst draw
10.1 ms, tap to first moving frame 18–39 ms; decoded tiles 1.3 GB at home, peak about 1.9 GB
in a story. That predates the intro card: home now holds only the tiles on screen, and each
story's first flight is decoded on its intro card, so re-measure (memory at home, and Begin
tapped soon after a story opens).

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
- [ ] **Inertia tuning on the table:** `inertiaDeceleration`, `easeLinearity` and
      `inertiaMaxSpeed` are Leaflet's defaults; the touch sandbox's tuning (3000, 0.2, 5000)
      is a starting point.
- [ ] **Map limits:** the real extent (`events/kiosk-limits` is a placeholder). Tune
      `limits/resistance` on the table.
- [ ] **GEBCO basemap past zoom 10:** its tiles at zoom 11–14 fail (315 failed fetches in a
      warm-up), so zoomed in, only its coarse copy and floor show. Its catalogue entry
      probably needs a `maxNativeZoom`, so Leaflet scales up its deepest tiles instead.
- [ ] **Swipe back:** a sideways flick on the map took Edge back a page (seen in testing, in a
      window with history); the kiosk never navigates, so it has none, but anything that does
      (a link in story content) would make a flick leave the kiosk. Turn off Edge's swipe
      navigation, or `overscroll-behavior: none` on the page?
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
      (about 160 MB per story, plus up to 0.6 GB in a story), the tile cache's 1 GB budget.
- [ ] Turn off Edge's kiosk idle reset on the kiosk PC.
- [ ] Enable CORS on the kiosk WordPress uploads (needed to fetch media into Blobs).
- [ ] IMAS GeoServer's caching headers vary by layer (some WMS layers send `no-store`, others
      `max-age=86400`), so no-store layers are re-downloaded on every visit in every Seamap
      deployment. Deliberate? Per-layer settings or GeoWebCache would help everywhere.
- [ ] Test on touch hardware: a touch mid-flight is swallowed by the canvas.

### Content *(needs a person)*

- [ ] Agree one property schema for the KIOSK feature layers' media (ideally the story
      fields: `url`, `lat`, `lon`, `caption`, `overlay`), so `feature->media` can drop the
      per-layer fallbacks. The VME layer has no captions; the iNaturalist layer has no
      credit or licence field (its photos need attribution).

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
- [ ] The story card changes as a flight starts.
- [ ] Begin tapped the moment a story opens may find its flight still decoding (it sets off
      anyway, from coarser tiles). Measure how long the intro takes to get it ready.
- [ ] On a cold visit the canvas gives up after 3 s if Leaflet's basemap hasn't loaded, and
      shows a part-loaded map. Keep it up longer?
- [ ] Basemap sub-layers (eg ESRI Ocean's labels) aren't drawn in flight; only `:tile`
      basemaps fly (others jump).
- [ ] **Flight frames, checked on the table only.** Flights missed frames in every build,
      the last commit included: about one flight in two into a story, 1–3 frames early on,
      none on the way home. Traces showed the GPU blocked presenting
      (`DXGISwapChainImageBacking::Present`), not the page's main thread. Two causes, both
      fixed: tiles uploaded to the GPU mid-flight (now uploaded at rest), and Begin's view
      transition taking its snapshots mid-flight (now before it sets off). Release build,
      20 flights (into five stories and home, twice) in one page load:

      | | Flights missing frames | Frames missed |
      |---|---|---|
      | Before | 5 of 20 | 8 |
      | Uploads at rest | 5 of 20 (frame 0 only) | 5 |
      | Uploads at rest, and Begin after the snapshots | 0 of 40 (two runs) | 0 |
      | With overlays' coarse copies and placeholders (8 stories) | 5 of 192 (four runs; 4 of 128 with copies, 1 of 64 without) | 5, 1 each |

      The missed frames since the coarse copies were added are single frames, two of them at
      a flight's first frame and one in the first flight after a reload, and come and go
      between runs with or without the copies.

      None of the touch-sandbox ports cost flights frames: switching each off (continuous
      zoom, limits, inertia, tiers) changed nothing beyond run-to-run noise. Still to check:
      flights between steps (Next; every local test story has one step), the kiosk
      hardware's GPU (this PC's is an NVIDIA T400), and the first flight after a reload,
      which missed frames more often before the fixes.

### UI (prototype layout)

- [ ] Attract screen: looping video with Start exploring (needs a video).
- [ ] Graphic map legend beside the story card.
- [ ] Closing panel ("Take this story with you"), if stories gain a takeaway or QR code.
- [ ] Idle countdown, if wanted (see Decisions).
