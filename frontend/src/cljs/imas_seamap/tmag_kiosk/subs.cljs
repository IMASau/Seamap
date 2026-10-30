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

(defn intro? [db _]
  (get-in db [:kiosk :intro?]))

(defn media
  "The current step's media, for the media card: [{:url :lat :lon :caption
  :overlay}]. Only once its flight has landed: the media belongs to the place,
  and loading it (a video's first frame) mid-flight can cost a frame. None on
  the story's intro card. A tapped feature's media replaces the step's."
  [db _]
  (let [{:keys [landed-step step-index intro? feature-media]} (:kiosk db)]
    (when (and (= landed-step step-index) (not intro?))
      (or feature-media
          (get-in (tkevents/current-story db) [:steps step-index :media])))))

(defn media-index [db _]
  (get-in db [:kiosk :media-index] 0))

(defn media-markers
  "The map's media markers: those of the step whose map is showing, which
  lags the story card by a flight; `:selected` is the item in the media card,
  once the map has caught up. None on the story's intro card."
  [db _]
  (let [{:keys [landed-step step-index media-index intro? feature-media]} (:kiosk db)]
    (when (and landed-step (not intro?))
      {:media    (or feature-media (get-in (tkevents/current-story db) [:steps landed-step :media]))
       :selected (when (= landed-step step-index) (or media-index 0))})))

(defn story-markers
  "The home map's story markers: [{:id :lat :lon :icon :selected?}], for
  stories with a location. Only with the map home: on the story list, and on
  a story's intro card opened from it, where the story's marker is
  `:selected?`. Not on the attract screen (behind it, the tour moves the map),
  back on the intro card from the first step (the map's there), or while the
  map flies home, when nothing is added to the page (see
  `imas-seamap.tmag-kiosk.flight`)."
  [db _]
  (let [{:keys [stories story-id intro? landed-step attract? homeward?]} (:kiosk db)]
    (when (and (not (or attract? homeward?))
               (or (not story-id) (and intro? (not landed-step))))
      (for [{:keys [id marker]} stories :when marker]
        (assoc marker :id id :selected? (= id story-id))))))

(defn attract? [db _]
  (get-in db [:kiosk :attract?]))

(defn layers-loading?
  "Whether any layer on the map is still loading. Only active layers count:
  one removed mid-load never reports that it finished."
  [[active-layers loading-state] _]
  (boolean (some #(= :map.layer/loading (get loading-state %)) active-layers)))
