;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2021, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.interop.leaflet
  (:require [reagent.core :as r]
            ["leaflet" :as L]
            ["react-leaflet" :as ReactLeaflet]
            ["react-leaflet-control" :as ReactLeafletControl]
            ["react-leaflet-draw" :as ReactLeafletDraw]
            ;; Remove for now, should be easy to wrap for React 17 later 
            ;; ["react-leaflet-easyprint" :as ReactLeafletEasyprint]
            #_[debux.cs.core :refer [dbg] :include-macros true]))


(def crs-epsg4326  L/CRS.EPSG4326)
(def tile-layer    (r/adapt-react-class ReactLeaflet/TileLayer))
(def wms-layer     (r/adapt-react-class ReactLeaflet/WMSTileLayer))
(def geojson-layer (r/adapt-react-class ReactLeaflet/GeoJSON))
(def leaflet-map   (r/adapt-react-class ReactLeaflet/Map))
(def marker        (r/adapt-react-class ReactLeaflet/Marker))
(def popup         (r/adapt-react-class ReactLeaflet/Popup))
(def feature-group (r/adapt-react-class ReactLeaflet/FeatureGroup))
(def edit-control  (r/adapt-react-class ReactLeafletDraw/EditControl))
(def circle-marker (r/adapt-react-class ReactLeaflet/CircleMarker))
;; (def print-control (r/adapt-react-class ReactLeafletEasyprint))
(def custom-control (r/adapt-react-class ReactLeafletControl/default)) ; Might be a misinterpretation of the module ("exports.default=..."
