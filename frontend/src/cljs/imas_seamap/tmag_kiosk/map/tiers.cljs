;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.map.tiers
  "No grey map, whatever the network: tiles are loaded ahead of the visitor,
  and where a layer's tiles haven't arrived, a coarser copy shows instead.
  From the touch-sandbox project, reworked.

  The basemap has three tiers, top to bottom:

  - Detail: the basemap itself, which loads `detail-ring` tiles beyond the
    screen on every side.
  - Coarse (`backdrop`): the basemap `coarse-levels` zoom levels coarser,
    loading `coarse-ring` screens beyond each edge. Its tiles cover
    2^coarse-levels x 2^coarse-levels of the basemap's, so that's few tiles,
    and wherever the basemap is missing, the map still shows, blurred. It
    loads nothing at or below the floor's zoom, which already covers it.
  - Floor (`backdrop`): the basemap at the kiosk's minimum zoom
    (`imas-seamap.tmag-kiosk.map.limits`; coarser where that would take over
    `max-floor-tiles`), covering its whole extent. Inside the extent,
    something is always shown; zoomed out all the way, it's the map at full
    resolution. Warm-up fetches it (`keep-floors!`), and the tile cache keeps
    it.

  Overlays (the other tile layers, eg the catalogue's WMS layers) load
  `detail-ring` tiles beyond the screen too, and each has a coarse copy
  (`attach!`) that loads as the basemap's does, but isn't shown: under an
  overlay that isn't opaque, it would show through. Instead, an overlay's tile
  that hasn't arrived shows its part of the nearest coarser tile in the tile
  cache (`placeholder`), until it does.

  Off-screen tiles are fetched after those on screen, and the coarse ones
  first (`rank`). Most visitors stay near the views the stories take them to,
  so the attract screen's tour keeps what every layer loads at each step, the
  rings included, in the tile cache (`keep-surroundings!`).

  `install!` patches `L.GridLayer`'s `_getTiledPixelBounds` (the area a layer
  loads) to pad it, and raises Leaflet's `keepBuffer` (rows of tiles beyond it
  that stay in the page) from 2 to 4, for every tile layer. It patches the
  prototype for the whole page, so only the kiosk installs it.
  `_getTiledPixelBounds` is private: check it against this on a Leaflet
  upgrade (Seamap pins 1.9.0).

  A layer's tiles on screen, rather than every tile it holds, are `on-screen`,
  and `loading-on-screen?` is Leaflet's `isLoading` for those alone."
  (:require ["leaflet" :as L]
            ["@react-leaflet/core" :as ReactLeafletCore]
            [goog.object :as gobject]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [imas-seamap.interop.leaflet :as leaflet]
            [imas-seamap.tmag-kiosk.map.limits :as limits]
            [imas-seamap.tmag-kiosk.tile-cache :as tile-cache]))

;; Tiles beyond the screen, on every side, that the basemap and overlays load.
(def detail-ring 1)
;; How many zoom levels coarser the coarse copies are than their layers, and
;; how many screens beyond each edge they load.
(def coarse-levels 3)
(def coarse-ring 0.5)
;; Tiles beyond the extent the floor covers, as a drag past it goes a little way.
(def floor-margin 1)
;; Most tiles in a floor (about two screens): a floor that would need more (a
;; high min zoom over a wide extent) goes down a zoom level, for a quarter of them.
(def max-floor-tiles 600)

;; Rows of tiles beyond those a layer loads that stay in the page (Leaflet's
;; default is 2).
(def keep-buffer 4)

;; Leaflet's own: the screen at the layer's tile zoom, in pixels.
(def ^:private tiled-pixel-bounds (.. L/default -GridLayer -prototype -_getTiledPixelBounds))

(defn backdrop?
  "Whether the layer is one of the kiosk's copies of a layer: the basemap's
  under it, or an overlay's coarse copy."
  [^js layer]
  (some? (.. layer -options -kioskTier)))

