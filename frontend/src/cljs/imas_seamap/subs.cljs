(ns imas-seamap.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [imas-seamap.map.views :refer [point-along-line point->latlng]]
              [re-frame.core :as re-frame]
              [debux.cs.core :refer-macros [dbg]]))

(defn transect-info [{:keys [map transect] :as db} _]
  {:drawing? (boolean (get-in map [:controls :transect]))
   :query (:query transect)
   :mouse-loc (point->latlng
               (point-along-line (-> transect :query :geometry :coordinates)
                                 (:mouse-percentage transect)))})

(defn- transect-query-status [{:keys [habitat bathymetry] :as args}]
  (cond
    (every? nil? [habitat bathymetry]) :transect.results.status/empty
    (some string? [habitat bathymetry]) :transect.results.status/error
    (#{habitat bathymetry} :loading) :transect.results.status/loading
    ;; Any cases missed?? (We're assuming things are reset to nil/loading on clear/query)
    :default :transect.results.status/ready))

(defn transect-results [{{:keys [query habitat bathymetry] :as transect} :transect :as db} _]
  {:transect.results/query (:query transect)
   :transect.results/status (transect-query-status transect)
   :transect.results/habitat habitat
   :transect.results/bathymetry bathymetry
   :transect.results/zone-colours {}})

(defn transect-show? [db _]
  (get-in db [:transect :show?] false))

(defn help-layer-open? [db _]
  (get-in db [:display :help-overlay]))
