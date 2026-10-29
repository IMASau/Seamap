;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.flight
  "Flights between map views (story steps, and home): a smooth zoom out and back
  in, zooming out further the further the flight goes (van Wijk and Nuij's
  path, as Leaflet's `flyTo` uses).

  A flight must set off on the frame after the tap and hold 60fps at 4K on
  integrated graphics, so it isn't Leaflet that animates. Leaflet loads and
  decodes tiles for every zoom level it passes through, as it goes, in the DOM.
  Instead, a canvas covers the map and draws the flight from tiles decoded in
  advance (`imas-seamap.tmag-kiosk.bitmaps`):

  - Wherever the map comes to rest, the flights a visitor might take next
    (`prepare!`) are planned from there (`flight-plan`, every tile each one
    draws) and their tiles decoded, and so are the tiles on screen (`rest!`),
    which every flight from there starts with. A tap finds its flight planned
    and decoded, and it sets off on the next frame. Otherwise it sets off
    anyway, drawing missing tiles from coarser ones while they decode.
  - Only the basemap is drawn: one layer, or two while crossfading to the next
    view's basemap. The overlays on screen when the flight starts are drawn too,
    from their tiles decoded at rest, fading out as it sets off.
  - Leaflet stays put under the canvas until the flight lands, then jumps to
    the new view and gets the new view's layers (`on-land`). The canvas is
    removed once Leaflet's basemap is showing. The overlays load from there,
    each hidden until all its tiles are in, then faded in whole.

  Nothing is added to the page while a flight animates, and nothing is decoded
  unless the flight set off without its tiles. Only `:tile` basemaps can be
  drawn; for anything else the map jumps without a flight.

  `prefetch!` fetches flights' tiles into the tile cache without decoding them,
  so every flight of every story is a fetch away from ready."
  (:require ["leaflet" :as L]
            [goog.object :as gobject]
            [imas-seamap.map.utils :refer [map->bounds]]
            [imas-seamap.tmag-kiosk.bitmaps :as bitmaps]
            [imas-seamap.tmag-kiosk.tile-cache :as tile-cache]
            [re-frame.core :as re-frame]))

;;; Tuning

;; How far a flight zooms out for the distance it covers (van Wijk and Nuij's
;; rho; Leaflet's is 1.42). Higher zooms out further.
(def rho 1.42)
;; Seconds per unit of the path's length (Leaflet's `flyTo` takes 0.8), and the
;; limits on a flight's duration.
(def seconds-per-unit 0.5)
(def min-duration 0.8)
(def max-duration 2.5)
;; Fraction of the flight spent accelerating from rest, and decelerating to it.
(def ramp 0.3)
;; Decodes allowed at once while a flight animates, if it set off before all
;; its tiles were decoded (a tap just after landing, or a flight redirected
;; mid-air); the rest of the time, nothing decodes while a flight animates.
;; Missing tiles are drawn from coarser ones meanwhile.
(def max-decodes-flying 2)
;; How long the map must stay put, after the visitor moves it, before the
;; flights from there are prepared again.
(def rest-delay-ms 300)
;; Longest the map's layers are waited for, at rest, before the tiles on screen
;; are decoded anyway.
(def rest-timeout-ms 10000)
;; Fraction of the flight over which the overlays it started with fade out.
(def overlay-fade-until 0.3)
;; Fractions of the flight between which one basemap crossfades to the next.
(def crossfade-from 0.35)
(def crossfade-to 0.65)
;; Zoom levels coarser the basemap's tiles are while the flight is under way
;; (from `detail-from` to `detail-to` of the way through) than at rest. Each
;; level coarser needs about a quarter of the tiles: at full detail, the
;; flights from a zoom-18 step need over 2 GB decoded.
(def detail-drop 1)
(def detail-from 0.15)
(def detail-to 0.85)
;; A missing tile is drawn from a decoded tile up to this many zoom levels
;; coarser, if there is one.
(def max-parent-levels 4)
;; Rate the path is sampled at when planning its tiles.
(def plan-hz 120)
;; Decoded tiles of the flights from here uploaded to the GPU per frame, at
;; rest (see `upload-gradually!`).
(def uploads-per-frame 8)
;; How long an overlay takes to fade in, once loaded after landing.
(def overlay-fade-in-ms 300)

;;; Views: {:center L.Point, in pixels at zoom 0 :zoom}

(defn- latlng [x] ((.-latLng L/default) (clj->js x)))

(defn- ->view [^js m center zoom]
  {:center (.project m (latlng center) 0) :zoom zoom})

(defn- target-view
  "The view for `target`: {:zoom :center} or {:bounds} (fitted as Leaflet's
  `fitBounds` would)."
  [^js m {:keys [zoom center bounds]}]
  (if bounds
    (let [fitted (._getBoundsCenterZoom m ((.-latLngBounds L/default) (clj->js (map->bounds bounds))))]
      (->view m (.-center fitted) (.-zoom fitted)))
    (->view m center zoom)))

