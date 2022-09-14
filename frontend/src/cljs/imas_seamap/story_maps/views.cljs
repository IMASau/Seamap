;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.story-maps.views
  (:require [re-frame.core :as re-frame]
            [imas-seamap.blueprint :as b]
            [imas-seamap.components :as components]
            [clojure.pprint]))

(defn- featured-map [{:keys [title content image] :as story-map}]
  [b/card
   {:elevation   1
    :interactive true
    :class       "featured-map"
    :on-click    #(re-frame/dispatch [:sm/featured-map story-map])}
   (when (seq image)
     [:div.image-container
      [:img {:src image}]])
   [:div.title title]
   [:div content]])

(defn featured-maps []
  (let [story-maps @(re-frame/subscribe [:sm/featured-maps])]
    [:div
     (for [{:keys [id] :as story-map} story-maps]
       ^{:key (str id)}
       [featured-map story-map])]))

(defn featured-map-drawer []
  (let [{:keys [title content] :as _story-map} @(re-frame/subscribe [:sm/featured-map])]
   [components/drawer
    {:title    title
     :position "right"
     :size     "460px"
     :isOpen   @(re-frame/subscribe [:sm.featured-map/open?])
     :onClose  #(re-frame/dispatch [:sm.featured-map/open false])
     :hasBackdrop false}
    [:div content]]))
