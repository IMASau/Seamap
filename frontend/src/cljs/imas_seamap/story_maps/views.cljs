;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.story-maps.views
  (:require [re-frame.core :as re-frame]
            [imas-seamap.blueprint :as b]
            [imas-seamap.components :as components]
            [clojure.pprint]))

(defn- featured-map [{:keys [title content image] :as story-map}]
  [:div.featured-map
   (when (seq image)
     [:img {:src image}])
   [:div.title title]
   [:div.content
    {:ref #(when % (set! (.-innerHTML %) content))}
    content]
   [b/button
    {:icon     "search"
     :text     "Show me"
     :intent   b/INTENT-PRIMARY
     :class    "show-me"
     :large    true
     :on-click #(re-frame/dispatch [:sm/featured-map story-map])}]])

(defn featured-maps []
  (let [story-maps @(re-frame/subscribe [:sm/featured-maps])]
    [:div.featured-maps
     [:div.orientation "Connecting raw data with narrative and scientific context."]
     [:div
      (for [{:keys [id] :as story-map} story-maps]
        ^{:key (str id)}
        [featured-map story-map])]]))

(defn- map-link [{:keys [subtitle description shortcode] :as _map-link}]
  [:div.map-link
   [:div
    [:div.subtitle subtitle]
    [b/button
     {:icon     "search"
      :text     "Show me"
      :intent   b/INTENT-PRIMARY
      :class    "show-me"
      :large    true
      :on-click #(re-frame/dispatch [:get-save-state shortcode [:merge-state]])}]]
   [:div.description
    {:ref
     #(when %
        (set! (.-innerHTML %) description)
        (let [hyperlinks (js/Array.prototype.slice.call (.getElementsByTagName % "a"))]
          (doseq [hyperlink hyperlinks]
            (when-not (.getAttribute hyperlink "target")
              (.setAttribute hyperlink "target" "_blank")))))}
    description]])

(defn featured-map-drawer []
  (let [{:keys [title content map-links] :as _story-map} @(re-frame/subscribe [:sm/featured-map])]
   [components/drawer
    {:title       (or title "")
     :position    "right"
     :size        "368px"
     :isOpen      true
     :onClose     #(re-frame/dispatch [:sm.featured-map/open false])
     :hasBackdrop false
     :className   "featured-map-drawer"}
    [:div.summary
     {:ref #(when % (set! (.-innerHTML %) content))}
     content]
    [:div.map-links
     (map-indexed
      (fn [index map-link-val]
        ^{:key index}
        [map-link map-link-val])
      map-links)]]))