(defn- path
  "van Wijk and Nuij's smooth zoom and pan from view `a` to `b` (Leaflet's
  `flyTo`): {:length, :at fn from 0 to length to the view}."
  [^js size a b]
  (let [wmax (max (.-x size) (.-y size))
        w0   (/ wmax (js/Math.pow 2 (:zoom a)))
        w1   (/ wmax (js/Math.pow 2 (:zoom b)))
        c0   (:center a)
        c1   (:center b)
        u1   (.distanceTo c0 c1)]
    (if (< (* u1 (js/Math.pow 2 (max (:zoom a) (:zoom b)))) 1)
      ;; Under a pixel apart: zoom only.
      (let [length (/ (js/Math.abs (js/Math.log (/ w1 w0))) rho)]
        {:length length
         :at     (fn [s] {:center c0
                          :zoom   (if (zero? length)
                                    (:zoom b)
                                    (+ (:zoom a) (* (- (:zoom b) (:zoom a)) (/ s length))))})})
      (let [rho2 (* rho rho)
            r    (fn [i]
                   (let [b  (/ (+ (- (* w1 w1) (* w0 w0)) (* (if (zero? i) 1 -1) rho2 rho2 u1 u1))
                               (* 2 (if (zero? i) w0 w1) rho2 u1))
                         sq (- (js/Math.sqrt (+ (* b b) 1)) b)]
                     (if (< sq 1e-9) -18 (js/Math.log sq))))
            r0   (r 0)
            w    (fn [s] (* w0 (/ (js/Math.cosh r0) (js/Math.cosh (+ r0 (* rho s))))))
            u    (fn [s] (/ (* w0 (- (* (js/Math.cosh r0) (js/Math.tanh (+ r0 (* rho s)))) (js/Math.sinh r0)))
                            rho2))]
        {:length (/ (- (r 1) r0) rho)
         :at     (fn [s] {:center (.add c0 (.multiplyBy (.subtract c1 c0) (/ (u s) u1)))
                          :zoom   (js/Math.log2 (/ wmax (w s)))})}))))

(defn- ease
  "Fraction of the way along at `t` (fraction of the duration): constant
  acceleration, cruise, constant deceleration."
  [t]
  (let [v (/ 1 (- 1 ramp))]
    (cond
      (<= t 0)          0
      (>= t 1)          1
      (< t ramp)        (/ (* v t t) (* 2 ramp))
      (< t (- 1 ramp))  (* v (- t (/ ramp 2)))
      :else             (- 1 (/ (* v (- 1 t) (- 1 t)) (* 2 ramp))))))

(defn- smoothstep [x]
  (let [x (-> x (max 0) (min 1))]
    (* x x (- 3 (* 2 x)))))

;;; Basemap tiles

