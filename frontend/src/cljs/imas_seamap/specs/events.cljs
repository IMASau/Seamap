;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.specs.events
  (:require [cljs.spec.alpha :as s]
            [imas-seamap.specs.app-state]
            [imas-seamap.events :as events]
            [imas-seamap.subs :as subs]))

;;; convenience; event-vector for a no-args event handler:
(s/def ::event-v (s/coll-of keyword? :count 1))

(s/def :seamap.fx/db :seamap/app-state)
(s/def :seamap/fx-appstate (s/keys :req-un [:seamap.fx/db]))

(s/fdef events/not-yet-implemented
  :args (s/cat :db :seamap/app-state
               :event-v vector?)
  :ret :seamap/app-state)

(s/fdef events/initialise-db
  :args (s/cat :db map?       ; Presumably uninitialised at this point
               :event-v ::event-v)
  :ret :seamap/app-state)

(s/fdef events/transect-query
  :args (s/cat :db :seamap/fx-appstate
               :event-v
               (s/spec
                (s/cat :event-id keyword?
                       :geojson :imas-seamap.specs.app-state/geojson)))
  :ret :seamap/fx-appstate)

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

(s/def :transect.results/query :imas-seamap.specs.app-state/geojson)
(s/def :transect.results/status #{:transect.results.status/empty
                                  :transect.results.status/loading
                                  :transect.results.status/partial
                                  :transect.results.status/ready
                                  :transect.results.status/error})
(s/def :transect.results/habitat    :imas-seamap.specs.app-state/transect-results-format)
(s/def :transect.results/bathymetry :imas-seamap.specs.app-state/transect-results-format)
(s/def :transect.results/zone-colours map?)
(s/def :transect.results/zone-legend map?)
(s/def :transect/results
  (s/keys :req [:transect.results/query
                :transect.results/status
                :transect.results/habitat
                :transect.results/bathymetry
                :transect.results/zone-colours
                :transect.results/zone-legend]))

(s/fdef subs/transect-results
  :args (s/cat :db :seamap/app-state
               :event-v vector?)
  :ret :transect/results)
