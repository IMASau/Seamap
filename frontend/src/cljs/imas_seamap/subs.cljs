;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.subs
    (:require [clojure.set :refer [rename-keys]]
              [imas-seamap.utils :refer [first-where]]
              [imas-seamap.map.views :refer [point->latlng point-distance]]
              #_[debux.cs.core :refer [dbg] :include-macros true]))

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
        [lower upper [s1 s2]] (first-where (fn [[p1 p2 _s]] (<= p1 pct p2)) pct-distances)
        remainder-pct (/ (- pct lower) (- upper lower))]
    (scale-distance s1 s2 remainder-pct)))

(defn feature-info [{:keys [feature] :as _db} _]
  (if-let [{:keys [status info location had-insecure?]} feature]
    {:has-info? true
     :had-insecure? had-insecure?
     :status status
     :info-body info
     :location ((juxt :lat :lng) location)}
    {:has-info? false}))

(defn download-info [db _]
  (-> db
      (get-in [:map :controls :download])
      (rename-keys {:selecting :outlining?
                    :type      :download-type
                    :layer     :download-layer})))

(defn transect-info [{:keys [map transect] :as _db} _]
  (merge
   {:drawing? (boolean (get-in map [:controls :transect]))
    :query (:query transect)}
   (when-let [pctg (:mouse-percentage transect)]
     {:mouse-loc (point->latlng
                  (point-along-line (-> transect :query :geometry :coordinates)
                                    pctg))})))

(defn- transect-query-status [{:keys [habitat bathymetry]}]
  (cond
    (every? nil? [habitat bathymetry])      :transect.results.status/empty
    (every? string? [habitat bathymetry])   :transect.results.status/error
    (= habitat bathymetry :loading)         :transect.results.status/loading
    (some #{:loading} [habitat bathymetry]) :transect.results.status/partial
    :default                                :transect.results.status/ready))

(defn transect-results [{{:keys [query habitat bathymetry] :as transect} :transect
                         :keys [habitat-colours habitat-titles]
                         :as _db} _]
  (letfn [(always-vec [d] (if (vector? d) d []))]
    {:transect.results/query        query
     :transect.results/status       (transect-query-status transect)
     :transect.results/habitat      (always-vec habitat)
     :transect.results/bathymetry   (always-vec bathymetry)
     :transect.results/zone-colours habitat-colours
     :transect.results/zone-legend  habitat-titles}))

(defn transect-show? [db _]
  (get-in db [:transect :show?] false))

(defn help-layer-open? [db _]
  (get-in db [:display :help-overlay]))

(defn welcome-layer-open? [db _]
  (get-in db [:display :welcome-overlay]))

(defn map-layer-info [db _]
  (let [info (get-in db [:display :info-card])
        download (get-in db [:map :controls :download])]
    (cond-> info
      (map? info) (assoc :hidden? (or (:selecting download)
                                      (:display-link download))))))

(defn sorting-info [db _] (get db :sorting))

(defn catalogue-tab [db _]
  (get-in db [:display :catalogue :tab]))

(defn catalogue-nodes [db _]
  (->> (get-in db [:display :catalogue :expanded])
       (map #(vector % true))
       (into {})))

(defn preview-layer-url [db _]
  (let [url (get-in db [:config :layer-previews-url])
        layer (get-in db [:map :preview-layer])]
    (when layer (str url (:id layer) ".png"))))

(defn sidebar-state [db _]
  (get-in db [:display :sidebar]))

(defn app-loading? [db _]
  (get db :loading false))

(defn load-normal-msg [db _]
  (get db :loading-message))

(defn load-error-msg [db _]
  (get db :load-error-msg))

(defn user-message [db _]
  (get-in db [:info :message]))

(defn left-drawer-open? [db _]
  (get-in db [:display :left-drawer]))

(defn left-drawer-tab [db _]
  (get-in db [:display :left-drawer-tab]))

(defn layers-search-omnibar-open? [db _]
  (get-in db [:display :layers-search-omnibar]))

(defn drawer-panel-stack [db _]
  (get-in db [:display :drawer-panel-stack]))
