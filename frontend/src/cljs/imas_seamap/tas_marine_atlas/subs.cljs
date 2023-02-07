;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tas-marine-atlas.subs
  (:require [imas-seamap.utils :refer [ids->layers]]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn data-in-region-open? [db _]
  (get-in db [:data-in-region :open?]))

(defn data-in-region [db _]
  (let [data   (get-in db [:data-in-region :data])
        layers (get-in db [:map :layers])
        status (cond
                 (keyword? data) data
                 data            :data-in-region/loaded
                 :else           :data-in-region/none)]
    {:status    status
     :layers      (when (= status :data-in-region/loaded) (ids->layers data layers))}))
