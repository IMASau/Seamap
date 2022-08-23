;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2021, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.interop.leaflet
  (:require [reagent.core :as r]
            ["leaflet" :as L]
            ["react-leaflet" :as ReactLeaflet]
            ["@react-leaflet/core" :as ReactLeafletCore]
            ["react-leaflet-custom-control" :as ReactLeafletControl]
            ;; ["react-leaflet-draw" :as ReactLeafletDraw]
            ["leaflet-easyprint"]
            ;; ["react-esri-leaflet/v2" :as ReactEsriLeaflet]
            ["/leaflet-coordinates/leaflet-coordinates"] ; Cannot use Leaflet.Coordinates module directly, because clojurescript isn't friendly with dots in module import names.
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(def crs-epsg4326  L/CRS.EPSG4326)
(def crs-epsg3857  L/CRS.EPSG3857)
(def tile-layer    (r/adapt-react-class ReactLeaflet/TileLayer))
(def wms-layer     (r/adapt-react-class ReactLeaflet/WMSTileLayer))
(def geojson-layer (r/adapt-react-class ReactLeaflet/GeoJSON))
(def leaflet-map   (r/adapt-react-class ReactLeaflet/MapContainer))
(def marker        (r/adapt-react-class ReactLeaflet/Marker))
(def popup         (r/adapt-react-class ReactLeaflet/Popup))
(def feature-group (r/adapt-react-class ReactLeaflet/FeatureGroup))
;; (def edit-control  (r/adapt-react-class ReactLeafletDraw/EditControl))
(def edit-control (fn [_props] [:div]))
(def circle-marker (r/adapt-react-class ReactLeaflet/CircleMarker))
(def print-control       (r/adapt-react-class (ReactLeafletCore/createControlComponent #(.easyPrint L %))))
(def scale-control       (r/adapt-react-class ReactLeaflet/ScaleControl))
(def custom-control      (r/adapt-react-class ReactLeafletControl/default))
(def coordinates-control (r/adapt-react-class (ReactLeafletCore/createControlComponent #(.coordinates (.-control L) %))))
(def geojson-feature L/geoJson)
(def latlng          L/LatLng)
;; (def esri-base-layer (r/adapt-react-class ReactEsriLeaflet/BasemapLayer))
;; (def esri-feature-layer (r/adapt-react-class ReactEsriLeaflet/FeatureLayer))

;;; Multiple basemaps:
(def layers-control         (r/adapt-react-class ReactLeaflet/LayersControl))
(def layers-control-basemap (r/adapt-react-class ReactLeaflet/LayersControl.BaseLayer))
