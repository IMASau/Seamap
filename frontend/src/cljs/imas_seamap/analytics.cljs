;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.analytics
  "Google analytics integration; ultimately, provides an interceptor
  that can generate GA events from re-frame events.  The events and
  formatting is customisable."
  (:require [re-frame.interceptor :refer [->interceptor get-coeffect]]))


(defn event->action [kw]
  (-> kw str (subs 1)))

(defmulti format-event (fn [event-v] (first event-v)))

(defmethod format-event :map/toggle-layer [[_ layer :as _event-v]]
  {:eventCategory "layers"
   :eventAction   "toggle"
   :eventLabel    (:layer_name layer)})

(defmethod format-event :map/add-layer [[_ layer :as _event-v]]
  {:eventCategory "layers"
   :eventAction   "add"
   :eventLabel    (:layer_name layer)})

(defmethod format-event :map/remove-layer [[_ layer :as _event-v]]
  {:eventCategory "layers"
   :eventAction   "remove"
   :eventLabel    (:layer_name layer)})

(defmethod format-event :map/pan-to-layer [[_ layer :as _event-v]]
  {:eventCategory "layers"
   :eventAction   "pan"
   :eventLabel    (:layer_name layer)})

(defmethod format-event :default [[id & _args :as _event-v]]
  {:eventCategory "general"
   :eventAction   (event->action id)})

;;; Note, for testing you can set "ga = console.log" in the browser
;;; developer console (also note that by default the analytics
;;; interceptor is disabled in the dev profile)
(defn track-event [ga-event]
  (when (exists? js/ga)
    (js/ga "send"
       (-> {:hitType "event"}
           (merge ga-event)
           clj->js))))

(defn analytics-for [events-to-log]
  (let [loggable-events (set events-to-log)]
    (->interceptor
     :id    :generate-analytics
     :after (fn analytics-for-after
              [context]
              (let [event (get-coeffect context :event)]
                (when (loggable-events (first event))
                  (-> event format-event track-event))
                context)))))
