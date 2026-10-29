;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.inertia
  "A flicked map glides off at the speed the finger left it, whenever it's
  flicked.

  Leaflet (1.9.0) measures the release speed from the finger's moves in the
  50 ms before the lift is handled, and doesn't glide with fewer than two. The
  touch table reports a lift 44-47 ms after the finger's last move (measured
  over 23 swipes, however they were made), and moves come every ~15 ms, so it
  came down to a few milliseconds whether a second move was still in the
  window: two thirds of swipes didn't glide. Here the speed comes from the
  moves in the 50 ms before the finger's last move (which `_onDrag` already
  keeps), and the map doesn't glide if the finger was still for longer than
  `max-still-ms` before lifting.

  Leaflet's drag inertia also starts at half the release speed, or less
  when `easeLinearity` is under 0.2: `Map.Drag`'s `_onDragEnd` sizes the glide
  as if it slowed at a steady rate (distance = speed x duration / 2), but the
  glide is animated by `PosAnimation` on the curve 1 - (1 - t)^p, with
  p = 1 / max(easeLinearity, 0.2), which starts at p x distance / duration.
  Measured on the kiosk map, a 1500 px/s drag glided off at about 730 px/s.

  `install!` replaces `_onDragEnd` with Leaflet's own, except for the moves
  the speed is measured from (above), and that the glide's distance is the
  one that curve needs to start at the release speed: speed x duration / p.
  Its duration, Leaflet's speed limit and everything else are unchanged, so a
  glide covers twice the distance in the same time. The glide still starts a
  frame after release, as in Leaflet.

  Leaflet's drag handler takes `_onDragEnd` when a map is made, so this must be
  installed before the map is. It patches the prototype for the whole page, so
  only the kiosk installs it. `attach!` also has a touch stop a glide. `_onDragEnd` is private: check it against this on
  a Leaflet upgrade (Seamap pins 1.9.0)."
  (:require ["leaflet" :as L]
            [goog.object :as gobject]))

;; How long the finger can be still before lifting and still flick the map:
;; the table's lift delay (~45 ms) and a margin. A finger held longer has
;; stopped the map.
(def max-still-ms 100)

(defn- still?
  "Whether a glide offset is no movement: zero, or NaN (no time between the
  moves it was measured from)."
  [^js offset]
  (and (not (pos? (js/Math.abs (.-x offset))))
       (not (pos? (js/Math.abs (.-y offset))))))

(defn- on-drag-end
  "`L.Map.Drag.prototype._onDragEnd`, except for the moves the speed is measured
  from and the glide's distance (see the namespace doc)."
  [^js handler ^js e]
  (let [^js m       (.-_map handler)
        ^js options (.-options m)]
    (.fire m "dragend" e)
    (if (or (not (.-inertia options)) (.-noInertia e) (< (.-length (.-_times handler)) 2)
            (> (- (js/Date.now) (.-_lastTime handler)) max-still-ms))
      (.fire m "moveend")
      ;; `_onDrag` keeps the moves in the 50 ms before the last one. Leaflet
      ;; prunes them again here, to the 50 ms before now; that's the step left out.
      (let [direction (.subtract (.-_lastPos handler) (aget (.-_positions handler) 0))
            seconds   (/ (- (.-_lastTime handler) (aget (.-_times handler) 0)) 1000)
            ease      (.-easeLinearity options)
            velocity  (.divideBy direction seconds) ; px/s
            speed     (.distanceTo velocity #js [0 0])
            ;; Leaflet's limit applies to speed x easeLinearity.
            limited   (min speed (/ (.-inertiaMaxSpeed options) ease))
            duration  (/ limited (.-inertiaDeceleration options))
            power     (/ 1 (max ease 0.2)) ; PosAnimation's easeOutPower
            offset    (.round (.multiplyBy velocity (/ (* (- limited) duration) (* speed power))))]
        (if (still? offset)
          (.fire m "moveend")
          (let [offset (._limitOffset m offset (.-maxBounds options))]
            (js/requestAnimationFrame
             #(.panBy m offset #js {:duration      duration
                                    :easeLinearity ease
                                    :noMoveStart   true
                                    :animate       true}))))))))

(defonce ^:private attached (js/WeakSet.))

(defn attach!
  "A touch stops a gliding map, where Leaflet stopped it only once the finger
  moved (a drag). That touch isn't a tap on the map: it doesn't ask for
  features. From the touch-sandbox project."
  [^js m]
  (when-not (.has attached m)
    (.add attached m)
    (let [stopped?  (volatile! false)
          container (.getContainer m)]
      ;; Once per press: Leaflet's own "down" comes again from the mousedown a
      ;; browser makes up after a tap.
      (.addEventListener container "pointerdown"
                         (fn [e]
                           (when (.-isPrimary e)
                             (vreset! stopped? (boolean (some-> (.-_panAnim m) .-_inProgress)))
                             (when @stopped?
                               (.stop m))))
                         true)
      ;; Ahead of Leaflet's own listener on the container
      (.addEventListener container "click"
                         (fn [e]
                           (when @stopped?
                             (vreset! stopped? false)
                             (.stopPropagation e)))
                         true))))

(defonce ^:private installed? (atom false))

(defn install! []
  (when (compare-and-set! installed? false true)
    (gobject/set (.. L/default -Map -Drag -prototype) "_onDragEnd"
                 (fn [e] (this-as handler (on-drag-end handler e))))))
