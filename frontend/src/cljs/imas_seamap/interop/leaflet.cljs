;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2021, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.interop.leaflet
  (:require [reagent.core :as r]
            ["leaflet" :as L]
            ["react-leaflet" :as ReactLeaflet]
            ["@react-leaflet/core" :as ReactLeafletCore]
            ["esri-leaflet" :as esri]
            ["leaflet-draw"]
            ["leaflet-easyprint"]
            ["/leaflet-coordinates/leaflet-coordinates"] ; Cannot use Leaflet.Coordinates module directly, because clojurescript isn't friendly with dots in module import names.
            ["react-esri-leaflet/plugins/VectorTileLayer" :as VectorTileLayer]
            ["leaflet.nontiledlayer"]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(def crs-epsg4326        L/CRS.EPSG4326)
(def crs-epsg3857        L/CRS.EPSG3857)
(def tile-layer          (r/adapt-react-class ReactLeaflet/TileLayer))
;; (def wms-layer           (r/adapt-react-class ReactLeaflet/WMSTileLayer))
(def wms-layer           (r/adapt-react-class
                          (ReactLeafletCore/createLayerComponent
                           ;; Create layer fn
                           (fn [props context]
                             (let [instance ((-> L/default .-tileLayer .-wms) (.-url props) (clj->js props))]
                               #js{:instance instance :context context}))
                           ;; Update layer fn
                           (fn [instance ^js/Object props ^js/Object prev-props]
                             ; TODO: More prop updates?
                             (when (not= (.-opacity props) (.-opacity prev-props))
                               (.setOpacity instance (.-opacity props)))
                             (when (not= (.-cql_filter props) (.-cql_filter prev-props))
                               (.setParams instance (js-obj "cql_filter" (or (.-cql_filter props) "")))))))) ; ISA-574: L.TileLayer.WMS.setParams doesn't remove values when 'undefined', requiring explicit empty string for the CQL filter case
(def geojson-layer       (r/adapt-react-class ReactLeaflet/GeoJSON))
(def vector-tile-layer   (r/adapt-react-class VectorTileLayer/default))
(def non-tiled-layer     (r/adapt-react-class
                          (ReactLeafletCore/createLayerComponent
                           ;; Create layer fn
                           (fn [props context]
                             (let [instance ((-> L/default .-nonTiledLayer .-wms) (.-url props) (clj->js props))]
                               #js{:instance instance :context context}))
                           ;; Update layer fn
                           (fn [instance ^js/Object props ^js/Object prev-props]
                             ; TODO: More prop updates?
                             (when (not= (.-opacity props) (.-opacity prev-props))
                               (.setOpacity instance (.-opacity props)))
                             (when (not= (.-cql_filter props) (.-cql_filter prev-props))
                               (.setParams instance (js-obj "cql_filter" (or (.-cql_filter props) "")))))))) ; ISA-574: L.TileLayer.WMS.setParams doesn't remove values when 'undefined', requiring explicit empty string for the CQL filter case
(defn- make-update-feature-style-fn
  "We want to apply some props (currently just opacity) on top of
  existing styles. This creates a function that can be given to
  .setStyle, handling the case where the current style is a function,
  as well as just a map. Needs the instance to access the current
  style."
  [instance js-props]
  (let [prev-style (.. instance -options -style)]
    (fn [feature]
      (js/Object.assign
       (cond-> feature (fn? prev-style) prev-style)
       js-props))))

(def feature-layer     (r/adapt-react-class
                        (ReactLeafletCore/createLayerComponent
                         ;; Create layer fn
                         (fn [props context]
                           (cljs.core/js-delete props "opacity")
                           (let [instance ((-> esri .-featureLayer) props)]
                             (.setStyle
                              instance
                              (make-update-feature-style-fn instance
                                                            #js{:opacity     (.-opacity props)
                                                                :fillOpacity (.-opacity props)}))
                             #js{:instance instance :context context}))
                         ;; Update layer fn
                         (fn [instance props prev-props]
                           (when (not= (.-opacity props) (.-opacity prev-props))
                             (.setStyle
                              instance
                              (make-update-feature-style-fn instance
                                                            #js{:opacity     (.-opacity props)
                                                                :fillOpacity (.-opacity props)})))))))

(def dynamic-map-layer
  (r/adapt-react-class
   (ReactLeafletCore/createLayerComponent
    ;; Create layer fn
    (fn [props context]
      (let [instance ((-> esri .-dynamicMapLayer) props)]
        #js{:instance instance :context context}))
    ;; Update layer fn
    (fn [instance props prev-props]
      (when (not= (.-opacity props) (.-opacity prev-props))
        (.setOpacity instance (.-opacity props)))))))

(def map-container       (r/adapt-react-class ReactLeaflet/MapContainer))
(def pane                (r/adapt-react-class ReactLeaflet/Pane))
(def marker              (r/adapt-react-class ReactLeaflet/Marker))
(def popup               (r/adapt-react-class ReactLeaflet/Popup))
(def feature-group       (r/adapt-react-class ReactLeaflet/FeatureGroup))
(def edit-control        (r/adapt-react-class (ReactLeafletCore/createControlComponent #(new (.. L/default -Control -Draw) %)))) ; horrible workaround for react-leaflet-draw not working; using leaflet-draw directly
(def circle-marker       (r/adapt-react-class ReactLeaflet/CircleMarker))
(def print-control       (r/adapt-react-class (ReactLeafletCore/createControlComponent #(.easyPrint L/default %))))
(def scale-control       (r/adapt-react-class ReactLeaflet/ScaleControl))
(def coordinates-control (r/adapt-react-class (ReactLeafletCore/createControlComponent #((-> L/default .-control .-coordinates) %))))
(def geojson-feature     L/geoJson)
(def latlng              L/LatLng)
(def esri-query          #(.query esri (clj->js %)))
(defn dynamic-map-layer-query
  [url leaflet-map point callback]
  (let [dynamic-map-layer ((-> esri .-dynamicMapLayer) (clj->js {:url url}))]
    (->
     dynamic-map-layer
     (.identify)
     (.on leaflet-map)
     (.at point)
     (.returnGeometry false)
     (.run callback))))

;;; Multiple basemaps:
(def layers-control         (r/adapt-react-class ReactLeaflet/LayersControl))
(def layers-control-basemap (r/adapt-react-class ReactLeaflet/LayersControl.BaseLayer))
