;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.story-maps.events
  (:require [imas-seamap.utils :refer [decode-html-entities]]))

(defn- response->story-map
  "Extracts useful parts of a story-map response."
  [response]
  (let [id        (:id response)
        title     (decode-html-entities (get-in response [:title :rendered]))
        content   (get-in response [:acf :description])
        image     (or (get-in response [:acf :image]) nil) ; we do this because we want nil instead of false if there's no image
        map-links (get-in response [:acf :map_links])]
    (js/console.log "content" content)
    {:id        id
     :title     title
     :content   content
     :image     image
     :map-links map-links}))

(defn update-featured-maps [db [_ response]]
  (assoc-in db [:story-maps :featured-maps] (mapv response->story-map response)))

(defn featured-map [{:keys [db]} [_ story-map]]
  (let [db        (assoc-in db [:story-maps :featured-map] story-map)]
    {:db         db
     :dispatch-n [[:sm.featured-map/open true]
                  [:maybe-autosave]]}))

(defn featured-map-open [{:keys [db]} [_ open?]]
  {:db       (assoc-in db [:story-maps :open?] open?)
   :dispatch [:maybe-autosave]})
