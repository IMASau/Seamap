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

(defmethod format-event :map/add-layer [[_ layer :as _event-v]]
  {:event_category "layers"
   :event_action   "add-layer"
   :event_label    (:layer_name layer)
   :layer_name     (:layer_name layer)
   :layer_display_name (:name layer)})

(defmethod format-event :map/remove-layer [[_ layer :as _event-v]]
  {:event_category "layers"
   :event_action   "remove-layer"
   :event_label    (:layer_name layer)
   :layer_name     (:layer_name layer)
   :layer_display_name (:name layer)})

(defmethod format-event :map/toggle-layer [[_ layer :as _event-v]]
  {:event_category "layers"
   :event_action   "toggle-layer"
   :event_label    (:layer_name layer)
   :layer_name     (:layer_name layer)
   :layer_display_name (:name layer)})

(defmethod format-event :map.layer/metadata-click [[_ {:keys [link layer]} :as _event-v]]
  {:event_category "layers"
   :event_action   "metadata-click"
   :event_label    (:layer_name layer)
   :layer_name     (:layer_name layer)
   :layer_display_name (:name layer)
   :metadata_link  link})

(defmethod format-event :map/pan-to-layer [[_ layer :as _event-v]]
  {:event_category "layers"
   :event_action   "pan-layer"
   :event_label    (:layer_name layer)
   :layer_name     (:layer_name layer)
   :layer_display_name (:name layer)})

(defmethod format-event :map/toggle-layer-visibility [[_ layer :as _event-v]]
  {:event_category "layers"
   :event_action   "toggle-layer-visibility"
   :event_label    (:layer_name layer)
   :layer_name     (:layer_name layer)
   :layer_display_name (:name layer)})

(defmethod format-event :map.layer/load-error [[_ layer :as _event-v]]
  {:event_category "layers"
   :event_action   "layer-load-error"
   :event_label    (:layer_name layer)
   :layer_name     (:layer_name layer)
   :layer_display_name (:name layer)})

(defmethod format-event :download-click [[_ {:keys [link layer type]} :as _event-v]]
  {:event_category "layers"
   :event_action   "download-click"
   :event_label    (:layer_name layer)
   :layer_name     (:layer_name layer)
   :layer_display_name (:name layer)
   :download_link  link
   :download_type  type})

(defmethod format-event :sm/featured-map [[_ {:keys [title] :as _story-map} :as _event-v]]
  {:event_action   "sm/featured-map"
   :event_label    title
   :featured_map   title})

(defmethod format-event :default [[id & _args :as _event-v]]
  {:event_category "general"
   :event_action   (event->action id)})

;;; Note, for testing you can set "gtag = console.log" in the browser
;;; developer console (also note that by default the analytics
;;; interceptor is disabled in the dev profile)
(defn track-event [ga-event]
  (when (exists? js/gtag)
    (js/gtag "event" (:event_action ga-event)
       (-> {:hit_type "event"}
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
