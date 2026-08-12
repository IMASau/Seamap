;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.views
  (:require [re-frame.core :as re-frame]
            [imas-seamap.blueprint :refer [use-hotkeys]]
            [imas-seamap.components :as components]
            [imas-seamap.interop.react :refer [use-memo]]
            [imas-seamap.views :as views]
            [imas-seamap.tmag-kiosk.map.views :refer [map-component]]
            [imas-seamap.story-maps.views :refer [featured-maps]]
            [goog.string.format]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn custom-leaflet-controls []
  [:div.custom-leaflet-controls.leaflet-top.leaflet-left.leaflet-touch
   [views/zoom-control]

   [views/control-block
    [views/control-block-child
     {:on-click #(re-frame/dispatch [:layers-search-omnibar/open])
      :tooltip  "Search All Layers"
      :id       "omnisearch-control"
      :icon     "search"}]]])

(defn reset-control []
  [:div.custom-leaflet-controls.leaflet-top.leaflet-right.leaflet-touch
   [views/control-block
    [views/control-block-child
     {:on-click #(re-frame/dispatch [:re-boot])
      :tooltip  "Reset Interface"
      :id       "reset-control"
      :icon     "undo"}]]])

(defn left-drawer []
  ;; Kiosk drawer is permanently open (no menu button to re-open it), and
  ;; contains only the featured-maps panel, so no tabs either.
  [components/drawer
   {:title
    [:div
     [:a {:href "https://seamapaustralia.org/"}
      [:img {:src "img/Seamap2_V2_RGB.png"}]]]
    :position    "left"
    :size        "368px"
    :isOpen      true
    :onClose     (fn [])
    :className   "left-drawer seamap-drawer"
    :isCloseButtonShown false
    :hasBackdrop false}
   [featured-maps]])

(def hotkeys-combos
  (let [keydown-wrapper
        (fn [m keydown-v]
          (assoc m :global    true
                 :group "Keyboard Shortcuts"
                 :onKeyDown #(re-frame/dispatch keydown-v)))]
    ;; See note on `use-hotkeys' for rationale invoking `clj->js' here:
    (clj->js
     [(keydown-wrapper
       {:label "Zoom In"                :combo "plus"}
       [:map/zoom-in])
      (keydown-wrapper
       {:label "Zoom Out"               :combo "-"}
       [:map/zoom-out])
      (keydown-wrapper
       {:label "Pan Up"                 :combo "up"}
       [:map/pan-direction :up])
      (keydown-wrapper
       {:label "Pan Down"               :combo "down"}
       [:map/pan-direction :down])
      (keydown-wrapper
       {:label "Pan Left"               :combo "left"}
       [:map/pan-direction :left])
      (keydown-wrapper
       {:label "Pan Right"              :combo "right"}
       [:map/pan-direction :right])
      (keydown-wrapper
       {:label "Layer Power Search"     :combo "s"}
       [:layers-search-omnibar/toggle])
      (keydown-wrapper
       {:label "Reset"                  :combo "shift + r"}
       [:re-boot])])))

(defn layout-app []
  (let [hot-keys (use-memo (fn [] hotkeys-combos))
        ;; We don't need the results of this, just need to ensure it's called!
        _ (use-hotkeys hot-keys)
        right-drawer-open? (seq @(re-frame/subscribe [:ui/right-sidebar]))
        loading?           @(re-frame/subscribe [:app/loading?])]
    ;; catalogue-open is always set because the kiosk left drawer is permanently open
    [:div#main-wrapper.seamap.catalogue-open
     {:class (str (when right-drawer-open? " right-drawer-open") (when loading? " loading"))}
     [:div#content-wrapper
      [map-component]]

     [views/loading-display]
     [left-drawer]
     [views/right-drawer @(re-frame/subscribe [:ui/right-sidebar])]
     ;; [views/layers-search-omnibar]
     [custom-leaflet-controls]
     [reset-control]]))
