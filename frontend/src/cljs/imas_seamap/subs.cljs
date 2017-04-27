(ns imas-seamap.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [imas-seamap.map.views :refer [point->latlng point-distance]]
              [re-frame.core :as re-frame]
              [debux.cs.core :refer-macros [dbg]]))

(defn scale-distance
  "Given a line from two x-y points and a percentage, return the point
  that is that percentage along the line."
  [[x1 y1] [x2 y2] pct]
  [(+ x1 (* pct (- x2 x1)))
   (+ y1 (* pct (- y2 y1)))])

(defn point-along-line
  "Given a series of coordinates representing a line, and a
  percentage (assumed to be between 0 and 100), return a point as
  two-element vector that occurs at that percentage distance along the
  line."
  [coords pct]
  (let [pairs (map vector coords (rest coords))
        seg-distances (loop [[[p1 p2 :as seg] & rest-pairs] pairs
                             acc-len 0
                             acc ()] ; accumlator has tuples of start-len, end-len, pair (segment)
                        (if-not p1
                          acc
                          (let [distance (point-distance p1 p2)
                                end-len (+ acc-len distance)]
                            (recur rest-pairs
                                   end-len
                                   ;; Note, we cons, so the last entry is first on the list:
                                   (cons [acc-len end-len seg] acc)))))
        [_ total-distance _] (first seg-distances)
        ->pctg #(/ % total-distance)
        pct (/ pct 100)
        pct-distances (map (fn [[d1 d2 seg]] [(->pctg d1) (->pctg d2) seg]) seg-distances)
        [lower upper [s1 s2]] (first (filter (fn [[p1 p2 s]] (<= p1 pct p2)) pct-distances))
        remainder-pct (/ (- pct lower) (- upper lower))]
    (scale-distance s1 s2 remainder-pct)))

(defn transect-info [{:keys [map transect] :as db} _]
  (merge
   {:drawing? (boolean (get-in map [:controls :transect]))
    :query (:query transect)}
   (when-let [pctg (:mouse-percentage transect)]
     {:mouse-loc (point->latlng
                  (point-along-line (-> transect :query :geometry :coordinates)
                                    pctg))})))

(defn- transect-query-status [{:keys [habitat bathymetry] :as args}]
  (cond
    (every? nil? [habitat bathymetry]) :transect.results.status/empty
    (some string? [habitat bathymetry]) :transect.results.status/error
    (#{habitat bathymetry} :loading) :transect.results.status/loading
    ;; Any cases missed?? (We're assuming things are reset to nil/loading on clear/query)
    :default :transect.results.status/ready))

;;; TODO: This needs to map the values from the SM_HAB_CLS common
;;; column from the database tables into a colour scheme
(def ^:private habitat-mapping-colours
  {})

(defn transect-results [{{:keys [query habitat bathymetry] :as transect} :transect :as db} _]
  {:transect.results/query query
   :transect.results/status (transect-query-status transect)
   :transect.results/habitat habitat
   :transect.results/bathymetry bathymetry
   :transect.results/zone-colours habitat-mapping-colours})

(defn transect-show? [db _]
  (get-in db [:transect :show?] false))

(defn help-layer-open? [db _]
  (get-in db [:display :help-overlay]))
