;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2021, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.

;; =============================================================================
;; WARNING: Leaflet Import Considerations
;;
;; Leaflet plugins assume a single global `L`. Importing new Leaflet modules can
;; create multiple instances, overwriting or resetting extensions added by existing
;; plugins. 
;;
;; To avoid breaking existing functionality:
;;   1. Ensure any new Leaflet plugin import does not introduce a second Leaflet
;;      instance.
;;   2. If a plugin causes conflicts, consider vendoring its code directly into the
;;      project rather than importing it from npm.
;; =============================================================================
(ns imas-seamap.interop.leaflet
  (:require [reagent.core :as r]
            ["leaflet" :as L]
            ["react-leaflet" :as ReactLeaflet]
            ["@react-leaflet/core" :as ReactLeafletCore]
            ["esri-leaflet" :as esri]
            ["leaflet-draw"]
            ["leaflet-easyprint"]
            ["leaflet-timedimension" :as LeafletTimeDimension]
            ["/leaflet-coordinates/leaflet-coordinates"] ; Cannot use Leaflet.Coordinates module directly, because clojurescript isn't friendly with dots in module import names.
            ["react-esri-leaflet/plugins/VectorTileLayer" :as VectorTileLayer]
            ["leaflet.nontiledlayer"]
            ["@alcalin/leaflet-tilelayer-wmts" :refer [wmts]]
            ["proj4leaflet" :as proj4leaflet]
            ["/SideBySide/SideBySide" :as SideBySide]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(def crs-epsg4326        L/CRS.EPSG4326)
(def crs-epsg3857        L/CRS.EPSG3857)
(def crs-epsg3031
  (proj4leaflet/CRS.
   "EPSG:3031"
   "+proj=stere +lat_0=-90 +lat_ts=-71 +lon_0=0 +k=1 +x_0=0 +y_0=0 +ellps=WGS84 +datum=WGS84 +units=m +no_defs"
   ;; resolutions are extent / tilesize / 2^zoom
   ;; where tilesize is 256; see
   ;; https://tiles.arcgis.com/tiles/tPxy1hrFDhJfZ0Mf/arcgis/rest/services/Antarctica_and_the_Southern_Ocean/MapServer
   #js{:resolutions
       #js[29917.72523676488   ; Level 0 (maps to tile matrix 3)
           14958.862618382442  ; Level 1 (maps to tile matrix 4)
           7479.4813091912213  ; Level 2 (maps to tile matrix 5)
           3739.7406545956107  ; Level 3 (maps to tile matrix 6)
           1869.8703272978053  ; Level 4 (maps to tile matrix 7)
           934.9351636489027   ; Level 5 (maps to tile matrix 8)
           467.46758182445134  ; Level 6 (maps to tile matrix 9)
           233.73379091222567  ; Level 7 (maps to tile matrix 10)
           116.86689545611284  ; Level 8 (maps to tile matrix 11)
           58.43344772805642   ; Level 9 (maps to tile matrix 12)
           29.216723864028208  ; Level 10 (maps to tile matrix 13)
           14.608361932        ; Level 11 (maps to tile matrix 14)
           7.304180966         ; Level 12 (maps to tile matrix 15)
           3.652090483         ; Level 13 (maps to tile matrix 16)
           1.8260452415        ; Level 14 (maps to tile matrix 17)
           0.91302262075       ; Level 15 (maps to tile matrix 18)
           0.456511310375      ; Level 16 (maps to tile matrix 19)
           0.228255655188      ; Level 17 (maps to tile matrix 20)
           0.114127827594      ; Level 18 (maps to tile matrix 21)
           0.057063913797      ; Level 19 (maps to tile matrix 22)
           0.0285319568985     ; Level 20 (maps to tile matrix 23)
           ]
       :origin     #js[-30635955.4472718 30635955.4472718]
       :bounds     (L/bounds #js[-4898635.244666547 -4903364.755333453] #js[4898864.755333463 4898864.755333455])}))

(def tile-layer          (r/adapt-react-class ReactLeaflet/TileLayer))
;; (def wms-layer           (r/adapt-react-class ReactLeaflet/WMSTileLayer))
(def wms-layer           (r/adapt-react-class
                          (ReactLeafletCore/createLayerComponent
                           ;; Create layer fn
                           (fn [props context]
                             (let [url (.-url props)]
                               (js-delete props "url")
                               (js-delete props "eventHandlers")
                               (let [instance ((-> L/default .-tileLayer .-wms) url props)]
                                 #js{:instance instance :context context})))
                           ;; Update layer fn
                           (fn [instance ^js/Object props ^js/Object prev-props]
                             ; TODO: More prop updates?
                             (when (not= (.-opacity props) (.-opacity prev-props))
                               (.setOpacity instance (.-opacity props)))
                             (when (not= (.-cql_filter props) (.-cql_filter prev-props))
                               (.setParams instance (js-obj "cql_filter" (or (.-cql_filter props) "")))))))) ; ISA-574: L.TileLayer.WMS.setParams doesn't remove values when 'undefined', requiring explicit empty string for the CQL filter case
(def wmts-layer          (r/adapt-react-class
                          (ReactLeafletCore/createLayerComponent
                           ;; Create layer fn
                           (fn [props context]
                             (let [url (.-url props)]
                               (js-delete props "url")
                               (js-delete props "eventHandlers")
                               (let [instance (wmts url props)]
                                 #js{:instance instance :context context})))
                           ;; Update layer fn
                           (fn [instance ^js/Object props ^js/Object prev-props]
                                                       ; TODO: More prop updates?
                             (when (not= (.-opacity props) (.-opacity prev-props))
                               (.setOpacity instance (.-opacity props)))
                             (when (not= (.-cql_filter props) (.-cql_filter prev-props))
                               (.setParams instance (js-obj "cql_filter" (or (.-cql_filter props) ""))))))))
(def geojson-layer       (r/adapt-react-class ReactLeaflet/GeoJSON))
(def vector-tile-layer   (r/adapt-react-class VectorTileLayer/default))
(def non-tiled-layer     (r/adapt-react-class
                          (ReactLeafletCore/createLayerComponent
                           ;; Create layer fn
                           (fn [props context]
                             (let [url (.-url props)]
                               (js-delete props "url")
                               (js-delete props "eventHandlers")
                               (let [instance ((-> L/default .-nonTiledLayer .-wms) url props)]
                                 #js{:instance instance :context context})))
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

(def esri-image-map-layer
  (r/adapt-react-class
   (ReactLeafletCore/createLayerComponent
    ;; Create layer fn
    (fn [props context]
      (let [url (.-url props)
            normalized-url (if (and url (not (.endsWith url "/"))) (str url "/") url)] ; Counter to all the other layers, L.esri.imageMapLayer doesn't won't work unless the URL ends in a trailing "/".
        (aset props "url" normalized-url)
        (let [instance ((-> esri .-imageMapLayer) props)]
          #js{:instance instance :context context})))
    ;; Update layer fn
    (fn [instance props prev-props]
      (when (not= (.-opacity props) (.-opacity prev-props))
        (.setOpacity instance (.-opacity props)))))))

(def wms-timeseries-layer
  (r/adapt-react-class
   (ReactLeafletCore/createLayerComponent
    ;; Create layer fn
    (fn [props context]
      (let [url (.-url props)]
        (js-delete props "url")
        (js-delete props "eventHandlers")
        (let [wms-layer ((-> L/default .-tileLayer .-wms) url props)
              instance ((-> LeafletTimeDimension/default .-timeDimension .-layer .-wms)
                        wms-layer
                        #js{:request-time-from-capabilities true
                            :update-time-dimension true
                            :wms-version "1.3.0"})]
          #js{:instance instance :context context})))
    ;; Update layer fn
    (fn [instance ^js/Object props ^js/Object prev-props]
      (when (not= (.-opacity props) (.-opacity prev-props))
        (.setOpacity instance (.-opacity props)))
      (when (not= (.-cql_filter props) (.-cql_filter prev-props))
        (.setParams instance (js-obj "cql_filter" (or (.-cql_filter props) ""))))))))

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
(def time-dimension      (r/adapt-react-class (ReactLeafletCore/createControlComponent
                                               (fn [options] ((-> LeafletTimeDimension/default .-timeDimension) options)))))
(def time-dimension-control (r/adapt-react-class (ReactLeafletCore/createControlComponent
                                                  (fn [options] ((-> LeafletTimeDimension/default .-control .-timeDimension) options)))))
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

(def side-by-side (r/adapt-react-class SideBySide/default))
