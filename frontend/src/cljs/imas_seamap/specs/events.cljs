(ns imas-seamap.specs.events
  (:require [cljs.spec :as s]
            [imas-seamap.specs.app-state]
            [imas-seamap.events :as events]))

;;; convenience; event-vector for a no-args event handler:
(s/def ::event-v (s/coll-of keyword? :count 1))

(s/fdef events/not-yet-implemented
  :args (s/cat :db :seamap/app-state
               :event-v vector?)
  :ret :seamap/app-state)

(s/fdef events/initialise-db
  :args (s/cat :db map?       ; Presumably uninitialised at this point
               :event-v ::event-v)
  :ret :seamap/app-state)

(s/fdef events/transect-query
  :args (s/cat :db :seamap/app-state
               :event-v
               (s/spec
                (s/cat :event-id keyword?
                       :geojson :imas-seamap.specs.app-state/geojson)))
  :ret :seamap/app-state)

(s/fdef events/transect-drawing-start
  :args (s/cat :db :seamap/app-state
               :event-v ::event-v)
  :ret :seamap/app-state)

(s/fdef events/transect-drawing-finish
  :args (s/cat :db :seamap/app-state
               :event-v ::event-v)
  :ret :seamap/app-state)

(s/def :ajax/handler keyword?)
(s/def :ajax/err-handler keyword?)
(s/def :ajax/override-opts map?)
(s/fdef events/ajax
  :args (s/cat :db :seamap/app-state
               :event-v
               (s/spec
                (s/cat :event-id keyword?
                       :url string?
                       :opts (s/keys :req-un [:ajax/handler]
                                     :opt-un [:ajax/err-handler :ajax/override-opts]))))
  :ret :seamap/app-state)

