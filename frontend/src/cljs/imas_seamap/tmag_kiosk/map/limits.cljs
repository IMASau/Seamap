;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.map.limits
  "How far the visitor can take the map: an extent and a zoom range. A drag or
  pinch past them meets resistance, and the map springs back inside on
  release. From the touch-sandbox project.

  The kiosk has one set of limits, for every view (home, the story steps, the
  attract screen's tour), given to `attach!` (see
  `imas-seamap.tmag-kiosk.events/kiosk-limits`): {:bounds {:west :south :east
  :north} :min-zoom :max-zoom}, each optional. With bounds and no `:min-zoom`,
  the map zooms out until the extent fits the screen. `apply!` sets them just
  before the map is moved to a view, taking the view in, so they never move the
  map off it: for a view outside them, the extent grows to hold it, and the
  zoom range to include its zoom, until the map next moves.

  Where the extent is smaller than the screen at some zoom, it's widened to the
  screen there, about its own centre (`bounds-at`), and again at every zoom.
  Leaflet would otherwise hold the extent's centre at the screen's centre, so
  every drag sprang back.

  Leaflet resists drags past `maxBounds` (`maxBoundsViscosity`), but not
  pinches: a pinch went freely past the zoom range and the extent, and jumped
  back on release. `install!` replaces `L.Map.TouchZoom`'s `_onTouchMove` with
  Leaflet's own, except that past the zoom range the zoom is held back, and
  then past the extent the centre is, as a drag is. It patches the prototype
  for the whole page, so only the kiosk installs it. `_onTouchMove` is
  private: check it against this on a Leaflet upgrade (Seamap pins 1.9.0)."
  (:require ["leaflet" :as L]
            [goog.object :as gobject]
            [re-frame.core :as re-frame]
            [imas-seamap.map.utils :refer [map->bounds]]))

;; How firmly a limit holds a drag or pinch past it, from 0 (not at all, until
;; the map springs back on release) to 1 (a wall): past a limit, the map
;; follows the finger by 1 - resistance. Also the map's `maxBoundsViscosity`.
(def resistance 0.75)

;; The kiosk's limits, from `attach!`.
(defonce ^:private kiosk-limits (atom nil))
;; The extent now (an L.LatLngBounds, taking in the current view), before it's
;; widened to the screen; nil for none.
(defonce ^:private extent (atom nil))

(defn- lat-lng-bounds [bounds]
  ((.-latLngBounds L/default) (clj->js (map->bounds bounds))))

(defn bounds-at
  "`extent` at `zoom`, widened to the screen where it's smaller."
  [^js m ^js extent zoom]
  (let [sw   (.project m (.getSouthWest extent) zoom)
        ne   (.project m (.getNorthEast extent) zoom)
        size (.getSize m)
        dx   (max 0 (/ (- (.-x size) (- (.-x ne) (.-x sw))) 2))
        dy   (max 0 (/ (- (.-y size) (- (.-y sw) (.-y ne))) 2))]
    ((.-latLngBounds L/default)
     (.unproject m ((.-point L/default) (- (.-x sw) dx) (+ (.-y sw) dy)) zoom)
     (.unproject m ((.-point L/default) (+ (.-x ne) dx) (- (.-y ne) dy)) zoom))))

(defn- update-bounds!
  "Re-widens the extent for the map's zoom. Run on zoomend, before Leaflet's
  moveend pulls the map inside it."
  [^js m]
  (when-let [e @extent]
    (set! (.. m -options -maxBounds) (bounds-at m e (.getZoom m)))))

(defn- unlimited
  "Calls `f` with the map's limits lifted, eg to fit a view the current limits
  would bend."
  [^js m f]
  (let [^js options             (.-options m)
        [min-zoom max-zoom bounds] [(.-minZoom options) (.-maxZoom options) (.-maxBounds options)]]
    (set! (.-minZoom options) js/undefined)
    (set! (.-maxZoom options) js/undefined)
    (set! (.-maxBounds options) nil)
    (try
      (f)
      (finally
        (set! (.-minZoom options) min-zoom)
        (set! (.-maxZoom options) max-zoom)
        (set! (.-maxBounds options) bounds)))))

