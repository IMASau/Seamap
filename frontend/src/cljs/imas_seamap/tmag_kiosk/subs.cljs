;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.subs
  (:require [imas-seamap.tmag-kiosk.events :as tkevents]))

(defn stories [db _]
  (get-in db [:kiosk :stories]))

(defn story [db _]
  (tkevents/current-story db))

(defn step-index [db _]
  (get-in db [:kiosk :step-index]))

(defn media
  "The current step's media, for the media card: [{:url :lat :lon :caption
  :overlay}]. Only once its flight has landed: the media belongs to the place,
  and loading it (a video's first frame) mid-flight can cost a frame."
  [db _]
  (let [{:keys [landed-step step-index]} (:kiosk db)]
    (when (= landed-step step-index)
      (get-in (tkevents/current-story db) [:steps step-index :media]))))

(defn media-index [db _]
  (get-in db [:kiosk :media-index] 0))

(defn media-markers
  "The map's media markers: those of the step whose map is showing, which
  lags the story card by a flight; `:selected` is the item in the media card,
  once the map has caught up."
  [db _]
  (let [{:keys [landed-step step-index media-index]} (:kiosk db)]
    (when landed-step
      {:media    (get-in (tkevents/current-story db) [:steps landed-step :media])
       :selected (when (= landed-step step-index) (or media-index 0))})))

(defn attract? [db _]
  (get-in db [:kiosk :attract?]))

(defn layer-names
  "Names of the layers on the map, for the story card's \"On the map\" key."
  [active-layers _]
  (->> active-layers (map :name) distinct vec))

(defn layers-loading?
  "Whether any layer on the map is still loading. Only active layers count:
  one removed mid-load never reports that it finished."
  [[active-layers loading-state] _]
  (boolean (some #(= :map.layer/loading (get loading-state %)) active-layers)))
