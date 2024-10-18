;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tas-marine-atlas.subs
  (:require [imas-seamap.utils :refer [ids->layers]]
            [imas-seamap.map.utils :refer [rich-layer-children->parents]]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn data-in-region [db _]
  (let [data   (get-in db [:data-in-region :data])
        layers (get-in db [:map :layers])
        rich-layer-children (get-in db [:map :layers])
        status (cond
                 (keyword? data) data
                 data            :data-in-region/loaded
                 :else           :data-in-region/none)
        data-layers (when (= status :data-in-region/loaded) (ids->layers data layers))
        data-layers (when data-layers (rich-layer-children->parents data-layers rich-layer-children))]
    {:status    status
     :layers    data-layers}))

(defn welcome-layer-open? [db _]
  (get-in db [:display :welcome-overlay]))