(defn- tile-source
  "A `:tile` catalogue base layer's tiles, with their URLs exactly as Leaflet
  requests them (so they're shared with the tile cache)."
  [{:keys [server_url layer_type]}]
  (when (and server_url (= layer_type :tile))
    (let [^js layer ((.-tileLayer L/default) server_url)
          urls      (js/Map.)]
      {:key      server_url
       :max-zoom (.. layer -options -maxZoom)
       :url      (fn [z x y]
                   (let [k (str z "/" x "/" y)]
                     (or (.get urls k)
                         (do (set! (.-_tileZoom layer) z) ; Leaflet's URL zoom comes from here
                             (let [url (.getTileUrl layer #js {:x x :y y :z z})]
                               (.set urls k url)
                               url)))))})))

(defn- tile-zoom [{:keys [max-zoom]} zoom]
  (-> (js/Math.round zoom) (max 0) (min max-zoom)))

(defn- layout
  "Where zoom `tz`'s tiles go on screen for `view`: [tile size, x and y of the
  tile grid's origin]."
  [^js size {:keys [^js center zoom]} tz]
  (let [k (js/Math.pow 2 zoom)]
    [(* 256 (js/Math.pow 2 (- zoom tz)))
     (- (/ (.-x size) 2) (* (.-x center) k))
     (- (/ (.-y size) 2) (* (.-y center) k))]))

(defn- each-tile
  "Calls (f z x y left top width height) for each of the source's tiles
  covering `view`, x wrapped round the world, at whole-pixel edges."
  [^js size source view f]
  (let [tz           (tile-zoom source (- (:zoom view) (:drop view 0)))
        [ts ox oy]   (layout size view tz)
        n            (js/Math.pow 2 tz)
        i0           (js/Math.floor (/ (- ox) ts))
        i1           (js/Math.floor (/ (- (.-x size) ox) ts))
        j0           (max 0 (js/Math.floor (/ (- oy) ts)))
        j1           (min (dec n) (js/Math.floor (/ (- (.-y size) oy) ts)))]
    (loop [j j0]
      (when (<= j j1)
        (let [y0 (js/Math.round (+ oy (* j ts)))
              y1 (js/Math.round (+ oy (* (inc j) ts)))]
          (loop [i i0]
            (when (<= i i1)
              (let [x0 (js/Math.round (+ ox (* i ts)))
                    x1 (js/Math.round (+ ox (* (inc i) ts)))]
                (f tz (mod i n) j x0 y0 (- x1 x0) (- y1 y0)))
              (recur (inc i)))))
        (recur (inc j))))))

;;; Planning

(defn- view-at
  "The view `t` (fraction of the duration) of the way through the flight."
  [{:keys [path to]} t]
  (if (>= t 1)
    to
    (assoc ((:at path) (* (:length path) (ease t)))
           :drop (if (< detail-from t detail-to) detail-drop 0))))

(defn- flight-plan
  "Everything about a flight from view `from` to `to` that can be worked out in
  advance, including every tile it draws (`:urls`). Nil if it can't be flown
  (a basemap that isn't tiles, or no distance to fly)."
  [^js size from to from-basemap to-basemap]
  (let [from-source (tile-source from-basemap)
        to-source   (tile-source to-basemap)
        {:keys [length] :as p} (path size from to)]
    (when (and from-source to-source (> length 0.01))
      (let [duration (-> length (* seconds-per-unit) (max min-duration) (min max-duration))
            same?    (= (:key from-source) (:key to-source))
            plan     {:size        size
                      :from        from
                      :to          to
                      :path        p
                      :duration    duration
                      :from-source from-source
                      :to-source   (if same? from-source to-source)
                      :same?       same?}
            urls     (js/Set.)
            add      (fn [source] (fn [z x y] (.add urls ((:url source) z x y))))
            samples  (js/Math.ceil (* duration plan-hz))]
        (dotimes [k (inc samples)]
          (let [t    (/ k samples)
                view (view-at plan t)]
            (when (or same? (<= t crossfade-to))
              (each-tile size from-source view (add from-source)))
            (when (and (not same?) (>= t crossfade-from))
              (each-tile size to-source view (add to-source)))))
        (assoc plan :urls (vec (es6-iterator-seq (.values urls))))))))

;;; Drawing

(defn- draw-tile!
  "Draws a tile, or failing that part of a coarser one, if one is decoded."
  [^js ctx source z x y dx dy dw dh]
  (loop [k 0]
    (when (<= k (min max-parent-levels z))
      (let [d      (js/Math.pow 2 k)
            bitmap (bitmaps/bitmap ((:url source) (- z k) (js/Math.floor (/ x d)) (js/Math.floor (/ y d))))]
        (if bitmap
          (let [sub (/ 256 d)]
            (.drawImage ctx bitmap (* sub (mod x d)) (* sub (mod y d)) sub sub dx dy dw dh))
          (recur (inc k)))))))

(defn- draw-overlays!
  "Draws the overlays a flight started with, from their tiles decoded at rest."
  [^js ctx ^js size overlays view alpha]
  (doseq [{:keys [opacity tiles]} overlays]
    (set! (.-globalAlpha ctx) (* alpha opacity))
    (doseq [{:keys [z x y url]} tiles]
      (when-let [bitmap (bitmaps/bitmap url)]
        (let [[ts ox oy] (layout size view z)
              x0         (js/Math.round (+ ox (* x ts)))
              y0         (js/Math.round (+ oy (* y ts)))]
          (.drawImage ctx bitmap x0 y0 (- (js/Math.round (+ ox (* (inc x) ts))) x0)
                      (- (js/Math.round (+ oy (* (inc y) ts))) y0)))))))

(defn- draw!
  "Draws the flight `t` (fraction of its duration) of the way through."
  [^js ctx {:keys [^js size from-source to-source same? overlays background] :as plan} t]
  (let [view (view-at plan t)
        mix  (if same? 0 (smoothstep (/ (- t crossfade-from) (- crossfade-to crossfade-from))))
        fade (- 1 (smoothstep (/ t overlay-fade-until)))]
    (set! (.-globalAlpha ctx) 1)
    (set! (.-fillStyle ctx) background)
    (.fillRect ctx 0 0 (.-x size) (.-y size))
    (when (< mix 1)
      (each-tile size from-source view (fn [z x y dx dy dw dh]
                                         (draw-tile! ctx from-source z x y dx dy dw dh))))
    (when (pos? mix)
      (set! (.-globalAlpha ctx) mix)
      (each-tile size to-source view (fn [z x y dx dy dw dh]
                                       (draw-tile! ctx to-source z x y dx dy dw dh))))
    (when (and (pos? fade) (seq overlays))
      (draw-overlays! ctx size overlays view fade))
    (set! (.-globalAlpha ctx) 1)
    view))

;;; The map's tile layers

(defn- grid-layers [^js m]
  (filter #(instance? (.-GridLayer L/default) %) (gobject/getValues (.-_layers m))))

(defn- overlay? [^js layer]
  (and (instance? (.-GridLayer L/default) layer)
       (contains? @(re-frame/subscribe [:map.layers/lookup])
                  [(.-_url layer) (.. layer -options -layers)])))

(defn- tiles-on-screen
  "The layer's tiles on screen, with the URLs they were loaded from (see
  `imas-seamap.tmag-kiosk.tile-cache`): [{:z :x :y :url :img}]."
  [^js layer]
  (for [^js tile (gobject/getValues (.-_tiles layer))
        :let [^js img (.-el tile)]
        :when (and (.-current tile) (.-loaded tile) (.-kioskUrl img))]
    {:z   (.. tile -coords -z)
     :x   (.. tile -coords -x)
     :y   (.. tile -coords -y)
     :url (.-kioskUrl img)
     :img img}))

(defn- overlays-now
  "The overlays on screen, bottom first: [{:opacity :tiles}]."
  [^js m]
  (let [z-index (fn [^js layer]
                  (let [z (some-> (.getPane m (.. layer -options -pane)) .-style .-zIndex js/parseInt)]
                    (if (and z (not (js/isNaN z))) z 0)))]
    (->> (grid-layers m)
         (filter overlay?)
         (sort-by z-index)
         (mapv (fn [^js layer] {:opacity (or (.. layer -options -opacity) 1)
                                :tiles   (vec (tiles-on-screen layer))})))))

;;; Flying

;; {:id :map :plan :on-land :phase (:flying :landing) :view :t :frames #js
;;  [frame intervals] ...}
(defonce ^:private flight (atom nil))
;; The last flight's frame intervals and timings, for checking smoothness.
(defonce ^:private last-flight (atom nil))

(defonce ^:private canvas* (atom nil))

(defn- canvas [^js m]
  (let [container (.getContainer m)
        ^js c     (or @canvas* (reset! canvas* (doto (js/document.createElement "canvas")
                                                 (-> .-className (set! "kiosk-flight")))))]
    (when-not (identical? (.-parentNode c) container)
      (js/Object.assign (.-style c) #js {:position "absolute" :left 0 :top 0 :zIndex 650
                                         :display "none" :transition "opacity 0.2s"})
      (.appendChild container c))
    c))

(defn- wait [ms] (js/Promise. (fn [resolve _] (js/setTimeout resolve ms))))

;; A frame at 60Hz.
(def ^:private frame-ms (/ 1000 60))

(defn- map-pane [^js m] (.getPane m "mapPane"))

(declare prepare-now!)

(defn- finish!
  "Forgets the flight `id` (if it's still the current one), shows Leaflet again
  and removes the canvas, then prepares the flights from where the map is."
  [id]
  (when-let [{^js m :map :keys [plan finished]} (and (= id (:id @flight)) @flight)]
    (reset! flight nil)
    (some-> finished .-resolve (.call nil))
    (js/cancelAnimationFrame (:raf plan))
    (set! (.. (map-pane m) -style -visibility) "")
    (let [^js c (canvas m)]
      (set! (.. c -style -display) "none")
      (set! (.. c -style -opacity) 1))
    (bitmaps/limit! bitmaps/max-decodes)
    (prepare-now!)))

(defn- decoded
  "A Promise that resolves once the layer's current tiles are decoded (loaded
  isn't decoded: a tile not yet decoded pops in later)."
  [^js layer]
  (js/Promise.all
   (clj->js (for [^js tile (gobject/getValues (.-_tiles layer)) :when (.-current tile)]
              (.catch (.decode (.-el tile)) (constantly nil))))))

(defn- polled
  "A Promise of `(ready)` once it's truthy, checked every 50 ms, or of nil after
  `timeout-ms`."
  [ready timeout-ms]
  (js/Promise.race
   #js [(.then (wait timeout-ms) (constantly nil))
        (js/Promise. (fn [resolve _]
                       ((fn poll []
                          (if-let [x (ready)]
                            (resolve x)
                            (js/setTimeout poll 50))))))]))

(defn- basemap-shown
  "A Promise that resolves once Leaflet shows `source`'s tiles at `zoom` (or
  after a while, if it doesn't)."
  [^js m source zoom]
  (.then (polled (fn []
                   (some (fn [^js layer]
                           (and (= (.-_url layer) (:key source))
                                (= (.-_tileZoom layer) (tile-zoom source zoom))
                                (not (.isLoading layer))
                                layer))
                         (grid-layers m)))
                 3000)
         #(when % (decoded %))))

;;; Overlays after landing: each is hidden until all its tiles are in, then
;;; faded in whole, rather than appearing tile by tile.

(defn- own-pane
  "The layer's pane, if it has one to itself (Seamap gives each layer its own)."
  [^js m ^js layer]
  (let [pane (.. layer -options -pane)]
    (when-not (#{"tilePane" "overlayPane"} pane)
      (.getPane m pane))))

(defn- fade-in-when-loaded!
  "Hides the overlay, then fades it in once it has loaded and `after` (a
  Promise) has resolved."
  [^js m ^js layer after]
  (when-let [^js pane (own-pane m layer)]
    (set! (.. pane -style -transition) "")
    (set! (.. pane -style -opacity) 0)
    (-> (js/Promise.all #js [(.then (polled #(not (.isLoading layer)) 3000) #(decoded layer)) after])
        (.then (fn []
                 (when (.hasLayer m layer) ; not removed on landing
                   (swap! last-flight update :overlays-shown-after (fnil conj [])
                          (js/Math.round (- (js/performance.now) (:landed @last-flight)))))
                 (set! (.. pane -style -transition) (str "opacity " overlay-fade-in-ms "ms"))
                 (set! (.. pane -style -opacity) 1))))))

(defn- land!
  "Puts Leaflet at the flight's destination with its layers (`on-land`), then
  fades the canvas out once Leaflet's basemap is showing."
  [id]
  (let [{^js m :map :keys [plan on-land]} @flight
        {:keys [to to-source]} plan]
    ;; Overlays already on the map that the destination keeps reload there:
    ;; they fade in too.
    (let [resolve  (atom nil)
          finished (js/Promise. (fn [res _] (reset! resolve res)))]
      (set! (.-resolve finished) @resolve)
      (swap! flight assoc :phase :landing :view nil :finished finished)
      (doseq [layer (grid-layers m) :when (overlay? layer)]
        (fade-in-when-loaded! m layer finished)))
    (reset! last-flight {:frames    (:frames @flight)
                         :draw-ms   (:draw-ms @flight)
                         :ts        (:ts @flight)
                         :duration  (:duration plan)
                         :tiles     (count (:urls plan))
                         :prepared? (:prepared? @flight)
                         :missing   (:missing @flight)
                         ;; From the flight being asked for to its first frame
                         :set-off   (- (:first-frame @flight) (:requested @flight))
                         :requested   (:requested @flight)
                         :first-frame (:first-frame @flight)
                         :landed    (js/performance.now)})
    (.setView m (.unproject m (:center to) 0) (:zoom to) #js {:animate false})
    (re-frame/dispatch on-land)
    (bitmaps/limit! bitmaps/max-decodes)
    (-> (basemap-shown m to-source (:zoom to))
        (.then (fn []
                 (when (= id (:id @flight))
                   (swap! last-flight assoc :revealed-after (- (js/performance.now) (:landed @last-flight)))
                   (set! (.. (map-pane m) -style -visibility) "")
                   (set! (.. (canvas m) -style -opacity) 0)
                   (wait 250))))
        (.then #(finish! id)))))

(defn- animate!
  "Animates the flight from the next frame. That frame swaps the map for the
  canvas and is already moving: it's drawn a frame into the flight, as the tap
  came at least a frame before it."
  [id]
  (let [{^js m :map :keys [plan]} @flight
        ^js c   (canvas m)
        ctx     (.getContext c "2d" #js {:alpha false})
        ms      (* 1000 (:duration plan))
        frames  #js []
        draw-ms #js []
        ts      #js []
        start   (atom nil)
        prev    (atom nil)
        frame   (fn frame [now]
                  (when (= id (:id @flight))
                    (if @start
                      (.push frames (- now @prev))
                      (do (reset! start (- now frame-ms))
                          (swap! flight assoc :first-frame (js/performance.now))
                          (js/Object.assign (.-style c) #js {:display "block" :opacity 1})
                          (set! (.. (map-pane m) -style -visibility) "hidden")))
                    (reset! prev now)
                    (let [t    (min 1 (/ (- now @start) ms))
                          t0   (js/performance.now)
                          view (draw! ctx plan t)]
                      (.push draw-ms (- (js/performance.now) t0))
                      (.push ts t)
                      (swap! flight assoc :view view :t t)
                      (if (< t 1)
                        (swap! flight assoc-in [:plan :raf] (js/requestAnimationFrame frame))
                        (land! id)))))]
    (swap! flight assoc :phase :flying :frames frames :draw-ms draw-ms :ts ts)
    ;; Drawn in CSS pixels, at the device's resolution, as Leaflet's tiles are.
    (let [size (:size plan)
          dpr  js/devicePixelRatio]
      (when-not (and (= (.-width c) (js/Math.round (* dpr (.-x size))))
                     (= (.-height c) (js/Math.round (* dpr (.-y size)))))
        (set! (.-width c) (js/Math.round (* dpr (.-x size))))
        (set! (.-height c) (js/Math.round (* dpr (.-y size))))
        (js/Object.assign (.-style c) #js {:width  (str (.-x size) "px")
                                           :height (str (.-y size) "px")}))
      (.setTransform ctx dpr 0 0 dpr 0 0))
    (swap! flight assoc-in [:plan :raf] (js/requestAnimationFrame frame))))

(defn cancel!
  "Stops any flight where it is, without landing: Leaflet shows its own view."
  []
  (when-let [{:keys [id]} @flight]
    (finish! id)))

;;; Preparing. Wherever the map comes to rest, the flights a visitor might take
;;; from there are planned, and their tiles decoded, so a tap finds its flight
;;; ready. So are the tiles on screen: every flight from there starts with them.

;; What to keep ready, from `prepare!`: {:map :from :flights}.
(defonce ^:private standing (atom nil))
;; plan key -> plan, for the flights kept ready
(defonce ^:private plans (js/Map.))
;; The plan keys of the last preparation (see `prepare-now!`).
(defonce ^:private held (atom #{}))
;; Bumped whenever the map moves or a flight sets off, abandoning preparation
;; under way.
(defonce ^:private generation (atom 0))
;; See `suspend!`.
(defonce ^:private suspended? (atom false))
;; The map's background colour, drawn behind the tiles.
(defonce ^:private background (atom nil))

(defn- current-view [^js m]
  (->view m (.getCenter m) (.getZoom m)))

(defn- view-key [{:keys [^js center zoom]}]
  (let [k (js/Math.pow 2 zoom)]
    (str zoom "@" (js/Math.round (* k (.-x center))) "," (js/Math.round (* k (.-y center))))))

(defn- plan-key
  "Identifies a flight, and the set of decoded tiles that holds its tiles."
  [^js size from to from-basemap to-basemap]
  (str (.-x size) "x" (.-y size) " " (view-key from) " " (:server_url from-basemap)
       " > " (view-key to) " " (:server_url to-basemap)))

(defn- in-turn
  "Calls (f x) for each of `xs`, each in a task of its own (planning a flight
  takes a while), while `gen` is still current."
  [gen xs f]
  (reduce (fn [p x]
            (.then p #(when (= gen @generation)
                        (.then (wait 0) (fn [] (when (= gen @generation) (f x)))))))
          (js/Promise.resolve nil)
          xs))

(defn- upload!
  "Has the GPU take the decoded tiles at `urls` now, rather than in a flight's
  first frame: that frame draws every tile on screen, and uploading them all
  at once can take longer than a frame. A canvas of its own, drawn to and read
  from once, so its drawing is done at once (and it's never moved off the
  GPU for being read)."
  [urls]
  (let [^js c   (doto (js/document.createElement "canvas") (-> .-width (set! 256)) (-> .-height (set! 256)))
        ^js ctx (.getContext c "2d")]
    (doseq [url urls]
      (when-let [bitmap (bitmaps/bitmap url)]
        (.drawImage ctx bitmap 0 0)))
    (.getImageData ctx 0 0 1 1)))

(defn- upload-gradually!
  "`upload!`s the decoded tiles at `urls`, `uploads-per-frame` a frame, while
  `gen` is current and no flight is under way. A Promise of when it's done or
  given up. A flight's tiles uploaded mid-flight held up its frames: on the
  table, flights missed frames early on (as the zoom-out reached tiles that
  weren't on screen at rest), with the GPU blocked presenting."
  [gen urls]
  (let [urls (vec urls)
        n    (count urls)]
    (js/Promise.
     (fn [resolve _]
       ((fn step [i]
          (if (and (< i n) (= gen @generation) (nil? @flight))
            (do (upload! (subvec urls i (min n (+ i uploads-per-frame))))
                (js/requestAnimationFrame #(step (+ i uploads-per-frame))))
            (resolve nil)))
        0)))))

(defn- rest!
  "Once the map's layers have loaded, decodes the tiles on screen (as the set
  :rest), ahead of everything else, and uploads them to the GPU: every flight
  from here starts with them."
  [^js m gen]
  (-> (polled #(not-any? (fn [^js layer] (.isLoading layer)) (grid-layers m)) rest-timeout-ms)
      (.then (fn []
               (when (and (= gen @generation) (nil? @flight))
                 (let [tiles (mapcat tiles-on-screen (grid-layers m))
                       urls  (mapv :url tiles)]
                   (doseq [{:keys [url img]} tiles]
                     (bitmaps/image! url img))
                   ;; The last flight's tiles, held until now in case it was
                   ;; one back here.
                   (bitmaps/release! :flight)
                   (.then (bitmaps/retain! :rest urls true)
                          #(when (and (= gen @generation) (nil? @flight))
                             (upload! urls)))))))))

(defn- prepare-now! []
  (when-let [{^js m :map :keys [from flights]} @standing]
    (let [gen       (swap! generation inc)
          size      (.getSize m)
          ;; Flights from here leave from wherever the visitor has left the map.
          here      (if @suspended? (target-view m from) (current-view m))
          spec      (fn [from-view from-basemap to]
                      (let [to-view (target-view m to)]
                        {:key  (plan-key size from-view to-view from-basemap (:basemap to))
                         :args [size from-view to-view from-basemap (:basemap to)]}))
          specs     (for [to flights] (spec here (:basemap from) to))
          wanted    (set (map :key specs))]
      (reset! background (.-backgroundColor (js/getComputedStyle (.getContainer m))))
      ;; Other flights' tiles are freed once these flights are held, so those
      ;; they share aren't decoded again; meanwhile, only the last
      ;; preparation's are kept (preparation can be interrupted repeatedly).
      (bitmaps/retain-only! (concat wanted @held [:rest :flight]))
      (reset! held wanted)
      (when-not @suspended?
        (rest! m gen))
      (-> (in-turn gen specs
                   (fn [{:keys [key args]}]
                     (when-let [plan (or (.get plans key) (apply flight-plan args))]
                       (.set plans key plan)
                       (.then (bitmaps/retain! key (:urls plan) false)
                              #(upload-gradually! gen (:urls plan))))))
          (.then #(when (= gen @generation)
                    (bitmaps/retain-only! (concat wanted [:rest :flight]))
                    (doseq [k (vec (es6-iterator-seq (.keys plans))) :when (not (wanted k))]
                      (.delete plans k))))))))

(defn- watch!
  "Watches the map: overlays added while a flight lands fade in once loaded,
  and wherever the visitor leaves the map, the flights from there are
  prepared again."
  [^js m]
  (when-not (.-kioskWatched m)
    (set! (.-kioskWatched m) true)
    (let [timer   (atom nil)
          at-rest #(and (nil? @flight) (not @suspended?))]
      (.on m "layeradd" (fn [^js e]
                          (when-let [{:keys [finished]} (and (= :landing (:phase @flight)) @flight)]
                            (when (overlay? (.-layer e))
                              (fade-in-when-loaded! m (.-layer e) finished)))))
      (.on m "movestart" #(when (at-rest) (swap! generation inc)))
      (.on m "moveend" #(when (at-rest)
                          (js/clearTimeout @timer)
                          (reset! timer (js/setTimeout (fn [] (when (at-rest) (prepare-now!)))
                                                       rest-delay-ms)))))))

;;; Entry points

(defn fly!
  "Flies `leaflet-map` to `to` ({:zoom :center} or {:bounds}, plus the
  catalogue base layer it shows, `:basemap`), from its current view and
  `from-basemap`, setting off on the next frame. Once there, dispatches
  `on-land` (which applies the destination's layers). Interrupting a flight
  starts the new one from wherever the canvas is."
  [{:keys [^js leaflet-map to from-basemap on-land]}]
  (let [m         leaflet-map
        size      (.getSize m)
        current   @flight
        mid-air?  (and (:view current) (= :flying (:phase current)))
        from      (if mid-air?
                    (:view current)
                    (current-view m))
        ;; Mid-air, carry on from the basemap that was showing.
        from-base (if (and mid-air? (> (:t current) (/ (+ crossfade-from crossfade-to) 2)))
                    (:to-basemap current)
                    (if mid-air? (:from-basemap current) from-basemap))
        to-view   (target-view m to)
        prepared  (.get plans (plan-key size from to-view from-base (:basemap to)))
        plan      (or prepared (flight-plan size from to-view from-base (:basemap to)))
        overlays  (when (and plan (not mid-air?)) (overlays-now m))
        id        (random-uuid)]
    (if mid-air?
      ;; Keep the canvas up, and the frame it shows, but stop animating.
      (js/cancelAnimationFrame (get-in current [:plan :raf]))
      (cancel!))
    (swap! generation inc)
    (if-not plan
      (do (when mid-air? (finish! (:id current)))
          (.setView m (.unproject m (:center to-view) 0) (:zoom to-view) #js {:animate false})
          (re-frame/dispatch on-land))
      (do (watch! m)
          (reset! flight {:id           id
                          :map          m
                          :view         (when mid-air? from)
                          :t            0
                          :requested    (js/performance.now)
                          :prepared?    (some? prepared)
                          :on-land      on-land
                          :from-basemap from-base
                          :to-basemap   (:basemap to)
                          :plan         (assoc plan
                                               :background (or @background
                                                               (.-backgroundColor (js/getComputedStyle (.getContainer m))))
                                               :overlays overlays)})
          ;; Held for the flight; decoded already, unless the tap came before
          ;; they could be (then the most urgent first, a few at a time).
          (bitmaps/retain! :flight (into (:urls plan) (for [{:keys [tiles]} overlays, {:keys [url]} tiles] url)) true)
          (let [missing (bitmaps/missing (:urls plan))]
            (swap! flight assoc :missing missing)
            (bitmaps/limit! (if (pos? missing) max-decodes-flying 0)))
          (animate! id)))))

(defn prepare!
  "Keeps flights ready to fly, from the map's place at rest, `from` (a target
  as for `fly!`), to each of `flights` (targets). They leave from wherever the
  visitor leaves the map. They're planned and their tiles decoded in the
  background, now and whenever the map comes to rest; tiles of any other
  flight are freed. `moving?` means the map is about to move to `from`: they're
  prepared once it gets there, not from where it is now."
  [{:keys [leaflet-map moving?] :as args}]
  (watch! leaflet-map)
  (reset! standing (assoc args :map leaflet-map))
  (when-not (or @flight moving?)
    (prepare-now!)))

(defn prefetch!
  "Fetches the tiles of each of `flights` ([{:from :to}], targets as for `fly!`)
  into the tile cache, in the background and in order, without decoding them."
  [{:keys [^js leaflet-map flights]}]
  (let [m    leaflet-map
        size (.getSize m)]
    (reduce (fn [p {:keys [from to]}]
              (.then p #(.then (wait 0)
                               (fn []
                                 (some-> (flight-plan size (target-view m from) (target-view m to)
                                                      (:basemap from) (:basemap to))
                                         :urls
                                         tile-cache/prefetch!)))))
            (js/Promise.resolve nil)
            flights)))

(defn suspend!
  "While `on?`, the map is moved for some other reason than a visitor (the
  attract screen's tour, see `imas-seamap.tmag-kiosk.events/tour-next`): where
  it rests is ignored, and flights are prepared from their `:from`."
  [on?]
  (reset! suspended? on?))

(defn visit!
  "For the attract screen's tour: puts the map at `to` at once, loads `images`
  (photos, so they're cached), and dispatches `on-loaded` once the map's
  layers have loaded (or after `rest-timeout-ms`)."
  [{:keys [^js leaflet-map to images on-loaded]}]
  (let [m                     leaflet-map
        {:keys [center zoom]} (target-view m to)]
    (.setView m (.unproject m center 0) zoom #js {:animate false})
    (doseq [url images]
      (set! (.-src (js/Image.)) url))
    ;; The step's layers are added with the next render.
    (-> (wait 500)
        (.then #(polled (fn [] (not-any? (fn [^js layer] (.isLoading layer)) (grid-layers m)))
                        rest-timeout-ms))
        (.then #(re-frame/dispatch on-loaded)))))

(defn ^:export stats
  "The last flight's frames, for checking smoothness from the console:
  `imas_seamap.tmag_kiosk.flight.stats()`."
  []
  (when-let [{:keys [frames draw-ms ts] :as f} @last-flight]
    (let [sorted (sort frames)]
      (clj->js (assoc (dissoc f :frames :draw-ms :ts)
                      ;; [t, ms] of each frame that took over half the budget to draw
                      :drawSlow   (keep-indexed #(when (> %2 8) [(.toFixed (aget ts %1) 3) (js/Math.round %2)]) draw-ms)
                      ;; script time drawing each frame (not the GPU's)
                      :drawMax    (apply max draw-ms)
                      :drawMedian (nth (sort draw-ms) (quot (count draw-ms) 2))
                      :frames  (count frames)
                      :max     (last sorted)
                      ;; At 60Hz frames are 16.7 ms apart, give or take vsync
                      ;; jitter; a missed vsync shows as about 33 ms.
                      :missed  (count (filter #(> % 25) frames))
                      :slow    (keep-indexed #(when (> %2 25) [%1 (js/Math.round %2)]) frames)
                      :median  (nth sorted (quot (count sorted) 2))
                      :bitmaps (bitmaps/stats))))))

(re-frame/reg-fx :kiosk/fly fly!)
(re-frame/reg-fx :kiosk/prepare-flights prepare!)
(re-frame/reg-fx :kiosk/prefetch-flights prefetch!)
(re-frame/reg-fx :kiosk/suspend-flights suspend!)
(re-frame/reg-fx :kiosk/tour-visit visit!)
(re-frame/reg-fx :kiosk/cancel-flight (fn [_] (cancel!)))
