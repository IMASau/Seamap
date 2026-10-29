;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.map.views
  (:require ["leaflet" :as L]
            [re-frame.core :as re-frame]
            [imas-seamap.map.utils :refer [map->bounds]]
            [imas-seamap.map.views :as map-views]
            [imas-seamap.interop.leaflet :as leaflet]
            [imas-seamap.tmag-kiosk.events :refer [home-bounds kiosk-limits]]
            [imas-seamap.tmag-kiosk.inertia :as inertia]
            [imas-seamap.tmag-kiosk.map.limits :as limits]
            ["esri-leaflet-renderers"]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn- design-unit
  "1px of the kiosk's 1920x1080 design, in CSS px: 2 on the table (see
  `--kiosk-u` in tmag-kiosk.scss)."
  []
  (min (/ js/innerWidth 1920) (/ js/innerHeight 1080)))

(defn- media-markers
  "A marker at each of the step's media items; the one in the media card is
  highlighted, and tapping another shows it there. Markers follow the map, not
  the story card, so they arrive with the flight (see `:kiosk/media-markers`)."
  []
  (let [{:keys [media selected]} @(re-frame/subscribe [:kiosk/media-markers])
        u (design-unit)]
    (into [:<>]
          ;; The selected marker last, so it's drawn on top.
          (for [[i {:keys [lat lon]}] (sort-by (fn [[i]] (= i selected)) (map-indexed vector media))
                :when (and (number? lat) (number? lon))
                :let [selected? (= i selected)]]
            ^{:key (str i selected?)}
            [leaflet/circle-marker
             {:center        [lat lon]
              :radius        (* u (if selected? 16 11))
              :pathOptions   {:color       "#ffffff"
                              :weight      (* u 3)
                              :fillColor   (if selected? "#fbbf24" "#6d267f")
                              :fillOpacity 1}
              ;; Kept from the map, which would ask for features there.
              :eventHandlers {:click (fn [e]
                                       (.. L/default -DomEvent (stopPropagation e))
                                       (re-frame/dispatch [:kiosk.media/select i]))}}]))))

(defonce ^:private set-up (js/WeakSet.))

(defn- set-up!
  "Sets up a new map (see `inertia/attach!` and `limits/attach!`), at the home
  view with the kiosk's limits."
  [^js m]
  (when-not (.has set-up m)
    (.add set-up m)
    (inertia/attach! m)
    (limits/attach! m kiosk-limits)
    (limits/apply-to-target! {:leaflet-map m :to {:bounds home-bounds}})))

(defn map-component
  "Seamap's layers with Leaflet's default interaction and controls: no Seamap
  controls, keyboard shortcuts or feature-info popups (the kiosk's
  `:map/clicked` puts tapped features' media in the media card). The map
  starts at the kiosk's home view; after that it's moved with
  `:map/update-map-view`, as in Seamap. The zoom control is bottom-left, in
  easy reach. Zoom is continuous (`zoomSnap` 0): a pinch stays where the
  fingers leave it instead of snapping to a whole zoom level, and the zoom
  buttons step a whole level from wherever it is. Drags and pinches are held
  back past the kiosk's limits (`limits`), and a touch stops a glide
  (`inertia`)."
  []
  [leaflet/map-container
   {:id           "map"
    :crs          leaflet/crs-epsg3857
    :preferCanvas true
    :use-fly-to   false
    :bounds       (map->bounds home-bounds)
    :zoomSnap     0
    :maxBoundsViscosity limits/resistance
    :zoomControl  false
    :ref          #(when % (set-up! %) (re-frame/dispatch [:map/update-leaflet-map %]))} ; obtain a reference to the leaflet map in re-frame state, so we can call leaflet map methods from anywhere in the app
   [leaflet/zoom-control {:position "bottomleft"}]
   [map-views/basemap-layers]
   [map-views/catalogue-layers]
   [media-markers]])
