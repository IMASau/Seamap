;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.story-maps.views
  (:require [re-frame.core :as re-frame]
            [imas-seamap.blueprint :as b]
            [clojure.pprint]))

(defn featured-map [{:keys [title content image] :as _story-map}]
  [b/card
   {:elevation   1
    :interactive true
    :class       "featured-map"}
   (when (seq image) [:img {:src image}])
   [:div.details
    [:div.title title]
    [:div content]]])

(defn featured-maps []
  (let [story-maps @(re-frame/subscribe [:sm/featured-maps])]
    [:div
     (for [{:keys [id] :as story-map} story-maps]
       ^{:key (str id)}
       [featured-map story-map])]))
