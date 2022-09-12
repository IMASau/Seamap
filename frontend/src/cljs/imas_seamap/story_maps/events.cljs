;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.story-maps.events)

(defn- response->story-map
  "Extracts useful parts of a story-map response."
  [response]
  (let [id    (:id response)
        title (get-in response [:title :rendered])
        content (->> (get-in response [:content :rendered])
                     (re-find (re-pattern "(?<=^<p>).+(?=</p>\n$)")))
        map-links (vector (get-in response [:acf :map_link]))] ;; TODO: Remove 'vector' when upgraded to ACF Pro and can use repeater groups
    {:id        id
     :title     title
     :content   content
     :map-links map-links}))

(defn update-featured-maps [db [_ response]]
  (assoc-in db [:story-maps :featured-maps] (mapv response->story-map response)))