(defn- tier
  "\"floor\", \"coarse\", \"detail\" (the basemap) or nil (anything else)."
  [^js layer]
  (or (.. layer -options -kioskTier)
      (let [url (.-_url layer)]
        (when (and url (= url (some-> (.-_map layer) .-kioskBasemapUrl)))
          "detail"))))

(defn- overlay?
  "Whether the layer is an overlay with a coarse copy: a tile layer, but not
  the basemap nor one of the copies."
  [^js layer]
  (and (instance? (.-TileLayer L/default) layer) (nil? (tier layer))))

;;; Tile URLs

(defn tile-urls
  "A tile basemap's tiles, with their URLs exactly as Leaflet requests them (so
  they're shared with the tile cache): {:max-zoom :url (fn [z x y])}.
  `:max-zoom` is its last tiles' zoom: `max-native-zoom` if it has one (see
  `imas-seamap.tmag-kiosk.events/native-max-zooms`)."
  [server-url max-native-zoom]
  (let [^js layer ((.-tileLayer L/default) server-url)
        urls      (js/Map.)]
    {:max-zoom (or max-native-zoom (.. layer -options -maxZoom))
     :url      (fn [z x y]
                 (let [k (str z "/" x "/" y)]
                   (or (.get urls k)
                       (do (set! (.-_tileZoom layer) z) ; Leaflet's URL zoom comes from here
                           (let [url (.getTileUrl layer #js {:x x :y y :z z})]
                             (.set urls k url)
                             url)))))}))

(defn- url-at
  "The URL Leaflet requests for the layer's tile at `coords` (wrapped), at any
  zoom: `getTileUrl` takes the zoom from the layer's `_tileZoom`."
  [^js layer ^js coords]
  (let [z (.-_tileZoom layer)]
    (set! (.-_tileZoom layer) (.-z coords))
    (try
      (.getTileUrl layer coords)
      (finally (set! (.-_tileZoom layer) z)))))

(defn- tile-coords [x y z]
  (doto ((.-point L/default) x y) (gobject/set "z" z)))

;;; The floor

;; The current floor's zoom, or nil for none.
(defonce ^:private floor-zoom (atom nil))

(defn- floor-tiles
  "The floor for `extent` (an L.LatLngBounds, or nil for none) and `min-zoom`:
  {:zoom :x0 :x1 :y0 :y1}, its tiles (x unwrapped), at most `max-floor-tiles`."
  [^js m ^js extent min-zoom max-zoom]
  (when extent
    (let [^js b (limits/bounds-at m extent min-zoom)
          tile  #(js/Math.floor (/ % 256))]
      (loop [z (-> (js/Math.round min-zoom) (max 0) (min max-zoom))]
        (let [nw    (.project m (.getNorthWest b) z)
              se    (.project m (.getSouthEast b) z)
              floor {:zoom z
                     :x0   (- (tile (.-x nw)) floor-margin)
                     :x1   (+ (tile (.-x se)) floor-margin)
                     :y0   (max 0 (- (tile (.-y nw)) floor-margin))
                     :y1   (min (dec (js/Math.pow 2 z)) (+ (tile (.-y se)) floor-margin))}
              {:keys [x0 x1 y0 y1]} floor]
          (if (and (pos? z) (> (* (inc (- x1 x0)) (inc (- y1 y0))) max-floor-tiles))
            (recur (dec z))
            floor))))))

(defn- floor-urls
  "The URLs of the floor of `basemap` (a catalogue base layer)."
  [^js m {:keys [server_url layer_type max_native_zoom]}]
  (when (and server_url (= layer_type :tile))
    (let [{:keys [max-zoom url]}    (tile-urls server_url max_native_zoom)
          {:keys [extent min-zoom]} (limits/own-extent m)]
      (when-let [{:keys [zoom x0 x1 y0 y1]} (floor-tiles m extent min-zoom max-zoom)]
        (let [n (js/Math.pow 2 zoom)]
          (for [y (range y0 (inc y1)) x (range x0 (inc x1))]
            (url zoom (mod x n) y)))))))

(defn keep-floors!
  "Fetches the floors of `basemaps` (catalogue base layers) into the tile cache
  in the background, and keeps them there in place of the last ones."
  [{:keys [^js leaflet-map basemaps]}]
  (let [urls (vec (distinct (mapcat #(floor-urls leaflet-map %) basemaps)))]
    (tile-cache/pin! ::floors urls)
    (tile-cache/prefetch! urls)))

(re-frame/reg-fx :kiosk.map/keep-floors keep-floors!)

(defn- refloor!
  "Takes the floor from the kiosk's limits, reloading the layer if it's
  changed and `reset?`."
  [^js layer reset?]
  (let [^js m (.-_map layer)
        {:keys [extent min-zoom]} (limits/own-extent m)
        floor (floor-tiles m extent min-zoom (.. layer -options -maxZoom))]
    (when (not= floor (.-_kioskFloor layer))
      (set! (.-_kioskFloor layer) floor)
      (reset! floor-zoom (:zoom floor))
      (when reset? (._resetView layer)))))

(def ^:private FloorLayer
  ;; Its tile zoom is the floor's, at any map zoom, and it loads the floor's
  ;; tiles (`padded-pixel-bounds`) and nothing else. It keeps them when the
  ;; map jumps: Leaflet empties every tile layer then (`viewprereset`), and
  ;; even from the tile cache, its tiles took a couple of frames to show
  ;; again, leaving the screen blank where nothing else had loaded.
  (.extend (.-TileLayer L/default)
           #js {:options      #js {:kioskTier "floor"}
                :getEvents    (fn []
                                (this-as ^js this
                                  (doto (.call (.. L/default -GridLayer -prototype -getEvents) this)
                                    (gobject/remove "viewprereset")
                                    (gobject/set "zoomlevelschange" (.-_kioskRefloor this))
                                    (gobject/set "resize" (.-_kioskRefloor this)))))
                :_kioskRefloor (fn [] (this-as this (refloor! this true)))
                :onAdd        (fn [^js m]
                                (this-as ^js this
                                  (set! (.-kioskBasemapUrl m) (.-_url this))
                                  (refloor! this false)
                                  (.call (.. L/default -GridLayer -prototype -onAdd) this m)))
                :onRemove     (fn [^js m]
                                (this-as ^js this
                                  (when (= (.-kioskBasemapUrl m) (.-_url this))
                                    (set! (.-kioskBasemapUrl m) nil))
                                  (.call (.. L/default -GridLayer -prototype -onRemove) this m)))
                :_clampZoom   (fn [zoom]
                                (this-as ^js this
                                  (or (:zoom (.-_kioskFloor this)) zoom)))
                :_isValidTile (fn [coords]
                                (this-as ^js this
                                  (and (some? (.-_kioskFloor this))
                                       (.call (.. L/default -GridLayer -prototype -_isValidTile) this coords))))}))

(defn- floor-pixel-bounds
  "The floor's tiles, and the screen (in case the map's past them), in pixels
  at the floor's zoom."
  [^js layer ^js screen]
  (when-let [{:keys [x0 x1 y0 y1]} (.-_kioskFloor layer)]
    ((.-bounds L/default)
     #js [(min (.. screen -min -x) (* 256 x0)) (min (.. screen -min -y) (* 256 y0))]
     #js [(max (.. screen -max -x) (* 256 (inc x1))) (max (.. screen -max -y) (* 256 (inc y1)))])))

;;; Placeholders

;; Most zoom levels coarser a placeholder can be.
(def ^:private placeholder-levels (+ coarse-levels 2))

(defn- ancestor [^js coords d]
  (let [n (js/Math.pow 2 d)]
    (tile-coords (js/Math.floor (/ (.-x coords) n)) (js/Math.floor (/ (.-y coords) n)) (- (.-z coords) d))))

(defn- show-part!
  "Shows the part of the tile at `object-url`, `d` zoom levels coarser, that
  the tile at `coords` covers, in an element just under the tile's image. Not
  as the image's background: Leaflet hides a tile until it loads, and Chrome
  frames an image with no source."
  [^js layer ^js img ^js coords d object-url]
  (when (and object-url (.-parentNode img) (not (.-_kioskPlaceholder img))
             (not (.contains (.-classList img) "leaflet-tile-loaded")))
    (let [n        (js/Math.pow 2 d)
          ^js size (.getTileSize layer)
          div      (js/document.createElement "div")]
      (set! (.. div -style -cssText)
            (str "position:absolute;left:0;top:0;pointer-events:none;"
                 "width:" (.. img -style -width) ";height:" (.. img -style -height) ";"
                 "transform:" (.. img -style -transform) ";"
                 "background-image:url(\"" object-url "\");"
                 "background-size:" (* n (.-x size)) "px " (* n (.-y size)) "px;"
                 "background-position:" (- (* (mod (.-x coords) n) (.-x size))) "px "
                 (- (* (mod (.-y coords) n) (.-y size))) "px"))
      (.insertBefore (.-parentNode img) div img)
      (set! (.-_kioskPlaceholder img) div))))

(defn- remove-placeholder!
  "Removes the tile's placeholder, after `delay-ms` (eg once the tile has faded
  in over it)."
  [^js img delay-ms]
  (when-let [^js div (.-_kioskPlaceholder img)]
    (js-delete img "_kioskPlaceholder")
    (if (pos? delay-ms)
      (js/setTimeout #(.remove div) delay-ms)
      (.remove div))))

(defn- on-tile-load [^js e]
  (let [^js m (.. e -target -_map)]
    ;; Leaflet fades a tile in over 200 ms.
    (remove-placeholder! (.-tile e) (if (and m (.-_fadeAnimated m)) 250 0))))

(defn- on-tile-unload [^js e]
  (remove-placeholder! (.-tile e) 0))

(defn- placeholder
  "For `tile-cache/placeholder-by!`: until an overlay's tile at `coords`
  arrives, its part of the nearest coarser tile in the cache shows under it,
  or failing that of its coarse copy's tile, fetched (with the copy's own
  request, if it's made one)."
  [^js layer ^js coords ^js img]
  (when (and (overlay? layer) (.-_map layer))
    (if-let [[d object-url] (some (fn [d]
                                    (when (<= d (.-z coords))
                                      (some->> (tile-cache/cached (url-at layer (ancestor coords d))) (vector d))))
                                  (range 1 (inc placeholder-levels)))]
      ;; Once Leaflet has put the image in the page
      (js/queueMicrotask #(show-part! layer img coords d object-url))
      (when (<= coarse-levels (.-z coords))
        (.then (tile-cache/object-url (url-at layer (ancestor coords coarse-levels)))
               #(show-part! layer img coords coarse-levels %))))))

;;; The coarse copies

(defn- coarse-zoom
  "`_clampZoom` for a coarse copy. Leaflet picks the zoom of a layer's tiles
  with it (which `maxNativeZoom` caps), from the map's zoom and again from
  zooms it has already clamped, so this is a cap too: `coarse-levels` under
  the map's zoom, or the zoom it's animating to."
  [^js layer zoom]
  (let [^js m (.-_map layer)
        to    (if (.-_animatingZoom m) (.-_animateToZoom m) (.getZoom m))]
    (.call (.. L/default -TileLayer -prototype -_clampZoom)
           layer (min zoom (max 0 (- (js/Math.round to) coarse-levels))))))

(def ^:private CoarseLayer
  ;; The basemap's.
  (.extend (.-TileLayer L/default)
           #js {:options      #js {:kioskTier "coarse"}
                :_clampZoom   (fn [zoom] (this-as this (coarse-zoom this zoom)))
                :_isValidTile (fn [^js coords]
                                (this-as ^js this
                                  (and (let [floor @floor-zoom] (not (and floor (<= (.-z coords) floor))))
                                       (.call (.. L/default -GridLayer -prototype -_isValidTile) this coords))))}))

(def ^:private OverlayCopy
  ;; An overlay's, in a hidden pane: its tiles are the overlay's own
  ;; (`_kioskOverlay`), for `placeholder`.
  (.extend (.-TileLayer L/default)
           #js {:options    #js {:kioskTier "coarse"}
                :getTileUrl (fn [coords] (this-as ^js this (url-at (.-_kioskOverlay this) coords)))
                :_clampZoom (fn [zoom] (this-as this (coarse-zoom this zoom)))}))

(def ^:private copies-pane "kiosk-overlay-copies")

(defn- add-copy! [^js m ^js layer]
  (when (and (overlay? layer) (not (.-_kioskCopy layer)))
    (let [^js copy (OverlayCopy. "" (js/Object.assign #js {} (.-options layer)
                                                      #js {:pane copies-pane :kioskTier "coarse"}))]
      (set! (.-_kioskOverlay copy) layer)
      (set! (.-_kioskCopy layer) copy)
      (.on layer "tileload" on-tile-load)
      (.on layer "tileunload" on-tile-unload)
      ;; The overlay redraws when its URL or WMS parameters change: so must
      ;; its copy.
      (set! (.-redraw layer) (fn []
                               (this-as ^js this
                                 (.call (.. L/default -GridLayer -prototype -redraw) this)
                                 (.redraw copy)
                                 this)))
      (.addLayer m copy))))

(defn- remove-copy! [^js m ^js layer]
  (when-let [copy (.-_kioskCopy layer)]
    (js-delete layer "_kioskCopy")
    (js-delete layer "redraw")
    (.off layer "tileload" on-tile-load)
    (.off layer "tileunload" on-tile-unload)
    (.removeLayer m copy)))

(defonce ^:private attached (js/WeakSet.))

(defn attach!
  "Gives each overlay on the map, now and to come, a coarse copy."
  [^js m]
  (when-not (.has attached m)
    (.add attached m)
    (set! (.. (.createPane m copies-pane) -style -display) "none")
    (.eachLayer m #(add-copy! m %))
    (.on m "layeradd" #(add-copy! m (.-layer ^js %)))
    (.on m "layerremove" #(remove-copy! m (.-layer ^js %)))))

;;; Loading ahead

(defn- padded [^js b dx dy]
  ((.-bounds L/default) (.subtract (.-min b) #js [dx dy]) (.add (.-max b) #js [dx dy])))

(defn- padded-pixel-bounds
  "`_getTiledPixelBounds`: the area the layer loads, by its tier. Notes the
  tiles on screen, for `rank`."
  [^js layer center]
  (let [^js screen (.call tiled-pixel-bounds layer center)]
    (set! (.-_kioskVisible layer) (._pxBoundsToTileRange layer screen))
    (case (tier layer)
      "coarse" (let [^js size (.getSize screen)]
                 (padded screen (* coarse-ring (.-x size)) (* coarse-ring (.-y size))))
      "floor"  (or (floor-pixel-bounds layer screen) screen)
      (let [^js size (.getTileSize layer)]
        (padded screen (* detail-ring (.-x size)) (* detail-ring (.-y size)))))))

(defn- rank
  "The order the tile cache fetches a layer's tiles in, lower first: coarse
  and on screen; the basemap (and overlays) on screen; coarse off screen; the
  basemap (and overlays) off screen; the floor off screen."
  [^js img]
  (let [^js layer   (.-kioskLayer img)
        ^js visible (some-> layer .-_kioskVisible)
        on?         (or (nil? visible) (.contains visible (.-kioskCoords img)))]
    (case (some-> layer tier)
      "floor"  (if on? 0 4)
      "coarse" (if on? 0 2)
      (if on? 1 3))))

(defonce ^:private installed? (atom false))

(defn install! []
  (when (compare-and-set! installed? false true)
    (.mergeOptions (.-GridLayer L/default) #js {:keepBuffer keep-buffer})
    (gobject/set (.. L/default -GridLayer -prototype) "_getTiledPixelBounds"
                 (fn [center] (this-as layer (padded-pixel-bounds layer center))))
    (tile-cache/rank-by! rank)
    (tile-cache/placeholder-by! placeholder)))

;;; Each step's surroundings

;; Keys `keep-surroundings!` has kept tiles under.
(defonce ^:private surroundings (atom #{}))

(defn- loads
  "The URLs of the tiles the layer loads with the map where it is: those on
  screen and its ring."
  [^js layer]
  (let [^js m (.-_map layer)
        z     (.-_tileZoom layer)]
    (when (and m (some? z))
      (let [^js tiles (._pxBoundsToTileRange layer (._getTiledPixelBounds layer (.getCenter m)))]
        (for [x     (range (.. tiles -min -x) (inc (.. tiles -max -x)))
              y     (range (.. tiles -min -y) (inc (.. tiles -max -y)))
              :let  [coords (tile-coords x y z)]
              :when (._isValidTile layer coords)]
          (url-at layer (._wrapCoords layer coords)))))))

(defn keep-surroundings!
  "Fetches what each of the map's tile layers but the floor loads where the
  map is now (eg a story step, `k`) into the tile cache in the background, and
  keeps it there, in place of what was last kept under `k`."
  [^js m k]
  (let [urls (->> (gobject/getValues (.-_layers m))
                  (filter #(and (instance? (.-TileLayer L/default) %) (not= "floor" (tier %))))
                  (mapcat loads)
                  distinct
                  vec)]
    (swap! surroundings conj k)
    (tile-cache/pin! [::surroundings k] urls)
    (tile-cache/prefetch! urls)))

(defn drop-surroundings!
  "Lets the tile cache evict the surroundings kept under any key but `ks`."
  [ks]
  (let [keep? (set ks)]
    (doseq [k @surroundings :when (not (keep? k))]
      (tile-cache/unpin! [::surroundings k]))
    (swap! surroundings #(set (filter keep? %)))))

(re-frame/reg-fx :kiosk.map/drop-surroundings drop-surroundings!)

;;; Tiles on screen

(defn- visible-range
  "The layer's tiles on screen, as an L.Bounds of tile coordinates."
  [^js layer]
  (._pxBoundsToTileRange layer (.call tiled-pixel-bounds layer (.getCenter (.-_map layer)))))

(defn on-screen
  "The layer's tiles on screen at its tile zoom (Leaflet's tile records: `el`,
  `coords`, `loaded` etc)."
  [^js layer]
  (when (and (.-_map layer) (some? (.-_tileZoom layer)))
    (let [^js tiles (visible-range layer)
          z         (.-_tileZoom layer)]
      (filter (fn [^js tile]
                (and (.-current tile) (= z (.. tile -coords -z)) (.contains tiles (.-coords tile))))
              (gobject/getValues (.-_tiles layer))))))

(defn loading-on-screen?
  "Whether the layer is still loading tiles on screen."
  [^js layer]
  (and (.isLoading layer)
       (let [tiles (on-screen layer)]
         (or (empty? tiles) (boolean (some #(not (.-loaded ^js %)) tiles))))))

;;; The layers

(defn- layer-component [Layer]
  (r/adapt-react-class
   (ReactLeafletCore/createLayerComponent
    (fn [props context]
      #js {:instance (Layer. (.-url props) (ReactLeafletCore/withPane
                                            (cond-> #js {:referrerPolicy "origin"}
                                              (.-maxNativeZoom props) (doto (gobject/set "maxNativeZoom" (.-maxNativeZoom props))))
                                            context))
           :context  context})
    (fn [_ _ _]))))

(def ^:private floor-layer (layer-component FloorLayer))
(def ^:private coarse-layer (layer-component CoarseLayer))

(defn backdrop
  "The floor and the coarse copy, under the basemap. Only for tile basemaps."
  []
  (let [{:keys [active-base-layer]} @(re-frame/subscribe [:map/base-layers])
        {:keys [layer_type server_url max_native_zoom]} active-base-layer]
    (when (and (= layer_type :tile) server_url)
      ;; Under the basemap's pane (z-index 0, see `map-views/basemap-layers`)
      [:<>
       [leaflet/pane {:name "kiosk-floor" :style {:z-index -3}}
        ^{:key server_url} [floor-layer {:url server_url :maxNativeZoom max_native_zoom}]]
       [leaflet/pane {:name "kiosk-backdrop" :style {:z-index -2}}
        ^{:key server_url} [coarse-layer {:url server_url :maxNativeZoom max_native_zoom}]]])))
