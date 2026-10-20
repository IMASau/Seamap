;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.idle
  "Watches for visitor input and dispatches `:kiosk/idle` once none has been
  seen for `timeout-ms`. Input is tracked outside re-frame (pointer moves are
  far too frequent to be events); see `imas-seamap.tmag-kiosk.events/go-idle`
  for what going idle does."
  (:require [re-frame.core :as re-frame]))

(def timeout-ms (* 2 60 1000))

(def ^:private check-ms 1000)

(def ^:private activity-events
  ["pointerdown" "pointermove" "touchstart" "wheel" "keydown"])

;; performance.now is monotonic, so clock changes can't trigger (or stall) a reset.
(defonce ^:private last-activity (atom (js/performance.now)))

(defn- record-activity [_]
  (reset! last-activity (js/performance.now)))

(defn- check []
  ;; A visitor watching a video isn't touching anything.
  (when (some #(not (.-paused ^js %)) (array-seq (js/document.querySelectorAll "video")))
    (record-activity nil))
  (when (>= (- (js/performance.now) @last-activity) timeout-ms)
    ;; Count the reset itself as activity, so it fires once per idle period.
    (record-activity nil)
    (re-frame/dispatch [:kiosk/idle])))

(defonce ^:private started? (atom false))

(defn start!
  "Starts watching. Safe to call more than once (eg on hot reload)."
  []
  (when (compare-and-set! started? false true)
    (doseq [event activity-events]
      ;; Capture phase, so we still see input that Leaflet or Blueprint stop
      ;; from propagating.
      (js/window.addEventListener event record-activity #js {:capture true :passive true}))
    (js/setInterval check check-ms)))