(defn fitted
  "[L.LatLng zoom] of `bounds` ({:west :south :east :north}) fitted to the map,
  as `fitBounds` would, whatever its current limits."
  [^js m bounds]
  (let [^js view (unlimited m #(._getBoundsCenterZoom m (lat-lng-bounds bounds)))]
    [(.-center view) (.-zoom view)]))

(defn- view-bounds [^js m center zoom]
  (let [c    (.project m center zoom)
        half (.divideBy (.getSize m) 2)]
    ((.-latLngBounds L/default) (.unproject m (.subtract c half) zoom) (.unproject m (.add c half) zoom))))

(defn- settled
  "`limits` for the view `center` (an L.LatLng) at `zoom`, taking it in:
  {:extent (an L.LatLngBounds, or nil) :min-zoom :max-zoom (or nil)}."
  [^js m {:keys [bounds min-zoom max-zoom]} center zoom]
  (let [ext (when bounds (.extend (lat-lng-bounds bounds) (view-bounds m center zoom)))]
    {:extent   ext
     :min-zoom (min zoom (or min-zoom (when ext (unlimited m #(.getBoundsZoom m ext))) 0))
     :max-zoom (when max-zoom (max zoom max-zoom))}))

(defn own-extent
  "The kiosk's own extent, taking in no view: {:extent (an L.LatLngBounds)
  :min-zoom}, or nil for none."
  [^js m]
  (when-let [{:keys [bounds min-zoom]} (when (:bounds @kiosk-limits) @kiosk-limits)]
    (let [ext (lat-lng-bounds bounds)]
      {:extent   ext
       :min-zoom (or min-zoom (unlimited m #(.getBoundsZoom m ext)))})))

(defn apply!
  "Sets the kiosk's limits (see the namespace doc) for the view the map is
  about to be moved to, `center` (an L.LatLng) at `zoom`."
  [^js m center zoom]
  (let [^js options (.-options m)
        {^js ext :extent :keys [min-zoom max-zoom]} (settled m @kiosk-limits center zoom)
        pan-inside  (.-_panInsideMaxBounds m)]
    (reset! extent ext)
    (set! (.-minZoom options) min-zoom)
    (set! (.-maxZoom options) (or max-zoom js/undefined))
    (set! (.-maxBounds options) (when ext (bounds-at m ext zoom)))
    ;; What `setMaxBounds` does, without pulling the map inside now: it's
    ;; about to move.
    (if ext
      (when-not (.listens m "moveend" pan-inside)
        (.on m "moveend" pan-inside))
      (.off m "moveend" pan-inside))
    (.fire m "zoomlevelschange")))

(defn center-zoom
  "[L.LatLng zoom] of a flight target ({:zoom :center} or {:bounds})."
  [^js m {:keys [bounds center zoom]}]
  (if bounds
    (fitted m bounds)
    [((.-latLng L/default) (clj->js center)) zoom]))

(defn apply-to-target!
  "`apply!` for a flight target ({:zoom :center} or {:bounds}): for the
  kiosk's own moves that don't fly (`re-boot`)."
  [{:keys [^js leaflet-map to]}]
  (let [[center zoom] (center-zoom leaflet-map to)]
    (apply! leaflet-map center zoom)))

(re-frame/reg-fx :kiosk.map/limits apply-to-target!)

(defonce ^:private attached (js/WeakSet.))

(defn attach!
  "Gives the map the kiosk's `limits` (see the namespace doc), set by `apply!`,
  and keeps the extent widened to the screen as the map zooms and resizes."
  [^js m limits]
  (reset! kiosk-limits limits)
  (when-not (.has attached m)
    (.add attached m)
    (.on m "zoomend resize" #(update-bounds! m))))

;;; Resisting pinches

(defn- held-back
  "`value` past `limit`, held back by `resistance`."
  [value limit]
  (- value (* (- value limit) resistance)))

(defn- held-in
  "`center` at `zoom`, held back by `resistance` where the screen would go past
  the extent."
  [^js m center zoom]
  (if-let [e @extent]
    (let [c      (.project m center zoom)
          half   (.divideBy (.getSize m) 2)
          offset (._getBoundsOffset m ((.-bounds L/default) (.subtract c half) (.add c half))
                                    (bounds-at m e zoom) zoom)]
      (if (.equals (.round offset) #js [0 0])
        center
        (.unproject m (.add c (.multiplyBy offset resistance)) zoom)))
    center))

(defn- on-touch-move
  "`L.Map.TouchZoom.prototype._onTouchMove`, holding the zoom and centre back
  past the limits (see the namespace doc)."
  [^js handler ^js e]
  (let [touches (.-touches e)
        ^js m   (.-_map handler)]
    (when (and touches (= 2 (.-length touches)) (.-_zooming handler))
      (let [^js options (.-options m)
            p1          (.mouseEventToContainerPoint m (aget touches 0))
            p2          (.mouseEventToContainerPoint m (aget touches 1))
            scale       (/ (.distanceTo p1 p2) (.-_startDist handler))
            zoom        (.getScaleZoom m scale (.-_startZoom handler))
            min-zoom    (.getMinZoom m)
            max-zoom    (.getMaxZoom m)
            zoom        (cond
                          (and (not (.-bounceAtZoomLimits options))
                               (or (and (< zoom min-zoom) (< scale 1))
                                   (and (> zoom max-zoom) (> scale 1))))
                          (._limitZoom m zoom)
                          (< zoom min-zoom) (held-back zoom min-zoom)
                          (> zoom max-zoom) (held-back zoom max-zoom)
                          :else             zoom)
            center?     (= "center" (.-touchZoom options))
            ;; From the pinch's start to its centre now
            delta       (when-not center?
                          (.subtract (.divideBy (.add p1 p2) 2) (.-_centerPoint handler)))]
        (set! (.-_zoom handler) zoom)
        (when center?
          (set! (.-_center handler) (.-_startLatLng handler)))
        (when-not (and (= scale 1) (or center? (and (zero? (.-x delta)) (zero? (.-y delta)))))
          (let [center (if center?
                         (.-_startLatLng handler)
                         (.unproject m (.subtract (.project m (.-_pinchStartLatLng handler) zoom) delta) zoom))
                center (held-in m center zoom)]
            (set! (.-_center handler) center)
            (when-not (.-_moved handler)
              (._moveStart m true false)
              (set! (.-_moved handler) true))
            (js/cancelAnimationFrame (.-_animRequest handler))
            (set! (.-_animRequest handler)
                  (js/requestAnimationFrame
                   #(._move m center zoom #js {:pinch true :round false} js/undefined)))
            (.preventDefault e)))))))

(defonce ^:private installed? (atom false))

(defn install! []
  (when (compare-and-set! installed? false true)
    (gobject/set (.. L/default -Map -TouchZoom -prototype) "_onTouchMove"
                 (fn [e] (this-as handler (on-touch-move handler e))))))
